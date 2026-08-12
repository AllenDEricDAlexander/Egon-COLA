package top.egon.cola.component.gateway.admin.application.credential;

/**
 * 中文说明：{@code GatewaySecretProtector} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关SecretProtector相关的职责与边界。
 * English summary: {@code GatewaySecretProtector} is an interface contract in the current Gateway module; it owns the gateway secret protector-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewaySecretProtector {

    /**
     * 中文说明：执行 protect 操作；该方法是 {@code GatewaySecretProtector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the protect operation; this method is the invocation entry point on {@code GatewaySecretProtector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecretProtector.protect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param plaintext 参数 plaintext；parameter plaintext。
     * @param associatedData 参数 associatedData；parameter associated data。
     * @return 返回 protect 的处理结果；returns the result of the operation.
     */
    ProtectedSecret protect(String plaintext, String associatedData);

    /**
     * 中文说明：执行 unprotect 操作；该方法是 {@code GatewaySecretProtector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the unprotect operation; this method is the invocation entry point on {@code GatewaySecretProtector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecretProtector.unprotect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param secret 参数 secret；parameter secret。
     * @param associatedData 参数 associatedData；parameter associated data。
     * @return 返回 unprotect 的处理结果；returns the result of the operation.
     */
    String unprotect(ProtectedSecret secret, String associatedData);

    /**
     * 中文说明：{@code ProtectedSecret} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责ProtectedSecret相关的职责与边界。
     * English summary: {@code ProtectedSecret} is an immutable data carrier in the current Gateway module; it owns the protected secret-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param ciphertext 参数 ciphertext；parameter ciphertext。
     * @param keyVersion 参数 键Version；parameter key version。
     */
    record ProtectedSecret(
    /**
     * 中文说明：保存 ciphertext 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewaySecretProtector.ProtectedSecret} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by ciphertext; its type is {@code String}, and {@code GatewaySecretProtector.ProtectedSecret} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewaySecretProtector.ProtectedSecret} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecretProtector.ProtectedSecret}; do not couple callers to its representation when the owning type exposes an API.
     */
    String ciphertext,
    /**
     * 中文说明：保存 键Version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewaySecretProtector.ProtectedSecret} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by key version; its type is {@code String}, and {@code GatewaySecretProtector.ProtectedSecret} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewaySecretProtector.ProtectedSecret} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecretProtector.ProtectedSecret}; do not couple callers to its representation when the owning type exposes an API.
     */
    String keyVersion) {
    }
}
