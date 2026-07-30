package top.egon.cola.component.ddc.admin.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class DdcAdminJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        addAuthorities(jwt.getClaim("capabilities"), "CAP_", authorities);
        addAuthorities(jwt.getClaim("roles"), "ROLE_", authorities);
        return new JwtAuthenticationToken(
                jwt,
                Set.copyOf(authorities),
                jwt.getSubject()
        );
    }

    private void addAuthorities(
            Object claim,
            String prefix,
            Set<GrantedAuthority> authorities) {
        if (claim instanceof Collection<?> values) {
            values.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(value ->
                            (GrantedAuthority) new SimpleGrantedAuthority(
                                    prefix + value
                            ))
                    .forEach(authorities::add);
            return;
        }
        if (claim instanceof String value) {
            for (String item : value.split("[,\\s]+")) {
                if (!item.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(
                            prefix + item.trim()
                    ));
                }
            }
        }
    }
}
