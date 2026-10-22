package io.github.kang30984.minisso.adapter.outbound.persistence.history

import org.springframework.data.jpa.repository.JpaRepository

interface LoginHistoryJpaRepository : JpaRepository<LoginHistoryJpaEntity, Long>
