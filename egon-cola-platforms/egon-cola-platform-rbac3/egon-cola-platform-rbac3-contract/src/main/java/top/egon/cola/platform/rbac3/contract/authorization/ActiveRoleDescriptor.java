package top.egon.cola.platform.rbac3.contract.authorization;

import java.util.Objects;

/**
 * Stable role facts that are eligible for the current authorization context.
 *
 * <p>Only roles that are valid, explicitly active, and otherwise eligible are exposed. This
 * descriptor contains no permission payload and no tenant-owned mutable state.</p>
 */
public record ActiveRoleDescriptor(
        String roleId,
        String roleCode,
        String applicationCode) {

    public ActiveRoleDescriptor {
        roleId = required(roleId, "roleId");
        roleCode = required(roleCode, "roleCode");
        applicationCode = required(applicationCode, "applicationCode");
    }

    private static String required(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
