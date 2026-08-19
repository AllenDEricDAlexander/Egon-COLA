package top.egon.cola.component.gateway.engine.mcp.adapter;

import top.egon.cola.component.gateway.engine.mcp.service.McpEngineHttpHandler;
import top.egon.cola.component.gateway.engine.mcp.service.McpTransportIntegrationTest;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.mcp.common.transport.McpHttpRequest;
import top.egon.cola.component.gateway.mcp.common.transport.McpHttpResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpCrossNodeSessionTest {

    @Test
    void stablePostOnNodeBDeliversEventToGetStreamOnNodeA() {
        SharedMcpTransportStore shared = new SharedMcpTransportStore();
        McpEngineHttpHandler nodeA = McpTransportIntegrationTest.handler(
                shared
        );
        McpEngineHttpHandler nodeB = McpTransportIntegrationTest.handler(
                shared
        );

        McpHttpResponse initialized = nodeA.handle(request(
                "POST",
                Map.of("content-type", "application/json"),
                """
                {"jsonrpc":"2.0","id":1,"method":"initialize",\
                "params":{"protocolVersion":"2025-11-25"}}
                """
        )).block();
        String sessionId = initialized.header("Mcp-Session-Id");
        assertNotNull(sessionId);

        McpHttpResponse posted = nodeB.handle(request(
                "POST",
                Map.of(
                        "content-type", "application/json",
                        "Mcp-Protocol-Version", "2025-11-25",
                        "Mcp-Session-Id", sessionId
                ),
                """
                {"jsonrpc":"2.0","id":2,"method":"ping","params":{}}
                """
        )).block();
        assertEquals(200, posted.status());

        McpHttpResponse stream = nodeA.handle(request(
                "GET",
                Map.of(
                        "accept", "text/event-stream",
                        "Mcp-Session-Id", sessionId
                ),
                ""
        )).block();
        String event = Flux.from(stream.body())
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .next()
                .block(Duration.ofSeconds(1));
        assertTrue(event.contains("event:message"));
        assertTrue(event.contains("\"id\":2"));
        assertTrue(event.contains("\"result\":{}"));
    }

    @Test
    void sessionBindingRejectsAnotherSubject() {
        SharedMcpTransportStore shared = new SharedMcpTransportStore();
        McpEngineHttpHandler node = McpTransportIntegrationTest.handler(
                shared
        );
        String sessionId = node.handle(request(
                "POST",
                Map.of("content-type", "application/json"),
                """
                {"jsonrpc":"2.0","id":1,"method":"initialize",\
                "params":{"protocolVersion":"2025-11-25"}}
                """
        )).block().header("Mcp-Session-Id");

        McpHttpRequest foreign = new McpHttpRequest(
                "POST",
                "/mcp/orders",
                Map.of(
                        "content-type", "application/json",
                        "Mcp-Protocol-Version", "2025-11-25",
                        "Mcp-Session-Id", sessionId
                ),
                """
                {"jsonrpc":"2.0","id":2,"method":"ping","params":{}}
                """,
                Map.of("testSubject", "user-2")
        );
        McpEngineHttpHandler foreignNode = McpTransportIntegrationTest.handler(
                shared,
                Map.of(
                        "callerId", "user-2",
                        "tenantId", "tenant-1",
                        "idp.client-id", "client-1"
                )
        );

        McpHttpResponse denied = foreignNode.handle(foreign).block();
        assertEquals(403, denied.status());
        assertTrue(body(denied).contains("MCP_SESSION_BINDING_MISMATCH"));
    }

    private McpHttpRequest request(
            String method,
            Map<String, String> headers,
            String body) {
        return new McpHttpRequest(
                method,
                "/mcp/orders",
                headers,
                body,
                Map.of()
        );
    }

    private String body(McpHttpResponse response) {
        return Flux.from(response.body())
                .reduce(new StringBuilder(), (result, bytes) -> result.append(
                        new String(bytes, StandardCharsets.UTF_8)
                ))
                .map(StringBuilder::toString)
                .block();
    }
}
