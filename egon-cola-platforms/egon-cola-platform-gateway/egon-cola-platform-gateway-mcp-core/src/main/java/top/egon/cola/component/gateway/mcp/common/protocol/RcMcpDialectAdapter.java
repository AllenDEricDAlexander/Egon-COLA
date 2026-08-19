package top.egon.cola.component.gateway.mcp.common.protocol;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;

import java.util.Objects;

/**
 * 中文说明：{@code RcMcpDialectAdapter} 是适配器，位于当前 Gateway 模块的相关包中，负责RcMCPDialectAdapter相关的职责与边界。
 * English summary: {@code RcMcpDialectAdapter} is a rc mcp dialect adapter adapter in the current Gateway module; it owns the rc mcp dialect adapter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RcMcpDialectAdapter extends AbstractMcpDialectAdapter {

    /**
     * 中文说明：创建 {@code RcMcpDialectAdapter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RcMcpDialectAdapter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param codec 参数 codec；parameter codec。
     */
    public RcMcpDialectAdapter(McpJsonRpcCodec codec) {
        super(codec);
    }

    /**
     * 中文说明：执行 dialect 操作；该方法是 {@code RcMcpDialectAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dialect operation; this method is the invocation entry point on {@code RcMcpDialectAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RcMcpDialectAdapter.dialect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 dialect 的处理结果；returns the result of the operation.
     */
    @Override
    public McpProtocolDialect dialect() {
        return McpProtocolDialect.RC_2026_07_28;
    }

    /**
     * 中文说明：执行 validateDialect 操作；该方法是 {@code RcMcpDialectAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate dialect operation; this method is the invocation entry point on {@code RcMcpDialectAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RcMcpDialectAdapter.validateDialect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param decoded 参数 decoded；parameter decoded。
     */
    @Override
    protected void validateDialect(
            HttpMcpRequest request,
            McpJsonRpcRequest decoded) {
        requireProtocolVersion(request, true);
        if (decoded.meta().isEmpty()) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_CLIENT_CAPABILITY_REQUIRED,
                    "MCP RC request metadata is required"
            );
        }
        requireAgreement(
                request.header("Mcp-Method"),
                decoded.method(),
                "method"
        );
        Object name = decoded.params().get("name");
        if (name != null) {
            requireAgreement(
                    request.header("Mcp-Name"),
                    String.valueOf(name),
                    "name"
            );
        } else if (request.header("Mcp-Name") != null) {
            throw mismatch("name");
        }
    }

    /**
     * 中文说明：执行 requireAgreement 操作；该方法是 {@code RcMcpDialectAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require agreement operation; this method is the invocation entry point on {@code RcMcpDialectAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RcMcpDialectAdapter.requireAgreement(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param header 参数 header；parameter header。
     * @param body 参数 body；parameter body。
     * @param field 参数 field；parameter field。
     */
    private void requireAgreement(
            String header,
            String body,
            String field) {
        if (!Objects.equals(header, body)) {
            throw mismatch(field);
        }
    }

    /**
     * 中文说明：执行 mismatch 操作；该方法是 {@code RcMcpDialectAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mismatch operation; this method is the invocation entry point on {@code RcMcpDialectAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RcMcpDialectAdapter.mismatch(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param field 参数 field；parameter field。
     * @return 返回 mismatch 的处理结果；returns the result of the operation.
     */
    private McpProtocolException mismatch(String field) {
        return new McpProtocolException(
                McpErrorCode.MCP_HEADER_MISMATCH,
                "MCP " + field + " header does not match the JSON-RPC body"
        );
    }
}
