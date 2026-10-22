package io.github.kang30984.minisso.sdk.spring.api;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * <pre>
 * mini-sso:
 *   issuer-uri: http://localhost:9000      # 필수
 *   audiences: [ sample-app-a ]            # 이 서비스가 받아들일 클라이언트의 토큰
 *   public-paths: [ /health, /public/** ]  # 인증 없이 허용할 경로
 * </pre>
 *
 * issuer-uri가 없으면 기동 시점에 실패한다. 설정 누락을 운영 중 401 폭증이 아니라 배포 단계에서 발견하기 위함이다.
 */
@ConfigurationProperties("mini-sso")
public record MiniSsoProperties(
        String issuerUri,
        String jwkSetUri,
        @DefaultValue List<String> audiences,
        @DefaultValue List<String> publicPaths,
        @DefaultValue("30s") Duration clockSkew,
        @DefaultValue("5m") Duration jwksCacheTtl,
        @DefaultValue("30s") Duration jwksRefreshMinInterval) {

    public MiniSsoProperties {
        if (issuerUri == null || issuerUri.isBlank()) {
            throw new IllegalArgumentException("mini-sso.issuer-uri는 필수입니다.");
        }
        audiences = List.copyOf(audiences);
        publicPaths = List.copyOf(publicPaths);
    }
}
