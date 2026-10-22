package io.github.kang30984.minisso.domain.signingkey

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class SigningKeyTest {

    private val now = Instant.parse("2026-10-01T00:00:00Z")
    private val policy = SigningKeyPolicy(retention = Duration.ofHours(1))

    @Test
    fun `활성 키는 항상 게시된다`() {
        val key = SigningKey.activate("kid-1", "{}", "encrypted", now)

        assertThat(key.isPublished(now.plus(Duration.ofDays(365)), policy)).isTrue()
    }

    @Test
    fun `교체된 키는 보존 기간 동안만 게시된다`() {
        val key = SigningKey.activate("kid-1", "{}", "encrypted", now)
        key.retire(now)

        assertThat(key.status).isEqualTo(SigningKeyStatus.RETIRING)
        assertThat(key.isPublished(now.plus(Duration.ofMinutes(59)), policy)).isTrue()
        assertThat(key.isPublished(now.plus(Duration.ofHours(1)), policy)).isFalse()
    }

    @Test
    fun `이미 교체된 키는 다시 교체할 수 없다`() {
        val key = SigningKey.activate("kid-1", "{}", "encrypted", now)
        key.retire(now)

        assertThatThrownBy { key.retire(now) }.isInstanceOf(IllegalStateException::class.java)
    }
}
