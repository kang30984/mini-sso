package io.github.kang30984.minisso.adapter.inbound.oauth2.client

import io.github.kang30984.minisso.application.client.port.inbound.GetOAuthClientQuery
import io.github.kang30984.minisso.application.client.port.inbound.OAuthClientView
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.time.Duration

/**
 * Spring Authorization Server의 확장 지점을 애플리케이션 포트에 연결한다 (DESIGN 4.2).
 * 저장 구조는 우리 도메인이 소유하고, 프레임워크 모델(RegisteredClient)로의 변환만 여기서 한다.
 */
class RegisteredClientRepositoryAdapter(
    private val getOAuthClientQuery: GetOAuthClientQuery,
) : RegisteredClientRepository {

    override fun findById(id: String): RegisteredClient? =
        id.toLongOrNull()?.let(getOAuthClientQuery::findById)?.toRegisteredClient()

    override fun findByClientId(clientId: String): RegisteredClient? =
        getOAuthClientQuery.findByClientId(clientId)?.toRegisteredClient()

    /** 클라이언트 등록은 RegisterOAuthClientUseCase로만 한다 */
    override fun save(registeredClient: RegisteredClient) {
        throw UnsupportedOperationException("클라이언트는 RegisterOAuthClientUseCase로 등록합니다.")
    }

    private fun OAuthClientView.toRegisteredClient(): RegisteredClient =
        RegisteredClient.withId(id.toString())
            .clientId(clientId)
            .clientName(clientName)
            .clientSecret(clientSecretHash)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUris { it.addAll(redirectUris) }
            .postLogoutRedirectUris { it.addAll(postLogoutRedirectUris) }
            .scopes { it.addAll(scopes) }
            .clientSettings(CLIENT_SETTINGS)
            .tokenSettings(TOKEN_SETTINGS)
            .build()

    companion object {
        // ADR-010: 사내 1st-party 서비스만 등록하므로 동의 화면은 생략하고, PKCE는 기밀 클라이언트에도 강제한다
        private val CLIENT_SETTINGS: ClientSettings = ClientSettings.builder()
            .requireProofKey(true)
            .requireAuthorizationConsent(false)
            .build()

        // DESIGN 10장: Access 10분, Refresh 14일 + 회전(재사용 금지)
        private val TOKEN_SETTINGS: TokenSettings = TokenSettings.builder()
            .accessTokenTimeToLive(Duration.ofMinutes(10))
            .refreshTokenTimeToLive(Duration.ofDays(14))
            .reuseRefreshTokens(false)
            .idTokenSignatureAlgorithm(SignatureAlgorithm.RS256)
            .build()
    }
}
