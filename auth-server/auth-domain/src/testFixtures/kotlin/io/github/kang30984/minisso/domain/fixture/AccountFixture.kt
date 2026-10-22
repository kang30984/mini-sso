package io.github.kang30984.minisso.domain.fixture

import io.github.kang30984.minisso.domain.account.Account
import io.github.kang30984.minisso.domain.account.AccountId
import io.github.kang30984.minisso.domain.account.AccountRole
import io.github.kang30984.minisso.domain.account.AccountStatus
import io.github.kang30984.minisso.domain.account.Email
import io.github.kang30984.minisso.domain.account.LoginId
import io.github.kang30984.minisso.domain.account.PasswordHasher
import java.time.Instant

object AccountFixture {
    const val LOGIN_ID = "hong.gildong"
    const val PASSWORD = "Sso-Passw0rd!"
    val NOW: Instant = Instant.parse("2026-10-01T00:00:00Z")

    fun active(
        id: Long = 1L,
        loginId: String = LOGIN_ID,
        password: String = PASSWORD,
        hasher: PasswordHasher = FakePasswordHasher(),
        status: AccountStatus = AccountStatus.ACTIVE,
        passwordChangedAt: Instant = NOW,
        role: AccountRole = AccountRole.USER,
    ): Account = Account.reconstitute(
        id = AccountId(id),
        loginId = LoginId(loginId),
        role = role,
        email = Email("$loginId@example.com"),
        name = "홍길동",
        department = "플랫폼개발팀",
        passwordHash = hasher.hash(password),
        status = status,
        failedLoginCount = 0,
        lockedUntil = null,
        passwordChangedAt = passwordChangedAt,
        lastLoginAt = null,
    )
}
