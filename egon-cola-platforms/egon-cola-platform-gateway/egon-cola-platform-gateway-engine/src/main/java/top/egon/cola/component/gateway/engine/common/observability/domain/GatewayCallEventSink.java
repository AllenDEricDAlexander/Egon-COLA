package top.egon.cola.component.gateway.engine.common.observability.domain;

import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

/**
 * 中文说明：{@code GatewayCallEventSink} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关调用事件Sink相关的职责与边界。
 * English summary: {@code GatewayCallEventSink} is an interface contract in the current Gateway module; it owns the gateway call event sink-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@FunctionalInterface
public interface GatewayCallEventSink extends AutoCloseable {

    /**
     * 中文说明：执行 send 操作；该方法是 {@code GatewayCallEventSink} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the send operation; this method is the invocation entry point on {@code GatewayCallEventSink} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventSink.send(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     * @param payload 参数 payload；parameter payload。
     */
    void send(GatewayCallEventV1 event, byte[] payload);

    /**
     * 中文说明：执行 close 操作；该方法是 {@code GatewayCallEventSink} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code GatewayCallEventSink} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventSink.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    default void close() {
    }
}
