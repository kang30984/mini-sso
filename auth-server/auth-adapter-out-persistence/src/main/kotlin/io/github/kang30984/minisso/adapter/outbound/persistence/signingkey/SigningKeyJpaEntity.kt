package io.github.kang30984.minisso.adapter.outbound.persistence.signingkey

import io.github.kang30984.minisso.domain.signingkey.SigningKeyStatus
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

@Entity
@Table(name = "signing_key")
class SigningKeyJpaEntity(
    @Column(nullable = false, unique = true, length = 100)
    var kid: String,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    var status: SigningKeyStatus,

    @Column(nullable = false, length = 4000)
    var publicJwk: String,

    /** AES-256-GCM으로 암호화한 개인키 JWK (Base64). 평문 개인키는 DB에 저장하지 않는다. */
    @Column(nullable = false, length = 4000)
    var encryptedPrivateJwk: String,

    @Column(nullable = false)
    var activatedAt: Instant,

    var rotatedAt: Instant?,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
