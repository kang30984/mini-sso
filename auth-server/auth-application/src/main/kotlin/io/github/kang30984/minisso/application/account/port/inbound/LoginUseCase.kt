package io.github.kang30984.minisso.application.account.port.inbound

import io.github.kang30984.minisso.domain.history.LoginFailureReason

/**
 * 아이디/비밀번호 인증. 3주차에 Spring Authorization Server의 AuthenticationProvider가 이 포트를 호출한다.
 */
interface LoginUseCase {
    fun login(command: LoginCommand): AuthenticationResult
}

data class LoginCommand(
    val loginId: String,
    val password: String,
    val clientId: String?,
    val ip: String,
    val userAgent: String?,
) {
    override fun toString(): String =
        "LoginCommand(loginId=$loginId, password=****, clientId=$clientId, ip=$ip, userAgent=$userAgent)"
}

sealed interface AuthenticationResult {
    data class Authenticated(
        val accountId: Long,
        val loginId: String,
        val passwordChangeRequired: Boolean,
    ) : AuthenticationResult

    /**
     * 실패 사유는 감사 로그와 내부 판단용이다.
     * 사용자에게는 계정 존재 여부를 알 수 없도록 UNKNOWN_ACCOUNT, WITHDRAWN, BAD_CREDENTIALS를 같은 메시지로 보여줘야 한다.
     */
    data class Failed(val reason: LoginFailureReason) : AuthenticationResult
}
