package top.egon.cola.platform.rbac3.starter.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;

import java.util.List;
import java.util.Objects;

/**
 * Authenticated request principal carrying only the validated runtime context.
 */
public final class Rbac3AuthenticationToken extends AbstractAuthenticationToken {

    private final AuthorizationService.RuntimeAuthorizationContext context;

    public Rbac3AuthenticationToken(
            AuthorizationService.RuntimeAuthorizationContext context
    ) {
        super(List.of());
        this.context = Objects.requireNonNull(context, "context");
        setAuthenticated(true);
    }

    public AuthorizationService.RuntimeAuthorizationContext context() {
        return context;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return context.claims().sub();
    }
}
