package io.github.kang30984.minisso.domain.client

import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import io.github.kang30984.minisso.domain.fixture.FakePasswordHasher
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class OAuthClientTest {

    private val hasher = FakePasswordHasher()

    @Test
    fun `등록하면 시크릿은 해시로 저장된다`() {
        val client = register()

        assertThat(client.clientSecretHash.value).isNotEqualTo(SECRET)
        assertThat(hasher.matches(SECRET, client.clientSecretHash)).isTrue()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://app.example.com/login/oauth2/code/mini-sso",
            "http://localhost:8081/callback",
            "http://127.0.0.1:8081/callback",
            "http://app-a.localhost:8081/callback",
        ],
    )
    fun `허용되는 리다이렉트 URI`(uri: String) {
        assertThat(RedirectUriPolicy.isAllowed(uri)).isTrue()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/relative/callback",                          // 상대 경로
            "http://app.example.com/callback",             // 운영 주소의 http
            "https://*.example.com/callback",              // 와일드카드
            "https://app.example.com/callback#token",      // fragment
            "https://user:pass@app.example.com/callback",  // 사용자 정보
            "javascript:alert(1)",                         // 위험한 스킴
            "http://localhost.evil.com/callback",          // localhost로 위장한 외부 도메인
        ],
    )
    fun `허용되지 않는 리다이렉트 URI`(uri: String) {
        assertThat(RedirectUriPolicy.isAllowed(uri)).isFalse()
    }

    @Test
    fun `scope에 openid가 없으면 등록할 수 없다`() {
        assertThatThrownBy { register(scopes = setOf("profile")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SCOPE)
    }

    @Test
    fun `지원하지 않는 scope는 등록할 수 없다`() {
        assertThatThrownBy { register(scopes = setOf("openid", "admin")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SCOPE)
    }

    @Test
    fun `짧은 시크릿은 등록할 수 없다`() {
        assertThatThrownBy { register(secret = "short") }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CLIENT_SECRET)
    }

    private fun register(
        secret: String = SECRET,
        scopes: Set<String> = setOf("openid", "profile", "email"),
    ) = OAuthClient.register(
        clientId = "sample-app-a",
        clientName = "Sample App A",
        rawSecret = secret,
        redirectUris = setOf("http://127.0.0.1:8081/login/oauth2/code/mini-sso"),
        postLogoutRedirectUris = setOf("http://127.0.0.1:8081/"),
        scopes = scopes,
        hasher = hasher,
    )

    companion object {
        private const val SECRET = "sample-app-a-local-secret"
    }
}
