package io.github.kang30984.minisso.adapter.outbound.persistence.account

import io.github.kang30984.minisso.domain.account.AccountRole
import io.github.kang30984.minisso.domain.account.AccountStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.OptimisticLock
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import java.time.Instant

/**
 * 영속성 전용 모델. 도메인 Account와 분리해 JPA 제약(기본 생성자, open 클래스, 가변 필드)이 도메인으로 번지지 않게 한다.
 */
@Entity
@Table(name = "account")
class AccountJpaEntity(
    @Column(nullable = false, unique = true, length = 30)
    var loginId: String,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    var role: AccountRole,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(nullable = false, length = 100)
    var department: String,

    @Column(nullable = false)
    var passwordHash: String,

    // Hibernate 6은 MySQL에서 enum을 ENUM 타입으로 매핑하므로, 값 추가 시 DDL 변경이 없도록 VARCHAR로 고정
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    var status: AccountStatus,

    @Column(nullable = false)
    var failedLoginCount: Int,

    var lockedUntil: Instant?,

    @Column(nullable = false)
    var passwordChangedAt: Instant,

    /**
     * 로그인 성공 시마다 바뀌는 값이라 낙관적 락 대상에서 제외한다 (ADR-011).
     * 같은 계정으로 동시에 로그인해도 서로 충돌하지 않고, 마지막에 커밋한 시각이 남는다.
     */
    @OptimisticLock(excluded = true)
    var lastLoginAt: Instant?,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Version
    var version: Long = 0

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null

    @UpdateTimestamp
    @OptimisticLock(excluded = true)
    @Column(nullable = false)
    var updatedAt: Instant? = null
}
