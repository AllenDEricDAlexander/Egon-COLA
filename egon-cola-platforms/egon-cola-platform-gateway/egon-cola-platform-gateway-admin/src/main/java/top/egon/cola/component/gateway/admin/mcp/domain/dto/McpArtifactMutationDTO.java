package top.egon.cola.component.gateway.admin.mcp.domain.dto;


import java.util.Set;

/**
 * 中文说明：{@code McpArtifactMutationDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责制品Mutation相关的职责与边界。
 * English summary: {@code McpArtifactMutationDTO} is an immutable data carrier in the current Gateway module; it owns the artifact mutation-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
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
 * @param expectedRevision 参数 expectedRevision；parameter expected revision。
 * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
 * @param changeReason 参数 changeReason；parameter change reason。
 */
public record McpArtifactMutationDTO(
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String appCode,
        /**
         * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String version,
        /**
         * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String displayName,
        /**
         * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String resourceUri,
        /**
         * 中文说明：保存 制品Reference 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by artifact reference; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String artifactReference,
        /**
         * 中文说明：保存 sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String sha256,
        /**
         * 中文说明：保存 sizeBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by size bytes; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long sizeBytes,
        /**
         * 中文说明：保存 mimeType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by mime type; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String mimeType,
        /**
         * 中文说明：保存 content安全策略 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by content security policy; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String contentSecurityPolicy,
        /**
         * 中文说明：保存 permissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by permissions; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> permissions,
        /**
         * 中文说明：保存 allowedOrigins 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by allowed origins; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> allowedOrigins,
        /**
         * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long expectedRevision,
        /**
         * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long expectedDraftRevision,
        /**
         * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeReason
) {
}
