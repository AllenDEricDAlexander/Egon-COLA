package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain;

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
