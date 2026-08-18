package top.egon.cola.platform.rbac3.starter.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;

import java.util.Objects;

/**
 * Authenticated RBAC3 request token whose principal is the unified UserDetails projection.
 */
public final class Rbac3AuthenticationToken extends AbstractAuthenticationToken
        implements Rbac3ContextAuthentication {

    private final Rbac3UserDetails principal;
    private final AuthorizationService.RuntimeAuthorizationContext context;

    /**
     * Compatibility constructor for callers that already hold the runtime context.
     */
    public Rbac3AuthenticationToken(
            AuthorizationService.RuntimeAuthorizationContext context) {
        this(new Rbac3UserDetails(
                        context.identity(),
                        context.snapshot()),
                context);
    }

    /**
     * Creates an authenticated token from the unified UserDetails principal.
     */
    public Rbac3AuthenticationToken(Rbac3UserDetails principal) {
        this(
                Objects.requireNonNull(principal, "principal"),
                new AuthorizationService.RuntimeAuthorizationContext(
                        principal.identity(), principal.snapshot(), false));
    }

    private Rbac3AuthenticationToken(
            Rbac3UserDetails principal,
            AuthorizationService.RuntimeAuthorizationContext context) {
        super(principal.getAuthorities());
        this.principal = Objects.requireNonNull(principal, "principal");
        this.context = Objects.requireNonNull(context, "context");
        setAuthenticated(true);
    }

    @Override
    public AuthorizationService.RuntimeAuthorizationContext context() {
        return context;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Rbac3UserDetails getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.getUsername();
    }
}
