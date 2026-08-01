package top.egon.cola.platform.rbac3.contract.management;

import java.time.Instant;
import java.util.Objects;

public record AssignmentCommand(
        String targetUserId,
        String roleId,
        Instant validFrom,
        Instant validTo,
        String assignmentType,
        String reason,
        String ticketNo,
        long expectedUserAuthVersion
) {

    public AssignmentCommand {
        targetUserId = required(targetUserId, "targetUserId");
        roleId = required(roleId, "roleId");
        validFrom = Objects.requireNonNull(validFrom, "validFrom");
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException(
                    "validTo must be after validFrom"
            );
        }
        assignmentType = required(assignmentType, "assignmentType");
        reason = required(reason, "reason");
        ticketNo = optional(ticketNo, "ticketNo");
        if (expectedUserAuthVersion < 0L) {
            throw new IllegalArgumentException(
                    "expectedUserAuthVersion must not be negative"
            );
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String optional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
        return value.trim();
    }
}
