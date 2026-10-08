package top.egon.cola.platform.tianquan.shoubing.core.audit;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;

/** Secret-free usage decision carried to the durable audit stream. */
public record TokenUsageAuditEvent(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._~-]{1,64}") String eventId,
        @Min(1) int schemaVersion,
        @NotNull URI issuer,
        @NotBlank @Size(max = 2048) String resource,
        @Size(max = 64) @Pattern(regexp = "[0-9a-f]{64}") String subjectRef,
        @Size(max = 64) @Pattern(regexp = "[0-9a-f]{64}") String clientRef,
        @Size(max = 64) @Pattern(regexp = "[0-9a-f]{64}") String tokenRef,
        @NotBlank @Pattern(regexp = "SUCCESS|DENIED|FAILED") String result,
        @NotBlank @Size(max = 128) @Pattern(regexp = "[A-Z0-9_]{1,128}") String reasonCode,
        @NotNull Instant occurredAt,
        @Size(max = 128) @Pattern(regexp = "[A-Za-z0-9._~-]{1,128}") String traceId
) {

    public TokenUsageAuditEvent {
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("TOKEN_USAGE_AUDIT_SCHEMA_UNSUPPORTED");
        }
        if (eventId == null || !eventId.matches("[A-Za-z0-9._~-]{1,64}")
                || resource == null || resource.isBlank() || resource.length() > 2048
                || !validReference(subjectRef) || !validReference(clientRef) || !validReference(tokenRef)
                || result == null || !result.matches("SUCCESS|DENIED|FAILED")
                || reasonCode == null || !reasonCode.matches("[A-Z0-9_]{1,128}")
                || occurredAt == null
                || traceId != null && !traceId.matches("[A-Za-z0-9._~-]{1,128}")) {
            throw new IllegalArgumentException("TOKEN_USAGE_AUDIT_EVENT_INVALID");
        }
        if (issuer == null || !issuer.isAbsolute() || issuer.getHost() == null
                || issuer.getUserInfo() != null || issuer.getQuery() != null || issuer.getFragment() != null
                || !"https".equalsIgnoreCase(issuer.getScheme())) {
            throw new IllegalArgumentException("TOKEN_USAGE_AUDIT_ISSUER_INVALID");
        }
        issuer = issuer.normalize();
    }

    @Override
    public String toString() {
        return "TokenUsageAuditEvent[eventId=" + eventId + ", issuer=" + issuer
                + ", result=" + result + ", reasonCode=" + reasonCode
                + ", occurredAt=" + occurredAt + "]";
    }

    private static boolean validReference(String value) {
        return value == null || value.matches("[0-9a-f]{64}");
    }
}
