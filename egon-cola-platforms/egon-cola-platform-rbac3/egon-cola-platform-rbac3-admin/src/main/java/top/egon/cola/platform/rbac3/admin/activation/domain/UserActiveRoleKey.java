package top.egon.cola.platform.rbac3.admin.activation.domain;

import java.io.Serializable;

/**
 * Composite key for the user-scoped active-root projection.
 */
public record UserActiveRoleKey(
        Long tenantId,
        Long userId,
        Long applicationId,
        Long rootRoleId) implements Serializable {
}
