package top.egon.cola.component.gateway.engine.common.observability.service;

import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.util.Arrays;
import java.util.List;

/**
 * 中文说明：{@code GatewayCallCompletionListener} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关调用补全监听器相关的职责与边界。
 * English summary: {@code GatewayCallCompletionListener} is an interface contract in the current Gateway module; it owns the gateway call completion listener-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@FunctionalInterface
public interface GatewayCallCompletionListener {

    /**
     * 中文说明：执行 onComplete 操作；该方法是 {@code GatewayCallCompletionListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the on complete operation; this method is the invocation entry point on {@code GatewayCallCompletionListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallCompletionListener.onComplete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     */
    void onComplete(GatewayCallEventV1 event);

    /**
     * 中文说明：执行 noop 操作；该方法是 {@code GatewayCallCompletionListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the noop operation; this method is the invocation entry point on {@code GatewayCallCompletionListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallCompletionListener.noop(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 noop 的处理结果；returns the result of the operation.
     */
    static GatewayCallCompletionListener noop() {
        return event -> {
        };
    }

    /**
     * 中文说明：执行 composite 操作；该方法是 {@code GatewayCallCompletionListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the composite operation; this method is the invocation entry point on {@code GatewayCallCompletionListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallCompletionListener.composite(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param listeners 参数 listeners；parameter listeners。
     * @return 返回 composite 的处理结果；returns the result of the operation.
     */
    static GatewayCallCompletionListener composite(
            GatewayCallCompletionListener... listeners) {
        List<GatewayCallCompletionListener> snapshot =
                Arrays.stream(listeners)
                        .filter(listener -> listener != null)
                        .toList();
        return event -> snapshot.forEach(listener -> {
            try {
                listener.onComplete(event);
            } catch (RuntimeException ignored) {
                // An observability sink must never change the business result.
            }
        });
    }
}
