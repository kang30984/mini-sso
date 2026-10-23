package io.github.kang30984.minisso.application.account.service

import io.github.kang30984.minisso.application.account.port.inbound.AuthenticationResult
import io.github.kang30984.minisso.application.account.port.inbound.LoginCommand
import io.github.kang30984.minisso.application.account.port.inbound.LoginUseCase
import io.github.kang30984.minisso.application.account.port.outbound.LoadAccountPort
import io.github.kang30984.minisso.application.account.port.outbound.LoginRateLimitPort
import io.github.kang30984.minisso.application.account.port.outbound.SaveAccountPort
import io.github.kang30984.minisso.application.history.port.outbound.SaveLoginHistoryPort
import io.github.kang30984.minisso.domain.account.Account
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
    private val loginRateLimitPort: LoginRateLimitPort = LoginRateLimitPort.UNLIMITED,
    private val throttlePolicy: LoginThrottlePolicy = LoginThrottlePolicy(),
) : LoginUseCase {

    // 존재하지 않는 계정에도 실제 해시 비교를 수행해, 응답 시간 차이로 계정 존재 여부를 추측할 수 없게 한다.
    private val timingEqualizerHash: PasswordHash = passwordHasher.hash("timing-equalizer-Passw0rd!")

    override fun login(command: LoginCommand): AuthenticationResult {
        // 비밀번호 비교(Argon2)보다 먼저 검사해, 차단된 요청이 CPU를 쓰지 않게 한다.
        // 차단된 시도는 이력에 남기지 않는다. 공격 중에는 차단 건수만큼 DB 쓰기가 늘어나기 때문이다.
        if (!loginRateLimitPort.tryAcquire("ip:${command.ip}", throttlePolicy.maxAttemptsPerIp, throttlePolicy.window)) {
            return AuthenticationResult.Failed(LoginFailureReason.RATE_LIMITED)
        }

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
            LoginResult.Success -> succeed(command, account, passwordChangeRequired = false, now)
            LoginResult.PasswordChangeRequired -> succeed(command, account, passwordChangeRequired = true, now)
        }
    }

    private fun succeed(
        command: LoginCommand,
        account: Account,
        passwordChangeRequired: Boolean,
        now: Instant,
    ): AuthenticationResult {
        val accountId = checkNotNull(account.id)
        val outcome = if (passwordChangeRequired) LoginOutcome.PASSWORD_CHANGE_REQUIRED else LoginOutcome.SUCCESS
        record(command, accountId, outcome, null, now)
        return AuthenticationResult.Authenticated(
            accountId = accountId.value,
            loginId = account.loginId.value,
            role = account.role.name,
            passwordChangeRequired = passwordChangeRequired,
        )
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
