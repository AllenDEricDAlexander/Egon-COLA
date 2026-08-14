package top.egon.cola.component.gateway.admin.observability.domain.dto;


import java.time.Instant;

/**
 * 中文说明：{@code GatewayRequestPointDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责请求Point相关的职责与边界。
 * English summary: {@code GatewayRequestPointDTO} is an immutable data carrier in the current Gateway module; it owns the request point-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param time 参数 time；parameter time。
 * @param requests 参数 requests；parameter requests。
 * @param errors 参数 errors；parameter errors。
 * @param p50 参数 p50；parameter p50。
 * @param p95 参数 p95；parameter p95。
 * @param p99 参数 p99；parameter p99。
 */
public record GatewayRequestPointDTO(
        /**
         * 中文说明：保存 time 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by time; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant time,
        /**
         * 中文说明：保存 requests 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by requests; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long requests,
        /**
         * 中文说明：保存 errors 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by errors; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long errors,
        /**
         * 中文说明：保存 p50 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by p50; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long p50,
        /**
         * 中文说明：保存 p95 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by p95; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long p95,
        /**
         * 中文说明：保存 p99 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by p99; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long p99
) {
}
