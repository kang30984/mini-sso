package io.github.kang30984.minisso.domain.fixture

import io.github.kang30984.minisso.domain.account.PasswordHash
import io.github.kang30984.minisso.domain.account.PasswordHasher

/** 테스트용 해시. 실제 해시 대신 접두어만 붙이고, 비교 호출 횟수를 센다. */
class FakePasswordHasher : PasswordHasher {
    var matchCount: Int = 0
        private set

    override fun hash(rawPassword: String): PasswordHash = PasswordHash("$PREFIX$rawPassword")

    override fun matches(rawPassword: String, hash: PasswordHash): Boolean {
        matchCount += 1
        return hash.value == "$PREFIX$rawPassword"
    }

    companion object {
        private const val PREFIX = "fake-hash:"
    }
}
