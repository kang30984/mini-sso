package io.github.kang30984.minisso.adapter.outbound.redis

import io.github.kang30984.minisso.application.session.port.outbound.SsoSessionPort
import io.github.kang30984.minisso.domain.client.OAuthClientId
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.security.oauth2.core.AbstractOAuth2Token
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * 인가 상태(인가 코드, 액세스/리프레시/ID 토큰)를 Redis에 저장한다 (ADR-004).
 *
 * 키 구조
 * - minisso:authz:{id}                      인가 본문 (TTL = 가장 늦게 만료되는 토큰)
 * - minisso:authz:idx:{type}:{sha256(token)} 토큰 -> 인가 ID (TTL = 해당 토큰 만료)
 * - minisso:authz:rotated:{sha256(token)}   회전되어 더 이상 쓰면 안 되는 리프레시 토큰 -> 인가 ID (ADR-007)
 * - minisso:sso:{sid}                       SSO 세션으로 발급된 인가 ID 집합 (로그아웃 시 일괄 폐기)
 *
 * 토큰 원문은 키에 넣지 않고 SHA-256 해시를 쓴다. Redis 키 목록이 노출되어도 토큰을 재구성할 수 없다.
 */
@Component
class RedisOAuth2AuthorizationService(
    connectionFactory: RedisConnectionFactory,
    private val clock: Clock,
) : OAuth2AuthorizationService, SsoSessionPort {

    private val log = LoggerFactory.getLogger(javaClass)

    // OAuth2Authorization과 내부 토큰, 인증 주체가 모두 Serializable이므로 JDK 직렬화를 쓴다 (ADR-004)
    private val redis: RedisTemplate<String, Any> = RedisTemplate<String, Any>().apply {
        setConnectionFactory(connectionFactory)
        keySerializer = StringRedisSerializer()
        valueSerializer = JdkSerializationRedisSerializer()
        afterPropertiesSet()
    }

    override fun save(authorization: OAuth2Authorization) {
        val now = clock.instant()
        findById(authorization.id)?.let { previous ->
            deleteIndexes(previous)
            rememberRotatedRefreshToken(previous, authorization, now)
        }

        val ttl = timeToLive(authorization, now)
        redis.opsForValue().set(authorizationKey(authorization.id), authorization, ttl)
        indexedTokens(authorization).forEach { indexed ->
            redis.opsForValue().set(indexKey(indexed.type, indexed.value), authorization.id, until(indexed.expiresAt, now))
        }
        sessionIdOf(authorization)?.let { sid ->
            redis.opsForSet().add(sessionKey(sid), authorization.id)
            redis.expire(sessionKey(sid), ttl)
        }
    }

    override fun remove(authorization: OAuth2Authorization) {
        val stored = findById(authorization.id) ?: authorization
        deleteIndexes(stored)
        redis.delete(authorizationKey(stored.id))
        sessionIdOf(stored)?.let { sid -> redis.opsForSet().remove(sessionKey(sid), stored.id) }
    }

    override fun findById(id: String): OAuth2Authorization? =
        redis.opsForValue().get(authorizationKey(id)) as? OAuth2Authorization

    override fun findByToken(token: String, tokenType: OAuth2TokenType?): OAuth2Authorization? {
        val types = tokenType?.let { listOf(it.value) } ?: SEARCHABLE_TOKEN_TYPES
        for (type in types) {
            val id = redis.opsForValue().get(indexKey(type, token)) as? String ?: continue
            return findById(id)
        }
        if (tokenType == null || tokenType.value == OAuth2TokenType.REFRESH_TOKEN.value) {
            revokeIfRotatedRefreshTokenReused(token)
        }
        return null
    }

    override fun revokeBySessionId(sessionId: String): List<OAuthClientId> {
        val key = sessionKey(sessionId)
        val authorizations = redis.opsForSet().members(key).orEmpty()
            .mapNotNull { findById(it as String) }
        authorizations.forEach { remove(it) }
        redis.delete(key)
        return authorizations.mapNotNull { it.registeredClientId.toLongOrNull()?.let(::OAuthClientId) }
    }

    /**
     * 이미 회전된 리프레시 토큰이 다시 들어오면, 정상 클라이언트와 공격자 중 누군가가 탈취한 토큰을 쓰고 있다는 뜻이다.
     * 누가 정상인지 구분할 수 없으므로 해당 인가(같은 로그인에서 이어진 토큰 계보) 전체를 폐기한다 (ADR-007).
     */
    private fun revokeIfRotatedRefreshTokenReused(token: String) {
        val id = redis.opsForValue().get(rotatedKey(token)) as? String ?: return
        val authorization = findById(id) ?: return
        log.warn(
            "[SECURITY] 회전된 리프레시 토큰 재사용 탐지. 인가 전체를 폐기합니다. authorizationId={}, principal={}, clientId={}",
            authorization.id,
            authorization.principalName,
            authorization.registeredClientId,
        )
        remove(authorization)
    }

    private fun rememberRotatedRefreshToken(previous: OAuth2Authorization, current: OAuth2Authorization, now: Instant) {
        val previousToken = previous.refreshToken?.token ?: return
        if (current.refreshToken?.token?.tokenValue == previousToken.tokenValue) return
        redis.opsForValue().set(rotatedKey(previousToken.tokenValue), current.id, until(previousToken.expiresAt, now))
    }

    private fun deleteIndexes(authorization: OAuth2Authorization) {
        val keys = indexedTokens(authorization).map { indexKey(it.type, it.value) }
        if (keys.isNotEmpty()) redis.delete(keys)
    }

    private fun indexedTokens(authorization: OAuth2Authorization): List<IndexedToken> = buildList {
        authorization.getAttribute<String>(OAuth2ParameterNames.STATE)?.let {
            add(IndexedToken(OAuth2ParameterNames.STATE, it, null))
        }
        authorization.getToken(OAuth2AuthorizationCode::class.java)?.token?.let { add(indexed(OAuth2ParameterNames.CODE, it)) }
        authorization.accessToken?.token?.let { add(indexed(OAuth2TokenType.ACCESS_TOKEN.value, it)) }
        authorization.refreshToken?.token?.let { add(indexed(OAuth2TokenType.REFRESH_TOKEN.value, it)) }
        authorization.getToken(OidcIdToken::class.java)?.token?.let { add(indexed(OidcParameterNames.ID_TOKEN, it)) }
    }

    private fun indexed(type: String, token: AbstractOAuth2Token) = IndexedToken(type, token.tokenValue, token.expiresAt)

    private fun sessionIdOf(authorization: OAuth2Authorization): String? =
        authorization.getToken(OidcIdToken::class.java)?.token?.getClaimAsString(SID_CLAIM)

    private fun timeToLive(authorization: OAuth2Authorization, now: Instant): Duration =
        indexedTokens(authorization).mapNotNull { it.expiresAt }.maxOrNull()?.let { until(it, now) } ?: DEFAULT_TTL

    private fun until(expiresAt: Instant?, now: Instant): Duration {
        if (expiresAt == null) return DEFAULT_TTL
        val remaining = Duration.between(now, expiresAt)
        return if (remaining < MIN_TTL) MIN_TTL else remaining
    }

    private data class IndexedToken(val type: String, val value: String, val expiresAt: Instant?)

    companion object {
        private const val PREFIX = "minisso:"
        private const val SID_CLAIM = "sid"

        /** 만료 시각이 없는 상태값(state 등)의 보관 시간 */
        private val DEFAULT_TTL: Duration = Duration.ofMinutes(10)
        private val MIN_TTL: Duration = Duration.ofSeconds(1)

        private val SEARCHABLE_TOKEN_TYPES = listOf(
            OAuth2TokenType.ACCESS_TOKEN.value,
            OAuth2TokenType.REFRESH_TOKEN.value,
            OAuth2ParameterNames.CODE,
            OidcParameterNames.ID_TOKEN,
            OAuth2ParameterNames.STATE,
        )

        private fun authorizationKey(id: String) = "${PREFIX}authz:$id"

        private fun indexKey(type: String, token: String) = "${PREFIX}authz:idx:$type:${sha256(token)}"

        private fun rotatedKey(token: String) = "${PREFIX}authz:rotated:${sha256(token)}"

        private fun sessionKey(sid: String) = "${PREFIX}sso:$sid"

        private fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
