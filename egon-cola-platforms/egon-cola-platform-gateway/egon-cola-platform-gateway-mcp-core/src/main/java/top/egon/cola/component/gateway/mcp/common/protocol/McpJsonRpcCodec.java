package top.egon.cola.component.gateway.mcp.common.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;

import java.util.Map;

/**
 * 中文说明：{@code McpJsonRpcCodec} 是类型，位于当前 Gateway 模块的相关包中，负责MCPJsonRpcCodec相关的职责与边界。
 * English summary: {@code McpJsonRpcCodec} is a type in the current Gateway module; it owns the mcp json rpc codec-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpJsonRpcCodec {

    /**
     * 中文说明：表示 MAXJSONDEPTH 这一固定值；它属于 {@code McpJsonRpcCodec} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value max json depth; it is a state, type, or protocol value of {@code McpJsonRpcCodec} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpJsonRpcCodec} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpJsonRpcCodec}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int MAX_JSON_DEPTH = 64;
    /**
     * 中文说明：表示 OBJECTMAP 这一固定值；它属于 {@code McpJsonRpcCodec} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value object map; it is a state, type, or protocol value of {@code McpJsonRpcCodec} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpJsonRpcCodec} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpJsonRpcCodec}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final TypeReference<Map<String, Object>> OBJECT_MAP =
            new TypeReference<>() {
            };

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpJsonRpcCodec} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpJsonRpcCodec} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpJsonRpcCodec} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpJsonRpcCodec}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：创建 {@code McpJsonRpcCodec} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpJsonRpcCodec} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    public McpJsonRpcCodec() {
        this(new ObjectMapper());
    }

    /**
     * 中文说明：创建 {@code McpJsonRpcCodec} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpJsonRpcCodec} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public McpJsonRpcCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
    }

    /**
     * 中文说明：执行 decode 操作；该方法是 {@code McpJsonRpcCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the decode operation; this method is the invocation entry point on {@code McpJsonRpcCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpJsonRpcCodec.decode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param body 参数 body；parameter body。
     * @return 返回 decode 的处理结果；returns the result of the operation.
     */
    public McpJsonRpcRequest decode(String body) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JsonProcessingException error) {
            throw protocolError(
                    McpErrorCode.MCP_PARSE_ERROR,
                    "MCP request contains invalid JSON"
            );
        }
        if (root == null || !root.isObject()) {
            throw protocolError(
                    McpErrorCode.MCP_INVALID_REQUEST,
                    "MCP batch and non-object requests are not supported"
            );
        }
        if (depth(root) > MAX_JSON_DEPTH) {
            throw protocolError(
                    McpErrorCode.MCP_INVALID_REQUEST,
                    "MCP request exceeds the maximum JSON depth"
            );
        }

        ObjectNode object = (ObjectNode) root;
        String version = text(object, "jsonrpc");
        if (!McpJsonRpcRequest.VERSION.equals(version)) {
            throw protocolError(
                    McpErrorCode.MCP_INVALID_REQUEST,
                    "MCP JSON-RPC version must be 2.0"
            );
        }
        String method = text(object, "method");
        if (method == null || method.isBlank()) {
            throw protocolError(
                    McpErrorCode.MCP_INVALID_REQUEST,
                    "MCP method is required"
            );
        }
        Object id = decodeId(object);
        ObjectNode paramsNode = decodeParams(object);
        JsonNode metaNode = paramsNode.remove("_meta");
        if (metaNode != null && !metaNode.isObject()) {
            throw protocolError(
                    McpErrorCode.MCP_INVALID_PARAMS,
                    "MCP _meta must be an object"
            );
        }
        Map<String, Object> params = objectMapper.convertValue(
                paramsNode,
                OBJECT_MAP
        );
        Map<String, Object> meta = metaNode == null
                ? Map.of()
                : objectMapper.convertValue(metaNode, OBJECT_MAP);
        return new McpJsonRpcRequest(version, id, method, params, meta);
    }

    /**
     * 中文说明：执行 decodeId 操作；该方法是 {@code McpJsonRpcCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the decode id operation; this method is the invocation entry point on {@code McpJsonRpcCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpJsonRpcCodec.decodeId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param object 参数 object；parameter object。
     * @return 返回 decodeId 的处理结果；returns the result of the operation.
     */
    private Object decodeId(ObjectNode object) {
        if (!object.has("id")) {
            return null;
        }
        JsonNode id = object.get("id");
        if (id == null || id.isNull()
                || (!id.isTextual() && !id.isIntegralNumber())) {
            throw protocolError(
                    McpErrorCode.MCP_INVALID_REQUEST,
                    "MCP request id must be a string or integer"
            );
        }
        return id.isTextual() ? id.textValue() : id.longValue();
    }

    /**
     * 中文说明：执行 decodeParams 操作；该方法是 {@code McpJsonRpcCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the decode params operation; this method is the invocation entry point on {@code McpJsonRpcCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpJsonRpcCodec.decodeParams(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param object 参数 object；parameter object。
     * @return 返回 decodeParams 的处理结果；returns the result of the operation.
     */
    private ObjectNode decodeParams(ObjectNode object) {
        JsonNode params = object.get("params");
        if (params == null) {
            return objectMapper.createObjectNode();
        }
        if (!params.isObject()) {
            throw protocolError(
                    McpErrorCode.MCP_INVALID_PARAMS,
                    "MCP params must be an object"
            );
        }
        return ((ObjectNode) params).deepCopy();
    }

    /**
     * 中文说明：执行 depth 操作；该方法是 {@code McpJsonRpcCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the depth operation; this method is the invocation entry point on {@code McpJsonRpcCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpJsonRpcCodec.depth(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param node 参数 node；parameter node。
     * @return 返回 depth 的处理结果；returns the result of the operation.
     */
    private int depth(JsonNode node) {
        if (!node.isContainerNode() || node.isEmpty()) {
            return 1;
        }
        int maxChildDepth = 0;
        for (JsonNode child : node) {
            maxChildDepth = Math.max(maxChildDepth, depth(child));
        }
        return 1 + maxChildDepth;
    }

    /**
     * 中文说明：执行 text 操作；该方法是 {@code McpJsonRpcCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the text operation; this method is the invocation entry point on {@code McpJsonRpcCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpJsonRpcCodec.text(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param object 参数 object；parameter object。
     * @param field 参数 field；parameter field。
     * @return 返回 text 的处理结果；returns the result of the operation.
     */
    private String text(ObjectNode object, String field) {
        JsonNode value = object.get(field);
        return value != null && value.isTextual() ? value.textValue() : null;
    }

    /**
     * 中文说明：执行 protocolError 操作；该方法是 {@code McpJsonRpcCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the protocol error operation; this method is the invocation entry point on {@code McpJsonRpcCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpJsonRpcCodec.protocolError(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param code 参数 code；parameter code。
     * @param message 参数 消息；parameter message。
     * @return 返回 protocolError 的处理结果；returns the result of the operation.
     */
    private McpProtocolException protocolError(
            McpErrorCode code,
            String message) {
        return new McpProtocolException(code, message);
    }
}
