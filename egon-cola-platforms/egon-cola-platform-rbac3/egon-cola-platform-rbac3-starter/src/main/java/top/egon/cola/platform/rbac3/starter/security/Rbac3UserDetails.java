package top.egon.cola.platform.rbac3.starter.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.ActiveRoleDescriptor;
import top.egon.cola.platform.rbac3.contract.authorization.DataScopeDecision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable Spring Security USER principal assembled from the verified IdP identity and one
 * current RBAC authorization snapshot.
 *
 * <p>The password is deliberately absent. This object participates in request authorization only;
 * it is never used for DAO authentication or persisted as a session.</p>
 */
public final class Rbac3UserDetails implements UserDetails {

    private final IdentityPrincipal identity;
    private final SystemAuthorizationSnapshot snapshot;
    private final List<GrantedAuthority> authorities;

    public Rbac3UserDetails(
            IdentityPrincipal identity,
            SystemAuthorizationSnapshot snapshot) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        if (!identity.subject().equals(snapshot.identitySub())
                || !identity.tenantId().equals(snapshot.tenantId())) {
            throw new IllegalArgumentException(
                    "RBAC3 snapshot is not bound to the IdP identity");
        }
        this.authorities = authorities(snapshot.permissions());
    }

    public IdentityPrincipal identity() {
        return identity;
    }

    public SystemAuthorizationSnapshot snapshot() {
        return snapshot;
    }

    public String tenantId() {
        return identity.tenantId();
    }

    public String identitySub() {
        return identity.subject();
    }

    public String rbac3UserId() {
        return snapshot.rbac3UserId();
    }

    public long authVersion() {
        return snapshot.authVersion();
    }

    public long policyVersion() {
        return snapshot.policyVersion();
    }

    public List<ActiveRoleDescriptor> activeRoles() {
        return snapshot.activeRoles();
    }

    public Set<String> permissions() {
        return snapshot.permissions();
    }

    public Map<String, DataScopeDecision> dataScopes() {
        return snapshot.dataScopes();
    }

    public Map<String, FieldPolicyDecision> fieldPolicies() {
        return snapshot.fieldPolicies();
    }

    public boolean hasPermission(String permission) {
        return permission != null && snapshot.permissions().contains(permission);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return identity.subject();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private static List<GrantedAuthority> authorities(Set<String> permissions) {
        LinkedHashSet<GrantedAuthority> values = new LinkedHashSet<>();
        permissions.stream().sorted().forEach(permission -> {
            values.add(new SimpleGrantedAuthority("RBAC3_" + permission));
            values.add(new SimpleGrantedAuthority("CAP_" + permission));
        });
        return List.copyOf(values);
    }
}
