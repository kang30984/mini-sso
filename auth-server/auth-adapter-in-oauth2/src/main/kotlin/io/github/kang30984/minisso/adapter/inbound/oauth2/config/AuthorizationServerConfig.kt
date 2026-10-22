package io.github.kang30984.minisso.adapter.inbound.oauth2.config

import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.github.kang30984.minisso.adapter.inbound.oauth2.client.RegisteredClientRepositoryAdapter
import io.github.kang30984.minisso.adapter.inbound.oauth2.key.SigningKeyJwkSource
import io.github.kang30984.minisso.adapter.inbound.oauth2.logout.SsoLogoutSuccessHandler
import io.github.kang30984.minisso.adapter.inbound.oauth2.token.AccountClaimsTokenCustomizer
import io.github.kang30984.minisso.application.account.port.inbound.GetAccountQuery
import io.github.kang30984.minisso.application.client.port.inbound.GetOAuthClientQuery
import io.github.kang30984.minisso.application.session.port.inbound.LogoutUseCase
import io.github.kang30984.minisso.application.signingkey.port.inbound.GetSigningKeysQuery
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import java.time.Clock
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class AuthorizationServerConfig {

    /**
     * 인가 서버 엔드포인트(/oauth2/authorize, /oauth2/token, /oauth2/jwks, /userinfo, /.well-known/...)를 담당하는 체인.
     * 로그인하지 않은 브라우저 요청은 로그인 페이지로 보낸다.
     */
    @Bean
    @Order(1)
    fun authorizationServerSecurityFilterChain(http: HttpSecurity, logoutUseCase: LogoutUseCase): SecurityFilterChain {
        val authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer()
        val logoutSuccessHandler = SsoLogoutSuccessHandler(logoutUseCase)

        http
            .securityMatcher(authorizationServerConfigurer.endpointsMatcher)
            .with(authorizationServerConfigurer) { server ->
                server.oidc { oidc ->
                    oidc.logoutEndpoint { it.logoutResponseHandler(logoutSuccessHandler) }
                }
            }
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .exceptionHandling {
                it.defaultAuthenticationEntryPointFor(
                    LoginUrlAuthenticationEntryPoint("/login"),
                    MediaTypeRequestMatcher(MediaType.TEXT_HTML),
                )
            }
            // /userinfo는 액세스 토큰으로 호출한다
            .oauth2ResourceServer { it.jwt(Customizer.withDefaults()) }

        return http.build()
    }

    @Bean
    fun registeredClientRepository(getOAuthClientQuery: GetOAuthClientQuery): RegisteredClientRepository =
        RegisteredClientRepositoryAdapter(getOAuthClientQuery)

    @Bean
    fun accountClaimsTokenCustomizer(getAccountQuery: GetAccountQuery): OAuth2TokenCustomizer<JwtEncodingContext> =
        AccountClaimsTokenCustomizer(getAccountQuery)

    /** DB에 저장된 서명 키 (ADR-008). 기동 시 활성 키가 없으면 BootstrapDataInitializer가 만든다. */
    @Bean
    fun signingKeyJwkSource(
        getSigningKeysQuery: GetSigningKeysQuery,
        clock: Clock,
        @Value("\${minisso.signing-key.cache-ttl:30s}") cacheTtl: Duration,
    ) = SigningKeyJwkSource(getSigningKeysQuery, clock, cacheTtl)

    /** JWKS 엔드포인트(/oauth2/jwks)와 토큰 검증이 쓰는 공개키 집합 */
    @Bean
    fun jwkSource(signingKeyJwkSource: SigningKeyJwkSource): JWKSource<SecurityContext> = signingKeyJwkSource.publishedKeys

    /** 토큰 서명. 인가 서버의 토큰 발급과 Back-Channel Logout Token 서명이 같은 활성 키를 쓴다. */
    @Bean
    fun jwtEncoder(signingKeyJwkSource: SigningKeyJwkSource): JwtEncoder = NimbusJwtEncoder(signingKeyJwkSource.signingKeys)

    /** 자기 자신이 발급한 토큰을 검증할 때 네트워크 호출 없이 같은 키를 사용한다 */
    @Bean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder =
        OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)

    /** issuer를 지정하지 않으면 요청 호스트로 결정된다. 운영에서는 반드시 고정값을 쓴다. */
    @Bean
    fun authorizationServerSettings(@Value("\${minisso.issuer-uri:}") issuerUri: String): AuthorizationServerSettings =
        AuthorizationServerSettings.builder()
            .apply { if (issuerUri.isNotBlank()) issuer(issuerUri) }
            .build()

}
