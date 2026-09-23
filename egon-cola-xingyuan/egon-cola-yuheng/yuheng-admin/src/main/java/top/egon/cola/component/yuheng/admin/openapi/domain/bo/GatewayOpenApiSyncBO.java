package top.egon.cola.component.yuheng.admin.openapi.domain.bo;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;
import top.egon.cola.component.yuheng.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code GatewayOpenApiSyncBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code GatewayOpenApiSyncBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class GatewayOpenApiSyncBO {

    private static final Pattern GROUP = Pattern.compile(
            "^[a-z][a-z0-9-]{0,63}$"
    );

    private String id;
    private String applicationId;
    private String buildId;
    private String artifactVersion;
    private String openapiGroup;
    private String providerServiceName;
    private String providerGroup;
    private String providerVersion;
    private GatewayOpenApiSyncStateEnum status;
    private String latestSnapshotId;
    private String definitionSetId;
    private String lastInstanceId;
    private int attemptCount;
    private String lastErrorCode;
    private String lastErrorMessage;
    private Instant firstDiscoveredAt;
    private Instant lastAttemptAt;
    private Instant lastSuccessAt;
    private Instant nextRetryAt;
    private long revision;
    private Instant updatedAt;

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

    /**
     * 中文说明：按同步记录的既有契约复核并规范化各字段；Lombok 的规范构造器不承担该校验，因此持久化边界与测试必须调用本方法。
     * English summary: Re-checks and normalizes the sync record against its established contract; Lombok's canonical constructor cannot enforce it, so persistence boundaries and tests must call this factory.
     * @param sync 参数 待校验的同步记录；parameter the sync record to validate.
     * @return 返回完成校验与规范化的同一实例；returns the same instance once validated.
     */
    public static GatewayOpenApiSyncBO validated(GatewayOpenApiSyncBO sync) {
        Objects.requireNonNull(sync, "sync");
        sync.setId(required(sync.getId(), "id", 64));
        sync.setApplicationId(required(sync.getApplicationId(), "applicationId", 64));
        sync.setBuildId(required(sync.getBuildId(), "buildId", 256));
        sync.setArtifactVersion(required(sync.getArtifactVersion(), "artifactVersion", 128));
        String group = defaulted(sync.getOpenapiGroup(), "default");
        if (!GROUP.matcher(group).matches()) {
            throw new IllegalArgumentException(
                    "openapiGroup must start with a lowercase letter and "
                            + "contain only lowercase letters, digits or hyphens"
            );
        }
        sync.setOpenapiGroup(group);
        sync.setProviderServiceName(required(
                sync.getProviderServiceName(),
                "providerServiceName",
                256
        ));
        String providerGroup = defaulted(sync.getProviderGroup(), "default");
        if (providerGroup.length() > 128) {
            throw new IllegalArgumentException("providerGroup exceeds 128 characters");
        }
        sync.setProviderGroup(providerGroup);
        sync.setProviderVersion(required(sync.getProviderVersion(), "providerVersion", 128));
        sync.setStatus(Objects.requireNonNull(sync.getStatus(), "status"));
        sync.setLatestSnapshotId(optional(sync.getLatestSnapshotId(), "latestSnapshotId", 64));
        sync.setDefinitionSetId(optional(sync.getDefinitionSetId(), "definitionSetId", 64));
        sync.setLastInstanceId(optional(sync.getLastInstanceId(), "lastInstanceId", 256));
        if (sync.getAttemptCount() < 0) {
            throw new IllegalArgumentException("attemptCount must not be negative");
        }
        sync.setLastErrorCode(optional(sync.getLastErrorCode(), "lastErrorCode", 128));
        sync.setLastErrorMessage(optional(sync.getLastErrorMessage(), "lastErrorMessage", 1024));
        sync.setFirstDiscoveredAt(Objects.requireNonNull(
                sync.getFirstDiscoveredAt(),
                "firstDiscoveredAt"
        ));
        if (sync.getRevision() < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
        sync.setUpdatedAt(Objects.requireNonNull(sync.getUpdatedAt(), "updatedAt"));
        if (sync.getStatus() == GatewayOpenApiSyncStateEnum.VALID
                && (sync.getLatestSnapshotId() == null || sync.getDefinitionSetId() == null)) {
            throw new IllegalArgumentException(
                    "VALID sync state requires snapshot and definition set"
            );
        }
        return sync;
    }
}
