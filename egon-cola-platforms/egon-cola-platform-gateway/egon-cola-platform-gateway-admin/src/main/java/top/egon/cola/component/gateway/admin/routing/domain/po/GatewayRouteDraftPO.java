package top.egon.cola.component.gateway.admin.routing.domain.po;


import java.time.Instant;
import java.util.Map;


/**
 * 中文说明：{@code GatewayRouteDraftPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责路由草稿相关的职责与边界。
 * English summary: {@code GatewayRouteDraftPO} is an immutable data carrier in the current Gateway module; it owns the route draft-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param routeId 参数 路由Id；parameter route id。
 * @param operationId 参数 操作Id；parameter operation id。
 * @param content 参数 content；parameter content。
 * @param enabled 参数 enabled；parameter enabled。
 * @param updatedAt 参数 updatedAt；parameter updated at。
 * @param updatedBy 参数 updatedBy；parameter updated by。
 */
public record GatewayRouteDraftPO(
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 路由Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by route id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String routeId,
        /**
         * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String operationId,
        /**
         * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> content,
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean enabled,
        /**
         * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant updatedAt,
        /**
         * 中文说明：保存 updatedBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by updated by; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String updatedBy
) {
}
