package top.egon.cola.component.gateway.admin.reporting.repository;


import top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO;

import java.time.Instant;
import java.util.Set;

/**
 * 中文说明：{@code GatewayDefinitionLifecycleRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关定义生命周期存储相关的职责与边界。
 * English summary: {@code GatewayDefinitionLifecycleRepository} is an interface contract in the current Gateway module; it owns the gateway definition lifecycle store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayDefinitionLifecycleRepository {

    /**
     * 中文说明：执行 reconcile 操作；该方法是 {@code GatewayDefinitionLifecycleRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the reconcile operation; this method is the invocation entry point on {@code GatewayDefinitionLifecycleRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionLifecycleRepository.reconcile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activeDefinitionSetIds 参数 active定义SetIds；parameter active definition set ids。
     * @param now 参数 now；parameter now。
     * @return 返回 reconcile 的处理结果；returns the result of the operation.
     */
    GatewayReconcileResultVO reconcile(
            Set<String> activeDefinitionSetIds,
            Instant now);


}
