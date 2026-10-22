package io.github.kang30984.minisso.application.account.port.inbound

import java.time.Instant

interface GetAccountQuery {
    fun getAccount(accountId: Long): AccountView
}

data class AccountView(
    val id: Long,
    val loginId: String,
    val role: String,
    val email: String,
    val name: String,
    val department: String,
    val status: String,
    val locked: Boolean,
    val lockedUntil: Instant?,
    val lastLoginAt: Instant?,
    val passwordChangedAt: Instant,
)
