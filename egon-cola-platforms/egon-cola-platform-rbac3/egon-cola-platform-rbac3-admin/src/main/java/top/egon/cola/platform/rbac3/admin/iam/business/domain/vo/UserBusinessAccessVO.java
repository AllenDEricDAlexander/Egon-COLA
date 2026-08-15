package top.egon.cola.platform.rbac3.admin.iam.business.domain.vo;

import java.time.Instant;

/** User Business authorization fact enriched with current DDC display fields. */
public record UserBusinessAccessVO(
        String accessId,
        String userId,
        String ddcBusinessId,
        String bizCode,
        String bizName,
        String status,
        Instant validFrom,
        Instant validTo,
        String sourceType,
        String sourceId,
        String reason,
        String ticketNo,
        long version) {
}
