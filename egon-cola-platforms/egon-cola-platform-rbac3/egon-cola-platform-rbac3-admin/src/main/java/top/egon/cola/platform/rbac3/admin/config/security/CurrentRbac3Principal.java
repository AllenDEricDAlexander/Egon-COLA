package top.egon.cola.platform.rbac3.admin.config.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;

/**
 * The RBAC3 request principal projected from the IdP identity and the current
 * user authorization snapshot.
 *
 * <p>This is deliberately user-scoped.  There is no server-side identity
 * session in RBAC3; invalidation is represented by the user
 * authorization version and the short-lived IdP access token.</p>
 */
public record CurrentRbac3Principal(
        String tenantId,
        String identitySub,
        String userId,
        long authVersion,
        long policyVersion,
        Set<String> permissions,
        boolean platformAdministrator) {

    /** Reads the current USER authorization facts without controller parameter injection. */
    public static CurrentRbac3Principal requireCurrent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException(
                    "RBAC3 USER principal is required");
        }
        if (authentication.getPrincipal() instanceof CurrentRbac3Principal principal) {
            return principal;
        }
        if (authentication.getPrincipal() instanceof Rbac3UserDetails details) {
            return new CurrentRbac3Principal(
                    details.tenantId(), details.identitySub(), details.rbac3UserId(),
                    details.authVersion(), details.policyVersion(), details.permissions(), false);
        }
        throw new AuthenticationCredentialsNotFoundException(
                "RBAC3 USER principal is required");
    }

    public CurrentRbac3Principal {
        tenantId = required(tenantId, "tenantId");
        identitySub = required(identitySub, "identitySub");
        userId = required(userId, "userId");
        permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions"));
        if (authVersion < 0 || policyVersion < 0) {
            throw new IllegalArgumentException("authorization versions must be non-negative");
        }
    }

    public CurrentRbac3Principal(
            String tenantId,
            String userId,
            long authVersion,
            long policyVersion,
            Set<String> permissions,
            boolean platformAdministrator) {
        this(tenantId, userId, userId, authVersion, policyVersion,
                permissions, platformAdministrator);
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return permissions.stream()
                .sorted()
                .map(permission -> new SimpleGrantedAuthority("RBAC3_" + permission))
                .toList();
    }

    public boolean hasPermission(String permission) {
        return permission != null && permissions.contains(permission);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
