package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * 中文说明：{@code GatewayGroupEntity} 是类型，位于当前 Gateway 模块的相关包中，负责网关GroupEntity相关的职责与边界。
 * English summary: {@code GatewayGroupEntity} is a type in the current Gateway module; it owns the gateway group entity-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Entity
@Table(name = "gateway_group")
public class GatewayGroupEntity {

    /**
     * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayGroupEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Id
    private String id;

    /**
     * 中文说明：保存 网关GroupCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by gateway group code; its type is {@code String}, and {@code GatewayGroupEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "gateway_group_code", nullable = false)
    private String gatewayGroupCode;

    /**
     * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code GatewayGroupEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    /**
     * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayGroupEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(nullable = false)
    private String env;

    /**
     * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code GatewayGroupEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(nullable = false)
    private String namespace;

    /**
     * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code GatewayGroupEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    private String description;

    /**
     * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayGroupEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayGroupEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(nullable = false)
    private boolean enabled;

    /**
     * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayGroupEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code GatewayGroupEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Version
    @Column(nullable = false)
    private long revision;

    /**
     * 中文说明：保存 deleted 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayGroupEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by deleted; its type is {@code boolean}, and {@code GatewayGroupEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(nullable = false)
    private boolean deleted;

    /**
     * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayGroupEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code GatewayGroupEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 中文说明：保存 createdBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by created by; its type is {@code String}, and {@code GatewayGroupEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    /**
     * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayGroupEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code GatewayGroupEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 中文说明：保存 updatedBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupEntity} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by updated by; its type is {@code String}, and {@code GatewayGroupEntity} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupEntity}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    /**
     * 中文说明：创建 {@code GatewayGroupEntity} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayGroupEntity} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    protected GatewayGroupEntity() {
    }

    /**
     * 中文说明：创建 {@code GatewayGroupEntity} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayGroupEntity} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupCode 参数 网关GroupCode；parameter gateway group code。
     * @param displayName 参数 displayName；parameter display name。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param description 参数 description；parameter description。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     */
    public GatewayGroupEntity(
            String id,
            String gatewayGroupCode,
            String displayName,
            String env,
            String namespace,
            String description,
            String actor,
            Instant now) {
        this.id = required(id, "id");
        this.gatewayGroupCode = required(
                gatewayGroupCode,
                "gatewayGroupCode"
        );
        this.displayName = required(displayName, "displayName");
        this.env = required(env, "env");
        this.namespace = required(namespace, "namespace");
        this.description = description;
        enabled = true;
        createdAt = now;
        updatedAt = now;
        createdBy = required(actor, "actor");
        updatedBy = actor;
    }

    /**
     * 中文说明：执行 update 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.update(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param displayName 参数 displayName；parameter display name。
     * @param description 参数 description；parameter description。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     */
    public void update(
            String displayName,
            String description,
            String actor,
            Instant now) {
        this.displayName = required(displayName, "displayName");
        this.description = description;
        updatedBy = required(actor, "actor");
        updatedAt = now;
    }

    /**
     * 中文说明：执行 setEnabled 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set enabled operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.setEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param enabled 参数 enabled；parameter enabled。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     */
    public void setEnabled(boolean enabled, String actor, Instant now) {
        this.enabled = enabled;
        updatedBy = required(actor, "actor");
        updatedAt = now;
    }

    /**
     * 中文说明：执行 getId 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get id operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.getId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getId 的处理结果；returns the result of the operation.
     */
    public String getId() {
        return id;
    }

    /**
     * 中文说明：执行 get网关GroupCode 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get gateway group code operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.getGatewayGroupCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get网关GroupCode 的处理结果；returns the result of the operation.
     */
    public String getGatewayGroupCode() {
        return gatewayGroupCode;
    }

    /**
     * 中文说明：执行 getDisplayName 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get display name operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.getDisplayName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getDisplayName 的处理结果；returns the result of the operation.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 中文说明：执行 getEnv 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get env operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.getEnv(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getEnv 的处理结果；returns the result of the operation.
     */
    public String getEnv() {
        return env;
    }

    /**
     * 中文说明：执行 get命名空间 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get namespace operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.getNamespace(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get命名空间 的处理结果；returns the result of the operation.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * 中文说明：执行 getDescription 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get description operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.getDescription(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getDescription 的处理结果；returns the result of the operation.
     */
    public String getDescription() {
        return description;
    }

    /**
     * 中文说明：执行 isEnabled 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the is enabled operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.isEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 isEnabled 的处理结果；returns the result of the operation.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 中文说明：执行 getRevision 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get revision operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.getRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getRevision 的处理结果；returns the result of the operation.
     */
    public long getRevision() {
        return revision;
    }

    /**
     * 中文说明：执行 isDeleted 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the is deleted operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.isDeleted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 isDeleted 的处理结果；returns the result of the operation.
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * 中文说明：执行 getCreatedAt 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get created at operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.getCreatedAt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getCreatedAt 的处理结果；returns the result of the operation.
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 中文说明：执行 getUpdatedAt 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get updated at operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.getUpdatedAt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getUpdatedAt 的处理结果；returns the result of the operation.
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayGroupEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayGroupEntity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupEntity.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
