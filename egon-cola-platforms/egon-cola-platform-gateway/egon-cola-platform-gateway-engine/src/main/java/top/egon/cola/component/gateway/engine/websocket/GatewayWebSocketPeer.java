package top.egon.cola.component.gateway.engine.websocket;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Netty-free bidirectional WebSocket peer port.
 * 补充说明 / Supplementary summary: {@code GatewayWebSocketPeer} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关WebSocketPeer相关的职责与边界。
 * English supplement: {@code GatewayWebSocketPeer} is an interface contract in the current Gateway module; it owns the gateway web socket peer-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayWebSocketPeer {

    /**
     * 中文说明：执行 receive 操作；该方法是 {@code GatewayWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the receive operation; this method is the invocation entry point on {@code GatewayWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketPeer.receive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 receive 的处理结果；returns the result of the operation.
     */
    Flux<GatewayWebSocketFrame> receive();

    /**
     * 中文说明：执行 send 操作；该方法是 {@code GatewayWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the send operation; this method is the invocation entry point on {@code GatewayWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketPeer.send(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param frames 参数 frames；parameter frames。
     * @return 返回 send 的处理结果；returns the result of the operation.
     */
    Mono<Void> send(Flux<GatewayWebSocketFrame> frames);

    /**
     * 中文说明：执行 sendClose 操作；该方法是 {@code GatewayWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the send close operation; this method is the invocation entry point on {@code GatewayWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketPeer.sendClose(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 sendClose 的处理结果；returns the result of the operation.
     */
    Mono<Void> sendClose(GatewayWebSocketCloseStatus status);

    /**
     * 中文说明：执行 dispose 操作；该方法是 {@code GatewayWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dispose operation; this method is the invocation entry point on {@code GatewayWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketPeer.dispose(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    void dispose();

    /**
     * 中文说明：执行 disposed 操作；该方法是 {@code GatewayWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the disposed operation; this method is the invocation entry point on {@code GatewayWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketPeer.disposed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 disposed 的处理结果；returns the result of the operation.
     */
    boolean disposed();
}
