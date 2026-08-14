package top.egon.cola.component.gateway.admin.scope.domain;


/**
 * 中文说明：{@code GatewayPhysicalApplicationKey} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责PhysicalApplication键相关的职责与边界。
 * English summary: {@code GatewayPhysicalApplicationKey} is an immutable data carrier in the current Gateway module; it owns the physical application key-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param bizCode 参数 bizCode；parameter biz code。
 * @param env 参数 env；parameter env。
 * @param appCode 参数 appCode；parameter app code。
 */
public record GatewayPhysicalApplicationKey(
        /**
         * 中文说明：保存 bizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by biz code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey}; do not couple callers to its representation when the owning type exposes an API.
         */
        String bizCode,
        /**
         * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey}; do not couple callers to its representation when the owning type exposes an API.
         */
        String env,
        /**
         * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey}; do not couple callers to its representation when the owning type exposes an API.
         */
        String appCode
) {
}
