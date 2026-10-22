package io.github.kang30984.minisso.application.fake

import io.github.kang30984.minisso.application.account.port.outbound.LoadAccountPort
import io.github.kang30984.minisso.application.account.port.outbound.SaveAccountPort
import io.github.kang30984.minisso.domain.account.Account
import io.github.kang30984.minisso.domain.account.AccountId
import io.github.kang30984.minisso.domain.account.Email
import io.github.kang30984.minisso.domain.account.LoginId

class InMemoryAccountRepository : LoadAccountPort, SaveAccountPort {
    private val store = linkedMapOf<Long, Account>()
    private var sequence = 0L

    override fun findById(id: AccountId): Account? = store[id.value]

    override fun findByLoginId(loginId: LoginId): Account? = store.values.firstOrNull { it.loginId == loginId }

    override fun existsByLoginId(loginId: LoginId): Boolean = findByLoginId(loginId) != null

    override fun existsByEmail(email: Email): Boolean = store.values.any { it.email == email }

    override fun save(account: Account): Account {
        val saved = account.id?.let { account } ?: Account.reconstitute(
            id = AccountId(++sequence),
            loginId = account.loginId,
            email = account.email,
            name = account.name,
            department = account.department,
            passwordHash = account.passwordHash,
            status = account.status,
            failedLoginCount = account.failedLoginCount,
            lockedUntil = account.lockedUntil,
            passwordChangedAt = account.passwordChangedAt,
            lastLoginAt = account.lastLoginAt,
        )
        store[checkNotNull(saved.id).value] = saved
        return saved
    }
}
