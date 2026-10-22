package io.github.kang30984.minisso.adapter.outbound.security

import io.github.kang30984.minisso.domain.account.PasswordHash
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class Argon2PasswordHasherTest {

    private val hasher = Argon2PasswordHasher(PasswordEncoderConfig.create())

    @Test
    fun `해시는 알고리즘 접두어를 가지며 같은 비밀번호도 매번 다른 값이 된다 (salt)`() {
        val first = hasher.hash("Sso-Passw0rd!")
        val second = hasher.hash("Sso-Passw0rd!")

        assertThat(first.value).startsWith("{argon2}")
        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `원문과 해시를 비교한다`() {
        val hash = hasher.hash("Sso-Passw0rd!")

        assertThat(hasher.matches("Sso-Passw0rd!", hash)).isTrue()
        assertThat(hasher.matches("Sso-Passw0rd?", hash)).isFalse()
    }

    @Test
    fun `알 수 없는 형식의 해시는 예외 없이 불일치로 처리한다`() {
        assertThat(hasher.matches("plain-text", PasswordHash("plain-text"))).isFalse()
        assertThat(hasher.matches("x", PasswordHash("{unknown}abc"))).isFalse()
    }
}
