package top.egon.cola.component.gateway.admin.reporting.repository;


import java.time.Instant;

/**
 * 中文说明：{@code GatewayHmacNonceRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关HmacNonce存储相关的职责与边界。
 * English summary: {@code GatewayHmacNonceRepository} is an interface contract in the current Gateway module; it owns the gateway hmac nonce store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayHmacNonceRepository {

    /**
     * 中文说明：执行 claim 操作；该方法是 {@code GatewayHmacNonceRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the claim operation; this method is the invocation entry point on {@code GatewayHmacNonceRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHmacNonceRepository.claim(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessKey 参数 access键；parameter access key。
     * @param nonce 参数 nonce；parameter nonce。
     * @param expiresAt 参数 expiresAt；parameter expires at。
     * @param now 参数 now；parameter now。
     * @return 返回 claim 的处理结果；returns the result of the operation.
     */
    boolean claim(
            String accessKey,
            String nonce,
            Instant expiresAt,
            Instant now);

    /**
     * 中文说明：执行 deleteExpired 操作；该方法是 {@code GatewayHmacNonceRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete expired operation; this method is the invocation entry point on {@code GatewayHmacNonceRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHmacNonceRepository.deleteExpired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param now 参数 now；parameter now。
     * @return 返回 deleteExpired 的处理结果；returns the result of the operation.
     */
    int deleteExpired(Instant now);
}
