package top.egon.cola.component.gateway.engine.http.websocket.service;

import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketFrameType;

/**
 * Passive lifecycle observation port; it cannot influence forwarding.
 * 补充说明 / Supplementary summary: {@code GatewayWebSocketObserver} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关WebSocketObserver相关的职责与边界。
 * English supplement: {@code GatewayWebSocketObserver} is an interface contract in the current Gateway module; it owns the gateway web socket observer-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@FunctionalInterface
public interface GatewayWebSocketObserver {

    /**
     * 中文说明：执行 observe 操作；该方法是 {@code GatewayWebSocketObserver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the observe operation; this method is the invocation entry point on {@code GatewayWebSocketObserver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketObserver.observe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param transportMode 参数 传输Mode；parameter transport mode。
     * @param commitPoint 参数 commitPoint；parameter commit point。
     * @param terminationReason 参数 terminationReason；parameter termination reason。
     */
    void observe(
            String transportMode,
            String commitPoint,
            String terminationReason);

    /**
     * 中文说明：执行 observeFrame 操作；该方法是 {@code GatewayWebSocketObserver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the observe frame operation; this method is the invocation entry point on {@code GatewayWebSocketObserver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketObserver.observeFrame(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param direction 参数 direction；parameter direction。
     * @param frameType 参数 frameType；parameter frame type。
     * @param payloadBytes 参数 payloadBytes；parameter payload bytes。
     * @param finalFragment 参数 finalFragment；parameter final fragment。
     */
    default void observeFrame(
            String direction,
            GatewayWebSocketFrameType frameType,
            long payloadBytes,
            boolean finalFragment) {
    }

    /**
     * 中文说明：执行 noop 操作；该方法是 {@code GatewayWebSocketObserver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the noop operation; this method is the invocation entry point on {@code GatewayWebSocketObserver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketObserver.noop(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 noop 的处理结果；returns the result of the operation.
     */
    static GatewayWebSocketObserver noop() {
        return (transportMode, commitPoint, terminationReason) -> {
        };
    }
}
