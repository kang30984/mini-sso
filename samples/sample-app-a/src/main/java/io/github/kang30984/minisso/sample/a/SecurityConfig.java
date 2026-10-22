package io.github.kang30984.minisso.sample.a;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrations)
            throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2Login(login -> login.authorizationEndpoint(
                        endpoint -> endpoint.authorizationRequestResolver(pkceResolver(clientRegistrations))))
                .logout(logout -> logout.logoutSuccessHandler(ssoLogoutHandler(clientRegistrations)))
                // 다른 서비스에서 로그아웃하면 인증 서버가 보내는 Logout Token을 받아 이 앱의 세션도 끊는다
                // 수신 주소: /logout/connect/back-channel/mini-sso
                .oidcLogout(logout -> logout.backChannel(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * mini-sso는 기밀 클라이언트에도 PKCE를 요구한다 (ADR-010).
     * Spring Security는 기밀 클라이언트에 PKCE를 기본으로 붙이지 않으므로 명시적으로 켠다.
     */
    private DefaultOAuth2AuthorizationRequestResolver pkceResolver(ClientRegistrationRepository clientRegistrations) {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrations, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    /**
     * 앱에서 로그아웃하면 인증 서버의 /connect/logout(RP-Initiated Logout)으로 이동해 SSO 세션까지 끊는다.
     * 인증 서버는 같은 세션으로 로그인한 다른 서비스에 Back-Channel Logout을 보낸다.
     */
    private OidcClientInitiatedLogoutSuccessHandler ssoLogoutHandler(ClientRegistrationRepository clientRegistrations) {
        var handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrations);
        handler.setPostLogoutRedirectUri("{baseUrl}/");
        return handler;
    }
}
