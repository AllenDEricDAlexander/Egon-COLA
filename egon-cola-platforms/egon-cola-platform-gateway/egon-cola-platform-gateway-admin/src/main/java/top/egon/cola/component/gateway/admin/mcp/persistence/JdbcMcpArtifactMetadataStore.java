package top.egon.cola.component.gateway.admin.mcp.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 中文说明：{@code JdbcMcpArtifactMetadataStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcMCP制品元数据存储相关的职责与边界。
 * English summary: {@code JdbcMcpArtifactMetadataStore} is a jdbc mcp artifact metadata store store in the current Gateway module; it owns the jdbc mcp artifact metadata store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcMcpArtifactMetadataStore {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcMcpArtifactMetadataStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcMcpArtifactMetadataStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 json 对应的状态、依赖或配置值；字段类型为 {@code McpJdbcJson}，由 {@code JdbcMcpArtifactMetadataStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by json; its type is {@code McpJdbcJson}, and {@code JdbcMcpArtifactMetadataStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpJdbcJson json;

    /**
     * 中文说明：创建 {@code JdbcMcpArtifactMetadataStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpArtifactMetadataStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcMcpArtifactMetadataStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    /**
     * 中文说明：执行 save 操作；该方法是 {@code JdbcMcpArtifactMetadataStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the save operation; this method is the invocation entry point on {@code JdbcMcpArtifactMetadataStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpArtifactMetadataStore.save(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param artifact 参数 制品；parameter artifact。
     */
    public void save(ArtifactMetadata artifact) {
        Objects.requireNonNull(artifact, "artifact");
        jdbc.update("""
                INSERT INTO gateway_mcp_app_artifact(
                    id, gateway_group_id, app_code, app_version,
                    display_name, resource_uri, artifact_reference,
                    artifact_sha256, size_bytes, mime_type,
                    content_security_policy, permission_manifest,
                    allowed_origins, status, created_at, created_by
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?::jsonb, ?::jsonb, 'ACTIVE', ?, ?
                )
                """,
                artifact.id(),
                artifact.gatewayGroupId(),
                artifact.appCode(),
                artifact.version(),
                artifact.displayName(),
                artifact.resourceUri(),
                artifact.artifactReference(),
                artifact.sha256(),
                artifact.sizeBytes(),
                artifact.mimeType(),
                artifact.contentSecurityPolicy(),
                json.write(artifact.permissions()),
                json.write(artifact.allowedOrigins()),
                McpJdbcJson.timestamp(artifact.createdAt()),
                artifact.createdBy()
        );
    }

    /**
     * 中文说明：执行 find 操作；该方法是 {@code JdbcMcpArtifactMetadataStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code JdbcMcpArtifactMetadataStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpArtifactMetadataStore.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    public Optional<ArtifactMetadata> find(String id) {
        List<ArtifactMetadata> values = jdbc.query("""
                SELECT id, gateway_group_id, app_code, app_version,
                       display_name, resource_uri, artifact_reference,
                       artifact_sha256, size_bytes, mime_type,
                       content_security_policy,
                       permission_manifest::text AS permission_manifest,
                       allowed_origins::text AS allowed_origins,
                       created_at, created_by
                  FROM gateway_mcp_app_artifact
                 WHERE id = ? AND status = 'ACTIVE'
                """, (result, row) -> map(result), id);
        return values.stream().findFirst();
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code JdbcMcpArtifactMetadataStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code JdbcMcpArtifactMetadataStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpArtifactMetadataStore.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    public List<ArtifactMetadata> list(String gatewayGroupId) {
        return jdbc.query("""
                SELECT id, gateway_group_id, app_code, app_version,
                       display_name, resource_uri, artifact_reference,
                       artifact_sha256, size_bytes, mime_type,
                       content_security_policy,
                       permission_manifest::text AS permission_manifest,
                       allowed_origins::text AS allowed_origins,
                       created_at, created_by
                  FROM gateway_mcp_app_artifact
                 WHERE gateway_group_id = ? AND status = 'ACTIVE'
                 ORDER BY app_code, app_version
                """, (result, row) -> map(result), gatewayGroupId);
    }

    /**
     * 中文说明：执行 revoke 操作；该方法是 {@code JdbcMcpArtifactMetadataStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revoke operation; this method is the invocation entry point on {@code JdbcMcpArtifactMetadataStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpArtifactMetadataStore.revoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 revoke 的处理结果；returns the result of the operation.
     */
    public boolean revoke(String id) {
        return jdbc.update("""
                UPDATE gateway_mcp_app_artifact
                   SET status = 'REVOKED'
                 WHERE id = ? AND status = 'ACTIVE'
                """, id) == 1;
    }

    /**
     * 中文说明：执行 map 操作；该方法是 {@code JdbcMcpArtifactMetadataStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the map operation; this method is the invocation entry point on {@code JdbcMcpArtifactMetadataStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpArtifactMetadataStore.map(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @return 返回 map 的处理结果；returns the result of the operation.
     */
    private ArtifactMetadata map(java.sql.ResultSet result)
            throws java.sql.SQLException {
        return new ArtifactMetadata(
                result.getString("id"),
                result.getString("gateway_group_id"),
                result.getString("app_code"),
                result.getString("app_version"),
                result.getString("display_name"),
                result.getString("resource_uri"),
                result.getString("artifact_reference"),
                result.getString("artifact_sha256"),
                result.getLong("size_bytes"),
                result.getString("mime_type"),
                result.getString("content_security_policy"),
                json.stringSet(result.getString("permission_manifest")),
                json.stringSet(result.getString("allowed_origins")),
                result.getString("created_by"),
                result.getTimestamp("created_at").toInstant()
        );
    }

    /**
     * 中文说明：{@code ArtifactMetadata} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责制品元数据相关的职责与边界。
     * English summary: {@code ArtifactMetadata} is an immutable data carrier in the current Gateway module; it owns the artifact metadata-related responsibility and boundary.
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
    public record ArtifactMetadata(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            String appCode,
            /**
             * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            String version,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            String displayName,
            /**
             * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code String}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            String resourceUri,
            /**
             * 中文说明：保存 制品Reference 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by artifact reference; its type is {@code String}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            String artifactReference,
            /**
             * 中文说明：保存 sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by sha256; its type is {@code String}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            String sha256,
            /**
             * 中文说明：保存 sizeBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by size bytes; its type is {@code long}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            long sizeBytes,
            /**
             * 中文说明：保存 mimeType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by mime type; its type is {@code String}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            String mimeType,
            /**
             * 中文说明：保存 content安全策略 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content security policy; its type is {@code String}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            String contentSecurityPolicy,
            /**
             * 中文说明：保存 permissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by permissions; its type is {@code Set<String>}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> permissions,
            /**
             * 中文说明：保存 allowedOrigins 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by allowed origins; its type is {@code Set<String>}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> allowedOrigins,
            /**
             * 中文说明：保存 createdBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created by; its type is {@code String}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            String createdBy,
            /**
             * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant createdAt
    ) {

        /**
         * 中文说明：创建 {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcMcpArtifactMetadataStore.ArtifactMetadata} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
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
        public ArtifactMetadata {
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
}
