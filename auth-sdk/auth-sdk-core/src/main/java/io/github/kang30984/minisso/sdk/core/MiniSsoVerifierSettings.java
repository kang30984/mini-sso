package io.github.kang30984.minisso.sdk.core;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * 토큰 검증 설정.
 *
 * @param issuer                 토큰의 iss와 정확히 일치해야 하는 값
 * @param jwkSetUri              공개키 조회 주소. 기본값은 {issuer}/oauth2/jwks
 * @param audiences              허용할 클라이언트 ID(aud). 비어 있으면 aud를 검사하지 않는다.
 * @param clockSkew              서버 간 시계 오차 허용 범위
 * @param jwksCacheTtl           공개키 캐시 시간
 * @param jwksRefreshMinInterval 모르는 kid로 인한 재조회의 최소 간격. 위조 토큰 폭주가 인증 서버 부하로 번지지 않게 한다.
 * @param jwksOutageTolerance    인증 서버 장애 시 마지막으로 받은 공개키로 계속 검증하는 시간
 */
public record MiniSsoVerifierSettings(
        String issuer,
        URI jwkSetUri,
        Set<String> audiences,
        Duration clockSkew,
        Duration jwksCacheTtl,
        Duration jwksRefreshMinInterval,
        Duration jwksOutageTolerance) {

    public MiniSsoVerifierSettings {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("issuer는 필수입니다.");
        }
        Objects.requireNonNull(jwkSetUri, "jwkSetUri");
        audiences = Set.copyOf(audiences);
        requireNonNegative(clockSkew, "clockSkew");
        requirePositive(jwksCacheTtl, "jwksCacheTtl");
        requireNonNegative(jwksRefreshMinInterval, "jwksRefreshMinInterval");
        requireNonNegative(jwksOutageTolerance, "jwksOutageTolerance");
    }

    /** 기본값으로 만든다. issuer 끝의 '/'는 무시한다. */
    public static MiniSsoVerifierSettings forIssuer(String issuer, Set<String> audiences) {
        String normalized = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
        return new MiniSsoVerifierSettings(
                normalized,
                URI.create(normalized + "/oauth2/jwks"),
                audiences,
                Duration.ofSeconds(30),
                Duration.ofMinutes(5),
                Duration.ofSeconds(30),
                Duration.ofHours(1));
    }

    public MiniSsoVerifierSettings withJwkSetUri(URI uri) {
        return new MiniSsoVerifierSettings(issuer, uri, audiences, clockSkew, jwksCacheTtl, jwksRefreshMinInterval, jwksOutageTolerance);
    }

    public MiniSsoVerifierSettings withJwksRefreshMinInterval(Duration interval) {
        return new MiniSsoVerifierSettings(issuer, jwkSetUri, audiences, clockSkew, jwksCacheTtl, interval, jwksOutageTolerance);
    }

    public MiniSsoVerifierSettings withClockSkew(Duration skew) {
        return new MiniSsoVerifierSettings(issuer, jwkSetUri, audiences, skew, jwksCacheTtl, jwksRefreshMinInterval, jwksOutageTolerance);
    }

    public MiniSsoVerifierSettings withJwksCacheTtl(Duration ttl) {
        return new MiniSsoVerifierSettings(issuer, jwkSetUri, audiences, clockSkew, ttl, jwksRefreshMinInterval, jwksOutageTolerance);
    }

    private static void requireNonNegative(Duration value, String name) {
        if (value == null || value.isNegative()) {
            throw new IllegalArgumentException(name + "는 0 이상이어야 합니다.");
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(name + "는 0보다 커야 합니다.");
        }
    }
}
