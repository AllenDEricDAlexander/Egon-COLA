package top.egon.cola.platform.idp.admin.support.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.LinkedHashSet;
import java.util.Objects;

public final class IdpJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Objects.requireNonNull(jwt, "jwt");
        Object versionValue = jwt.getClaim("token_version");
        if (!(versionValue instanceof Number version)) {
            throw new IllegalArgumentException("token_version is required");
        }
        IdentityPrincipal principal = new IdentityPrincipal(
                jwt.getSubject(),
                jwt.getClaimAsString("tid"),
                jwt.getClaimAsString("sid"),
                jwt.getClaimAsString("client_id"),
                jwt.getId(),
                version.longValue(),
                new LinkedHashSet<>(jwt.getAudience()),
                jwt.getIssuedAt(),
                jwt.getExpiresAt()
        );
        return new IdpAdminAuthenticationToken(
                principal,
                jwt.getTokenValue()
        );
    }
}
