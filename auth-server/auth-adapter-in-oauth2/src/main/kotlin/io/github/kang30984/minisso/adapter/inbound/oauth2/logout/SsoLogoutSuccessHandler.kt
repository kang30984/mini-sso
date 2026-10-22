package io.github.kang30984.minisso.adapter.inbound.oauth2.logout

import io.github.kang30984.minisso.application.session.port.inbound.LogoutCommand
import io.github.kang30984.minisso.application.session.port.inbound.LogoutUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcLogoutAuthenticationToken
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.web.util.UriComponentsBuilder

/**
 * OIDC RP-Initiated Logout(/connect/logout) 처리.
 * Spring Authorization Server가 id_token_hint와 post_logout_redirect_uri 검증을 마친 뒤 호출된다.
 *
 * 1. 해당 SSO 세션(sid)으로 발급된 토큰을 모두 폐기하고 참여 서비스에 Back-Channel Logout을 보낸다
 * 2. 인증 서버의 로그인 세션을 끊는다
 * 3. 등록된 post_logout_redirect_uri로 돌려보낸다
 */
class SsoLogoutSuccessHandler(
    private val logoutUseCase: LogoutUseCase,
) : AuthenticationSuccessHandler {

    private val sessionLogoutHandler = SecurityContextLogoutHandler()
    private val redirectStrategy = DefaultRedirectStrategy()

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val logout = authentication as OidcLogoutAuthenticationToken
        val idToken = logout.idToken

        val sid = idToken?.getClaimAsString(SID_CLAIM)
        if (idToken != null && sid != null) {
            logoutUseCase.logout(
                LogoutCommand(
                    sessionId = sid,
                    subject = idToken.subject,
                    issuer = AuthorizationServerContextHolder.getContext().issuer,
                ),
            )
        }

        sessionLogoutHandler.logout(request, response, SecurityContextHolder.getContext().authentication)
        redirectStrategy.sendRedirect(request, response, redirectTarget(logout))
    }

    private fun redirectTarget(logout: OidcLogoutAuthenticationToken): String {
        val postLogoutRedirectUri = logout.postLogoutRedirectUri
        if (postLogoutRedirectUri.isNullOrBlank()) return DEFAULT_TARGET

        return UriComponentsBuilder.fromUriString(postLogoutRedirectUri)
            .apply { logout.state?.takeIf { it.isNotBlank() }?.let { queryParam("state", it) } }
            .encode()
            .build()
            .toUriString()
    }

    companion object {
        private const val SID_CLAIM = "sid"
        private const val DEFAULT_TARGET = "/login?logout"
    }
}
