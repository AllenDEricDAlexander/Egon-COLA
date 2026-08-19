package top.egon.cola.component.gateway.engine.common.provider.service;

import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderSelectionHandle;

import java.util.Set;

/**
 * 中文说明：{@code ProviderSelector} 是接口契约，位于当前 Gateway 模块的相关包中，负责提供方Selector相关的职责与边界。
 * English summary: {@code ProviderSelector} is an interface contract in the current Gateway module; it owns the provider selector-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@FunctionalInterface
public interface ProviderSelector {

    /**
     * 中文说明：执行 select 操作；该方法是 {@code ProviderSelector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the select operation; this method is the invocation entry point on {@code ProviderSelector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderSelector.select(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serviceKey 参数 服务键；parameter service key。
     * @return 返回 select 的处理结果；returns the result of the operation.
     */
    ProviderSelectionHandle select(ProviderServiceKey serviceKey);

    /**
     * 中文说明：执行 select 操作；该方法是 {@code ProviderSelector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the select operation; this method is the invocation entry point on {@code ProviderSelector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderSelector.select(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serviceKey 参数 服务键；parameter service key。
     * @param policyRefs 参数 策略Refs；parameter policy refs。
     * @return 返回 select 的处理结果；returns the result of the operation.
     */
    default ProviderSelectionHandle select(
            ProviderServiceKey serviceKey,
            Set<String> policyRefs) {
        return select(serviceKey);
    }

    /**
     * 中文说明：执行 select 操作；该方法是 {@code ProviderSelector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the select operation; this method is the invocation entry point on {@code ProviderSelector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderSelector.select(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serviceKey 参数 服务键；parameter service key。
     * @param policyRefs 参数 策略Refs；parameter policy refs。
     * @param excludedRuntimeIdentities 参数 excluded运行时Identities；parameter excluded runtime identities。
     * @return 返回 select 的处理结果；returns the result of the operation.
     */
    default ProviderSelectionHandle select(
            ProviderServiceKey serviceKey,
            Set<String> policyRefs,
            Set<String> excludedRuntimeIdentities) {
        return select(serviceKey, policyRefs);
    }
}
