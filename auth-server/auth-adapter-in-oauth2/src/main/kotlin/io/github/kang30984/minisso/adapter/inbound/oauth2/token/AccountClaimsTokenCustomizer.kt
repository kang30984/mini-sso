package io.github.kang30984.minisso.adapter.inbound.oauth2.token

import io.github.kang30984.minisso.adapter.inbound.oauth2.authentication.MiniSsoPrincipal
import io.github.kang30984.minisso.application.account.port.inbound.GetAccountQuery
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2TokenType
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer

/**
 * 토큰에 계정 정보를 담는다.
 * - ID 토큰: 사용자 화면 표시용 (scope에 따라 profile, email 클레임)
 * - Access 토큰: API 인가용 (roles). 개인정보는 최소화한다.
 */
class AccountClaimsTokenCustomizer(
    private val getAccountQuery: GetAccountQuery,
) : OAuth2TokenCustomizer<JwtEncodingContext> {

    override fun customize(context: JwtEncodingContext) {
        val principal = context.getPrincipal<Authentication>().principal as? MiniSsoPrincipal ?: return
        val account = getAccountQuery.getAccount(principal.accountId)
        val scopes = context.authorizedScopes
        val claims = context.claims

        when (context.tokenType.value) {
            OidcParameterNames.ID_TOKEN -> {
                claims.claim("preferred_username", account.loginId)
                if (OidcScopes.PROFILE in scopes) {
                    claims.claim("name", account.name)
                    claims.claim("department", account.department)
                }
                if (OidcScopes.EMAIL in scopes) {
                    claims.claim("email", account.email)
                }
            }

            OAuth2TokenType.ACCESS_TOKEN.value -> {
                claims.claim(ROLES_CLAIM, listOf(account.role))
            }
        }
    }

    companion object {
        const val ROLES_CLAIM = "roles"
    }
}
