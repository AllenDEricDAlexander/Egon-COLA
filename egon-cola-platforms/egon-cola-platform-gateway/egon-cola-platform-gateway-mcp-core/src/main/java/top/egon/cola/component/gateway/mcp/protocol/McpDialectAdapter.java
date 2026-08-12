package top.egon.cola.component.gateway.mcp.protocol;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;

/**
 * 中文说明：{@code McpDialectAdapter} 是接口契约，位于当前 Gateway 模块的相关包中，负责MCPDialectAdapter相关的职责与边界。
 * English summary: {@code McpDialectAdapter} is an interface contract in the current Gateway module; it owns the mcp dialect adapter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface McpDialectAdapter {

    /**
     * 中文说明：执行 dialect 操作；该方法是 {@code McpDialectAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dialect operation; this method is the invocation entry point on {@code McpDialectAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpDialectAdapter.dialect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 dialect 的处理结果；returns the result of the operation.
     */
    McpProtocolDialect dialect();

    /**
     * 中文说明：执行 decode 操作；该方法是 {@code McpDialectAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the decode operation; this method is the invocation entry point on {@code McpDialectAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpDialectAdapter.decode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 decode 的处理结果；returns the result of the operation.
     */
    McpJsonRpcRequest decode(HttpMcpRequest request);
}
