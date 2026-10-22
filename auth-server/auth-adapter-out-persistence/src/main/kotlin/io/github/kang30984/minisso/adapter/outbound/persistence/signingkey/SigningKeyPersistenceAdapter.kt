package io.github.kang30984.minisso.adapter.outbound.persistence.signingkey

import io.github.kang30984.minisso.application.signingkey.port.outbound.SigningKeyPort
import io.github.kang30984.minisso.domain.signingkey.SigningKey
import io.github.kang30984.minisso.domain.signingkey.SigningKeyId
import org.springframework.stereotype.Component

@Component
class SigningKeyPersistenceAdapter(
    private val repository: SigningKeyJpaRepository,
) : SigningKeyPort {

    override fun findAll(): List<SigningKey> = repository.findAll().map { it.toDomain() }

    override fun save(key: SigningKey): SigningKey {
        val id = key.id ?: return repository.saveAndFlush(
            SigningKeyJpaEntity(
                kid = key.kid,
                status = key.status,
                publicJwk = key.publicJwk,
                encryptedPrivateJwk = key.encryptedPrivateJwk,
                activatedAt = key.activatedAt,
                rotatedAt = key.rotatedAt,
            ),
        ).toDomain()

        val entity = repository.findById(id.value).orElseThrow { IllegalStateException("서명 키가 없습니다: ${key.kid}") }
        entity.status = key.status
        entity.rotatedAt = key.rotatedAt
        return entity.toDomain()
    }

    private fun SigningKeyJpaEntity.toDomain() = SigningKey.reconstitute(
        id = SigningKeyId(checkNotNull(id)),
        kid = kid,
        status = status,
        publicJwk = publicJwk,
        encryptedPrivateJwk = encryptedPrivateJwk,
        activatedAt = activatedAt,
        rotatedAt = rotatedAt,
    )
}
