package top.egon.cola.component.gateway.mcp.server;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;

import java.util.Map;
import java.util.Objects;

public record McpRequestContext(
        McpRuntimeServer server,
        McpProtocolDialect dialect,
        String sessionId,
        Map<String, Object> attributes
) {

    public McpRequestContext {
        server = Objects.requireNonNull(server, "server");
        dialect = Objects.requireNonNull(dialect, "dialect");
        sessionId = sessionId == null || sessionId.isBlank()
                ? null
                : sessionId.trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
