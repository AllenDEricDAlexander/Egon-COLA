package top.egon.cola.component.yuheng.admin.openapi.domain.vo;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Read-only presentation of one OpenAPI application/build/Group sync row.
 *
 * <p>中文：仅暴露管理端监控所需的状态、快照、聚合、错误和时间字段，不携带
 * Provider 地址、凭证或响应体。</p>
 */
public record GatewayOpenApiSyncStateVO(
        String id,
        String applicationId,
        String buildId,
        String artifactVersion,
        String openapiGroup,
        String status,
        String snapshotId,
        String definitionSetId,
        Integer operationCount,
        Integer schemaCount,
        String canonicalSha256,
        String lastErrorCode,
        String lastErrorMessage,
        String sourceType,
        Instant lastAttemptAt,
        Instant lastSuccessAt,
        Instant nextRetryAt
) {

    private static final Pattern GROUP = Pattern.compile(
            "^[a-z][a-z0-9-]{0,63}$");

    private static final Pattern SHA256 = Pattern.compile(
            "^[0-9a-f]{64}$");

    /** Normalizes and validates the externally visible sync projection. */
    public GatewayOpenApiSyncStateVO {
        id = required(id, "id", 64);
        applicationId = required(applicationId, "applicationId", 64);
        buildId = required(buildId, "buildId", 256);
        artifactVersion = required(artifactVersion, "artifactVersion", 128);
        openapiGroup = required(openapiGroup, "openapiGroup", 64);
        if (!GROUP.matcher(openapiGroup).matches()) {
            throw new IllegalArgumentException(
                    "openapiGroup must use lowercase letters, digits or hyphens");
        }
        status = required(status, "status", 32);
        snapshotId = optional(snapshotId, "snapshotId", 64);
        definitionSetId = optional(definitionSetId, "definitionSetId", 64);
        if (operationCount != null && operationCount < 0) {
            throw new IllegalArgumentException(
                    "operationCount must not be negative");
        }
        if (schemaCount != null && schemaCount < 0) {
            throw new IllegalArgumentException(
                    "schemaCount must not be negative");
        }
        canonicalSha256 = optionalHash(canonicalSha256, "canonicalSha256");
        lastErrorCode = optional(lastErrorCode, "lastErrorCode", 128);
        lastErrorMessage = optional(lastErrorMessage, "lastErrorMessage", 1024);
        sourceType = optional(sourceType, "sourceType", 32);
    }

    /**
     * Compatibility constructor for projections that predate the explicit
     * source field; new query projections should provide {@code sourceType}.
     */
    public GatewayOpenApiSyncStateVO(
            String id,
            String applicationId,
            String buildId,
            String artifactVersion,
            String openapiGroup,
            String status,
            String snapshotId,
            String definitionSetId,
            Integer operationCount,
            Integer schemaCount,
            String canonicalSha256,
            String lastErrorCode,
            String lastErrorMessage,
            Instant lastAttemptAt,
            Instant lastSuccessAt,
            Instant nextRetryAt) {
        this(
                id,
                applicationId,
                buildId,
                artifactVersion,
                openapiGroup,
                status,
                snapshotId,
                definitionSetId,
                operationCount,
                schemaCount,
                canonicalSha256,
                lastErrorCode,
                lastErrorMessage,
                null,
                lastAttemptAt,
                lastSuccessAt,
                nextRetryAt);
    }

    private static String required(String value, String field, int max) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (normalized.length() > max) {
            throw new IllegalArgumentException(
                    field + " exceeds " + max + " characters");
        }
        return normalized;
    }

    private static String optional(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return required(value, field, max);
    }

    private static String optionalHash(String value, String field) {
        String normalized = optional(value, field, 64);
        if (normalized != null && !SHA256.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    field + " must be lowercase hexadecimal SHA-256");
        }
        return normalized;
    }
}
