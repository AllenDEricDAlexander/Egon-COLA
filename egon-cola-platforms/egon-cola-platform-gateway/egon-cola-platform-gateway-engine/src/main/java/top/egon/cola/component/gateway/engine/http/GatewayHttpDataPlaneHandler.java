package top.egon.cola.component.gateway.engine.http;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.engine.websocket.GatewayPreparedWebSocketSession;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketPeer;

/**
 * 中文说明：{@code GatewayHttpDataPlaneHandler} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关HttpDataPlane处理器相关的职责与边界。
 * English summary: {@code GatewayHttpDataPlaneHandler} is an interface contract in the current Gateway module; it owns the gateway http data plane handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@FunctionalInterface
public interface GatewayHttpDataPlaneHandler {

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code GatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code GatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpDataPlaneHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param request 参数 请求；parameter request。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    Mono<GatewayOutboundHttpResponse> handle(
            AccessZone accessZone,
            GatewayInboundHttpRequest request
    );

    /**
     * 中文说明：执行 prepareWebSocket 操作；该方法是 {@code GatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the prepare web socket operation; this method is the invocation entry point on {@code GatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpDataPlaneHandler.prepareWebSocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param request 参数 请求；parameter request。
     * @return 返回 prepareWebSocket 的处理结果；returns the result of the operation.
     */
    default Mono<GatewayWebSocketHandshakeResult> prepareWebSocket(
            AccessZone accessZone,
            GatewayInboundHttpRequest request) {
        return Mono.just(GatewayWebSocketHandshakeResult.rejected(
                426,
                "GATEWAY_WEBSOCKET_NOT_SUPPORTED",
                "the selected data plane does not support WebSocket"
        ));
    }

    /**
     * 中文说明：执行 bridgeWebSocket 操作；该方法是 {@code GatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bridge web socket operation; this method is the invocation entry point on {@code GatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpDataPlaneHandler.bridgeWebSocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param upstream 参数 upstream；parameter upstream。
     * @param downstream 参数 downstream；parameter downstream。
     * @return 返回 bridgeWebSocket 的处理结果；returns the result of the operation.
     */
    default Mono<Void> bridgeWebSocket(
            GatewayPreparedWebSocketSession upstream,
            GatewayWebSocketPeer downstream) {
        upstream.dispose();
        downstream.dispose();
        return Mono.error(new IllegalStateException(
                "GATEWAY_WEBSOCKET_NOT_SUPPORTED"
        ));
    }
}
