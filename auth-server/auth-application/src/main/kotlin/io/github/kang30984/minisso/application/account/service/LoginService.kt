package io.github.kang30984.minisso.application.account.service

import io.github.kang30984.minisso.application.account.port.inbound.AuthenticationResult
import io.github.kang30984.minisso.application.account.port.inbound.LoginCommand
import io.github.kang30984.minisso.application.account.port.inbound.LoginUseCase
import io.github.kang30984.minisso.application.account.port.outbound.LoadAccountPort
import io.github.kang30984.minisso.application.account.port.outbound.SaveAccountPort
import io.github.kang30984.minisso.application.history.port.outbound.SaveLoginHistoryPort
import io.github.kang30984.minisso.domain.account.AccountId
import io.github.kang30984.minisso.domain.account.AccountPolicy
import io.github.kang30984.minisso.domain.account.LoginId
import io.github.kang30984.minisso.domain.account.LoginResult
import io.github.kang30984.minisso.domain.account.PasswordHash
import io.github.kang30984.minisso.domain.account.PasswordHasher
import io.github.kang30984.minisso.domain.history.LoginFailureReason
import io.github.kang30984.minisso.domain.history.LoginHistory
import io.github.kang30984.minisso.domain.history.LoginOutcome
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

/**
 * 로그인 실패도 예외가 아닌 결과값으로 돌려준다.
 * 예외로 처리하면 트랜잭션이 롤백되어 실패 횟수와 이력이 저장되지 않기 때문이다.
 */
@Transactional
class LoginService(
    private val loadAccountPort: LoadAccountPort,
    private val saveAccountPort: SaveAccountPort,
    private val saveLoginHistoryPort: SaveLoginHistoryPort,
    private val passwordHasher: PasswordHasher,
    private val accountPolicy: AccountPolicy,
    private val clock: Clock,
) : LoginUseCase {

    // 존재하지 않는 계정에도 실제 해시 비교를 수행해, 응답 시간 차이로 계정 존재 여부를 추측할 수 없게 한다.
    private val timingEqualizerHash: PasswordHash = passwordHasher.hash("timing-equalizer-Passw0rd!")

    override fun login(command: LoginCommand): AuthenticationResult {
        val now = clock.instant()
        val account = LoginId.parseOrNull(command.loginId)?.let(loadAccountPort::findByLoginId)

        if (account == null) {
            passwordHasher.matches(command.password, timingEqualizerHash)
            return fail(command, null, LoginFailureReason.UNKNOWN_ACCOUNT, now)
        }

        val accountId = checkNotNull(account.id)
        val result = account.authenticate(command.password, passwordHasher, accountPolicy, now)
        saveAccountPort.save(account)

        return when (result) {
            is LoginResult.Rejected -> fail(command, accountId, LoginFailureReason.from(result.reason), now)
            LoginResult.Success -> succeed(command, accountId, account.loginId, passwordChangeRequired = false, now)
            LoginResult.PasswordChangeRequired -> succeed(command, accountId, account.loginId, passwordChangeRequired = true, now)
        }
    }

    private fun succeed(
        command: LoginCommand,
        accountId: AccountId,
        loginId: LoginId,
        passwordChangeRequired: Boolean,
        now: Instant,
    ): AuthenticationResult {
        val outcome = if (passwordChangeRequired) LoginOutcome.PASSWORD_CHANGE_REQUIRED else LoginOutcome.SUCCESS
        record(command, accountId, outcome, null, now)
        return AuthenticationResult.Authenticated(accountId.value, loginId.value, passwordChangeRequired)
    }

    private fun fail(
        command: LoginCommand,
        accountId: AccountId?,
        reason: LoginFailureReason,
        now: Instant,
    ): AuthenticationResult {
        record(command, accountId, LoginOutcome.FAILURE, reason, now)
        return AuthenticationResult.Failed(reason)
    }

    private fun record(
        command: LoginCommand,
        accountId: AccountId?,
        outcome: LoginOutcome,
        reason: LoginFailureReason?,
        now: Instant,
    ) {
        saveLoginHistoryPort.save(
            LoginHistory.attempt(
                accountId = accountId,
                loginId = command.loginId,
                clientId = command.clientId,
                ip = command.ip,
                userAgent = command.userAgent,
                outcome = outcome,
                failureReason = reason,
                occurredAt = now,
            ),
        )
    }
}
