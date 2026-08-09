package top.egon.cola.component.gateway.mcp.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;

import java.util.Map;

public final class McpJsonRpcCodec {

    private static final int MAX_JSON_DEPTH = 64;
    private static final TypeReference<Map<String, Object>> OBJECT_MAP =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;

    public McpJsonRpcCodec() {
        this(new ObjectMapper());
    }

    public McpJsonRpcCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
    }

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

    private String text(ObjectNode object, String field) {
        JsonNode value = object.get(field);
        return value != null && value.isTextual() ? value.textValue() : null;
    }

    private McpProtocolException protocolError(
            McpErrorCode code,
            String message) {
        return new McpProtocolException(code, message);
    }
}
