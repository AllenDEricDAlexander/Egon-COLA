package top.egon.cola.component.gateway.mcp.server;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.server.handler.McpDiscoverHandler;
import top.egon.cola.component.gateway.mcp.server.handler.McpInitializeHandler;
import top.egon.cola.component.gateway.mcp.server.handler.McpInitializedHandler;
import top.egon.cola.component.gateway.mcp.server.handler.McpPingHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpMethodDispatcherTest {

    private final McpRequestContext context = new McpRequestContext(
            new McpRuntimeServer(
                    "server-1",
                    "billing",
                    "Billing",
                    "Billing operations",
                    "Use approved billing capabilities.",
                    Set.of(
                            McpProtocolDialect.STABLE_2025_11_25,
                            McpProtocolDialect.RC_2026_07_28
                    ),
                    "gateway-mcp",
                    30,
                    true
            ),
            McpProtocolDialect.STABLE_2025_11_25,
            "session-1",
            Map.of()
    );

    @Test
    void dispatchesLifecycleAndNormalizesStableAndRcDescriptions() {
        McpMethodDispatcher dispatcher = dispatcher();

        McpJsonRpcResponse initialize = response(dispatcher, request(
                1L,
                "initialize",
                Map.of("protocolVersion", "2025-11-25")
        ));
        McpJsonRpcResponse discover = response(
                dispatcher,
                request(2L, "server/discover", Map.of())
        );
        McpJsonRpcResponse ping = response(
                dispatcher,
                request(3L, "ping", Map.of())
        );

        assertEquals(
                ((Map<?, ?>) initialize.result()).get("server"),
                ((Map<?, ?>) discover.result()).get("server")
        );
        assertEquals(Map.of(), ping.result());
    }

    @Test
    void returnsMethodNotFoundAndSanitizedProtocolErrors() {
        McpMethodHandler failure = new McpMethodHandler() {
            @Override
            public String method() {
                return "tools/call";
            }

            @Override
            public org.reactivestreams.Publisher<McpJsonRpcResponse> handle(
                    McpJsonRpcRequest request,
                    McpRequestContext context) {
                return Mono.error(new McpProtocolException(
                        McpErrorCode.MCP_FORBIDDEN,
                        "MCP capability is forbidden"
                ));
            }
        };
        McpMethodDispatcher dispatcher = new McpMethodDispatcher(List.of(failure));

        assertEquals(
                McpErrorCode.MCP_METHOD_NOT_FOUND,
                response(dispatcher, request(1L, "unknown", Map.of()))
                        .error().dataCode()
        );
        McpJsonRpcResponse denied = response(
                dispatcher,
                request(2L, "tools/call", Map.of())
        );
        assertEquals(McpErrorCode.MCP_FORBIDDEN, denied.error().dataCode());
        assertEquals(
                "MCP capability is forbidden",
                denied.error().message()
        );
    }

    @Test
    void suppressesNotificationResponsesAndRejectsDuplicateHandlers() {
        McpMethodDispatcher dispatcher = dispatcher();

        List<McpJsonRpcResponse> responses = Flux.from(dispatcher.dispatch(
                request(null, "notifications/initialized", Map.of()),
                context
        )).collectList().block();
        assertEquals(List.of(), responses);
        assertThrows(
                IllegalArgumentException.class,
                () -> new McpMethodDispatcher(List.of(
                        new McpPingHandler(),
                        new McpPingHandler()
                ))
        );
    }

    private McpMethodDispatcher dispatcher() {
        return new McpMethodDispatcher(List.of(
                new McpInitializeHandler(),
                new McpDiscoverHandler(),
                new McpInitializedHandler(),
                new McpPingHandler()
        ));
    }

    private McpJsonRpcResponse response(
            McpMethodDispatcher dispatcher,
            McpJsonRpcRequest request) {
        return Mono.from(dispatcher.dispatch(request, context)).block();
    }

    private McpJsonRpcRequest request(
            Object id,
            String method,
            Map<String, Object> params) {
        return new McpJsonRpcRequest("2.0", id, method, params, Map.of());
    }
}
