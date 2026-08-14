package top.egon.cola.component.gateway.admin.runtime.service;


/**
 * 中文说明：{@code GatewayProjectionCounts} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责投影Counts相关的职责与边界。
 * English summary: {@code GatewayProjectionCounts} is an immutable data carrier in the current Gateway module; it owns the projection counts-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param readyEngines 参数 readyEngines；parameter ready engines。
 * @param totalEngines 参数 totalEngines；parameter total engines。
 * @param inconsistentGroups 参数 inconsistentGroups；parameter inconsistent groups。
 * @param activeProviders 参数 activeProviders；parameter active providers。
 * @param abnormalProviders 参数 abnormalProviders；parameter abnormal providers。
 * @param stale 参数 stale；parameter stale。
 */
public record GatewayProjectionCounts(
        /**
         * 中文说明：保存 readyEngines 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by ready engines; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts}; do not couple callers to its representation when the owning type exposes an API.
         */
        long readyEngines,
        /**
         * 中文说明：保存 totalEngines 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by total engines; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts}; do not couple callers to its representation when the owning type exposes an API.
         */
        long totalEngines,
        /**
         * 中文说明：保存 inconsistentGroups 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by inconsistent groups; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts}; do not couple callers to its representation when the owning type exposes an API.
         */
        long inconsistentGroups,
        /**
         * 中文说明：保存 activeProviders 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by active providers; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts}; do not couple callers to its representation when the owning type exposes an API.
         */
        long activeProviders,
        /**
         * 中文说明：保存 abnormalProviders 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by abnormal providers; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts}; do not couple callers to its representation when the owning type exposes an API.
         */
        long abnormalProviders,
        /**
         * 中文说明：保存 stale 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by stale; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean stale
) {
}
