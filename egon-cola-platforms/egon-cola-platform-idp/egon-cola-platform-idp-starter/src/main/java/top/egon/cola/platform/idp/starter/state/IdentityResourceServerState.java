package top.egon.cola.platform.idp.starter.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;

import java.net.URI;
import java.util.Objects;

/**
 * IdP Resource Server 运行态投影的可信只读视图。
 *
 * <p>Trusted read-only view of an IdP Resource Server runtime projection.</p>
 *
 * @param resourceServerId Resource Server 标识；Resource Server identifier
 * @param resourceUri 唯一 Resource URI；unique Resource URI
 * @param bizCode 业务域编码；business-domain code
 * @param appCode 应用编码；application code
 * @param environment 环境编码；environment code
 * @param status 当前状态；current status
 * @param version 当前版本；current version
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IdentityResourceServerState(
        String resourceServerId,
        URI resourceUri,
        String bizCode,
        String appCode,
        String environment,
        ResourceServerStatus status,
        long version
) {

    /**
     * 校验并规范化 Resource Server 运行态投影。
     *
     * <p>Validates and normalizes the Resource Server runtime projection.</p>
     */
    public IdentityResourceServerState {
        resourceServerId = required(resourceServerId, "resourceServerId");
        resourceUri = Objects.requireNonNull(resourceUri, "resourceUri");
        if (!resourceUri.isAbsolute()
                || resourceUri.getFragment() != null
                || !resourceUri.equals(resourceUri.normalize())) {
            throw new IllegalArgumentException("resourceUri is invalid");
        }
        bizCode = required(bizCode, "bizCode");
        appCode = required(appCode, "appCode");
        environment = required(environment, "environment");
        status = Objects.requireNonNull(status, "status");
        if (version < 0L) {
            throw new IllegalArgumentException(
                    "version must not be negative"
            );
        }
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 规范化文本；normalized text
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
