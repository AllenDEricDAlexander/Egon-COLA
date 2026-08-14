package top.egon.cola.component.gateway.admin.credential.domain.vo;


import java.time.Instant;


/**
 * 中文说明：{@code GatewayCredentialVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责凭证View相关的职责与边界。
 * English summary: {@code GatewayCredentialVO} is an immutable data carrier in the current Gateway module; it owns the credential view-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param accessKey 参数 access键；parameter access key。
 * @param status 参数 status；parameter status。
 * @param validFrom 参数 validFrom；parameter valid from。
 * @param validUntil 参数 validUntil；parameter valid until。
 */
public record GatewayCredentialVO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 access键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by access key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String accessKey,
        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String status,
        /**
         * 中文说明：保存 validFrom 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by valid from; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant validFrom,
        /**
         * 中文说明：保存 validUntil 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by valid until; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayCredentialVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant validUntil
) {
}
