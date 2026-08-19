package top.egon.cola.component.gateway.mcp.common.protocol;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;

import java.util.Objects;

/**
 * 中文说明：{@code AbstractMcpDialectAdapter} 是适配器，位于当前 Gateway 模块的相关包中，负责AbstractMCPDialectAdapter相关的职责与边界。
 * English summary: {@code AbstractMcpDialectAdapter} is a abstract mcp dialect adapter adapter in the current Gateway module; it owns the abstract mcp dialect adapter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
abstract class AbstractMcpDialectAdapter implements McpDialectAdapter {

    /**
     * 中文说明：保存 codec 对应的状态、依赖或配置值；字段类型为 {@code McpJsonRpcCodec}，由 {@code AbstractMcpDialectAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by codec; its type is {@code McpJsonRpcCodec}, and {@code AbstractMcpDialectAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code AbstractMcpDialectAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AbstractMcpDialectAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpJsonRpcCodec codec;

    /**
     * 中文说明：创建 {@code AbstractMcpDialectAdapter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code AbstractMcpDialectAdapter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param codec 参数 codec；parameter codec。
     */
    AbstractMcpDialectAdapter(McpJsonRpcCodec codec) {
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    /**
     * 中文说明：执行 decode 操作；该方法是 {@code AbstractMcpDialectAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the decode operation; this method is the invocation entry point on {@code AbstractMcpDialectAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractMcpDialectAdapter.decode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 decode 的处理结果；returns the result of the operation.
     */
    @Override
    public final McpJsonRpcRequest decode(HttpMcpRequest request) {
        validateHttp(request);
        McpJsonRpcRequest decoded = codec.decode(request.body());
        validateDialect(request, decoded);
        return decoded;
    }

    /**
     * 中文说明：执行 validateDialect 操作；该方法是 {@code AbstractMcpDialectAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate dialect operation; this method is the invocation entry point on {@code AbstractMcpDialectAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractMcpDialectAdapter.validateDialect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param decoded 参数 decoded；parameter decoded。
     */
    protected abstract void validateDialect(
            HttpMcpRequest request,
            McpJsonRpcRequest decoded);

    /**
     * 中文说明：执行 requireProtocolVersion 操作；该方法是 {@code AbstractMcpDialectAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require protocol version operation; this method is the invocation entry point on {@code AbstractMcpDialectAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractMcpDialectAdapter.requireProtocolVersion(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param required 参数 required；parameter required。
     */
    protected final void requireProtocolVersion(
            HttpMcpRequest request,
            boolean required) {
        String version = request.header("Mcp-Protocol-Version");
        if (version == null && !required) {
            return;
        }
        if (!dialect().protocolVersion().equals(version)) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_PROTOCOL_UNSUPPORTED,
                    "MCP protocol version is not supported"
            );
        }
    }

    /**
     * 中文说明：执行 validateHttp 操作；该方法是 {@code AbstractMcpDialectAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate http operation; this method is the invocation entry point on {@code AbstractMcpDialectAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractMcpDialectAdapter.validateHttp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     */
    private void validateHttp(HttpMcpRequest request) {
        if (!"POST".equals(request.method())) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_INVALID_REQUEST,
                    "MCP JSON-RPC requests require POST"
            );
        }
        if (!request.contentType().startsWith("application/json")) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_INVALID_REQUEST,
                    "MCP JSON-RPC requests require application/json"
            );
        }
    }
}
