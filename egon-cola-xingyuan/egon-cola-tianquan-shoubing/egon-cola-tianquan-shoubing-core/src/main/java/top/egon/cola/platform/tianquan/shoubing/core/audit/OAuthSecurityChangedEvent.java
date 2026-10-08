package top.egon.cola.platform.tianquan.shoubing.core.audit;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/** Durable fact that OAuth security state changed for one registered issuer. */
public record OAuthSecurityChangedEvent(
        @NotBlank @Size(max = 128) @Pattern(regexp = "[A-Za-z0-9._~-]{1,128}") String eventId,
        @Min(1) int schemaVersion,
        @NotNull URI issuer,
        @NotBlank @Size(max = 64) @Pattern(regexp = "platform|tenant:[1-9][0-9]{0,18}") String realmKey,
        @PositiveOrZero long revision,
        @NotBlank @Pattern(regexp = "REALM|CLIENT|SUBJECT|JTI|AUTHORIZATION|KID") String targetType,
        @NotBlank @Size(max = 128) String targetKey,
        @PositiveOrZero Long generation,
        Instant expiresAt,
        @NotBlank @Size(max = 128) @Pattern(regexp = "[A-Z0-9_]{1,128}") String reasonCode,
        @NotNull Instant occurredAt,
        @Size(max = 128) @Pattern(regexp = "[A-Za-z0-9._~-]{1,128}") String traceId
) {

    public static final String OUTBOX_CHANNEL = "oauth-security";
    public static final String OUTBOX_DESTINATION = "redis-security-projection-v1";

    public OAuthSecurityChangedEvent {
        if (schemaVersion != 1 || revision < 0L || generation != null && generation < 0L) {
            throw new IllegalArgumentException("OAUTH_SECURITY_EVENT_VERSION_INVALID");
        }
        Objects.requireNonNull(issuer, "issuer");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (!issuer.isAbsolute() || issuer.getHost() == null || issuer.getUserInfo() != null
                || issuer.getQuery() != null || issuer.getFragment() != null
                || !"https".equalsIgnoreCase(issuer.getScheme())) {
            throw new IllegalArgumentException("OAUTH_SECURITY_EVENT_ISSUER_INVALID");
        }
        issuer = issuer.normalize();
        if (eventId == null || !eventId.matches("[A-Za-z0-9._~-]{1,128}")
                || realmKey == null || !realmKey.matches("platform|tenant:[1-9][0-9]{0,18}")
                || targetType == null || !targetType.matches("REALM|CLIENT|SUBJECT|JTI|AUTHORIZATION|KID")
                || targetKey == null || targetKey.isBlank() || !targetKey.equals(targetKey.trim())
                || targetKey.length() > 128 || reasonCode == null
                || !reasonCode.matches("[A-Z0-9_]{1,128}")
                || traceId != null && !traceId.matches("[A-Za-z0-9._~-]{1,128}")) {
            throw new IllegalArgumentException("OAUTH_SECURITY_EVENT_INVALID");
        }
        if (expiresAt != null && !expiresAt.isAfter(occurredAt)) {
            throw new IllegalArgumentException("OAUTH_SECURITY_EVENT_EXPIRY_INVALID");
        }
        if (requiresGeneration(targetType) != (generation != null)) {
            throw new IllegalArgumentException("OAUTH_SECURITY_EVENT_GENERATION_INVALID");
        }
        validateTarget(targetType, targetKey);
    }

    @Override
    public String toString() {
        return "OAuthSecurityChangedEvent[eventId=" + eventId + ", issuer=" + issuer
                + ", realmKey=" + realmKey + ", revision=" + revision
                + ", targetType=" + targetType + ", reasonCode=" + reasonCode
                + ", occurredAt=" + occurredAt + "]";
    }

    private static boolean requiresGeneration(String targetType) {
        return "REALM".equals(targetType) || "CLIENT".equals(targetType) || "SUBJECT".equals(targetType);
    }

    private static void validateTarget(String targetType, String targetKey) {
        boolean valid = switch (targetType) {
            case "REALM" -> "self".equals(targetKey);
            case "CLIENT", "AUTHORIZATION" -> positiveLong(targetKey);
            case "JTI" -> targetKey.matches("[0-9]{1,64}");
            case "SUBJECT" -> targetKey.matches("[A-Za-z0-9._~-]{1,64}");
            case "KID" -> targetKey.matches("[A-Za-z0-9_-]{1,128}");
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("OAUTH_SECURITY_EVENT_TARGET_INVALID");
        }
    }

    private static boolean positiveLong(String value) {
        if (!value.matches("[1-9][0-9]{0,18}")) {
            return false;
        }
        try {
            return Long.parseLong(value) > 0L;
        } catch (NumberFormatException invalid) {
            return false;
        }
    }
}
