package top.egon.cola.platform.tianquan.jianshen.starter.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Authenticated Tianquan-Jianshen request token whose principal is the unified UserDetails projection.
 */
public final class Rbac3AuthenticationToken extends AbstractAuthenticationToken
        implements Rbac3ContextAuthentication {

    private final Rbac3UserDetails principal;
    private final AuthorizationService.RuntimeAuthorizationContext context;

    /**
     * Compatibility constructor for callers that already hold the runtime context.
     */
    Rbac3AuthenticationToken(
            AuthorizationService.RuntimeAuthorizationContext context) {
        this(new Rbac3UserDetails(
                        context.identity(),
                        context.snapshot()),
                context,
                List.of());
    }

    /**
     * Creates an authenticated token from the unified UserDetails principal.
     */
    public Rbac3AuthenticationToken(Rbac3UserDetails principal) {
        this(
                Objects.requireNonNull(principal, "principal"),
                new AuthorizationService.RuntimeAuthorizationContext(
                        principal.identity(), principal.snapshot(), false),
                List.of());
    }

    /**
     * Creates an authenticated token with verified OAuth scopes kept separate from the RBAC snapshot.
     */
    public Rbac3AuthenticationToken(
            Rbac3UserDetails principal,
            Collection<? extends GrantedAuthority> verifiedScopeAuthorities) {
        this(
                Objects.requireNonNull(principal, "principal"),
                new AuthorizationService.RuntimeAuthorizationContext(
                        principal.identity(), principal.snapshot(), false),
                verifiedScopeAuthorities);
    }

    private Rbac3AuthenticationToken(
            Rbac3UserDetails principal,
            AuthorizationService.RuntimeAuthorizationContext context,
            Collection<? extends GrantedAuthority> verifiedScopeAuthorities) {
        super(authorities(principal, verifiedScopeAuthorities));
        this.principal = Objects.requireNonNull(principal, "principal");
        this.context = Objects.requireNonNull(context, "context");
        setAuthenticated(true);
    }

    private static List<GrantedAuthority> authorities(
            Rbac3UserDetails principal,
            Collection<? extends GrantedAuthority> verifiedScopeAuthorities) {
        LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>(
                Objects.requireNonNull(principal, "principal").getAuthorities());
        Objects.requireNonNull(verifiedScopeAuthorities, "verifiedScopeAuthorities")
                .stream()
                .filter(authority -> authority != null
                        && authority.getAuthority() != null
                        && authority.getAuthority().startsWith("SCOPE_"))
                .forEach(authorities::add);
        return List.copyOf(authorities);
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
