package io.github.kang30984.minisso.adapter.inbound.oauth2.authentication

import io.github.kang30984.minisso.application.account.port.inbound.AuthenticationResult
import io.github.kang30984.minisso.application.account.port.inbound.LoginCommand
import io.github.kang30984.minisso.application.account.port.inbound.LoginUseCase
import io.github.kang30984.minisso.domain.history.LoginFailureReason
import org.springframework.security.authentication.AuthenticationProvider
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
        val result = loginUseCase.login(
            LoginCommand(
                loginId = authentication.name,
                password = authentication.credentials?.toString().orEmpty(),
                clientId = details?.clientId,
                ip = details?.ip ?: UNKNOWN_IP,
                userAgent = details?.userAgent,
            ),
        )

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
    }

    companion object {
        private const val UNKNOWN_IP = "unknown"
    }
}
