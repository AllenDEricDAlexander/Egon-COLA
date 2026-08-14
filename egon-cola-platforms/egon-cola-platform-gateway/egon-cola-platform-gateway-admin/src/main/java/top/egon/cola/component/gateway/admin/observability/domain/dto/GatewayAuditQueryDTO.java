package top.egon.cola.component.gateway.admin.observability.domain.dto;


/**
 * 中文说明：{@code GatewayAuditQueryDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审计Query相关的职责与边界。
 * English summary: {@code GatewayAuditQueryDTO} is an immutable data carrier in the current Gateway module; it owns the audit query-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param env 参数 env；parameter env。
 * @param namespace 参数 命名空间；parameter namespace。
 * @param actorId 参数 actorId；parameter actor id。
 * @param resourceId 参数 资源Id；parameter resource id。
 * @param traceId 参数 traceId；parameter trace id。
 * @param successful 参数 successful；parameter successful。
 * @param page 参数 page；parameter page。
 * @param size 参数 size；parameter size。
 */
public record GatewayAuditQueryDTO(
        /**
         * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String env,
        /**
         * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String namespace,
        /**
         * 中文说明：保存 actorId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by actor id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String actorId,
        /**
         * 中文说明：保存 资源Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by resource id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String resourceId,
        /**
         * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String traceId,
        /**
         * 中文说明：保存 successful 对应的状态、依赖或配置值；字段类型为 {@code Boolean}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by successful; its type is {@code Boolean}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Boolean successful,
        /**
         * 中文说明：保存 page 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by page; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int page,
        /**
         * 中文说明：保存 size 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by size; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int size
) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param actorId 参数 actorId；parameter actor id。
     * @param resourceId 参数 资源Id；parameter resource id。
     * @param traceId 参数 traceId；parameter trace id。
     * @param successful 参数 successful；parameter successful。
     * @param page 参数 page；parameter page。
     * @param size 参数 size；parameter size。
     */
    public GatewayAuditQueryDTO {
        if (page < 1 || size < 1 || size > 200) {
            throw new IllegalArgumentException(
                    "invalid audit page request"
            );
        }
    }
}
