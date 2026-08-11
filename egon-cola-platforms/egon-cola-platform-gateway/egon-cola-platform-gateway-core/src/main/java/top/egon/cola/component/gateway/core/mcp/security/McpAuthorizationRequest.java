package top.egon.cola.component.gateway.core.mcp.security;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public record McpAuthorizationRequest(
        String issuer,
        String subjectId,
        String tenantId,
        String sessionId,
        String clientId,
        String tokenId,
        long tokenVersion,
        String resourceUri,
        Instant issuedAt,
        Instant expiresAt,
        Set<String> requiredPermissions,
        long minimumAuthVersion,
        long minimumContextVersion,
        long minimumPolicyVersion
) {

    public McpAuthorizationRequest {
        issuer = required(issuer, "issuer");
        subjectId = required(subjectId, "subjectId");
        tenantId = required(tenantId, "tenantId");
        sessionId = required(sessionId, "sessionId");
        clientId = required(clientId, "clientId");
        tokenId = required(tokenId, "tokenId");
        nonNegative(tokenVersion, "tokenVersion");
        resourceUri = required(resourceUri, "resourceUri");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after issuedAt"
            );
        }
        requiredPermissions = sorted(
                requiredPermissions,
                "requiredPermissions"
        );
        nonNegative(minimumAuthVersion, "minimumAuthVersion");
        nonNegative(minimumContextVersion, "minimumContextVersion");
        nonNegative(minimumPolicyVersion, "minimumPolicyVersion");
    }

    private static Set<String> sorted(Set<String> values, String field) {
        Objects.requireNonNull(values, field);
        if (values.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        TreeSet<String> normalized = new TreeSet<>();
        values.forEach(value -> normalized.add(required(value, field)));
        return Set.copyOf(normalized);
    }

    private static void nonNegative(long value, String field) {
        if (value < 0L) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
