package io.github.kang30984.minisso.sdk.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kang30984.minisso.sdk.core.MiniSsoTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

class MiniSsoAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MiniSsoAutoConfiguration.class, SecurityAutoConfiguration.class));

    @Test
    void issuer_uri만_지정하면_토큰_검증과_보안_체인이_구성된다() {
        runner.withPropertyValues("mini-sso.issuer-uri=http://localhost:9000")
                .run(context -> {
                    assertThat(context).hasSingleBean(MiniSsoTokenVerifier.class);
                    assertThat(context).hasSingleBean(JwtDecoder.class);
                    assertThat(context).hasBean("miniSsoSecurityFilterChain");
                });
    }

    @Test
    void issuer_uri가_없으면_기동에_실패한다() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void 서비스가_보안_체인을_직접_정의하면_SDK_기본_체인은_만들지_않는다() {
        runner.withPropertyValues("mini-sso.issuer-uri=http://localhost:9000")
                .withUserConfiguration(CustomSecurity.class)
                .run(context -> {
                    assertThat(context).hasBean("customChain");
                    assertThat(context).doesNotHaveBean("miniSsoSecurityFilterChain");
                    // 검증기와 디코더는 그대로 재사용할 수 있다
                    assertThat(context).hasSingleBean(JwtDecoder.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomSecurity {
        @Bean
        SecurityFilterChain customChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll()).build();
        }
    }
}
