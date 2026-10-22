package io.github.kang30984.minisso.application.client

import io.github.kang30984.minisso.application.client.port.inbound.RegisterOAuthClientCommand
import io.github.kang30984.minisso.application.client.service.OAuthClientService
import io.github.kang30984.minisso.application.fake.InMemoryOAuthClientRepository
import io.github.kang30984.minisso.domain.fixture.FakePasswordHasher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OAuthClientServiceTest {

    private val repository = InMemoryOAuthClientRepository()
    private val service = OAuthClientService(repository, repository, FakePasswordHasher())

    @Test
    fun `같은 clientId는 한 번만 등록되고 조회 결과에 원문 시크릿은 없다`() {
        assertThat(service.registerIfAbsent(command())).isTrue()
        assertThat(service.registerIfAbsent(command())).isFalse()

        val view = checkNotNull(service.findByClientId("sample-app-a"))
        assertThat(view.clientSecretHash).isNotEqualTo(SECRET)
        assertThat(view.toString()).doesNotContain(view.clientSecretHash)
        assertThat(command().toString()).doesNotContain(SECRET)
    }

    private fun command() = RegisterOAuthClientCommand(
        clientId = "sample-app-a",
        clientName = "Sample App A",
        clientSecret = SECRET,
        redirectUris = setOf("http://127.0.0.1:8081/login/oauth2/code/mini-sso"),
        postLogoutRedirectUris = emptySet(),
        scopes = setOf("openid", "profile"),
    )

    companion object {
        private const val SECRET = "sample-app-a-local-secret"
    }
}
