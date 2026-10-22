package io.github.kang30984.minisso.adapter.outbound.notifier

import io.github.kang30984.minisso.application.session.port.outbound.BackChannelLogoutNotice
import io.github.kang30984.minisso.application.session.port.outbound.BackChannelLogoutPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.Clock
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * OIDC Back-Channel Logout 1.0 구현.
 * 브라우저를 거치지 않고 인증 서버가 각 서비스에 직접 Logout Token(서명된 JWT)을 POST한다.
 *
 * - 로그아웃 응답이 느린 서비스 때문에 지연되지 않도록 가상 스레드에서 비동기로 보낸다
 * - 일시적 장애에 대비해 최대 3회까지 재시도한다
 * - 모든 시도가 실패해도 토큰은 이미 폐기된 상태라, 해당 서비스는 다음 토큰 갱신에서 로그아웃된다
 */
class BackChannelLogoutNotifier(
    private val jwtEncoder: JwtEncoder,
    private val restClient: RestClient,
    private val clock: Clock,
    private val retryBackoff: Duration = Duration.ofMillis(500),
) : BackChannelLogoutPort, DisposableBean {

    private val log = LoggerFactory.getLogger(javaClass)
    private val executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()

    override fun notify(notice: BackChannelLogoutNotice) {
        executor.execute { deliver(notice) }
    }

    /** @return 전송에 성공했으면 true */
    internal fun deliver(notice: BackChannelLogoutNotice): Boolean {
        val logoutToken = createLogoutToken(notice)
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                restClient.post()
                    .uri(notice.logoutUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(LinkedMultiValueMap<String, String>().apply { add(LOGOUT_TOKEN_PARAM, logoutToken) })
                    .retrieve()
                    .toBodilessEntity()
                return true
            } catch (e: RestClientException) {
                log.warn("Back-Channel Logout 전송 실패 ({}/{}). clientId={}, cause={}", attempt + 1, MAX_ATTEMPTS, notice.clientId, e.message)
                if (attempt < MAX_ATTEMPTS - 1) Thread.sleep(retryBackoff.multipliedBy(attempt + 1L).toMillis())
            }
        }
        log.error("Back-Channel Logout 최종 실패. clientId={}, sid={}", notice.clientId, notice.sessionId)
        return false
    }

    /**
     * Logout Token 클레임 (OIDC Back-Channel Logout 1.0, 2.4절)
     * iss, aud, iat, jti, events는 필수이고, sub와 sid 중 하나 이상이 있어야 하며, nonce는 금지된다.
     *
     * 규격은 typ 헤더에 "logout+jwt"를 권장(SHOULD)하지만 넣지 않는다.
     * Nimbus 기반 디코더(Spring Security 포함)는 기본 설정에서 typ이 "JWT"이거나 없을 때만 허용하므로,
     * 넣으면 클라이언트가 별도 설정 없이는 토큰을 거부한다. 대신 events 클레임으로 Logout Token임을 구분한다.
     */
    internal fun createLogoutToken(notice: BackChannelLogoutNotice): String {
        val now = clock.instant()
        val header = JwsHeader.with(SignatureAlgorithm.RS256).build()
        val claims = JwtClaimsSet.builder()
            .issuer(notice.issuer)
            .audience(listOf(notice.clientId))
            .issuedAt(now)
            .expiresAt(now.plus(TOKEN_LIFETIME))
            .id(UUID.randomUUID().toString())
            .subject(notice.subject)
            .claim("sid", notice.sessionId)
            .claim("events", mapOf(BACK_CHANNEL_LOGOUT_EVENT to emptyMap<String, Any>()))
            .build()
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }

    override fun destroy() {
        executor.shutdown()
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val LOGOUT_TOKEN_PARAM = "logout_token"
        const val BACK_CHANNEL_LOGOUT_EVENT = "http://schemas.openid.net/event/backchannel-logout"
        private val TOKEN_LIFETIME: Duration = Duration.ofMinutes(2)
    }
}
