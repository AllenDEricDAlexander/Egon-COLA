package top.egon.cola.component.gateway.mcp.server.service.handler;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.mcp.server.service.handler.McpServerDescription;
import top.egon.cola.component.gateway.mcp.server.service.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.domain.McpRequestContext;

/**
 * 中文说明：{@code McpDiscoverHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责MCPDiscover处理器相关的职责与边界。
 * English summary: {@code McpDiscoverHandler} is a mcp discover handler handler in the current Gateway module; it owns the mcp discover handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpDiscoverHandler implements McpMethodHandler {

    /**
     * 中文说明：执行 方法 操作；该方法是 {@code McpDiscoverHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the method operation; this method is the invocation entry point on {@code McpDiscoverHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpDiscoverHandler.method(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 方法 的处理结果；returns the result of the operation.
     */
    @Override
    public String method() {
        return "server/discover";
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code McpDiscoverHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code McpDiscoverHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpDiscoverHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        return Mono.just(McpJsonRpcResponse.success(
                request.id(),
                McpServerDescription.result(
                        new McpServerDescription.McpRequestContextView(
                                context.server(),
                                context.dialect().protocolVersion()
                        )
                )
        ));
    }
}
