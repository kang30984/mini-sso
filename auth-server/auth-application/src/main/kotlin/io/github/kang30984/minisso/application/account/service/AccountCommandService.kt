package io.github.kang30984.minisso.application.account.service

import io.github.kang30984.minisso.application.account.port.inbound.ChangePasswordCommand
import io.github.kang30984.minisso.application.account.port.inbound.ChangePasswordUseCase
import io.github.kang30984.minisso.application.account.port.inbound.EnsureAdminAccountUseCase
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountCommand
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountUseCase
import io.github.kang30984.minisso.application.account.port.inbound.UnlockAccountUseCase
import io.github.kang30984.minisso.application.account.port.outbound.LoadAccountPort
import io.github.kang30984.minisso.application.account.port.outbound.SaveAccountPort
import io.github.kang30984.minisso.domain.account.Account
import io.github.kang30984.minisso.domain.account.AccountId
import io.github.kang30984.minisso.domain.account.AccountPolicy
import io.github.kang30984.minisso.domain.account.AccountRole
import io.github.kang30984.minisso.domain.account.Email
import io.github.kang30984.minisso.domain.account.LoginId
import io.github.kang30984.minisso.domain.account.PasswordHasher
import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Transactional
class AccountCommandService(
    private val loadAccountPort: LoadAccountPort,
    private val saveAccountPort: SaveAccountPort,
    private val passwordHasher: PasswordHasher,
    private val accountPolicy: AccountPolicy,
    private val clock: Clock,
) : RegisterAccountUseCase, ChangePasswordUseCase, UnlockAccountUseCase, EnsureAdminAccountUseCase {

    override fun register(command: RegisterAccountCommand): Long = register(command, AccountRole.USER)

    override fun ensureAdmin(command: RegisterAccountCommand): Boolean {
        if (loadAccountPort.existsByLoginId(LoginId(command.loginId))) return false
        register(command, AccountRole.ADMIN)
        return true
    }

    private fun register(command: RegisterAccountCommand, role: AccountRole): Long {
        val loginId = LoginId(command.loginId)
        val email = Email(command.email)
        if (loadAccountPort.existsByLoginId(loginId)) throw BusinessException(ErrorCode.DUPLICATE_LOGIN_ID)
        if (loadAccountPort.existsByEmail(email)) throw BusinessException(ErrorCode.DUPLICATE_EMAIL)

        val account = Account.register(
            loginId = loginId,
            email = email,
            name = command.name,
            department = command.department,
            rawPassword = command.initialPassword,
            hasher = passwordHasher,
            policy = accountPolicy,
            now = clock.instant(),
            role = role,
        )
        return checkNotNull(saveAccountPort.save(account).id) { "저장된 계정에 ID가 없습니다." }.value
    }

    override fun changePassword(command: ChangePasswordCommand) {
        val account = loadOrThrow(command.accountId)
        account.changePassword(
            currentPassword = command.currentPassword,
            newPassword = command.newPassword,
            hasher = passwordHasher,
            policy = accountPolicy,
            now = clock.instant(),
        )
        saveAccountPort.save(account)
    }

    override fun unlock(accountId: Long) {
        val account = loadOrThrow(accountId)
        account.unlock()
        saveAccountPort.save(account)
    }

    private fun loadOrThrow(accountId: Long): Account =
        loadAccountPort.findById(AccountId(accountId)) ?: throw BusinessException(ErrorCode.ACCOUNT_NOT_FOUND)
}
