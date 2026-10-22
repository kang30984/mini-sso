package io.github.kang30984.minisso.sdk.core;

/** 토큰 검증 실패. 사유별로 다른 대응(재로그인, 재시도 등)을 할 수 있도록 {@link Reason}을 제공한다. */
public class TokenVerificationException extends Exception {

    public enum Reason {
        /** JWT 형식이 아님 */
        MALFORMED,
        /** 만료됨. 클라이언트는 리프레시 토큰으로 갱신하면 된다. */
        EXPIRED,
        /** 서명 검증 실패 또는 알 수 없는 키 */
        INVALID_SIGNATURE,
        /** issuer, audience 등 클레임 불일치 */
        INVALID_CLAIMS,
        /** JWKS를 가져올 수 없음 (캐시된 키도 없음) */
        KEY_SOURCE_UNAVAILABLE,
    }

    private final Reason reason;

    public TokenVerificationException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
