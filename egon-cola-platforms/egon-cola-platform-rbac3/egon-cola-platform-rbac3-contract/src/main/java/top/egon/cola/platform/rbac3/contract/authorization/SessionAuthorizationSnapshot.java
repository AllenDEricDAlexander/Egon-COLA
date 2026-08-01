package top.egon.cola.platform.rbac3.contract.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record SessionAuthorizationSnapshot(
        String sessionId,
        long authVersion,
        long sessionVersion,
        long policyVersion,
        List<AppAuthorizationContext> appContexts,
        String checksum,
        Instant generatedAt
) {

    public SessionAuthorizationSnapshot {
        sessionId = required(sessionId, "sessionId");
        nonNegative(authVersion, "authVersion");
        nonNegative(sessionVersion, "sessionVersion");
        nonNegative(policyVersion, "policyVersion");
        appContexts = List.copyOf(Objects.requireNonNull(
                appContexts,
                "appContexts"
        ));
        checksum = required(checksum, "checksum");
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static void nonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }
    }
}
