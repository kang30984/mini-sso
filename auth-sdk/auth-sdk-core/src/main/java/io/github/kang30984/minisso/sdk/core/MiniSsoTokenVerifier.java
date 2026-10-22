package io.github.kang30984.minisso.sdk.core;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.BadJWSException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * mini-sso가 발급한 액세스 토큰(JWT)을 검증한다. 스레드 안전하므로 애플리케이션에서 하나만 만들어 공유한다.
 *
 * <ul>
 *   <li>서명: JWKS의 공개키로 RS256 서명을 검증한다. 토큰의 kid로 키를 고르므로 키 교체 중에도 검증된다.</li>
 *   <li>공개키 조회: 캐시하고, 모르는 kid가 오면 재조회하되 최소 간격을 둔다.
 *       인증 서버 장애 시에는 마지막으로 받은 키로 일정 시간 계속 검증한다.</li>
 *   <li>클레임: iss 일치, aud 허용 목록, exp/nbf(시계 오차 허용), sub/exp/iat 필수</li>
 * </ul>
 */
public final class MiniSsoTokenVerifier {

    private static final long CACHE_REFRESH_TIMEOUT_MILLIS = 5_000;

    private final DefaultJWTProcessor<SecurityContext> processor;

    /** 테스트나 특수한 키 공급원(파일, 고정 키)을 쓸 때 사용한다. */
    public MiniSsoTokenVerifier(JWKSource<SecurityContext> jwkSource, MiniSsoVerifierSettings settings) {
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));

        // Nimbus는 내부에서 contains(null)을 호출하는데, Set.of/Set.copyOf로 만든 불변 Set은 이때 NPE를 던진다.
        // 그래서 null 조회를 허용하는 HashSet으로 넘긴다.
        DefaultJWTClaimsVerifier<SecurityContext> claimsVerifier = new DefaultJWTClaimsVerifier<>(
                settings.audiences().isEmpty() ? null : new HashSet<>(settings.audiences()),
                new JWTClaimsSet.Builder().issuer(settings.issuer()).build(),
                new HashSet<>(Set.of("sub", "exp", "iat")),
                null);
        claimsVerifier.setMaxClockSkew((int) settings.clockSkew().toSeconds());
        jwtProcessor.setJWTClaimsSetVerifier(claimsVerifier);

        this.processor = jwtProcessor;
    }

    /** 원격 JWKS를 쓰는 일반적인 방법. */
    public static MiniSsoTokenVerifier create(MiniSsoVerifierSettings settings) {
        return new MiniSsoTokenVerifier(remoteJwkSource(settings), settings);
    }

    public VerifiedToken verify(String token) throws TokenVerificationException {
        if (token == null || token.isBlank()) {
            throw new TokenVerificationException(TokenVerificationException.Reason.MALFORMED, "토큰이 없습니다.", null);
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWTClaimsSet claims = processor.process(jwt, null);
            return new VerifiedToken(
                    token,
                    jwt.getHeader().toJSONObject(),
                    toInstantClaims(claims.getClaims()),
                    toInstant(claims.getIssueTime()),
                    toInstant(claims.getExpirationTime()));
        } catch (ParseException e) {
            throw new TokenVerificationException(TokenVerificationException.Reason.MALFORMED, "JWT 형식이 아닙니다.", e);
        } catch (BadJWTException e) {
            TokenVerificationException.Reason reason = "Expired JWT".equals(e.getMessage())
                    ? TokenVerificationException.Reason.EXPIRED
                    : TokenVerificationException.Reason.INVALID_CLAIMS;
            throw new TokenVerificationException(reason, e.getMessage(), e);
        } catch (BadJWSException e) {
            throw new TokenVerificationException(TokenVerificationException.Reason.INVALID_SIGNATURE, e.getMessage(), e);
        } catch (BadJOSEException e) {
            // 일치하는 키가 없는 경우 등
            throw new TokenVerificationException(TokenVerificationException.Reason.INVALID_SIGNATURE, e.getMessage(), e);
        } catch (KeySourceException e) {
            throw new TokenVerificationException(TokenVerificationException.Reason.KEY_SOURCE_UNAVAILABLE, e.getMessage(), e);
        } catch (JOSEException e) {
            throw new TokenVerificationException(TokenVerificationException.Reason.INVALID_SIGNATURE, e.getMessage(), e);
        }
    }

    private static JWKSource<SecurityContext> remoteJwkSource(MiniSsoVerifierSettings settings) {
        try {
            JWKSourceBuilder<SecurityContext> builder = JWKSourceBuilder.<SecurityContext>create(settings.jwkSetUri().toURL())
                    .cache(settings.jwksCacheTtl().toMillis(), CACHE_REFRESH_TIMEOUT_MILLIS)
                    .retrying(true);
            builder = settings.jwksRefreshMinInterval().isZero()
                    ? builder.rateLimited(false)
                    : builder.rateLimited(settings.jwksRefreshMinInterval().toMillis());
            builder = settings.jwksOutageTolerance().isZero()
                    ? builder.outageTolerant(false)
                    : builder.outageTolerant(settings.jwksOutageTolerance().toMillis());
            return builder.build();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("jwkSetUri가 올바르지 않습니다: " + settings.jwkSetUri(), e);
        }
    }

    private static Map<String, Object> toInstantClaims(Map<String, Object> claims) {
        Map<String, Object> converted = new HashMap<>();
        claims.forEach((name, value) -> {
            if (value != null) {
                converted.put(name, value instanceof Date date ? date.toInstant() : value);
            }
        });
        return converted;
    }

    private static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
