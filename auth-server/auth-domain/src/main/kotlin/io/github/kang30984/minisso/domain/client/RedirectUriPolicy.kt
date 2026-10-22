package io.github.kang30984.minisso.domain.client

import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import java.net.URI
import java.net.URISyntaxException

/**
 * 리다이렉트 URI 등록 정책. 인가 코드가 엉뚱한 곳으로 새는 것(Open Redirect)을 막기 위한 첫 관문이다.
 * - 절대 URI만 허용, 와일드카드 금지 (인가 서버는 등록값과 정확히 일치하는지만 비교한다)
 * - https만 허용하되, 로컬 개발 주소(localhost, 127.0.0.1, *.localhost)는 http 허용
 * - fragment, 사용자 정보(user:pass@) 금지
 */
object RedirectUriPolicy {
    private const val MAX_LENGTH = 500
    private val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "[::1]")

    fun validate(uri: String) {
        if (!isAllowed(uri)) throw BusinessException(ErrorCode.INVALID_REDIRECT_URI)
    }

    fun isAllowed(raw: String): Boolean {
        if (raw.length > MAX_LENGTH || raw.contains('*')) return false
        val uri = try {
            URI(raw)
        } catch (e: URISyntaxException) {
            return false
        }
        if (!uri.isAbsolute || uri.rawFragment != null || uri.rawUserInfo != null) return false
        val host = uri.host ?: return false

        return when (uri.scheme?.lowercase()) {
            "https" -> true
            "http" -> host in LOOPBACK_HOSTS || host.endsWith(".localhost")
            else -> false
        }
    }
}
