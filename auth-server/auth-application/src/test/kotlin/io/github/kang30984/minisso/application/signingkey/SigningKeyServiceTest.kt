package io.github.kang30984.minisso.application.signingkey

import io.github.kang30984.minisso.application.fake.MutableClock
import io.github.kang30984.minisso.application.signingkey.port.outbound.GeneratedSigningKey
import io.github.kang30984.minisso.application.signingkey.port.outbound.SigningKeyCryptoPort
import io.github.kang30984.minisso.application.signingkey.port.outbound.SigningKeyPort
import io.github.kang30984.minisso.application.signingkey.service.SigningKeyService
import io.github.kang30984.minisso.domain.signingkey.SigningKey
import io.github.kang30984.minisso.domain.signingkey.SigningKeyId
import io.github.kang30984.minisso.domain.signingkey.SigningKeyPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class SigningKeyServiceTest {

    private val clock = MutableClock(Instant.parse("2026-10-01T00:00:00Z"))
    private val repository = InMemorySigningKeyRepository()
    private val service = SigningKeyService(repository, FakeCrypto(), SigningKeyPolicy(Duration.ofHours(1)), clock)

    @Test
    fun `활성 키가 없을 때만 새로 만든다`() {
        assertThat(service.ensureActiveKey()).isTrue()
        assertThat(service.ensureActiveKey()).isFalse()
        assertThat(repository.findAll()).hasSize(1)
    }

    @Test
    fun `교체하면 새 키로 서명하고 이전 키는 계속 게시한다`() {
        service.ensureActiveKey()
        val before = service.signingJwk()

        val newKid = service.rotate()

        assertThat(service.signingJwk()).isNotEqualTo(before).contains(newKid)
        assertThat(service.publishedPublicJwks()).hasSize(2)
        assertThat(service.list().map { it.status }).containsExactly("ACTIVE", "RETIRING")
    }

    @Test
    fun `보존 기간이 지난 이전 키는 게시하지 않는다`() {
        service.ensureActiveKey()
        service.rotate()

        clock.advance(Duration.ofHours(1))

        assertThat(service.publishedPublicJwks()).hasSize(1)
        assertThat(service.list().single { it.status == "RETIRING" }.published).isFalse()
    }

    private class FakeCrypto : SigningKeyCryptoPort {
        private var sequence = 0

        override fun generate(): GeneratedSigningKey {
            val kid = "kid-${++sequence}"
            return GeneratedSigningKey(kid, """{"kid":"$kid"}""", "encrypted:$kid")
        }

        override fun decryptPrivateJwk(kid: String, encryptedPrivateJwk: String): String = """{"kid":"$kid","d":"secret"}"""
    }

    private class InMemorySigningKeyRepository : SigningKeyPort {
        private val store = linkedMapOf<Long, SigningKey>()

        override fun findAll(): List<SigningKey> = store.values.toList()

        override fun save(key: SigningKey): SigningKey {
            val id = key.id ?: SigningKeyId(store.size + 1L)
            val saved = SigningKey.reconstitute(
                id, key.kid, key.status, key.publicJwk, key.encryptedPrivateJwk, key.activatedAt, key.rotatedAt,
            )
            store[id.value] = saved
            return saved
        }
    }
}
