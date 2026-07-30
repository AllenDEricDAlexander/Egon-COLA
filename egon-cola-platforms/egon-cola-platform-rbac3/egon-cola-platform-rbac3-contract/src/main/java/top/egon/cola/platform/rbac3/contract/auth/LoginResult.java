package top.egon.cola.platform.rbac3.contract.auth;

public record LoginResult(
        String tokenType,
        String accessToken,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn,
        String sessionId,
        boolean roleActivationRequired,
        int activationCandidateCount,
        String activationCandidatesUrl,
        boolean bootstrapRequired
) {

    public LoginResult {
        tokenType = required(tokenType, "tokenType");
        accessToken = required(accessToken, "accessToken");
        nonNegative(expiresIn, "expiresIn");
        refreshToken = optional(refreshToken, "refreshToken");
        nonNegative(refreshExpiresIn, "refreshExpiresIn");
        sessionId = required(sessionId, "sessionId");
        if (activationCandidateCount < 0) {
            throw new IllegalArgumentException(
                    "activationCandidateCount must not be negative"
            );
        }
        activationCandidatesUrl = required(
                activationCandidatesUrl,
                "activationCandidatesUrl"
        );
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
