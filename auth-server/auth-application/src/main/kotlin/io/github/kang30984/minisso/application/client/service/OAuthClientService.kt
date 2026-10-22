package io.github.kang30984.minisso.application.client.service

import io.github.kang30984.minisso.application.client.port.inbound.GetOAuthClientQuery
import io.github.kang30984.minisso.application.client.port.inbound.OAuthClientView
import io.github.kang30984.minisso.application.client.port.inbound.RegisterOAuthClientCommand
import io.github.kang30984.minisso.application.client.port.inbound.RegisterOAuthClientUseCase
import io.github.kang30984.minisso.application.client.port.outbound.LoadOAuthClientPort
import io.github.kang30984.minisso.application.client.port.outbound.SaveOAuthClientPort
import io.github.kang30984.minisso.domain.account.PasswordHasher
import io.github.kang30984.minisso.domain.client.OAuthClient
import io.github.kang30984.minisso.domain.client.OAuthClientId
import org.springframework.transaction.annotation.Transactional

@Transactional
class OAuthClientService(
    private val loadOAuthClientPort: LoadOAuthClientPort,
    private val saveOAuthClientPort: SaveOAuthClientPort,
    private val passwordHasher: PasswordHasher,
) : RegisterOAuthClientUseCase, GetOAuthClientQuery {

    override fun registerIfAbsent(command: RegisterOAuthClientCommand): Boolean {
        if (loadOAuthClientPort.existsByClientId(command.clientId)) return false

        val client = OAuthClient.register(
            clientId = command.clientId,
            clientName = command.clientName,
            rawSecret = command.clientSecret,
            redirectUris = command.redirectUris,
            postLogoutRedirectUris = command.postLogoutRedirectUris,
            scopes = command.scopes,
            hasher = passwordHasher,
            backchannelLogoutUri = command.backchannelLogoutUri,
        )
        saveOAuthClientPort.save(client)
        return true
    }

    @Transactional(readOnly = true)
    override fun findByClientId(clientId: String): OAuthClientView? =
        loadOAuthClientPort.findByClientId(clientId)?.toView()

    @Transactional(readOnly = true)
    override fun findById(id: Long): OAuthClientView? =
        loadOAuthClientPort.findById(OAuthClientId(id))?.toView()

    private fun OAuthClient.toView() = OAuthClientView(
        id = checkNotNull(id).value,
        clientId = clientId,
        clientName = clientName,
        clientSecretHash = clientSecretHash.value,
        redirectUris = redirectUris,
        postLogoutRedirectUris = postLogoutRedirectUris,
        scopes = scopes,
    )
}
