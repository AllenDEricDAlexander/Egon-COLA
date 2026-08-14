package top.egon.cola.component.gateway.admin.rule.service;


import java.util.List;
import java.util.Map;


/**
 * 中文说明：{@code GatewayYamlLeafLocation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责LeafLocation相关的职责与边界。
 * English summary: {@code GatewayYamlLeafLocation} is an immutable data carrier in the current Gateway module; it owns the leaf location-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param parent 参数 parent；parameter parent。
 * @param key 参数 键；parameter key。
 * @param value 参数 值；parameter value。
 * @param ancestors 参数 ancestors；parameter ancestors。
 */
public record GatewayYamlLeafLocation(
        /**
         * 中文说明：保存 parent 对应的状态、依赖或配置值；字段类型为 {@code Map<Object, Object>}，由 {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by parent; its type is {@code Map<Object, Object>}, and {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<Object, Object> parent,
        /**
         * 中文说明：保存 键 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by key; its type is {@code Object}, and {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation}; do not couple callers to its representation when the owning type exposes an API.
         */
        Object key,
        /**
         * 中文说明：保存 值 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by value; its type is {@code Object}, and {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation}; do not couple callers to its representation when the owning type exposes an API.
         */
        Object value,
        /**
         * 中文说明：保存 ancestors 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayYamlParentLink>}，由 {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by ancestors; its type is {@code List<GatewayYamlParentLink>}, and {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<GatewayYamlParentLink> ancestors) {
}
