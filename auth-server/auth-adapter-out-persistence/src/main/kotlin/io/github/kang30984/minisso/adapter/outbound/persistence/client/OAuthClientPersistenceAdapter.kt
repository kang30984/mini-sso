package io.github.kang30984.minisso.adapter.outbound.persistence.client

import io.github.kang30984.minisso.application.client.port.outbound.LoadOAuthClientPort
import io.github.kang30984.minisso.application.client.port.outbound.SaveOAuthClientPort
import io.github.kang30984.minisso.domain.account.PasswordHash
import io.github.kang30984.minisso.domain.client.OAuthClient
import io.github.kang30984.minisso.domain.client.OAuthClientId
import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class OAuthClientPersistenceAdapter(
    private val repository: OAuthClientJpaRepository,
) : LoadOAuthClientPort, SaveOAuthClientPort {

    override fun findByClientId(clientId: String): OAuthClient? = repository.findByClientId(clientId)?.toDomain()

    override fun findById(id: OAuthClientId): OAuthClient? =
        repository.findById(id.value).map { it.toDomain() }.orElse(null)

    override fun existsByClientId(clientId: String): Boolean = repository.existsByClientId(clientId)

    override fun save(client: OAuthClient): OAuthClient {
        check(client.id == null) { "클라이언트 수정은 아직 지원하지 않습니다." }
        return try {
            repository.saveAndFlush(
                OAuthClientJpaEntity(
                    clientId = client.clientId,
                    clientName = client.clientName,
                    clientSecretHash = client.clientSecretHash.value,
                    redirectUris = client.redirectUris,
                    postLogoutRedirectUris = client.postLogoutRedirectUris,
                    scopes = client.scopes,
                ),
            ).toDomain()
        } catch (e: DataIntegrityViolationException) {
            throw BusinessException(ErrorCode.DUPLICATE_CLIENT_ID)
        }
    }

    private fun OAuthClientJpaEntity.toDomain() = OAuthClient.reconstitute(
        id = OAuthClientId(checkNotNull(id)),
        clientId = clientId,
        clientName = clientName,
        clientSecretHash = PasswordHash(clientSecretHash),
        redirectUris = redirectUris,
        postLogoutRedirectUris = postLogoutRedirectUris,
        scopes = scopes,
    )
}
