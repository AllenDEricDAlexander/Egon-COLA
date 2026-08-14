package top.egon.cola.component.gateway.admin.routing.domain.po;


import java.time.Instant;
import java.util.Map;


/**
 * 中文说明：{@code GatewayPolicyDraftPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责策略草稿相关的职责与边界。
 * English summary: {@code GatewayPolicyDraftPO} is an immutable data carrier in the current Gateway module; it owns the policy draft-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param policyId 参数 策略Id；parameter policy id。
 * @param policyType 参数 策略Type；parameter policy type。
 * @param policyScope 参数 策略Scope；parameter policy scope。
 * @param content 参数 content；parameter content。
 * @param enabled 参数 enabled；parameter enabled。
 * @param updatedAt 参数 updatedAt；parameter updated at。
 * @param updatedBy 参数 updatedBy；parameter updated by。
 */
public record GatewayPolicyDraftPO(
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 策略Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String policyId,
        /**
         * 中文说明：保存 策略Type 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy type; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String policyType,
        /**
         * 中文说明：保存 策略Scope 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy scope; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String policyScope,
        /**
         * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> content,
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean enabled,
        /**
         * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant updatedAt,
        /**
         * 中文说明：保存 updatedBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by updated by; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String updatedBy
) {
}
