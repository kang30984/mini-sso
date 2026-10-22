package io.github.kang30984.minisso.adapter.inbound.oauth2.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.github.kang30984.minisso.adapter.inbound.oauth2.client.RegisteredClientRepositoryAdapter
import io.github.kang30984.minisso.adapter.inbound.oauth2.token.AccountClaimsTokenCustomizer
import io.github.kang30984.minisso.application.account.port.inbound.GetAccountQuery
import io.github.kang30984.minisso.application.client.port.inbound.GetOAuthClientQuery
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.UUID

@Configuration(proxyBeanMethods = false)
class AuthorizationServerConfig {

    /**
     * 인가 서버 엔드포인트(/oauth2/authorize, /oauth2/token, /oauth2/jwks, /userinfo, /.well-known/...)를 담당하는 체인.
     * 로그인하지 않은 브라우저 요청은 로그인 페이지로 보낸다.
     */
    @Bean
    @Order(1)
    fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer()

        http
            .securityMatcher(authorizationServerConfigurer.endpointsMatcher)
            .with(authorizationServerConfigurer) { it.oidc(Customizer.withDefaults()) }
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

    /**
     * 서명 키. 지금은 기동 시 생성하므로 재시작하면 이전 토큰을 검증할 수 없다.
     * 5주차에 DB 저장 + 무중단 로테이션(ADR-008)으로 교체한다.
     */
    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(RSA_KEY_SIZE) }.generateKeyPair()
        val rsaKey = RSAKey.Builder(keyPair.public as RSAPublicKey)
            .privateKey(keyPair.private as RSAPrivateKey)
            .keyID(UUID.randomUUID().toString())
            .build()
        return ImmutableJWKSet(JWKSet(rsaKey))
    }

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

    companion object {
        private const val RSA_KEY_SIZE = 2048
    }
}
