package io.github.kang30984.minisso.adapter.outbound.security

import io.github.kang30984.minisso.domain.account.PasswordHash
import io.github.kang30984.minisso.domain.account.PasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * 도메인의 PasswordHasher를 Spring Security PasswordEncoder(Argon2id)로 구현한다.
 * 저장값에 {argon2} 접두어가 붙어, 나중에 알고리즘을 바꿔도 기존 해시를 그대로 검증할 수 있다.
 *
 * 계정 비밀번호 검증이 클라이언트 시크릿용 캐시(ADR-012)를 타지 않도록 PasswordEncoder 빈을 주입받지 않고
 * 캐시 없는 인코더를 직접 만든다.
 */
@Component
class Argon2PasswordHasher() : PasswordHasher {

    // Spring은 Kotlin 클래스의 주 생성자를 쓰므로, 빈 주입이 일어나지 않도록 주 생성자를 인자 없이 둔다
    private var passwordEncoder: PasswordEncoder = PasswordEncoderConfig.create()

    /** 테스트용 */
    internal constructor(passwordEncoder: PasswordEncoder) : this() {
        this.passwordEncoder = passwordEncoder
    }

    override fun hash(rawPassword: String): PasswordHash = PasswordHash(checkNotNull(passwordEncoder.encode(rawPassword)))

    override fun matches(rawPassword: String, hash: PasswordHash): Boolean = passwordEncoder.matches(rawPassword, hash.value)
}
