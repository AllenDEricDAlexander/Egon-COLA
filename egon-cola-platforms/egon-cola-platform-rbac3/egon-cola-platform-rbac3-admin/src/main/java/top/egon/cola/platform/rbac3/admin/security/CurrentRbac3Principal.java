package top.egon.cola.platform.rbac3.admin.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Set;

public record CurrentRbac3Principal(
        String tenantId,
        String identitySub,
        String userId,
        String sessionId,
        long authVersion,
        long sessionVersion,
        long policyVersion,
        Set<String> permissions,
        boolean platformAdministrator
) {

    public CurrentRbac3Principal {
        tenantId = required(tenantId, "tenantId");
        identitySub = required(identitySub, "identitySub");
        userId = required(userId, "userId");
        sessionId = required(sessionId, "sessionId");
        permissions = Set.copyOf(permissions);
    }

    public CurrentRbac3Principal(
            String tenantId,
            String userId,
            String sessionId,
            long authVersion,
            long sessionVersion,
            long policyVersion,
            Set<String> permissions,
            boolean platformAdministrator) {
        this(tenantId, userId, userId, sessionId, authVersion, sessionVersion,
                policyVersion, permissions, platformAdministrator);
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return permissions.stream()
                .sorted()
                .map(permission -> new SimpleGrantedAuthority("RBAC3_" + permission))
                .toList();
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
