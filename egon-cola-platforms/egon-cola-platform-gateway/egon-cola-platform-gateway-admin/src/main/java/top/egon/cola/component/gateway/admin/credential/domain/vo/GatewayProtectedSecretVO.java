package top.egon.cola.component.gateway.admin.credential.domain.vo;


/**
 * 中文说明：{@code GatewayProtectedSecretVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GatewayProtectedSecretVO相关的职责与边界。
 * English summary: {@code GatewayProtectedSecretVO} is an immutable data carrier in the current Gateway module; it owns the protected secret-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param ciphertext 参数 ciphertext；parameter ciphertext。
 * @param keyVersion 参数 键Version；parameter key version。
 */
public record GatewayProtectedSecretVO(
/**
 * 中文说明：保存 ciphertext 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayProtectedSecretVO} 在其生命周期内读取或更新。
 * English summary: Holds the state, dependency, or configuration represented by ciphertext; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayProtectedSecretVO} reads or updates it during its lifecycle.
 *
 * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayProtectedSecretVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayProtectedSecretVO}; do not couple callers to its representation when the owning type exposes an API.
 */
String ciphertext,
/**
 * 中文说明：保存 键Version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayProtectedSecretVO} 在其生命周期内读取或更新。
 * English summary: Holds the state, dependency, or configuration represented by key version; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayProtectedSecretVO} reads or updates it during its lifecycle.
 *
 * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayProtectedSecretVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.credential.domain.vo.GatewayProtectedSecretVO}; do not couple callers to its representation when the owning type exposes an API.
 */
String keyVersion) {
}
