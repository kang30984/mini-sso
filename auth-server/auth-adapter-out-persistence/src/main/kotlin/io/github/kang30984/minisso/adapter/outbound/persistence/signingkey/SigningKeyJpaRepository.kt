package io.github.kang30984.minisso.adapter.outbound.persistence.signingkey

import org.springframework.data.jpa.repository.JpaRepository

interface SigningKeyJpaRepository : JpaRepository<SigningKeyJpaEntity, Long>
