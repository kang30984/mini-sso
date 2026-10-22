package io.github.kang30984.minisso.application.session.port.outbound

import io.github.kang30984.minisso.domain.client.OAuthClientId

interface SsoSessionPort {
    /**
     * 해당 SSO 세션으로 발급된 인가(토큰 묶음)를 모두 폐기한다.
     * @return 폐기된 인가가 속했던 클라이언트 목록
     */
    fun revokeBySessionId(sessionId: String): List<OAuthClientId>
}
