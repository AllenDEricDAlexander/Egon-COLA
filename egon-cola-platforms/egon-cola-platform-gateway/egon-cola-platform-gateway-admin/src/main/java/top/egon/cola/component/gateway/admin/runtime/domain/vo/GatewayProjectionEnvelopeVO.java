package top.egon.cola.component.gateway.admin.runtime.domain.vo;


import java.time.Instant;

/**
 * 中文说明：{@code GatewayProjectionEnvelopeVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责投影Envelope相关的职责与边界。
 * English summary: {@code GatewayProjectionEnvelopeVO} is an immutable data carrier in the current Gateway module; it owns the projection envelope-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param value 参数 值；parameter value。
 * @param observedAt 参数 observedAt；parameter observed at。
 * @param source 参数 source；parameter source。
 * @param stale 参数 stale；parameter stale。
 * @param refreshError 参数 refreshError；parameter refresh error。
 */
public record GatewayProjectionEnvelopeVO<T>(
        /**
         * 中文说明：保存 值 对应的状态、依赖或配置值；字段类型为 {@code T}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by value; its type is {@code T}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        T value,
        /**
         * 中文说明：保存 observedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observed at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant observedAt,
        /**
         * 中文说明：保存 source 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by source; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String source,
        /**
         * 中文说明：保存 stale 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by stale; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean stale,
        /**
         * 中文说明：保存 refreshError 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by refresh error; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String refreshError
) {
}
