package top.egon.cola.component.yuheng.admin.openapi.domain.po;

import top.egon.cola.component.yuheng.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Mutable synchronization row represented as an immutable persistence record.
 *
 * <p>中文：每个 application/build/group 一个同步状态行；所有更新通过 revision
 * CAS 完成。
 */
public record GatewayOpenApiSyncPO(
        String id,
        String applicationId,
        String buildId,
        String artifactVersion,
        String openapiGroup,
        String providerServiceName,
        String providerGroup,
        String providerVersion,
        GatewayOpenApiSyncStateEnum status,
        String latestSnapshotId,
        String definitionSetId,
        String lastInstanceId,
        int attemptCount,
        String lastErrorCode,
        String lastErrorMessage,
        Instant firstDiscoveredAt,
        Instant lastAttemptAt,
        Instant lastSuccessAt,
        Instant nextRetryAt,
        long revision,
        Instant updatedAt
) {

    private static final Pattern GROUP = Pattern.compile(
            "^[a-z][a-z0-9-]{0,63}$"
    );

    public GatewayOpenApiSyncPO {
        id = required(id, "id", 64);
        applicationId = required(applicationId, "applicationId", 64);
        buildId = required(buildId, "buildId", 256);
        artifactVersion = required(artifactVersion, "artifactVersion", 128);
        openapiGroup = defaulted(openapiGroup, "default");
        if (!GROUP.matcher(openapiGroup).matches()) {
            throw new IllegalArgumentException(
                    "openapiGroup must start with a lowercase letter and "
                            + "contain only lowercase letters, digits or hyphens"
            );
        }
        providerServiceName = required(
                providerServiceName,
                "providerServiceName",
                256
        );
        providerGroup = defaulted(providerGroup, "default");
        if (providerGroup.length() > 128) {
            throw new IllegalArgumentException(
                    "providerGroup exceeds 128 characters"
            );
        }
        providerVersion = required(providerVersion, "providerVersion", 128);
        status = Objects.requireNonNull(status, "status");
        latestSnapshotId = optional(latestSnapshotId, "latestSnapshotId", 64);
        definitionSetId = optional(definitionSetId, "definitionSetId", 64);
        lastInstanceId = optional(lastInstanceId, "lastInstanceId", 256);
        if (attemptCount < 0) {
            throw new IllegalArgumentException(
                    "attemptCount must not be negative"
            );
        }
        lastErrorCode = optional(lastErrorCode, "lastErrorCode", 128);
        lastErrorMessage = optional(lastErrorMessage, "lastErrorMessage", 1024);
        firstDiscoveredAt = Objects.requireNonNull(
                firstDiscoveredAt,
                "firstDiscoveredAt"
        );
        lastAttemptAt = lastAttemptAt;
        lastSuccessAt = lastSuccessAt;
        nextRetryAt = nextRetryAt;
        if (revision < 0) {
            throw new IllegalArgumentException(
                    "revision must not be negative"
            );
        }
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (status == GatewayOpenApiSyncStateEnum.VALID
                && (latestSnapshotId == null || definitionSetId == null)) {
            throw new IllegalArgumentException(
                    "VALID sync state requires snapshot and definition set"
            );
        }
    }

    private static String required(String value, String field, int max) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (normalized.length() > max) {
            throw new IllegalArgumentException(
                    field + " exceeds " + max + " characters"
            );
        }
        return normalized;
    }

    private static String optional(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return required(value, field, max);
    }

    private static String defaulted(String value, String defaultValue) {
        return value == null || value.isBlank()
                ? defaultValue
                : value.trim();
    }
}
