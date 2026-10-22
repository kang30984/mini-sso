package io.github.kang30984.minisso.adapter.outbound.security

import io.github.kang30984.minisso.domain.account.PasswordHash
import io.github.kang30984.minisso.domain.account.PasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * 도메인의 PasswordHasher를 Spring Security PasswordEncoder(Argon2id)로 구현한다.
 * 저장값에 {argon2} 접두어가 붙어, 나중에 알고리즘을 바꿔도 기존 해시를 그대로 검증할 수 있다.
 */
@Component
class Argon2PasswordHasher(
    private val passwordEncoder: PasswordEncoder,
) : PasswordHasher {

    override fun hash(rawPassword: String): PasswordHash = PasswordHash(checkNotNull(passwordEncoder.encode(rawPassword)))

    override fun matches(rawPassword: String, hash: PasswordHash): Boolean = passwordEncoder.matches(rawPassword, hash.value)
}
