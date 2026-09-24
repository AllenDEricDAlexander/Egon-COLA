package top.egon.cola.component.yuheng.admin.mcp.domain.bo;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code McpArtifactMetadataBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code McpArtifactMetadataBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class McpArtifactMetadataBO {

    private String id;
    private String gatewayGroupId;
    private String appCode;
    private String version;
    private String displayName;
    private String resourceUri;
    private String artifactReference;
    private String sha256;
    private long sizeBytes;
    private String mimeType;
    private String contentSecurityPolicy;
    private Set<String> permissions;
    private Set<String> allowedOrigins;
    private String createdBy;
    private Instant createdAt;

    /**
     * 中文说明：按原持久化载体的紧凑构造器契约建立制品元数据载体；Lombok 规范构造器不承担这些校验，故读写边界必须经此工厂。
     * English summary: Builds an artifact metadata carrier enforcing the invariants of the legacy persistence carrier's compact constructor; Lombok's canonical constructor cannot, so read/write boundaries must go through this factory.
     *
     * 用法 / Usage: 由服务写入或 Jdbc 载入构造制品时调用；/ Call it when a service registers or the Jdbc load path reads an artifact.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param appCode 参数 应用Code；parameter app code。
     * @param version 参数 version；parameter version。
     * @param displayName 参数 展示名称；parameter display name。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param artifactReference 参数 制品引用；parameter artifact reference。
     * @param sha256 参数 sha256；parameter sha256。
     * @param sizeBytes 参数 字节大小；parameter size in bytes。
     * @param mimeType 参数 媒体类型；parameter mime type。
     * @param contentSecurityPolicy 参数 内容安全策略；parameter content security policy。
     * @param permissions 参数 权限集合；parameter permissions。
     * @param allowedOrigins 参数 允许来源；parameter allowed origins。
     * @param createdBy 参数 创建者；parameter created by。
     * @param createdAt 参数 创建时间；parameter created at。
     * @return 返回已完成必填校验、集合复制与取值范围校验的制品载体；returns the validated artifact carrier.
     */
    public static McpArtifactMetadataBO normalized(
            String id,
            String gatewayGroupId,
            String appCode,
            String version,
            String displayName,
            String resourceUri,
            String artifactReference,
            String sha256,
            long sizeBytes,
            String mimeType,
            String contentSecurityPolicy,
            Set<String> permissions,
            Set<String> allowedOrigins,
            String createdBy,
            Instant createdAt
    ) {
        String checkedId = required(id, "id");
        String checkedGatewayGroupId =
                required(gatewayGroupId, "gatewayGroupId");
        String checkedAppCode = required(appCode, "appCode");
        String checkedVersion = required(version, "version");
        String checkedDisplayName =
                required(displayName, "displayName");
        String checkedResourceUri =
                required(resourceUri, "resourceUri");
        String checkedArtifactReference = required(
                artifactReference,
                "artifactReference"
        );
        String checkedSha256 = required(sha256, "sha256");
        String checkedMimeType =
                required(mimeType, "mimeType");
        String checkedContentSecurityPolicy = required(
                contentSecurityPolicy,
                "contentSecurityPolicy"
        );
        Set<String> checkedPermissions = Set.copyOf(permissions);
        Set<String> checkedAllowedOrigins = Set.copyOf(allowedOrigins);
        String checkedCreatedBy =
                required(createdBy, "createdBy");
        Instant checkedCreatedAt =
                Objects.requireNonNull(createdAt, "createdAt");
        if (checkedSha256.length() != 64) {
            throw new IllegalArgumentException(
                    "sha256 must contain 64 characters"
            );
        }
        if (sizeBytes < 0 || sizeBytes > 16L * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "artifact size is outside the supported range"
            );
        }
        return new McpArtifactMetadataBO(
                checkedId,
                checkedGatewayGroupId,
                checkedAppCode,
                checkedVersion,
                checkedDisplayName,
                checkedResourceUri,
                checkedArtifactReference,
                checkedSha256,
                sizeBytes,
                checkedMimeType,
                checkedContentSecurityPolicy,
                checkedPermissions,
                checkedAllowedOrigins,
                checkedCreatedBy,
                checkedCreatedAt
        );
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
