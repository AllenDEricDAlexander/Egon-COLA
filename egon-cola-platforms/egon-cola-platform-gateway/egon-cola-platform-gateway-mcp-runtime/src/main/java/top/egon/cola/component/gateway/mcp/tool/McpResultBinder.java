package top.egon.cola.component.gateway.mcp.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.core.operation.GatewayInvocationResult;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class McpResultBinder {

    private final ObjectMapper objectMapper;

    public McpResultBinder(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    public Map<String, Object> bind(
            McpRuntimeTool tool,
            GatewayInvocationResult result) {
        String text = new String(result.body(), StandardCharsets.UTF_8);
        Object decoded = decode(text);
        Object structured = select(tool, decoded);
        if (structured == null) {
            structured = Map.of();
        }
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("content", List.of(Map.of(
                "type", "text",
                "text", text
        )));
        response.put("structuredContent", structured);
        response.put("isError", result.statusCode() >= 400);
        return Map.copyOf(response);
    }

    private Object decode(String text) {
        try {
            return objectMapper.readValue(
                    text,
                    new TypeReference<Object>() {
                    }
            );
        } catch (Exception ignored) {
            return Map.of("text", text);
        }
    }

    private Object select(McpRuntimeTool tool, Object decoded) {
        if (tool.resultBindings().isEmpty()
                || !(decoded instanceof Map<?, ?> values)) {
            return decoded;
        }
        LinkedHashMap<String, Object> selected = new LinkedHashMap<>();
        tool.resultBindings().forEach((source, target) -> {
            if (values.containsKey(source)) {
                selected.put(target, values.get(source));
            }
        });
        return selected;
    }
}
