package top.egon.cola.component.gateway.mcp.task;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Durable MCP task snapshot. Bearer credentials are intentionally excluded.
 */
public record McpTask(
        String id,
        String principalFingerprint,
        String subjectId,
        String tenantId,
        String clientId,
        String serverCode,
        String toolName,
        String requestDigest,
        State state,
        Map<String, Object> inputPayload,
        Map<String, Object> resultPayload,
        Map<String, Object> errorPayload,
        String workerOwner,
        Instant leaseUntil,
        Instant executionDeadline,
        Instant expiresAt,
        int attemptCount,
        int maxAttempts,
        long revision,
        Instant createdAt,
        Instant updatedAt
) {

    public McpTask {
        id = required(id, "id");
        if (id.length() > 64) {
            throw new IllegalArgumentException("task id is too long");
        }
        principalFingerprint = required(
                principalFingerprint,
                "principalFingerprint"
        );
        subjectId = required(subjectId, "subjectId");
        tenantId = required(tenantId, "tenantId");
        clientId = required(clientId, "clientId");
        serverCode = required(serverCode, "serverCode");
        toolName = required(toolName, "toolName");
        requestDigest = digest(requestDigest);
        state = Objects.requireNonNull(state, "state");
        inputPayload = inputPayload == null ? Map.of() : Map.copyOf(
                inputPayload
        );
        resultPayload = copy(resultPayload);
        errorPayload = copy(errorPayload);
        workerOwner = optional(workerOwner);
        if ((workerOwner == null) != (leaseUntil == null)) {
            throw new IllegalArgumentException(
                    "workerOwner and leaseUntil must be set together"
            );
        }
        executionDeadline = Objects.requireNonNull(
                executionDeadline,
                "executionDeadline"
        );
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (!executionDeadline.isAfter(createdAt)
                || !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                    "task deadlines must be after creation"
            );
        }
        if (attemptCount < 0 || maxAttempts < 1
                || attemptCount > maxAttempts || revision < 0) {
            throw new IllegalArgumentException(
                    "task attempts or revision are invalid"
            );
        }
    }

    public boolean terminal() {
        return Set.of(
                State.COMPLETED,
                State.FAILED,
                State.CANCELLED
        ).contains(state);
    }

    public enum State {
        WORKING,
        INPUT_REQUIRED,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    private static Map<String, Object> copy(Map<String, Object> value) {
        return value == null ? null : Map.copyOf(value);
    }

    private static String digest(String value) {
        String result = required(value, "requestDigest");
        if (!result.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "requestDigest must contain a SHA-256 digest"
            );
        }
        return result;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
