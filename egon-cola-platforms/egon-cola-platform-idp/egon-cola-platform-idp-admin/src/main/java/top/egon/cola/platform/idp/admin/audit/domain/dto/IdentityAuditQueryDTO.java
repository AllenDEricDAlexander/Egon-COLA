package top.egon.cola.platform.idp.admin.audit.domain.dto;

import java.time.Instant;
import java.util.Set;

/**
 * 身份安全审计的分页查询条件。
 *
 * <p>Security-audit criteria; time bounds are inclusive from and exclusive to.</p>
 */
public record IdentityAuditQueryDTO(
        int page, int size, String actorSub, String eventType, String result,
        Instant from, Instant to, String traceId) {

    public IdentityAuditQueryDTO(int page, int size) {
        this(page, size, null, null, null, null, null, null);
    }

    public IdentityAuditQueryDTO {
        actorSub = optional(actorSub);
        eventType = optional(eventType);
        result = optional(result);
        traceId = optional(traceId);
        if (result != null && !Set.of("SUCCESS", "FAILURE").contains(result)) {
            throw new IllegalArgumentException("invalid audit result filter");
        }
        if (from != null && to != null && !to.isAfter(from)) {
            throw new IllegalArgumentException("invalid audit time range");
        }
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
