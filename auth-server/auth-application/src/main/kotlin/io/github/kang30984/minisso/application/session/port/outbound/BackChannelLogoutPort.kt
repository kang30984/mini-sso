package io.github.kang30984.minisso.application.session.port.outbound

interface BackChannelLogoutPort {
    /** 전송 실패가 로그아웃 자체를 막지 않도록 구현체는 비동기·재시도로 처리한다 */
    fun notify(notice: BackChannelLogoutNotice)
}

data class BackChannelLogoutNotice(
    val clientId: String,
    val logoutUri: String,
    val issuer: String,
    val subject: String,
    val sessionId: String,
)
