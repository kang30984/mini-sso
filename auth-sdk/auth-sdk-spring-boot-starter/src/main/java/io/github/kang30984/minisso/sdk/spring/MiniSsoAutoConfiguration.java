package io.github.kang30984.minisso.sdk.spring;

import io.github.kang30984.minisso.sdk.core.MiniSsoTokenVerifier;
import io.github.kang30984.minisso.sdk.core.MiniSsoVerifierSettings;
import io.github.kang30984.minisso.sdk.spring.api.MiniSsoProperties;
import io.github.kang30984.minisso.sdk.spring.internal.CurrentUserArgumentResolver;
import io.github.kang30984.minisso.sdk.spring.internal.MiniSsoAuthoritiesConverter;
import io.github.kang30984.minisso.sdk.spring.internal.MiniSsoJwtDecoder;
import io.github.kang30984.minisso.sdk.spring.internal.ProblemDetailAuthenticationEntryPoint;
import io.github.kang30984.minisso.sdk.spring.internal.RequiresScopeInterceptor;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * mini-sso 리소스 서버 자동 구성.
 *
 * <p>설계 원칙 (DESIGN 9.3)
 * <ul>
 *   <li>모든 빈은 {@code @ConditionalOnMissingBean}: 서비스가 직접 정의하면 SDK 기본값은 물러난다.</li>
 *   <li>공개 API는 {@code api} 패키지의 타입뿐이고, 나머지({@code internal})는 하위 호환을 보장하지 않는다.</li>
 *   <li>검증 로직은 core에만 둔다. 이 모듈은 Spring에 연결하는 역할만 한다.</li>
 * </ul>
 */
@AutoConfiguration(before = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({HttpSecurity.class, WebMvcConfigurer.class})
@EnableConfigurationProperties(MiniSsoProperties.class)
@EnableWebSecurity
public class MiniSsoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MiniSsoTokenVerifier miniSsoTokenVerifier(MiniSsoProperties properties) {
        MiniSsoVerifierSettings settings = MiniSsoVerifierSettings
                .forIssuer(properties.issuerUri(), new HashSet<>(properties.audiences()))
                .withClockSkew(properties.clockSkew())
                .withJwksCacheTtl(properties.jwksCacheTtl())
                .withJwksRefreshMinInterval(properties.jwksRefreshMinInterval());
        if (properties.jwkSetUri() != null && !properties.jwkSetUri().isBlank()) {
            settings = settings.withJwkSetUri(URI.create(properties.jwkSetUri()));
        }
        return MiniSsoTokenVerifier.create(settings);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder miniSsoJwtDecoder(MiniSsoTokenVerifier verifier) {
        return new MiniSsoJwtDecoder(verifier);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter miniSsoJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new MiniSsoAuthoritiesConverter());
        return converter;
    }

    /** 서비스가 SecurityFilterChain을 직접 정의하면 이 기본 체인은 만들지 않는다. */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain miniSsoSecurityFilterChain(
            HttpSecurity http,
            MiniSsoProperties properties,
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        ProblemDetailAuthenticationEntryPoint entryPoint = new ProblemDetailAuthenticationEntryPoint();
        List<String> publicPaths = properties.publicPaths();

        http
                .authorizeHttpRequests(authorize -> {
                    if (!publicPaths.isEmpty()) {
                        authorize.requestMatchers(publicPaths.toArray(String[]::new)).permitAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(ProblemDetailAuthenticationEntryPoint.accessDeniedHandler()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(ProblemDetailAuthenticationEntryPoint.accessDeniedHandler()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 쿠키를 쓰지 않는 Bearer 토큰 API이므로 CSRF 대상이 아니다
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Configuration(proxyBeanMethods = false)
    static class WebMvcConfiguration implements WebMvcConfigurer {

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new CurrentUserArgumentResolver());
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new RequiresScopeInterceptor());
        }
    }
}
