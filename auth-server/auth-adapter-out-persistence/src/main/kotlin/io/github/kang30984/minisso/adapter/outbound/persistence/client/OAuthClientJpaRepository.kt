package io.github.kang30984.minisso.adapter.outbound.persistence.client

import org.springframework.data.jpa.repository.JpaRepository

interface OAuthClientJpaRepository : JpaRepository<OAuthClientJpaEntity, Long> {
    fun findByClientId(clientId: String): OAuthClientJpaEntity?

    fun existsByClientId(clientId: String): Boolean
}
