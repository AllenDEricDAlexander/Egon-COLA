package top.egon.cola.platform.rbac3.admin.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Authenticated machine identity whose authority is bound to one application.
 */
public record CurrentRbac3ServicePrincipal(
        String tenantId,
        String serviceId,
        String applicationCode,
        String environment,
        String namespace,
        String credentialId,
        Set<String> permissions
) {

    public CurrentRbac3ServicePrincipal {
        tenantId = required(tenantId, "tenantId");
        serviceId = required(serviceId, "serviceId");
        applicationCode = required(applicationCode, "applicationCode");
        environment = required(environment, "environment");
        namespace = required(namespace, "namespace");
        credentialId = required(credentialId, "credentialId");
        permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions"));
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

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
