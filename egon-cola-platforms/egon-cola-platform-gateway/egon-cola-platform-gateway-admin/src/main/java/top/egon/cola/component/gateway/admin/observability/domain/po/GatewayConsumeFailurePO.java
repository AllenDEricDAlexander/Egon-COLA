package top.egon.cola.component.gateway.admin.observability.domain.po;


import java.time.Instant;

/**
 * 中文说明：{@code GatewayConsumeFailurePO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GatewayConsumeFailurePO相关的职责与边界。
 * English summary: {@code GatewayConsumeFailurePO} is an immutable data carrier in the current Gateway module; it owns the consume failure-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param topic 参数 topic；parameter topic。
 * @param partition 参数 partition；parameter partition。
 * @param offset 参数 offset；parameter offset。
 * @param eventId 参数 事件Id；parameter event id。
 * @param failureCode 参数 failureCode；parameter failure code。
 * @param failureMessage 参数 failure消息；parameter failure message。
 * @param payloadSha256 参数 payloadSha256；parameter payload sha256。
 * @param payloadSize 参数 payloadSize；parameter payload size。
 * @param occurredAt 参数 occurredAt；parameter occurred at。
 */
public record GatewayConsumeFailurePO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 topic 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by topic; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String topic,
        /**
         * 中文说明：保存 partition 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by partition; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int partition,
        /**
         * 中文说明：保存 offset 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by offset; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long offset,
        /**
         * 中文说明：保存 事件Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by event id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String eventId,
        /**
         * 中文说明：保存 failureCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by failure code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String failureCode,
        /**
         * 中文说明：保存 failure消息 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by failure message; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String failureMessage,
        /**
         * 中文说明：保存 payloadSha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by payload sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String payloadSha256,
        /**
         * 中文说明：保存 payloadSize 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by payload size; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int payloadSize,
        /**
         * 中文说明：保存 occurredAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by occurred at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant occurredAt
) {
}
