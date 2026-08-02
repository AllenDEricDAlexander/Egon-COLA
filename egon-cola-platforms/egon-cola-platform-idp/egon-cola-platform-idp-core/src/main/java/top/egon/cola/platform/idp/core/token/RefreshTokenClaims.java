package top.egon.cola.platform.idp.core.token;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record RefreshTokenClaims(
        String subject,
        String tenantId,
        String sessionId,
        String clientId,
        String familyId,
        String tokenId,
        long generation,
        long tokenVersion,
        List<String> audience,
        String nonce,
        Instant issuedAt,
        Instant expiresAt
) {

    public RefreshTokenClaims {
        subject = required(subject, "subject");
        tenantId = required(tenantId, "tenantId");
        sessionId = required(sessionId, "sessionId");
        clientId = required(clientId, "clientId");
        familyId = required(familyId, "familyId");
        tokenId = required(tokenId, "tokenId");
        nonce = required(nonce, "nonce");
        if (generation < 0L || tokenVersion < 0L) {
            throw new IllegalArgumentException(
                    "refresh versions must not be negative"
            );
        }
        audience = Objects.requireNonNull(audience, "audience").stream()
                .map(value -> required(value, "audience"))
                .sorted()
                .distinct()
                .toList();
        if (audience.isEmpty()) {
            throw new IllegalArgumentException("audience is required");
        }
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("refresh token is expired");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
