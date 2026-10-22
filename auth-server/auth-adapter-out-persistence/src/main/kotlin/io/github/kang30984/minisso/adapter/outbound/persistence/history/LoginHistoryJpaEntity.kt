package io.github.kang30984.minisso.adapter.outbound.persistence.history

import io.github.kang30984.minisso.domain.history.LoginFailureReason
import io.github.kang30984.minisso.domain.history.LoginOutcome
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

/** 쓰기 위주의 append-only 테이블. 계정과 FK를 두지 않는다 (ADR-009). */
@Entity
@Table(name = "login_history")
class LoginHistoryJpaEntity(
    var accountId: Long?,

    @Column(nullable = false, length = 100)
    var loginId: String,

    @Column(length = 100)
    var clientId: String?,

    @Column(nullable = false, length = 45)
    var ip: String,

    @Column(length = 512)
    var userAgent: String?,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    var outcome: LoginOutcome,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    var failureReason: LoginFailureReason?,

    @Column(nullable = false)
    var occurredAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
