package io.github.kang30984.minisso.application.account.service

import io.github.kang30984.minisso.application.account.port.inbound.AccountView
import io.github.kang30984.minisso.application.account.port.inbound.GetAccountQuery
import io.github.kang30984.minisso.application.account.port.outbound.LoadAccountPort
import io.github.kang30984.minisso.domain.account.AccountId
import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Transactional(readOnly = true)
class AccountQueryService(
    private val loadAccountPort: LoadAccountPort,
    private val clock: Clock,
) : GetAccountQuery {

    override fun getAccount(accountId: Long): AccountView {
        val account = loadAccountPort.findById(AccountId(accountId))
            ?: throw BusinessException(ErrorCode.ACCOUNT_NOT_FOUND)

        return AccountView(
            id = accountId,
            loginId = account.loginId.value,
            email = account.email.value,
            name = account.name,
            department = account.department,
            status = account.status.name,
            locked = account.isLocked(clock.instant()),
            lockedUntil = account.lockedUntil,
            lastLoginAt = account.lastLoginAt,
            passwordChangedAt = account.passwordChangedAt,
        )
    }
}
