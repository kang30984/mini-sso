package io.github.kang30984.minisso.application.session.port.inbound

/**
 * SSO 로그아웃. 하나의 SSO 세션(sid)으로 발급된 모든 토큰을 폐기하고,
 * 그 세션으로 로그인했던 서비스들에 로그아웃 사실을 알린다 (OIDC Back-Channel Logout).
 */
interface LogoutUseCase {
    fun logout(command: LogoutCommand)
}

data class LogoutCommand(
    /** ID 토큰의 sid 클레임 */
    val sessionId: String,
    /** ID 토큰의 sub 클레임 (계정 ID) */
    val subject: String,
    /** Logout Token의 iss. ID 토큰을 발급한 issuer와 같아야 클라이언트가 검증에 성공한다. */
    val issuer: String,
)
