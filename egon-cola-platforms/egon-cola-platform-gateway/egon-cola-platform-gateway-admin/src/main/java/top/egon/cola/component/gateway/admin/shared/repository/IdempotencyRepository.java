package top.egon.cola.component.gateway.admin.shared.repository;


import top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO;

import java.util.Optional;

/**
 * 中文说明：{@code IdempotencyRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责Idempotency存储相关的职责与边界。
 * English summary: {@code IdempotencyRepository} is an interface contract in the current Gateway module; it owns the idempotency store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface IdempotencyRepository {

    /**
     * 中文说明：执行 find 操作；该方法是 {@code IdempotencyRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code IdempotencyRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code IdempotencyRepository.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scopeType 参数 scopeType；parameter scope type。
     * @param scopeId 参数 scopeId；parameter scope id。
     * @param key 参数 键；parameter key。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    Optional<IdempotencyPO> find(String scopeType, String scopeId, String key);

    /**
     * 中文说明：执行 save 操作；该方法是 {@code IdempotencyRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the save operation; this method is the invocation entry point on {@code IdempotencyRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code IdempotencyRepository.save(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param record 参数 record；parameter record。
     */
    void save(IdempotencyPO record);


}
