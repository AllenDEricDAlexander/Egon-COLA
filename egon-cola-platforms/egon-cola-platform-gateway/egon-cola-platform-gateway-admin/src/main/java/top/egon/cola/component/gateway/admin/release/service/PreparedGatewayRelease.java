package top.egon.cola.component.gateway.admin.release.service;


import top.egon.cola.component.gateway.admin.rule.domain.vo.CompiledGatewayRelease;

/**
 * 中文说明：{@code PreparedGatewayRelease} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Prepared发布相关的职责与边界。
 * English summary: {@code PreparedGatewayRelease} is an immutable data carrier in the current Gateway module; it owns the prepared release-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param release 参数 发布；parameter release。
 * @param compiled 参数 compiled；parameter compiled。
 * @param attemptNo 参数 attemptNo；parameter attempt no。
 */
public record PreparedGatewayRelease(
        /**
         * 中文说明：保存 发布 对应的状态、依赖或配置值；字段类型为 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePO}，由 {@code top.egon.cola.component.gateway.admin.release.service.PreparedGatewayRelease} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by release; its type is {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePO}, and {@code top.egon.cola.component.gateway.admin.release.service.PreparedGatewayRelease} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.service.PreparedGatewayRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.service.PreparedGatewayRelease}; do not couple callers to its representation when the owning type exposes an API.
         */
        top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePO release,
        /**
         * 中文说明：保存 compiled 对应的状态、依赖或配置值；字段类型为 {@code CompiledGatewayRelease}，由 {@code top.egon.cola.component.gateway.admin.release.service.PreparedGatewayRelease} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by compiled; its type is {@code CompiledGatewayRelease}, and {@code top.egon.cola.component.gateway.admin.release.service.PreparedGatewayRelease} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.service.PreparedGatewayRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.service.PreparedGatewayRelease}; do not couple callers to its representation when the owning type exposes an API.
         */
        CompiledGatewayRelease compiled,
        /**
         * 中文说明：保存 attemptNo 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.release.service.PreparedGatewayRelease} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt no; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.release.service.PreparedGatewayRelease} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.service.PreparedGatewayRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.service.PreparedGatewayRelease}; do not couple callers to its representation when the owning type exposes an API.
         */
        int attemptNo
) {
}
