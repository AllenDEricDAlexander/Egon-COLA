package top.egon.cola.component.gateway.admin.scope.domain.vo;


/**
 * 中文说明：{@code GatewayScopeVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责ScopeView相关的职责与边界。
 * English summary: {@code GatewayScopeVO} is an immutable data carrier in the current Gateway module; it owns the scope view-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param bindingId 参数 bindingId；parameter binding id。
 * @param bizCode 参数 bizCode；parameter biz code。
 * @param namespace 参数 命名空间；parameter namespace。
 * @param env 参数 env；parameter env。
 * @param appCode 参数 appCode；parameter app code。
 * @param appName 参数 appName；parameter app name。
 * @param connected 参数 connected；parameter connected。
 * @param gatewayApplicationId 参数 网关ApplicationId；parameter gateway application id。
 */
public record GatewayScopeVO(
        /**
         * 中文说明：保存 bindingId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by binding id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String bindingId,
        /**
         * 中文说明：保存 bizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by biz code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String bizCode,
        /**
         * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String namespace,
        /**
         * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String env,
        /**
         * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String appCode,
        /**
         * 中文说明：保存 appName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by app name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String appName,
        /**
         * 中文说明：保存 connected 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by connected; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean connected,
        /**
         * 中文说明：保存 网关ApplicationId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway application id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayApplicationId
) {
}
