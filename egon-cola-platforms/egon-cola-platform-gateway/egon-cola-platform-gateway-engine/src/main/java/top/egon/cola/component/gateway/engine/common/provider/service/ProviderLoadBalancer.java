package top.egon.cola.component.gateway.engine.common.provider.service;

import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderSelectionHandle;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

import java.util.List;

/**
 * 中文说明：{@code ProviderLoadBalancer} 是接口契约，位于当前 Gateway 模块的相关包中，负责提供方LoadBalancer相关的职责与边界。
 * English summary: {@code ProviderLoadBalancer} is an interface contract in the current Gateway module; it owns the provider load balancer-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@FunctionalInterface
public interface ProviderLoadBalancer {

    /**
     * 中文说明：执行 select 操作；该方法是 {@code ProviderLoadBalancer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the select operation; this method is the invocation entry point on {@code ProviderLoadBalancer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderLoadBalancer.select(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serviceKey 参数 服务键；parameter service key。
     * @param candidates 参数 candidates；parameter candidates。
     * @return 返回 select 的处理结果；returns the result of the operation.
     */
    ProviderSelectionHandle select(
            ProviderServiceKey serviceKey,
            List<ProviderInstance> candidates
    );
}
