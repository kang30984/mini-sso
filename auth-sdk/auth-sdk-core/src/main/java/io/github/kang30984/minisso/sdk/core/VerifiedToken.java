package io.github.kang30984.minisso.sdk.core;

import java.time.Instant;
import java.util.Map;

/**
 * 서명과 클레임 검증을 통과한 토큰.
 * 시각 클레임(iat, exp, nbf)은 {@link Instant}로 변환되어 있다.
 */
public record VerifiedToken(
        String tokenValue,
        Map<String, Object> headers,
        Map<String, Object> claims,
        Instant issuedAt,
        Instant expiresAt) {

    public VerifiedToken {
        headers = Map.copyOf(headers);
        claims = Map.copyOf(claims);
    }

    public AuthUser user() {
        return AuthUser.fromClaims(claims);
    }
}
