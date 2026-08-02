package top.egon.cola.component.gateway.mcp.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class McpToolsListHandler implements McpMethodHandler {

    private final McpToolCatalog catalog;

    private final ObjectMapper objectMapper;

    public McpToolsListHandler(
            McpToolCatalog catalog,
            ObjectMapper objectMapper) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    @Override
    public String method() {
        return "tools/list";
    }

    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        List<Map<String, Object>> tools = catalog.localTools(
                context.server().serverCode()
        ).stream().map(this::describe).toList();
        return Mono.just(McpJsonRpcResponse.success(
                request.id(),
                Map.of("tools", tools)
        ));
    }

    private Map<String, Object> describe(McpRuntimeTool tool) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("name", tool.name());
        value.put("description", tool.description() == null
                ? ""
                : tool.description());
        value.put("inputSchema", schema(tool.inputSchema()));
        value.put("annotations", tool.annotations());
        return Map.copyOf(value);
    }

    private Map<String, Object> schema(String json) {
        if (json == null) {
            return Map.of("type", "object");
        }
        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    }
            );
        } catch (Exception failure) {
            throw new IllegalStateException(
                    "MCP tool input schema is invalid",
                    failure
            );
        }
    }
}
