package top.egon.cola.platform.idp.core.token;

import java.time.Instant;
import java.util.Objects;

/**
 * Minimal non-secret status returned for an IdP-owned refresh token.
 */
public record RefreshTokenStatus(
        String subject,
        String tenantId,
        Instant expiresAt
) {

    public RefreshTokenStatus {
        subject = required(subject, "subject");
        tenantId = required(tenantId, "tenantId");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
