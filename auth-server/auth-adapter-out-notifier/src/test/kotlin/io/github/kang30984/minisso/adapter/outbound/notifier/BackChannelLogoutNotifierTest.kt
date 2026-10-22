package io.github.kang30984.minisso.adapter.outbound.notifier

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import io.github.kang30984.minisso.application.session.port.outbound.BackChannelLogoutNotice
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URLDecoder
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Clock
import java.time.Duration

class BackChannelLogoutNotifierTest {

    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val encoder = NimbusJwtEncoder(
        ImmutableJWKSet(
            JWKSet(RSAKey.Builder(keyPair.public as RSAPublicKey).privateKey(keyPair.private as RSAPrivateKey).keyID("test").build()),
        ),
    )
    private val decoder = NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()

    private val restClientBuilder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val notifier = BackChannelLogoutNotifier(encoder, restClientBuilder.build(), Clock.systemUTC(), Duration.ZERO)

    private val notice = BackChannelLogoutNotice(
        clientId = "sample-app-b",
        logoutUri = "http://127.0.0.1:8082/logout/connect/back-channel/mini-sso",
        issuer = "http://localhost:9000",
        subject = "42",
        sessionId = "sid-1",
    )

    @Test
    fun `Logout Token은 규격의 필수 클레임을 담고 인가 서버 키로 서명된다`() {
        // 기본 설정의 디코더로 검증된다 = 클라이언트가 추가 설정 없이 받을 수 있다
        val jwt = decoder.decode(notifier.createLogoutToken(notice))

        assertThat(jwt.issuer.toString()).isEqualTo("http://localhost:9000")
        assertThat(jwt.audience).containsExactly("sample-app-b")
        assertThat(jwt.subject).isEqualTo("42")
        assertThat(jwt.getClaimAsString("sid")).isEqualTo("sid-1")
        assertThat(jwt.id).isNotBlank()
        assertThat(jwt.issuedAt).isNotNull()
        assertThat(jwt.getClaimAsMap("events")).containsKey(BackChannelLogoutNotifier.BACK_CHANNEL_LOGOUT_EVENT)
        assertThat(jwt.claims).doesNotContainKey("nonce")
    }

    @Test
    fun `logout_token을 form으로 POST한다`() {
        var body = ""
        server.expect(requestTo(notice.logoutUri))
            .andExpect(method(HttpMethod.POST))
            .andExpect { body = (it as MockClientHttpRequest).bodyAsString }
            .andRespond(withSuccess())

        assertThat(notifier.deliver(notice)).isTrue()

        server.verify()
        assertThat(body).startsWith("logout_token=")
        val token = URLDecoder.decode(body.removePrefix("logout_token="), Charsets.UTF_8)
        assertThat(decoder.decode(token).getClaimAsString("sid")).isEqualTo("sid-1")
    }

    @Test
    fun `일시적 오류는 재시도하고 성공하면 멈춘다`() {
        server.expect(ExpectedCount.times(2), requestTo(notice.logoutUri)).andRespond(withServerError())
        server.expect(requestTo(notice.logoutUri)).andRespond(withSuccess())

        assertThat(notifier.deliver(notice)).isTrue()
        server.verify()
    }

    @Test
    fun `3회 모두 실패하면 false를 돌려주고 예외를 던지지 않는다`() {
        server.expect(ExpectedCount.times(3), requestTo(notice.logoutUri)).andRespond(withServerError())

        assertThat(notifier.deliver(notice)).isFalse()
        server.verify()
    }
}
