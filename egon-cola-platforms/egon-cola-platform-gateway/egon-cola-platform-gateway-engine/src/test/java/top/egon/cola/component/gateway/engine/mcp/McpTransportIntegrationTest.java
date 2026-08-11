package top.egon.cola.component.gateway.engine.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.engine.http.GatewayCompositeHttpDataPlaneHandler;
import top.egon.cola.component.gateway.engine.http.GatewayHttpEngineProperties;
import top.egon.cola.component.gateway.engine.http.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.security.GatewayTransportSecurity;
import top.egon.cola.component.gateway.mcp.rule.McpRuleCompiler;
import top.egon.cola.component.gateway.mcp.server.McpMethodDispatcher;
import top.egon.cola.component.gateway.mcp.server.handler.McpDiscoverHandler;
import top.egon.cola.component.gateway.mcp.server.handler.McpInitializeHandler;
import top.egon.cola.component.gateway.mcp.server.handler.McpInitializedHandler;
import top.egon.cola.component.gateway.mcp.server.handler.McpPingHandler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpTransportIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void exposesStableRcAndMetadataBeforeNormalRoutes() throws Exception {
        SharedMcpTransportStore store = new SharedMcpTransportStore();
        McpEngineHttpHandler mcp = handler(store);
        GatewayCompositeHttpDataPlaneHandler composite =
                new GatewayCompositeHttpDataPlaneHandler(
                        mcp,
                        (zone, request) -> request.body().then(Mono.just(
                                GatewayOutboundHttpResponse.text(
                                        299,
                                        "normal-route"
                                )
                        ))
                );
        GatewayHttpServer server = new GatewayHttpServer(
                properties(),
                composite
        );
        server.start();
        try {
            HttpResult initialized = post(
                    server.publicPort(),
                    "/mcp/orders",
                    Map.of("content-type", "application/json"),
                    """
                    {"jsonrpc":"2.0","id":1,"method":"initialize",\
                    "params":{"protocolVersion":"2025-11-25"}}
                    """
            );
            assertEquals(200, initialized.status());
            assertNotNull(initialized.sessionId());
            assertEquals(
                    "2025-11-25",
                    MAPPER.readTree(initialized.body())
                            .path("result")
                            .path("protocolVersion")
                            .asText()
            );

            HttpResult rc = post(
                    server.publicPort(),
                    "/mcp/orders",
                    Map.of(
                            "content-type", "application/json",
                            "Mcp-Protocol-Version", "2026-07-28",
                            "Mcp-Method", "server/discover"
                    ),
                    """
                    {"jsonrpc":"2.0","id":2,"method":"server/discover",\
                    "params":{"_meta":{"client":"test"}}}
                    """
            );
            assertEquals(200, rc.status());
            assertEquals(
                    "2026-07-28",
                    MAPPER.readTree(rc.body())
                            .path("result")
                            .path("protocolVersion")
                            .asText()
            );

            JsonNode metadata = MAPPER.readTree(HttpClient.create()
                    .get()
                    .uri("http://127.0.0.1:" + server.publicPort()
                            + "/.well-known/oauth-protected-resource/mcp/orders")
                    .responseSingle((response, body) -> body.asString())
                    .block(Duration.ofSeconds(2)));
            assertEquals(
                    "https://resource.egon.top/gateway-mcp",
                    metadata.path("resourceUri").asText()
            );

            HttpResult delegated = get(
                    server.publicPort(),
                    "/ordinary"
            );
            assertEquals(299, delegated.status());
            assertEquals("normal-route", delegated.body());
        } finally {
            server.close();
        }
    }

    @Test
    void rejectsStableFollowUpWithoutSession() {
        SharedMcpTransportStore store = new SharedMcpTransportStore();
        GatewayHttpServer server = new GatewayHttpServer(
                properties(),
                new GatewayCompositeHttpDataPlaneHandler(
                        handler(store),
                        (zone, request) -> Mono.just(
                                GatewayOutboundHttpResponse.text(404, "none")
                        )
                )
        );
        server.start();
        try {
            HttpResult result = post(
                    server.publicPort(),
                    "/mcp/orders",
                    Map.of(
                            "content-type", "application/json",
                            "Mcp-Protocol-Version", "2025-11-25"
                    ),
                    """
                    {"jsonrpc":"2.0","id":3,"method":"ping","params":{}}
                    """
            );
            assertEquals(400, result.status());
            assertTrue(result.body().contains("MCP_SESSION_REQUIRED"));
        } finally {
            server.close();
        }
    }

    static McpEngineHttpHandler handler(SharedMcpTransportStore store) {
        return handler(store, identity());
    }

    static McpEngineHttpHandler handler(
            SharedMcpTransportStore store,
            Map<String, Object> identity) {
        McpRuntimeServer server = new McpRuntimeServer(
                "server-1",
                "orders",
                "Orders",
                "Order capabilities",
                "Use approved operations.",
                Set.of(
                        McpProtocolDialect.STABLE_2025_11_25,
                        McpProtocolDialect.RC_2026_07_28,
                        McpProtocolDialect.LEGACY_2024_SSE
                ),
                "https://resource.egon.top/gateway-mcp",
                30,
                true
        );
        var rules = new McpRuleCompiler().compile(new McpRuleContent(
                List.of(server),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        ));
        return new McpEngineHttpHandler(
                () -> rules,
                new McpMethodDispatcher(List.of(
                        new McpInitializeHandler(),
                        new McpInitializedHandler(),
                        new McpPingHandler(),
                        new McpDiscoverHandler()
                )),
                store,
                store,
                (request, selected) -> Mono.just(identity),
                MAPPER,
                Clock.fixed(
                        Instant.parse("2026-08-02T00:00:00Z"),
                        ZoneOffset.UTC
                ),
                Duration.ofMinutes(30),
                Duration.ofMillis(20),
                1024 * 1024
        );
    }

    static Map<String, Object> identity() {
        return Map.of(
                "callerId", "user-1",
                "tenantId", "tenant-1",
                "idp.client-id", "client-1",
                "identity.session-id", "login-session-1",
                "identity.token-id", "token-1"
        );
    }

    private HttpResult post(
            int port,
            String path,
            Map<String, String> headers,
            String body) {
        return HttpClient.create()
                .headers(target -> headers.forEach(target::set))
                .post()
                .uri("http://127.0.0.1:" + port + path)
                .send((request, outbound) -> outbound.sendString(
                        Mono.just(body)
                ))
                .responseSingle((response, content) -> content.asString()
                        .defaultIfEmpty("")
                        .map(value -> new HttpResult(
                                response.status().code(),
                                response.responseHeaders().get(
                                        "Mcp-Session-Id"
                                ),
                                value
                        )))
                .block(Duration.ofSeconds(2));
    }

    private HttpResult get(int port, String path) {
        return HttpClient.create()
                .get()
                .uri("http://127.0.0.1:" + port + path)
                .responseSingle((response, content) -> content.asString()
                        .defaultIfEmpty("")
                        .map(value -> new HttpResult(
                                response.status().code(),
                                response.responseHeaders().get(
                                        "Mcp-Session-Id"
                                ),
                                value
                        )))
                .block(Duration.ofSeconds(2));
    }

    private GatewayHttpEngineProperties properties() {
        return new GatewayHttpEngineProperties(
                new GatewayHttpEngineProperties.Listener(
                        true,
                        "127.0.0.1",
                        0,
                        GatewayTransportSecurity.developmentPlaintextConfig()
                ),
                new GatewayHttpEngineProperties.Listener(
                        false,
                        "127.0.0.1",
                        0,
                        GatewayTransportSecurity.developmentPlaintextConfig()
                ),
                64,
                8192,
                1024 * 1024,
                Duration.ofSeconds(30),
                Duration.ofSeconds(2),
                16,
                16
        );
    }

    private record HttpResult(int status, String sessionId, String body) {
    }
}
