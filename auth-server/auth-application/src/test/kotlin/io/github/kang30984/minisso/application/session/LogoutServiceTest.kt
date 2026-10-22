package io.github.kang30984.minisso.application.session

import io.github.kang30984.minisso.application.fake.InMemoryOAuthClientRepository
import io.github.kang30984.minisso.application.session.port.inbound.LogoutCommand
import io.github.kang30984.minisso.application.session.port.outbound.BackChannelLogoutNotice
import io.github.kang30984.minisso.application.session.port.outbound.BackChannelLogoutPort
import io.github.kang30984.minisso.application.session.port.outbound.SsoSessionPort
import io.github.kang30984.minisso.application.session.service.LogoutService
import io.github.kang30984.minisso.domain.client.OAuthClient
import io.github.kang30984.minisso.domain.client.OAuthClientId
import io.github.kang30984.minisso.domain.fixture.FakePasswordHasher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LogoutServiceTest {

    private val clients = InMemoryOAuthClientRepository()
    private val notices = mutableListOf<BackChannelLogoutNotice>()
    private val revokedSessions = mutableListOf<String>()

    @Test
    fun `세션의 토큰을 폐기하고 Back-Channel 주소가 있는 클라이언트에만 알린다`() {
        val withBackChannel = clients.save(client("app-a", "http://127.0.0.1:8081/logout/back-channel"))
        val withoutBackChannel = clients.save(client("app-b", null))
        val service = service(participants = listOf(withBackChannel.id!!, withoutBackChannel.id!!, withBackChannel.id!!))

        service.logout(LogoutCommand(sessionId = "sid-1", subject = "42", issuer = "http://localhost:9000"))

        assertThat(revokedSessions).containsExactly("sid-1")
        assertThat(notices).containsExactly(
            BackChannelLogoutNotice(
                clientId = "app-a",
                logoutUri = "http://127.0.0.1:8081/logout/back-channel",
                issuer = "http://localhost:9000",
                subject = "42",
                sessionId = "sid-1",
            ),
        )
    }

    @Test
    fun `참여한 클라이언트가 없어도 실패하지 않는다`() {
        service(participants = emptyList())
            .logout(LogoutCommand(sessionId = "sid-2", subject = "42", issuer = "http://localhost:9000"))

        assertThat(notices).isEmpty()
    }

    private fun service(participants: List<OAuthClientId>) = LogoutService(
        ssoSessionPort = object : SsoSessionPort {
            override fun revokeBySessionId(sessionId: String): List<OAuthClientId> {
                revokedSessions += sessionId
                return participants
            }
        },
        loadOAuthClientPort = clients,
        backChannelLogoutPort = object : BackChannelLogoutPort {
            override fun notify(notice: BackChannelLogoutNotice) {
                notices += notice
            }
        },
    )

    private fun client(clientId: String, backchannelLogoutUri: String?) = OAuthClient.register(
        clientId = clientId,
        clientName = clientId,
        rawSecret = "local-secret-1234567890",
        redirectUris = setOf("http://127.0.0.1:8081/login/oauth2/code/mini-sso"),
        postLogoutRedirectUris = emptySet(),
        scopes = setOf("openid"),
        hasher = FakePasswordHasher(),
        backchannelLogoutUri = backchannelLogoutUri,
    )
}
