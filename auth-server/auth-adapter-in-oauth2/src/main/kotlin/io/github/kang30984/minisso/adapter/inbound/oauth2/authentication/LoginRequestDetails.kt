package io.github.kang30984.minisso.adapter.inbound.oauth2.authentication

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.AuthenticationDetailsSource
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import java.io.Serializable

/** 로그인 이력에 남길 요청 정보. 어떤 서비스(client_id)에서 로그인하러 왔는지도 함께 기록한다. */
data class LoginRequestDetails(
    val ip: String,
    val userAgent: String?,
    val clientId: String?,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

class LoginRequestDetailsSource : AuthenticationDetailsSource<HttpServletRequest, LoginRequestDetails> {

    private val requestCache = HttpSessionRequestCache()

    override fun buildDetails(request: HttpServletRequest): LoginRequestDetails = LoginRequestDetails(
        ip = request.remoteAddr,
        userAgent = request.getHeader("User-Agent"),
        // 로그인 페이지로 오기 전의 /oauth2/authorize 요청이 세션에 저장되어 있다
        clientId = requestCache.getRequest(request, null)?.getParameterValues("client_id")?.firstOrNull(),
    )
}
