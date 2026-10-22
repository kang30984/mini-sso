package io.github.kang30984.minisso.sdk.spring.internal;

import io.github.kang30984.minisso.sdk.core.AuthUser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** roles 클레임 → ROLE_*, scope 클레임 → SCOPE_* 권한. hasRole('ADMIN'), hasAuthority('SCOPE_profile')로 쓸 수 있다. */
public final class MiniSsoAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        AuthUser user = AuthUser.fromClaims(jwt.getClaims());
        List<GrantedAuthority> authorities = new ArrayList<>();
        user.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        user.scopes().forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));
        return authorities;
    }
}
