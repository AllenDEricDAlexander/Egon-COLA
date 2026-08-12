package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * 中文说明：{@code GatewayApplicationEntity} 是类型，位于当前 Gateway 模块的相关包中，负责网关ApplicationEntity相关的职责与边界。
 * English summary: {@code GatewayApplicationEntity} is a type in the current Gateway module; it owns the gateway application entity-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Entity
@Table(name = "gateway_application")
public class GatewayApplicationEntity {

    /**
     * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayApplicationEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Id
    private String id;

    /**
     * 中文说明：保存 applicationCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by application code; its type is {@code String}, and {@code GatewayApplicationEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "application_code", nullable = false, updatable = false)
    private String applicationCode;

    /**
     * 中文说明：保存 bizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by biz code; its type is {@code String}, and {@code GatewayApplicationEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "biz_code", nullable = false, updatable = false)
    private String bizCode;

    /**
     * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code GatewayApplicationEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    /**
     * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayApplicationEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(nullable = false, updatable = false)
    private String env;

    /**
     * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code GatewayApplicationEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(nullable = false, updatable = false)
    private String namespace;

    /**
     * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code GatewayApplicationEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    private String description;

    /**
     * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayApplicationEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code GatewayApplicationEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Version
    @Column(nullable = false)
    private long revision;

    /**
     * 中文说明：保存 deleted 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayApplicationEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by deleted; its type is {@code boolean}, and {@code GatewayApplicationEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(nullable = false)
    private boolean deleted;

    /**
     * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayApplicationEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code GatewayApplicationEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * 中文说明：保存 createdBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by created by; its type is {@code String}, and {@code GatewayApplicationEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    /**
     * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayApplicationEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code GatewayApplicationEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 中文说明：保存 updatedBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by updated by; its type is {@code String}, and {@code GatewayApplicationEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    /**
     * 中文说明：创建 {@code GatewayApplicationEntity} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayApplicationEntity} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    protected GatewayApplicationEntity() {
    }

    /**
     * 中文说明：创建 {@code GatewayApplicationEntity} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayApplicationEntity} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param id 参数 id；parameter id。
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param applicationCode 参数 applicationCode；parameter application code。
     * @param displayName 参数 displayName；parameter display name。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param description 参数 description；parameter description。
     * @param actorId 参数 actorId；parameter actor id。
     * @param now 参数 now；parameter now。
     */
    public GatewayApplicationEntity(
            String id,
            String bizCode,
            String applicationCode,
            String displayName,
            String env,
            String namespace,
            String description,
            String actorId,
            Instant now) {
        this.id = id;
        this.bizCode = bizCode;
        this.applicationCode = applicationCode;
        this.displayName = displayName;
        this.env = env;
        this.namespace = namespace;
        this.description = description;
        this.createdAt = now;
        this.createdBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    /**
     * 中文说明：执行 update 操作；该方法是 {@code GatewayApplicationEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update operation; this method is the invocation entry point on {@code GatewayApplicationEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationEntity.update(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param displayName 参数 displayName；parameter display name。
     * @param description 参数 description；parameter description。
     * @param actorId 参数 actorId；parameter actor id。
     * @param now 参数 now；parameter now。
     */
    public void update(
            String displayName,
            String description,
            String actorId,
            Instant now) {
        this.displayName = displayName;
        this.description = description;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    /**
     * 中文说明：执行 getId 操作；该方法是 {@code GatewayApplicationEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get id operation; this method is the invocation entry point on {@code GatewayApplicationEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationEntity.getId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getId 的处理结果；returns the result of the operation.
     */
    public String getId() {
        return id;
    }

    /**
     * 中文说明：执行 getApplicationCode 操作；该方法是 {@code GatewayApplicationEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get application code operation; this method is the invocation entry point on {@code GatewayApplicationEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationEntity.getApplicationCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getApplicationCode 的处理结果；returns the result of the operation.
     */
    public String getApplicationCode() {
        return applicationCode;
    }

    /**
     * 中文说明：执行 getBizCode 操作；该方法是 {@code GatewayApplicationEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get biz code operation; this method is the invocation entry point on {@code GatewayApplicationEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationEntity.getBizCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getBizCode 的处理结果；returns the result of the operation.
     */
    public String getBizCode() {
        return bizCode;
    }

    /**
     * 中文说明：执行 getDisplayName 操作；该方法是 {@code GatewayApplicationEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get display name operation; this method is the invocation entry point on {@code GatewayApplicationEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationEntity.getDisplayName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getDisplayName 的处理结果；returns the result of the operation.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 中文说明：执行 getEnv 操作；该方法是 {@code GatewayApplicationEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get env operation; this method is the invocation entry point on {@code GatewayApplicationEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationEntity.getEnv(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getEnv 的处理结果；returns the result of the operation.
     */
    public String getEnv() {
        return env;
    }

    /**
     * 中文说明：执行 get命名空间 操作；该方法是 {@code GatewayApplicationEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get namespace operation; this method is the invocation entry point on {@code GatewayApplicationEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationEntity.getNamespace(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get命名空间 的处理结果；returns the result of the operation.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * 中文说明：执行 getDescription 操作；该方法是 {@code GatewayApplicationEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get description operation; this method is the invocation entry point on {@code GatewayApplicationEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationEntity.getDescription(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getDescription 的处理结果；returns the result of the operation.
     */
    public String getDescription() {
        return description;
    }

    /**
     * 中文说明：执行 getRevision 操作；该方法是 {@code GatewayApplicationEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get revision operation; this method is the invocation entry point on {@code GatewayApplicationEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationEntity.getRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getRevision 的处理结果；returns the result of the operation.
     */
    public long getRevision() {
        return revision;
    }

    /**
     * 中文说明：执行 getCreatedAt 操作；该方法是 {@code GatewayApplicationEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get created at operation; this method is the invocation entry point on {@code GatewayApplicationEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationEntity.getCreatedAt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getCreatedAt 的处理结果；returns the result of the operation.
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 中文说明：执行 getUpdatedAt 操作；该方法是 {@code GatewayApplicationEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get updated at operation; this method is the invocation entry point on {@code GatewayApplicationEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationEntity.getUpdatedAt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getUpdatedAt 的处理结果；returns the result of the operation.
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
