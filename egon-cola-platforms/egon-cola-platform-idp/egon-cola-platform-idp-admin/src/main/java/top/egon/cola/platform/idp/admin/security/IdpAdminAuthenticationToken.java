package top.egon.cola.platform.idp.admin.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.List;
import java.util.Objects;

public final class IdpAdminAuthenticationToken
        extends AbstractAuthenticationToken {

    private final IdentityPrincipal principal;
    private final String token;

    public IdpAdminAuthenticationToken(
            IdentityPrincipal principal,
            String token
    ) {
        super(List.of());
        this.principal = Objects.requireNonNull(principal, "principal");
        this.token = Objects.requireNonNull(token, "token");
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public IdentityPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.subject();
    }
}
