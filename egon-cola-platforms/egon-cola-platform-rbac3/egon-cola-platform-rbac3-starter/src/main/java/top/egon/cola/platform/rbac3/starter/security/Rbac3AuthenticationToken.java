package top.egon.cola.platform.rbac3.starter.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Authenticated request principal carrying only the validated runtime context.
 */
public final class Rbac3AuthenticationToken extends AbstractAuthenticationToken
        implements Rbac3ContextAuthentication {

    private final AuthorizationService.RuntimeAuthorizationContext context;

    public Rbac3AuthenticationToken(
            AuthorizationService.RuntimeAuthorizationContext context
    ) {
        super(authorities(context));
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
    public IdentityPrincipal getPrincipal() {
        return context.identity();
    }

    @Override
    public String getName() {
        return context.identity().subject();
    }

    private static List<GrantedAuthority> authorities(
            AuthorizationService.RuntimeAuthorizationContext context) {
        Objects.requireNonNull(context, "context");
        LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>();
        context.snapshot().permissions().stream().sorted().forEach(permission -> {
            authorities.add(new SimpleGrantedAuthority("RBAC3_" + permission));
            authorities.add(new SimpleGrantedAuthority("CAP_" + permission));
        });
        return List.copyOf(authorities);
    }
}
