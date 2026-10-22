package io.github.kang30984.minisso.adapter.inbound.oauth2.config

import io.github.kang30984.minisso.adapter.inbound.oauth2.authentication.AccountAuthenticationProvider
import io.github.kang30984.minisso.adapter.inbound.oauth2.authentication.LoginRequestDetailsSource
import io.github.kang30984.minisso.adapter.inbound.oauth2.token.AccountClaimsTokenCustomizer
import io.github.kang30984.minisso.application.account.port.inbound.LoginUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain

@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    /**
     * 관리 API는 세션 없이 액세스 토큰(JWT)으로만 호출한다.
     * 토큰의 roles 클레임이 ROLE_ 권한으로 변환되어 ADMIN만 /api/admin/** 에 접근할 수 있다.
     */
    @Bean
    @Order(2)
    fun apiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests {
                it.requestMatchers("/api/admin/**").hasRole("ADMIN")
                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer { resourceServer ->
                resourceServer.jwt { it.jwtAuthenticationConverter(rolesAuthenticationConverter()) }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            // 쿠키를 쓰지 않는 Bearer 토큰 API이므로 CSRF 대상이 아니다
            .csrf { it.disable() }

        return http.build()
    }

    /** 로그인 폼과 그 외 요청 */
    @Bean
    @Order(3)
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/health", "/error").permitAll()
                it.anyRequest().authenticated()
            }
            .formLogin { it.authenticationDetailsSource(LoginRequestDetailsSource()) }

        return http.build()
    }

    @Bean
    fun accountAuthenticationProvider(loginUseCase: LoginUseCase): AuthenticationProvider =
        AccountAuthenticationProvider(loginUseCase)

    private fun rolesAuthenticationConverter(): JwtAuthenticationConverter {
        val authoritiesConverter = JwtGrantedAuthoritiesConverter().apply {
            setAuthoritiesClaimName(AccountClaimsTokenCustomizer.ROLES_CLAIM)
            setAuthorityPrefix("ROLE_")
        }
        return JwtAuthenticationConverter().apply { setJwtGrantedAuthoritiesConverter(authoritiesConverter) }
    }
}
