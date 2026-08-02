package top.egon.cola.platform.idp.core.oauth;

import java.time.Instant;
import java.util.Objects;

public record AuthorizationCode(
        String identitySub,
        String tenantId,
        String rbac3UserId,
        String clientId,
        String audience,
        String redirectUri,
        String nonce,
        String codeChallenge,
        Instant issuedAt,
        Instant expiresAt
) {

    public AuthorizationCode {
        identitySub = required(identitySub, "identitySub");
        tenantId = required(tenantId, "tenantId");
        rbac3UserId = required(rbac3UserId, "rbac3UserId");
        clientId = required(clientId, "clientId");
        audience = required(audience, "audience");
        redirectUri = required(redirectUri, "redirectUri");
        nonce = required(nonce, "nonce");
        codeChallenge = required(codeChallenge, "codeChallenge");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after issuedAt"
            );
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
