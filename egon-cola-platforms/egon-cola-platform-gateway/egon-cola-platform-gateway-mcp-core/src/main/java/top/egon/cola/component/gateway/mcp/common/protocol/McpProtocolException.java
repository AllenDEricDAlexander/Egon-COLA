package top.egon.cola.component.gateway.mcp.common.protocol;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcError;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;

import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code McpProtocolException} 是异常类型，位于当前 Gateway 模块的相关包中，负责MCPProtocolException相关的职责与边界。
 * English summary: {@code McpProtocolException} is a mcp protocol exception exception in the current Gateway module; it owns the mcp protocol exception-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpProtocolException extends RuntimeException {

    /**
     * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code McpErrorCode}，由 {@code McpProtocolException} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code McpErrorCode}, and {@code McpProtocolException} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpProtocolException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpProtocolException}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpErrorCode code;
    /**
     * 中文说明：保存 data 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpProtocolException} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by data; its type is {@code Map<String, Object>}, and {@code McpProtocolException} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpProtocolException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpProtocolException}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, Object> data;

    /**
     * 中文说明：创建 {@code McpProtocolException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpProtocolException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param code 参数 code；parameter code。
     * @param message 参数 消息；parameter message。
     */
    public McpProtocolException(McpErrorCode code, String message) {
        this(code, message, Map.of());
    }

    /**
     * 中文说明：创建 {@code McpProtocolException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpProtocolException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param code 参数 code；parameter code。
     * @param message 参数 消息；parameter message。
     * @param data 参数 data；parameter data。
     */
    public McpProtocolException(
            McpErrorCode code,
            String message,
            Map<String, Object> data) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
        this.data = data == null ? Map.of() : Map.copyOf(data);
    }

    /**
     * 中文说明：执行 code 操作；该方法是 {@code McpProtocolException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the code operation; this method is the invocation entry point on {@code McpProtocolException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpProtocolException.code(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 code 的处理结果；returns the result of the operation.
     */
    public McpErrorCode code() {
        return code;
    }

    /**
     * 中文说明：执行 data 操作；该方法是 {@code McpProtocolException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the data operation; this method is the invocation entry point on {@code McpProtocolException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpProtocolException.data(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 data 的处理结果；returns the result of the operation.
     */
    public Map<String, Object> data() {
        return data;
    }

    /**
     * 中文说明：执行 to响应 操作；该方法是 {@code McpProtocolException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the to response operation; this method is the invocation entry point on {@code McpProtocolException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpProtocolException.toResponse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 to响应 的处理结果；returns the result of the operation.
     */
    public McpJsonRpcResponse toResponse(Object id) {
        return McpJsonRpcResponse.failure(
                id,
                new McpJsonRpcError(
                        code.jsonRpcCode(),
                        getMessage(),
                        code,
                        data
                )
        );
    }
}
