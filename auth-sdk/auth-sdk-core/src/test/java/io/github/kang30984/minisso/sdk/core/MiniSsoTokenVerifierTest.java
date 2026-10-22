package io.github.kang30984.minisso.sdk.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MiniSsoTokenVerifierTest {

    private static final String ISSUER = "http://localhost:9000";

    private final RSAKey currentKey = generate("key-current");
    private final RSAKey previousKey = generate("key-previous");
    private final RSAKey unknownKey = generate("key-unknown");

    private final MiniSsoTokenVerifier verifier = new MiniSsoTokenVerifier(
            new ImmutableJWKSet<>(new JWKSet(List.of(currentKey.toPublicJWK(), previousKey.toPublicJWK()))),
            MiniSsoVerifierSettings.forIssuer(ISSUER, Set.of("sample-app-a")));

    @Test
    void 유효한_토큰에서_사용자_정보를_꺼낸다() throws Exception {
        VerifiedToken token = verifier.verify(sign(currentKey, claims().build()));

        AuthUser user = token.user();
        assertThat(user.accountId()).isEqualTo("42");
        assertThat(user.roles()).containsExactly("USER");
        assertThat(user.scopes()).containsExactlyInAnyOrder("openid", "profile");
        assertThat(user.clientId()).isEqualTo("sample-app-a");
        assertThat(user.hasScope("profile")).isTrue();
        assertThat(token.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void 교체_전_키로_서명된_토큰도_kid로_골라_검증한다() throws Exception {
        assertThat(verifier.verify(sign(previousKey, claims().build())).user().accountId()).isEqualTo("42");
    }

    @Test
    void 만료된_토큰은_EXPIRED() {
        Instant past = Instant.now().minus(Duration.ofHours(1));
        String token = sign(currentKey, claims().issueTime(Date.from(past.minusSeconds(600))).expirationTime(Date.from(past)).build());

        assertReason(token, TokenVerificationException.Reason.EXPIRED);
    }

    @Test
    void 다른_issuer의_토큰은_INVALID_CLAIMS() {
        assertReason(sign(currentKey, claims().issuer("http://evil.example.com").build()),
                TokenVerificationException.Reason.INVALID_CLAIMS);
    }

    @Test
    void 허용하지_않은_클라이언트의_토큰은_INVALID_CLAIMS() {
        assertReason(sign(currentKey, claims().audience("other-app").build()),
                TokenVerificationException.Reason.INVALID_CLAIMS);
    }

    @Test
    void 게시되지_않은_키로_서명된_토큰은_INVALID_SIGNATURE() {
        assertReason(sign(unknownKey, claims().build()), TokenVerificationException.Reason.INVALID_SIGNATURE);
    }

    @Test
    void 서명을_위조한_토큰은_INVALID_SIGNATURE() {
        String token = sign(currentKey, claims().build());
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "AAAA" + token.substring(token.lastIndexOf('.') + 5);

        assertReason(tampered, TokenVerificationException.Reason.INVALID_SIGNATURE);
    }

    @Test
    void JWT가_아니면_MALFORMED() {
        assertReason("not-a-jwt", TokenVerificationException.Reason.MALFORMED);
        assertReason("", TokenVerificationException.Reason.MALFORMED);
    }

    private void assertReason(String token, TokenVerificationException.Reason reason) {
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(TokenVerificationException.class)
                .extracting(e -> ((TokenVerificationException) e).reason())
                .isEqualTo(reason);
    }

    private static JWTClaimsSet.Builder claims() {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject("42")
                .audience("sample-app-a")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofMinutes(10))))
                .claim("roles", List.of("USER"))
                .claim("scope", List.of("openid", "profile"));
    }

    private static String sign(RSAKey key, JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
            jwt.sign(new RSASSASigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RSAKey generate(String kid) {
        try {
            return new RSAKeyGenerator(2048).keyID(kid).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
