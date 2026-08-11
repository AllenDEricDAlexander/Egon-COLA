package top.egon.cola.component.gateway.mcp.server.handler;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class McpServerDescription {

    private McpServerDescription() {
    }

    static Map<String, Object> describe(McpRuntimeServer server) {
        LinkedHashMap<String, Object> description = new LinkedHashMap<>();
        description.put("code", server.serverCode());
        description.put("name", server.name());
        if (server.description() != null) {
            description.put("description", server.description());
        }
        if (server.instructions() != null) {
            description.put("instructions", server.instructions());
        }
        description.put("resourceUri", server.resourceUri());
        return Collections.unmodifiableMap(description);
    }

    static Map<String, Object> result(McpRequestContextView context) {
        return Map.of(
                "protocolVersion", context.protocolVersion(),
                "server", describe(context.server()),
                "capabilities", Map.of(
                        "tools", Map.of("listChanged", true),
                        "resources", Map.of("subscribe", true),
                        "prompts", Map.of("listChanged", true),
                        "tasks", Map.of("durable", true),
                        "apps", Map.of("uiResources", true)
                )
        );
    }

    record McpRequestContextView(
            McpRuntimeServer server,
            String protocolVersion) {
    }
}
