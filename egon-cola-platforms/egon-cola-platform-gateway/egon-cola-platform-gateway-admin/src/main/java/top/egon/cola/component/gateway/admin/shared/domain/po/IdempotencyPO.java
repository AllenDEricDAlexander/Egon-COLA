package top.egon.cola.component.gateway.admin.shared.domain.po;


import java.time.Instant;
import java.util.Map;


/**
 * 中文说明：{@code IdempotencyPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Record相关的职责与边界。
 * English summary: {@code IdempotencyPO} is an immutable data carrier in the current Gateway module; it owns the record-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param scopeType 参数 scopeType；parameter scope type。
 * @param scopeId 参数 scopeId；parameter scope id。
 * @param key 参数 键；parameter key。
 * @param payloadSha256 参数 payloadSha256；parameter payload sha256。
 * @param resourceId 参数 资源Id；parameter resource id。
 * @param response 参数 响应；parameter response。
 * @param createdAt 参数 createdAt；parameter created at。
 * @param expiresAt 参数 expiresAt；parameter expires at。
 */
public record IdempotencyPO(
        /**
         * 中文说明：保存 scopeType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by scope type; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String scopeType,
        /**
         * 中文说明：保存 scopeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by scope id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String scopeId,
        /**
         * 中文说明：保存 键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String key,
        /**
         * 中文说明：保存 payloadSha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by payload sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String payloadSha256,
        /**
         * 中文说明：保存 资源Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by resource id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String resourceId,
        /**
         * 中文说明：保存 响应 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by response; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> response,
        /**
         * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant createdAt,
        /**
         * 中文说明：保存 expiresAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expires at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant expiresAt
) {
}
