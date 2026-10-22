package io.github.kang30984.minisso.domain.account

/** 비밀번호 해시 알고리즘은 기술 선택이므로 도메인은 인터페이스만 정의하고 구현은 어댑터가 제공한다 */
interface PasswordHasher {
    fun hash(rawPassword: String): PasswordHash

    fun matches(rawPassword: String, hash: PasswordHash): Boolean
}
