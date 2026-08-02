package top.egon.cola.component.gateway.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.core.operation.GatewayInvocationResult;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvoker;
import top.egon.cola.component.gateway.mcp.rule.McpRuleCompiler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class McpLocalToolFlowTest {

    @Test
    void listsAndCallsLocalToolWithoutAcceptingRoutingCoordinates() {
        AtomicReference<GatewayOperationInvocation> invocation =
                new AtomicReference<>();
        GatewayOperationInvoker invoker = request -> {
            invocation.set(request);
            return Mono.just(new GatewayInvocationResult(
                    200,
                    Map.of("content-type", List.of("application/json")),
                    "{\"status\":\"paid\"}".getBytes(StandardCharsets.UTF_8)
            ));
        };
        McpToolCatalog catalog = new McpToolCatalog(() -> new McpRuleCompiler()
                .compile(rules(), Set.of("operation-42")));
        McpToolsListHandler listHandler = new McpToolsListHandler(
                catalog,
                new ObjectMapper()
        );
        McpToolsCallHandler callHandler = new McpToolsCallHandler(
                catalog,
                new McpArgumentBinder(),
                new McpResultBinder(new ObjectMapper()),
                invoker
        );
        McpRequestContext context = context();

        McpJsonRpcResponse listed = Mono.from(listHandler.handle(
                request(1L, "tools/list", Map.of()),
                context
        )).block();
        McpJsonRpcResponse called = Mono.from(callHandler.handle(
                request(2L, "tools/call", Map.of(
                        "name", "find_invoice",
                        "arguments", Map.of(
                                "invoiceId", "invoice-9",
                                "providerUrl", "https://attacker.invalid",
                                "routeId", "route-attacker"
                        )
                )),
                context
        )).block();

        Map<?, ?> listResult = (Map<?, ?>) listed.result();
        List<?> tools = (List<?>) listResult.get("tools");
        assertEquals("find_invoice", ((Map<?, ?>) tools.getFirst()).get("name"));
        assertEquals("operation-42", invocation.get().operationId());
        assertEquals(Map.of("invoiceId", "invoice-9"),
                invocation.get().arguments());
        assertFalse(invocation.get().arguments().containsKey("providerUrl"));
        assertEquals("Bearer local-jwt", invocation.get().originalBearerToken());
        assertEquals("paid", ((Map<?, ?>) ((Map<?, ?>) called.result())
                .get("structuredContent")).get("status"));
    }

    private McpRuleContent rules() {
        return new McpRuleContent(
                List.of(server()),
                List.of(new McpRuntimeTool(
                        "tool-1",
                        "billing",
                        "find_invoice",
                        "Find an invoice",
                        "LOCAL_OPERATION",
                        "operation-42",
                        null,
                        "{\"type\":\"object\"}",
                        "{\"type\":\"object\"}",
                        Map.of("invoiceId", "invoiceId"),
                        Map.of(),
                        Map.of("readOnlyHint", "true"),
                        Set.of("invoice:read"),
                        "LOW",
                        true,
                        true
                )),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private McpRequestContext context() {
        return new McpRequestContext(
                server(),
                McpProtocolDialect.STABLE_2025_11_25,
                "session-1",
                Map.of(
                        "originalBearerToken", "Bearer local-jwt",
                        "callerId", "user-7",
                        "clientIp", "127.0.0.1",
                        "traceparent", "00-trace-parent"
                )
        );
    }

    private McpRuntimeServer server() {
        return new McpRuntimeServer(
                "server-1",
                "billing",
                "Billing",
                "Billing operations",
                "Use approved billing capabilities.",
                Set.of(McpProtocolDialect.STABLE_2025_11_25),
                "gateway-mcp",
                30,
                true
        );
    }

    private McpJsonRpcRequest request(
            Object id,
            String method,
            Map<String, Object> params) {
        return new McpJsonRpcRequest("2.0", id, method, params, Map.of());
    }
}
