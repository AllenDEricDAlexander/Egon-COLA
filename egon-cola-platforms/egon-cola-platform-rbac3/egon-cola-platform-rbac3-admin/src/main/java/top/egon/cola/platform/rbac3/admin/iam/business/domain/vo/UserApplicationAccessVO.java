package top.egon.cola.platform.rbac3.admin.iam.business.domain.vo;

import java.time.Instant;

/** Derived Application access; it is never persisted as UserApplicationAccess. */
public record UserApplicationAccessVO(
        String applicationId,
        String ddcBusinessId,
        String ddcApplicationId,
        String businessCode,
        String applicationCode,
        String applicationName,
        String applicationStatus,
        Instant observedAt) {
}
