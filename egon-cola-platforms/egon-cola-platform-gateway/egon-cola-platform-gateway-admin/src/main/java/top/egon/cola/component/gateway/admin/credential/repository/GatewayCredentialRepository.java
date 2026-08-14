package top.egon.cola.component.gateway.admin.credential.repository;


import top.egon.cola.component.gateway.admin.credential.domain.po.GatewayCredentialPO;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayCredentialRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关凭证存储相关的职责与边界。
 * English summary: {@code GatewayCredentialRepository} is an interface contract in the current Gateway module; it owns the gateway credential store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayCredentialRepository {

    /**
     * 中文说明：执行 insert 操作；该方法是 {@code GatewayCredentialRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert operation; this method is the invocation entry point on {@code GatewayCredentialRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialRepository.insert(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param credential 参数 凭证；parameter credential。
     */
    void insert(GatewayCredentialPO credential);

    /**
     * 中文说明：执行 find 操作；该方法是 {@code GatewayCredentialRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code GatewayCredentialRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialRepository.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param keyId 参数 键Id；parameter key id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    Optional<GatewayCredentialPO> find(String applicationId, String keyId);

    /**
     * 中文说明：执行 findByAccess键 操作；该方法是 {@code GatewayCredentialRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find by access key operation; this method is the invocation entry point on {@code GatewayCredentialRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialRepository.findByAccessKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessKey 参数 access键；parameter access key。
     * @return 返回 findByAccess键 的处理结果；returns the result of the operation.
     */
    Optional<GatewayCredentialPO> findByAccessKey(String accessKey);

    /**
     * 中文说明：执行 list 操作；该方法是 {@code GatewayCredentialRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code GatewayCredentialRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialRepository.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    List<GatewayCredentialPO> list(String applicationId);

    /**
     * 中文说明：执行 overlap 操作；该方法是 {@code GatewayCredentialRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the overlap operation; this method is the invocation entry point on {@code GatewayCredentialRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialRepository.overlap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param validUntil 参数 validUntil；parameter valid until。
     * @param now 参数 now；parameter now。
     */
    void overlap(String id, Instant validUntil, Instant now);

    /**
     * 中文说明：执行 revoke 操作；该方法是 {@code GatewayCredentialRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revoke operation; this method is the invocation entry point on {@code GatewayCredentialRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialRepository.revoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param now 参数 now；parameter now。
     */
    void revoke(String id, Instant now);


}
