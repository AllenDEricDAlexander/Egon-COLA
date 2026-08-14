package top.egon.cola.component.gateway.admin.mcp.domain.po;


import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.McpJdbcJson;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;


/**
 * 中文说明：{@code McpArtifactMetadataPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责制品元数据相关的职责与边界。
 * English summary: {@code McpArtifactMetadataPO} is an immutable data carrier in the current Gateway module; it owns the artifact metadata-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param appCode 参数 appCode；parameter app code。
 * @param version 参数 version；parameter version。
 * @param displayName 参数 displayName；parameter display name。
 * @param resourceUri 参数 资源Uri；parameter resource uri。
 * @param artifactReference 参数 制品Reference；parameter artifact reference。
 * @param sha256 参数 sha256；parameter sha256。
 * @param sizeBytes 参数 sizeBytes；parameter size bytes。
 * @param mimeType 参数 mimeType；parameter mime type。
 * @param contentSecurityPolicy 参数 content安全策略；parameter content security policy。
 * @param permissions 参数 permissions；parameter permissions。
 * @param allowedOrigins 参数 allowedOrigins；parameter allowed origins。
 * @param createdBy 参数 createdBy；parameter created by。
 * @param createdAt 参数 createdAt；parameter created at。
 */
public record McpArtifactMetadataPO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String appCode,
        /**
         * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String version,
        /**
         * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String displayName,
        /**
         * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String resourceUri,
        /**
         * 中文说明：保存 制品Reference 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by artifact reference; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String artifactReference,
        /**
         * 中文说明：保存 sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String sha256,
        /**
         * 中文说明：保存 sizeBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by size bytes; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long sizeBytes,
        /**
         * 中文说明：保存 mimeType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by mime type; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String mimeType,
        /**
         * 中文说明：保存 content安全策略 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by content security policy; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String contentSecurityPolicy,
        /**
         * 中文说明：保存 permissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by permissions; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> permissions,
        /**
         * 中文说明：保存 allowedOrigins 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by allowed origins; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> allowedOrigins,
        /**
         * 中文说明：保存 createdBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by created by; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String createdBy,
        /**
         * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant createdAt
) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param appCode 参数 appCode；parameter app code。
     * @param version 参数 version；parameter version。
     * @param displayName 参数 displayName；parameter display name。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param artifactReference 参数 制品Reference；parameter artifact reference。
     * @param sha256 参数 sha256；parameter sha256。
     * @param sizeBytes 参数 sizeBytes；parameter size bytes。
     * @param mimeType 参数 mimeType；parameter mime type。
     * @param contentSecurityPolicy 参数 content安全策略；parameter content security policy。
     * @param permissions 参数 permissions；parameter permissions。
     * @param allowedOrigins 参数 allowedOrigins；parameter allowed origins。
     * @param createdBy 参数 createdBy；parameter created by。
     * @param createdAt 参数 createdAt；parameter created at。
     */
    public McpArtifactMetadataPO {
        id = McpJdbcJson.required(id, "id");
        gatewayGroupId = McpJdbcJson.required(
                gatewayGroupId,
                "gatewayGroupId"
        );
        appCode = McpJdbcJson.required(appCode, "appCode");
        version = McpJdbcJson.required(version, "version");
        displayName = McpJdbcJson.required(displayName, "displayName");
        resourceUri = McpJdbcJson.required(resourceUri, "resourceUri");
        artifactReference = McpJdbcJson.required(
                artifactReference,
                "artifactReference"
        );
        sha256 = McpJdbcJson.required(sha256, "sha256");
        mimeType = McpJdbcJson.required(mimeType, "mimeType");
        contentSecurityPolicy = McpJdbcJson.required(
                contentSecurityPolicy,
                "contentSecurityPolicy"
        );
        permissions = Set.copyOf(permissions);
        allowedOrigins = Set.copyOf(allowedOrigins);
        createdBy = McpJdbcJson.required(createdBy, "createdBy");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if (sha256.length() != 64) {
            throw new IllegalArgumentException(
                    "sha256 must contain 64 characters"
            );
        }
        if (sizeBytes < 0 || sizeBytes > 16L * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "artifact size is outside the supported range"
            );
        }
    }
}
