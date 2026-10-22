package io.github.kang30984.minisso.sdk.core;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 검증된 액세스 토큰이 나타내는 사용자.
 *
 * @param accountId 계정 ID (토큰의 sub). 서비스 간 사용자 식별자로 이 값을 쓴다.
 * @param roles     인증 서버가 부여한 역할 (roles 클레임)
 * @param scopes    클라이언트가 위임받은 범위 (scope 클레임)
 * @param clientId  토큰을 발급받은 클라이언트 (aud 클레임의 첫 값)
 * @param expiresAt 토큰 만료 시각
 */
public record AuthUser(
        String accountId,
        List<String> roles,
        Set<String> scopes,
        String clientId,
        Instant expiresAt) {

    public AuthUser {
        roles = List.copyOf(roles);
        scopes = Set.copyOf(scopes);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }

    /** 검증된 토큰의 클레임으로 만든다. Spring Security의 Jwt 클레임 맵에도 그대로 쓸 수 있다. */
    public static AuthUser fromClaims(Map<String, Object> claims) {
        return new AuthUser(
                String.valueOf(claims.get("sub")),
                stringList(claims.get("roles")),
                Set.copyOf(stringList(claims.get("scope"))),
                stringList(claims.get("aud")).stream().findFirst().orElse(null),
                toInstant(claims.get("exp")));
    }

    private static List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        // scope는 공백 구분 문자열로 올 수도 있다 (RFC 9068)
        return List.of(String.valueOf(value).split(" ")).stream()
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }
        if (value instanceof Number seconds) {
            return Instant.ofEpochSecond(seconds.longValue());
        }
        return null;
    }
}
