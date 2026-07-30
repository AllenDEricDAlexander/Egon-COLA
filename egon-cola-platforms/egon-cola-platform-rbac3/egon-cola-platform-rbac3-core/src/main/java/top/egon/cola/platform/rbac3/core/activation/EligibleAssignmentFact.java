package top.egon.cola.platform.rbac3.core.activation;

import java.time.Instant;

public record EligibleAssignmentFact(
        String id,
        String userId,
        String roleId,
        Status status,
        Instant validFrom,
        Instant validTo
) {

    public EligibleAssignmentFact {
        id = required(id, "id");
        userId = required(userId, "userId");
        roleId = required(roleId, "roleId");
        if (status == null || validFrom == null) {
            throw new IllegalArgumentException("status and validFrom are required");
        }
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
    }

    public boolean eligibleAt(Instant now) {
        return status == Status.ACTIVE
                && !validFrom.isAfter(now)
                && (validTo == null || now.isBefore(validTo));
    }

    public enum Status {
        PENDING,
        ACTIVE,
        SUSPENDED,
        EXPIRED,
        REVOKED
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
