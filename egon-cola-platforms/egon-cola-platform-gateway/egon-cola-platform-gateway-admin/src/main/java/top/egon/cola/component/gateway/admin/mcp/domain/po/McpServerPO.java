package top.egon.cola.component.gateway.admin.mcp.domain.po;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

/**
 * 中文说明：{@code McpServerPO} 是类型，位于当前 Gateway 模块的相关包中，负责MCP服务器Entity相关的职责与边界。
 * English summary: {@code McpServerPO} is a type in the current Gateway module; it owns the mcp server entity-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Entity
@Table(name = "gateway_mcp_server")
public class McpServerPO {

    /**
     * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Id
    private String id;

    /**
     * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "gateway_group_id", nullable = false)
    private String gatewayGroupId;

    /**
     * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "server_code", nullable = false)
    private String serverCode;

    /**
     * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    /**
     * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    private String description;

    /**
     * 中文说明：保存 instructions 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by instructions; its type is {@code String}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    private String instructions;

    /**
     * 中文说明：保存 dialects 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by dialects; its type is {@code Set<String>}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Set<String> dialects;

    /**
     * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code String}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "resource_uri", nullable = false)
    private String resourceUri;

    /**
     * 中文说明：保存 listCacheTtlSeconds 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by list cache ttl seconds; its type is {@code long}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "list_cache_ttl_seconds", nullable = false)
    private long listCacheTtlSeconds;

    /**
     * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(nullable = false)
    private boolean enabled;

    /**
     * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Version
    @Column(nullable = false)
    private long revision;

    /**
     * 中文说明：保存 deleted 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by deleted; its type is {@code boolean}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(nullable = false)
    private boolean deleted;

    /**
     * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 中文说明：保存 createdBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by created by; its type is {@code String}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    /**
     * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 中文说明：保存 updatedBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by updated by; its type is {@code String}, and {@code McpServerPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    /**
     * 中文说明：创建 {@code McpServerPO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpServerPO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    protected McpServerPO() {
    }

    /**
     * 中文说明：创建 {@code McpServerPO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpServerPO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param displayName 参数 displayName；parameter display name。
     * @param description 参数 description；parameter description。
     * @param instructions 参数 instructions；parameter instructions。
     * @param dialects 参数 dialects；parameter dialects。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param listCacheTtlSeconds 参数 listCacheTtlSeconds；parameter list cache ttl seconds。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     */
    public McpServerPO(
            String id,
            String gatewayGroupId,
            String serverCode,
            String displayName,
            String description,
            String instructions,
            Set<String> dialects,
            String resourceUri,
            long listCacheTtlSeconds,
            AdminActor actor,
            Instant now) {
        this.id = required(id, "id");
        this.gatewayGroupId = required(gatewayGroupId, "gatewayGroupId");
        this.serverCode = required(serverCode, "serverCode");
        this.displayName = required(displayName, "displayName");
        this.description = optional(description);
        this.instructions = optional(instructions);
        this.dialects = nonEmpty(dialects, "dialects");
        this.resourceUri = resourceUri(resourceUri);
        this.listCacheTtlSeconds = nonNegative(
                listCacheTtlSeconds,
                "listCacheTtlSeconds"
        );
        enabled = true;
        createdAt = now;
        updatedAt = now;
        createdBy = actor(actor);
        updatedBy = createdBy;
    }

    /**
     * 中文说明：执行 update 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.update(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param displayName 参数 displayName；parameter display name。
     * @param description 参数 description；parameter description。
     * @param instructions 参数 instructions；parameter instructions。
     * @param dialects 参数 dialects；parameter dialects。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param listCacheTtlSeconds 参数 listCacheTtlSeconds；parameter list cache ttl seconds。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     */
    public void update(
            String displayName,
            String description,
            String instructions,
            Set<String> dialects,
            String resourceUri,
            long listCacheTtlSeconds,
            boolean enabled,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        assertRevision(expectedRevision);
        this.displayName = required(displayName, "displayName");
        this.description = optional(description);
        this.instructions = optional(instructions);
        this.dialects = nonEmpty(dialects, "dialects");
        this.resourceUri = resourceUri(resourceUri);
        this.listCacheTtlSeconds = nonNegative(
                listCacheTtlSeconds,
                "listCacheTtlSeconds"
        );
        this.enabled = enabled;
        updatedAt = now;
        updatedBy = actor(actor);
    }

    /**
     * 中文说明：执行 softDelete 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the soft delete operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.softDelete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     */
    public void softDelete(
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        assertRevision(expectedRevision);
        deleted = true;
        enabled = false;
        updatedAt = now;
        updatedBy = actor(actor);
    }

    /**
     * 中文说明：执行 assertRevision 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the assert revision operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.assertRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     */
    public void assertRevision(long expectedRevision) {
        if (revision != expectedRevision) {
            throw new GatewayAdminRevisionConflictException(revision);
        }
    }

    /**
     * 中文说明：执行 getId 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get id operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getId 的处理结果；returns the result of the operation.
     */
    public String getId() {
        return id;
    }

    /**
     * 中文说明：执行 get网关GroupId 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get gateway group id operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getGatewayGroupId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get网关GroupId 的处理结果；returns the result of the operation.
     */
    public String getGatewayGroupId() {
        return gatewayGroupId;
    }

    /**
     * 中文说明：执行 get服务器Code 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get server code operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getServerCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get服务器Code 的处理结果；returns the result of the operation.
     */
    public String getServerCode() {
        return serverCode;
    }

    /**
     * 中文说明：执行 getDisplayName 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get display name operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getDisplayName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getDisplayName 的处理结果；returns the result of the operation.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 中文说明：执行 getDescription 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get description operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getDescription(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getDescription 的处理结果；returns the result of the operation.
     */
    public String getDescription() {
        return description;
    }

    /**
     * 中文说明：执行 getInstructions 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get instructions operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getInstructions(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getInstructions 的处理结果；returns the result of the operation.
     */
    public String getInstructions() {
        return instructions;
    }

    /**
     * 中文说明：执行 getDialects 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get dialects operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getDialects(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getDialects 的处理结果；returns the result of the operation.
     */
    public Set<String> getDialects() {
        return Set.copyOf(dialects);
    }

    /**
     * 中文说明：执行 get资源Uri 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get resource uri operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getResourceUri(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get资源Uri 的处理结果；returns the result of the operation.
     */
    public String getResourceUri() {
        return resourceUri;
    }

    /**
     * 中文说明：执行 getListCacheTtlSeconds 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get list cache ttl seconds operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getListCacheTtlSeconds(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getListCacheTtlSeconds 的处理结果；returns the result of the operation.
     */
    public long getListCacheTtlSeconds() {
        return listCacheTtlSeconds;
    }

    /**
     * 中文说明：执行 isEnabled 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the is enabled operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.isEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 isEnabled 的处理结果；returns the result of the operation.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 中文说明：执行 getRevision 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get revision operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getRevision 的处理结果；returns the result of the operation.
     */
    public long getRevision() {
        return revision;
    }

    /**
     * 中文说明：执行 isDeleted 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the is deleted operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.isDeleted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 isDeleted 的处理结果；returns the result of the operation.
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * 中文说明：执行 getCreatedAt 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get created at operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getCreatedAt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getCreatedAt 的处理结果；returns the result of the operation.
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 中文说明：执行 getCreatedBy 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get created by operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getCreatedBy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getCreatedBy 的处理结果；returns the result of the operation.
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 中文说明：执行 getUpdatedAt 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get updated at operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getUpdatedAt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getUpdatedAt 的处理结果；returns the result of the operation.
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 中文说明：执行 getUpdatedBy 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get updated by operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.getUpdatedBy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getUpdatedBy 的处理结果；returns the result of the operation.
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * 中文说明：执行 actor 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the actor operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.actor(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @return 返回 actor 的处理结果；returns the result of the operation.
     */
    private static String actor(AdminActor actor) {
        return java.util.Objects.requireNonNull(actor, "actor").actorId();
    }

    /**
     * 中文说明：执行 nonEmpty 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the non empty operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.nonEmpty(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param field 参数 field；parameter field。
     * @return 返回 nonEmpty 的处理结果；returns the result of the operation.
     */
    private static Set<String> nonEmpty(Set<String> values, String field) {
        Set<String> copy = Set.copyOf(values == null ? Set.of() : values);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return copy;
    }

    /**
     * 中文说明：执行 nonNegative 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the non negative operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.nonNegative(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 nonNegative 的处理结果；returns the result of the operation.
     */
    private static long nonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    /**
     * 中文说明：执行 optional 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.optional(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 optional 的处理结果；returns the result of the operation.
     */
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 中文说明：执行 资源Uri 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resource uri operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.resourceUri(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 资源Uri 的处理结果；returns the result of the operation.
     */
    private static String resourceUri(String value) {
        URI uri;
        try {
            uri = URI.create(required(value, "resourceUri")).normalize();
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException(
                    "resourceUri must be a valid URI", invalid);
        }
        if (!uri.isAbsolute() || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "resourceUri must be absolute and must not contain a fragment"
            );
        }
        return uri.toString();
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpServerPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpServerPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerPO.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
