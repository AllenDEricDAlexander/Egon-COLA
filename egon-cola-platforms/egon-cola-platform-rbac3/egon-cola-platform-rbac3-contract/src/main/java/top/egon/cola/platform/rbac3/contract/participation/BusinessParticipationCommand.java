package top.egon.cola.platform.rbac3.contract.participation;

import java.time.Instant;
import java.util.Objects;

public record BusinessParticipationCommand(
        String applicationCode,
        String businessResource,
        String businessId,
        String actorUserId,
        String actionCode,
        String businessEventId,
        Instant occurredAt,
        String traceId
) {

    public BusinessParticipationCommand {
        applicationCode = required(applicationCode, "applicationCode");
        businessResource = required(
                businessResource,
                "businessResource"
        );
        businessId = required(businessId, "businessId");
        actorUserId = required(actorUserId, "actorUserId");
        actionCode = required(actionCode, "actionCode");
        businessEventId = required(businessEventId, "businessEventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        traceId = required(traceId, "traceId");
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
