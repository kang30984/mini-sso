package io.github.kang30984.minisso.application.account.port.inbound

interface UnlockAccountUseCase {
    fun unlock(accountId: Long)
}
