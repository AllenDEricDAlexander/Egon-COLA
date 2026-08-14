package top.egon.cola.component.gateway.admin.rule.service;


import java.util.Map;


/**
 * 中文说明：{@code GatewayYamlPrefixMatch} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责PrefixMatch相关的职责与边界。
 * English summary: {@code GatewayYamlPrefixMatch} is an immutable data carrier in the current Gateway module; it owns the prefix match-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param map 参数 map；parameter map。
 * @param nextIndex 参数 next索引；parameter next index。
 */
public record GatewayYamlPrefixMatch(
        /**
         * 中文说明：保存 map 对应的状态、依赖或配置值；字段类型为 {@code Map<Object, Object>}，由 {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlPrefixMatch} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by map; its type is {@code Map<Object, Object>}, and {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlPrefixMatch} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlPrefixMatch} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlPrefixMatch}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<Object, Object> map,
        /**
         * 中文说明：保存 next索引 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlPrefixMatch} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by next index; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlPrefixMatch} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlPrefixMatch} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.rule.service.GatewayYamlPrefixMatch}; do not couple callers to its representation when the owning type exposes an API.
         */
        int nextIndex) {
}
