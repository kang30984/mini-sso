package io.github.kang30984.minisso.adapter.inbound.oauth2.key

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.github.kang30984.minisso.application.signingkey.port.inbound.GetSigningKeysQuery
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * DB에 저장된 서명 키를 인가 서버에 공급한다 (ADR-008).
 *
 * 두 가지 키 집합을 분리해서 제공한다.
 * - signingKeys: 활성 키 1개(개인키 포함). 토큰 서명용. 하나만 있어야 서명기가 키를 모호함 없이 고른다.
 * - publishedKeys: 활성 키 + 보존 기간 내의 교체된 키(공개키만). JWKS 엔드포인트와 토큰 검증용.
 *
 * 토큰 발급마다 DB를 읽지 않도록 짧게 캐시한다. 다른 인스턴스에서 키를 교체하면 최대 cacheTtl 뒤에 반영된다.
 */
class SigningKeyJwkSource(
    private val getSigningKeysQuery: GetSigningKeysQuery,
    private val clock: Clock,
    private val cacheTtl: Duration,
) {
    @Volatile
    private var snapshot: Snapshot? = null

    val signingKeys: JWKSource<SecurityContext> = JWKSource { selector, _ -> selector.select(JWKSet(current().signing)) }

    val publishedKeys: JWKSource<SecurityContext> = JWKSource { selector, _ -> selector.select(JWKSet(current().published)) }

    private fun current(): Snapshot {
        val now = clock.instant()
        snapshot?.takeIf { it.isFresh(now) }?.let { return it }
        synchronized(this) {
            snapshot?.takeIf { it.isFresh(now) }?.let { return it }
            return Snapshot(
                signing = JWK.parse(getSigningKeysQuery.signingJwk()),
                published = getSigningKeysQuery.publishedPublicJwks().map(JWK::parse),
                loadedAt = now,
            ).also { snapshot = it }
        }
    }

    private inner class Snapshot(val signing: JWK, val published: List<JWK>, val loadedAt: Instant) {
        fun isFresh(now: Instant): Boolean = Duration.between(loadedAt, now) < cacheTtl
    }
}
