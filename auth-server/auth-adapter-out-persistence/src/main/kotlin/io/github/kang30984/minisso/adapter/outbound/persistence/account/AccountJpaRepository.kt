package io.github.kang30984.minisso.adapter.outbound.persistence.account

import org.springframework.data.jpa.repository.JpaRepository

interface AccountJpaRepository : JpaRepository<AccountJpaEntity, Long> {
    fun findByLoginId(loginId: String): AccountJpaEntity?

    fun existsByLoginId(loginId: String): Boolean

    fun existsByEmail(email: String): Boolean
}
