package top.egon.cola.platform.idp.contract;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record IdentityPrincipal(
        String subject,
        String tenantId,
        String sessionId,
        String clientId,
        String tokenId,
        long tokenVersion,
        Set<String> audience,
        Instant issuedAt,
        Instant expiresAt
) {

    public IdentityPrincipal {
        subject = required(subject, "subject");
        tenantId = required(tenantId, "tenantId");
        sessionId = required(sessionId, "sessionId");
        clientId = required(clientId, "clientId");
        tokenId = required(tokenId, "tokenId");
        if (tokenVersion < 0L) {
            throw new IllegalArgumentException(
                    "tokenVersion must not be negative"
            );
        }
        audience = Set.copyOf(Objects.requireNonNull(
                audience,
                "audience"
        ));
        if (audience.isEmpty() || audience.stream().anyMatch(
                value -> value == null || value.isBlank()
        )) {
            throw new IllegalArgumentException(
                    "audience must contain only non-blank values"
            );
        }
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after issuedAt"
            );
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
