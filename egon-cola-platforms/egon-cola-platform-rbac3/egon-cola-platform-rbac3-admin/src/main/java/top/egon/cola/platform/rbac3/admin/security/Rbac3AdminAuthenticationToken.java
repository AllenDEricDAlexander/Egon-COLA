package top.egon.cola.platform.rbac3.admin.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.security.Rbac3ContextAuthentication;

import java.util.Objects;

/** RBAC3 Admin compatibility principal backed by the unified runtime context. */
public final class Rbac3AdminAuthenticationToken extends AbstractAuthenticationToken
        implements Rbac3ContextAuthentication {

    private final CurrentRbac3Principal principal;
    private final AuthorizationService.RuntimeAuthorizationContext context;

    public Rbac3AdminAuthenticationToken(
            CurrentRbac3Principal principal,
            AuthorizationService.RuntimeAuthorizationContext context) {
        super(Objects.requireNonNull(principal, "principal").authorities());
        this.principal = principal;
        this.context = Objects.requireNonNull(context, "context");
        setAuthenticated(true);
    }

    @Override
    public CurrentRbac3Principal getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public String getName() {
        return principal.identitySub();
    }

    @Override
    public AuthorizationService.RuntimeAuthorizationContext context() {
        return context;
    }
}
