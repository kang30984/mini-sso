package io.github.kang30984.minisso.application.account.port.outbound

import io.github.kang30984.minisso.domain.account.Account
import io.github.kang30984.minisso.domain.account.AccountId
import io.github.kang30984.minisso.domain.account.Email
import io.github.kang30984.minisso.domain.account.LoginId

interface LoadAccountPort {
    fun findById(id: AccountId): Account?

    fun findByLoginId(loginId: LoginId): Account?

    fun existsByLoginId(loginId: LoginId): Boolean

    fun existsByEmail(email: Email): Boolean
}
