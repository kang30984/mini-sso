package io.github.kang30984.minisso.adapter.outbound.persistence.client

import io.github.kang30984.minisso.adapter.outbound.persistence.common.SpaceDelimitedSetConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "oauth_client")
class OAuthClientJpaEntity(
    @Column(nullable = false, unique = true, length = 50)
    var clientId: String,

    @Column(nullable = false, length = 100)
    var clientName: String,

    @Column(nullable = false)
    var clientSecretHash: String,

    @Convert(converter = SpaceDelimitedSetConverter::class)
    @Column(nullable = false, length = 2000)
    var redirectUris: Set<String>,

    @Convert(converter = SpaceDelimitedSetConverter::class)
    @Column(nullable = false, length = 2000)
    var postLogoutRedirectUris: Set<String>,

    @Convert(converter = SpaceDelimitedSetConverter::class)
    @Column(nullable = false, length = 500)
    var scopes: Set<String>,

    @Column(length = 500)
    var backchannelLogoutUri: String?,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: Instant? = null
}
