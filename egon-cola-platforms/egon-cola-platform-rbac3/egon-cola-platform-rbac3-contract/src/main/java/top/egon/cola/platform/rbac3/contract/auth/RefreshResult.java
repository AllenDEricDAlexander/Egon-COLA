package top.egon.cola.platform.rbac3.contract.auth;

public record RefreshResult(
        String tokenType,
        String accessToken,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn,
        String sessionId,
        long authVersion,
        long sessionVersion,
        long policyVersion,
        boolean roleActivationRequired,
        String activationReasonCode,
        boolean bootstrapRequired
) {

    public RefreshResult {
        tokenType = required(tokenType, "tokenType");
        accessToken = required(accessToken, "accessToken");
        nonNegative(expiresIn, "expiresIn");
        refreshToken = optional(refreshToken, "refreshToken");
        nonNegative(refreshExpiresIn, "refreshExpiresIn");
        sessionId = required(sessionId, "sessionId");
        nonNegative(authVersion, "authVersion");
        nonNegative(sessionVersion, "sessionVersion");
        nonNegative(policyVersion, "policyVersion");
        if (activationReasonCode != null
                && activationReasonCode.isBlank()) {
            throw new IllegalArgumentException(
                    "activationReasonCode must not be blank"
            );
        }
    }

    @Override
    public String toString() {
        return "RefreshResult[tokenType="
                + tokenType
                + ", accessToken=<redacted>, expiresIn="
                + expiresIn
                + ", refreshToken=<redacted>, refreshExpiresIn="
                + refreshExpiresIn
                + ", sessionId="
                + sessionId
                + ", authVersion="
                + authVersion
                + ", sessionVersion="
                + sessionVersion
                + ", policyVersion="
                + policyVersion
                + ", roleActivationRequired="
                + roleActivationRequired
                + ", activationReasonCode="
                + activationReasonCode
                + ", bootstrapRequired="
                + bootstrapRequired
                + "]";
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

    private static void nonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }
    }
}
