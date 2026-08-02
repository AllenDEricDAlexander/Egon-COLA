package top.egon.cola.platform.idp.starter.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.List;
import java.util.Objects;

/**
 * Authenticated request token carrying only the validated identity principal.
 */
public final class IdpAuthenticationToken
        extends AbstractAuthenticationToken {

    private final IdentityPrincipal principal;

    public IdpAuthenticationToken(IdentityPrincipal principal) {
        super(List.of());
        this.principal = Objects.requireNonNull(principal, "principal");
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public IdentityPrincipal getPrincipal() {
        return principal;
    }
}
