package io.github.kang30984.minisso.sdk.spring.internal;

import io.github.kang30984.minisso.sdk.core.MiniSsoTokenVerifier;
import io.github.kang30984.minisso.sdk.core.TokenVerificationException;
import io.github.kang30984.minisso.sdk.core.VerifiedToken;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/** core의 검증기를 Spring Security 리소스 서버에 연결한다. 검증 로직은 core에만 있다. */
public final class MiniSsoJwtDecoder implements JwtDecoder {

    private final MiniSsoTokenVerifier verifier;

    public MiniSsoJwtDecoder(MiniSsoTokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            VerifiedToken verified = verifier.verify(token);
            return new Jwt(verified.tokenValue(), verified.issuedAt(), verified.expiresAt(), verified.headers(), verified.claims());
        } catch (TokenVerificationException e) {
            throw new BadJwtException(e.reason() + ": " + e.getMessage(), e);
        }
    }
}
