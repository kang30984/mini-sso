package io.github.kang30984.minisso.application.client.port.outbound

import io.github.kang30984.minisso.domain.client.OAuthClient
import io.github.kang30984.minisso.domain.client.OAuthClientId

interface LoadOAuthClientPort {
    fun findByClientId(clientId: String): OAuthClient?

    fun findById(id: OAuthClientId): OAuthClient?

    fun existsByClientId(clientId: String): Boolean
}
