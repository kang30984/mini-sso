package io.github.kang30984.minisso.application.fake

import io.github.kang30984.minisso.application.client.port.outbound.LoadOAuthClientPort
import io.github.kang30984.minisso.application.client.port.outbound.SaveOAuthClientPort
import io.github.kang30984.minisso.domain.client.OAuthClient
import io.github.kang30984.minisso.domain.client.OAuthClientId

class InMemoryOAuthClientRepository : LoadOAuthClientPort, SaveOAuthClientPort {
    private val store = linkedMapOf<Long, OAuthClient>()

    override fun findByClientId(clientId: String): OAuthClient? = store.values.firstOrNull { it.clientId == clientId }

    override fun findById(id: OAuthClientId): OAuthClient? = store[id.value]

    override fun existsByClientId(clientId: String): Boolean = findByClientId(clientId) != null

    override fun save(client: OAuthClient): OAuthClient {
        val id = client.id ?: OAuthClientId(store.size + 1L)
        val saved = OAuthClient.reconstitute(
            id, client.clientId, client.clientName, client.clientSecretHash,
            client.redirectUris, client.postLogoutRedirectUris, client.scopes,
        )
        store[id.value] = saved
        return saved
    }
}
