package io.github.kang30984.minisso.adapter.outbound.security

import io.github.kang30984.minisso.domain.account.PasswordHash
import io.github.kang30984.minisso.domain.account.PasswordHasher
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * Argon2id 해시. 저장값에 {argon2} 접두어를 붙여 두어,
 * 나중에 알고리즘을 바꿔도 기존 해시를 그대로 검증할 수 있게 한다 (DelegatingPasswordEncoder).
 */
@Component
class Argon2PasswordHasher : PasswordHasher {

    private val delegate: PasswordEncoder = DelegatingPasswordEncoder(
        ARGON2_ID,
        mapOf<String, PasswordEncoder>(ARGON2_ID to Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()),
    ).apply {
        // 접두어가 없거나 알 수 없는 형식의 해시는 예외(500) 대신 불일치로 처리한다
        setDefaultPasswordEncoderForMatches(RejectAllPasswordEncoder)
    }

    override fun hash(rawPassword: String): PasswordHash = PasswordHash(checkNotNull(delegate.encode(rawPassword)))

    override fun matches(rawPassword: String, hash: PasswordHash): Boolean = delegate.matches(rawPassword, hash.value)

    private object RejectAllPasswordEncoder : PasswordEncoder {
        override fun encode(rawPassword: CharSequence?): String =
            throw UnsupportedOperationException("인코딩에는 사용하지 않습니다.")

        override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean = false
    }

    companion object {
        private const val ARGON2_ID = "argon2"
    }
}
