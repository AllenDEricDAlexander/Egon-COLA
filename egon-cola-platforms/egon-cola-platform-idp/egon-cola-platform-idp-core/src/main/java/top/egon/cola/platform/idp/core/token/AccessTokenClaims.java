package top.egon.cola.platform.idp.core.token;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AccessTokenClaims(
        String subject,
        String tenantId,
        String sessionId,
        String clientId,
        String tokenId,
        long tokenVersion,
        List<String> audience,
        String nonce,
        Instant issuedAt,
        Instant notBefore,
        Instant expiresAt
) {

    public AccessTokenClaims {
        subject = required(subject, "subject");
        tenantId = required(tenantId, "tenantId");
        sessionId = required(sessionId, "sessionId");
        clientId = required(clientId, "clientId");
        tokenId = required(tokenId, "tokenId");
        nonce = required(nonce, "nonce");
        if (tokenVersion < 0L) {
            throw new IllegalArgumentException(
                    "tokenVersion must not be negative"
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
        notBefore = Objects.requireNonNull(notBefore, "notBefore");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (notBefore.isBefore(issuedAt) || !expiresAt.isAfter(notBefore)) {
            throw new IllegalArgumentException("invalid access token time range");
        }
    }

    public Map<String, Object> asMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sub", subject);
        values.put("tid", tenantId);
        values.put("sid", sessionId);
        values.put("client_id", clientId);
        values.put("jti", tokenId);
        values.put("token_version", tokenVersion);
        values.put("aud", audience);
        values.put("nonce", nonce);
        values.put("iat", issuedAt);
        values.put("nbf", notBefore);
        values.put("exp", expiresAt);
        return Map.copyOf(values);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
