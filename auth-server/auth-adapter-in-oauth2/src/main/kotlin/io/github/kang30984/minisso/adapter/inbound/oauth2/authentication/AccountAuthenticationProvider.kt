package io.github.kang30984.minisso.adapter.inbound.oauth2.authentication

import io.github.kang30984.minisso.application.account.port.inbound.AuthenticationResult
import io.github.kang30984.minisso.application.account.port.inbound.LoginCommand
import io.github.kang30984.minisso.application.account.port.inbound.LoginUseCase
import io.github.kang30984.minisso.domain.history.LoginFailureReason
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * Spring Security 로그인 폼을 LoginUseCase에 연결하는 어댑터.
 * 잠금, 이력 기록 같은 규칙은 모두 애플리케이션 계층에 있고, 여기서는 결과를 Spring Security 예외로 바꾸기만 한다.
 */
class AccountAuthenticationProvider(
    private val loginUseCase: LoginUseCase,
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val details = authentication.details as? LoginRequestDetails
        val command = LoginCommand(
            loginId = authentication.name,
            password = authentication.credentials?.toString().orEmpty(),
            clientId = details?.clientId,
            ip = details?.ip ?: UNKNOWN_IP,
            userAgent = details?.userAgent,
        )
        val result = loginWithRetry(command)

        return when (result) {
            is AuthenticationResult.Authenticated ->
                UsernamePasswordAuthenticationToken.authenticated(
                    MiniSsoPrincipal(result.accountId, result.loginId),
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_${result.role}")),
                ).apply { this.details = authentication.details }

            is AuthenticationResult.Failed -> throw toException(result.reason)
        }
    }

    /**
     * 같은 계정의 실패 횟수를 동시에 갱신하면 낙관적 락이 충돌한다 (ADR-011).
     * 트랜잭션 밖인 이곳에서 한 번 재시도해 최신 상태로 다시 판단하고,
     * 그래도 충돌하면 500 대신 인증 실패로 응답한다.
     */
    private fun loginWithRetry(command: LoginCommand): AuthenticationResult {
        repeat(MAX_ATTEMPTS - 1) {
            try {
                return loginUseCase.login(command)
            } catch (e: OptimisticLockingFailureException) {
                // 다음 시도에서 최신 계정 상태를 다시 읽는다
            }
        }
        return try {
            loginUseCase.login(command)
        } catch (e: OptimisticLockingFailureException) {
            throw AuthenticationServiceException("요청이 몰려 로그인을 처리하지 못했습니다. 다시 시도하세요.", e)
        }
    }

    override fun supports(authentication: Class<*>): Boolean =
        UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)

    /**
     * 계정 존재 여부가 드러나지 않도록 없는 계정, 탈퇴 계정, 비밀번호 오류는 같은 메시지를 쓴다.
     * 잠금과 휴면은 사용자가 조치해야 하므로 구분해서 알려준다.
     */
    private fun toException(reason: LoginFailureReason): AuthenticationException = when (reason) {
        LoginFailureReason.UNKNOWN_ACCOUNT,
        LoginFailureReason.WITHDRAWN,
        LoginFailureReason.BAD_CREDENTIALS,
        -> BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.")

        LoginFailureReason.LOCKED -> LockedException("로그인 실패가 반복되어 계정이 잠겼습니다. 잠시 후 다시 시도하세요.")
        LoginFailureReason.DORMANT -> DisabledException("휴면 계정입니다. 관리자에게 문의하세요.")
        LoginFailureReason.RATE_LIMITED -> LoginThrottledException("로그인 시도가 너무 많습니다. 잠시 후 다시 시도하세요.")
    }

    /** 시도 횟수 제한. 계정 상태와 무관하므로 계정 존재 여부를 드러내지 않는다. */
    class LoginThrottledException(message: String) : AuthenticationException(message)

    companion object {
        private const val UNKNOWN_IP = "unknown"
        private const val MAX_ATTEMPTS = 2
    }
}
