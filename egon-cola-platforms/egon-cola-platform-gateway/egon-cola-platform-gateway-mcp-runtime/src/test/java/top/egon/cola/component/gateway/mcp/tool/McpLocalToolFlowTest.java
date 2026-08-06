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
import top.egon.cola.component.gateway.core.mcp.security.McpApprovalPort;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationPort;
import top.egon.cola.component.gateway.mcp.rule.McpRuleCompiler;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpLocalToolFlowTest {

    @Test
    void listsAndCallsLocalToolWithPositionAwareArguments() {
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
                .compile(rules("HTTP"), Set.of("operation-42")));
        McpToolsListHandler listHandler = new McpToolsListHandler(
                catalog,
                new ObjectMapper()
        );
        McpToolsCallHandler callHandler = new McpToolsCallHandler(
                catalog,
                new McpResultBinder(new ObjectMapper()),
                invoker,
                new McpSecurityGate(
                        request -> Mono.just(
                                McpAuthorizationPort.Decision.allowed(
                                        1L,
                                        1L,
                                        1L
                                )
                        ),
                        request -> Mono.just(
                                McpApprovalPort.Result.APPROVED
                        ),
                        new ObjectMapper()
                )
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
                                "includeLines", true,
                                "request", Map.of("currency", "CNY")
                        )
                )),
                context
        )).block();

        Map<?, ?> listResult = (Map<?, ?>) listed.result();
        List<?> tools = (List<?>) listResult.get("tools");
        assertEquals("find_invoice", ((Map<?, ?>) tools.getFirst()).get("name"));
        assertEquals("operation-42", invocation.get().operationId());
        assertEquals(Map.of("invoiceId", "invoice-9"),
                invocation.get().call().pathArguments());
        assertEquals(Map.of("includeLines", true),
                invocation.get().call().queryArguments());
        assertEquals(Map.of("currency", "CNY"),
                invocation.get().call().body());
        assertEquals("Bearer local-jwt", invocation.get().originalBearerToken());
        assertEquals("paid", ((Map<?, ?>) ((Map<?, ?>) called.result())
                .get("structuredContent")).get("status"));
    }

    @Test
    void sendsTheEntireRpcArgumentsObjectAsTheRequestBody() {
        AtomicReference<GatewayOperationInvocation> invocation =
                new AtomicReference<>();
        McpToolCatalog catalog = new McpToolCatalog(() -> new McpRuleCompiler()
                .compile(rules("RPC"), Set.of("operation-42")));
        McpToolsCallHandler handler = new McpToolsCallHandler(
                catalog,
                new McpResultBinder(new ObjectMapper()),
                request -> {
                    invocation.set(request);
                    return Mono.just(new GatewayInvocationResult(
                            200,
                            Map.of(),
                            "{}".getBytes(StandardCharsets.UTF_8)
                    ));
                },
                new McpSecurityGate(
                        request -> Mono.just(
                                McpAuthorizationPort.Decision.allowed(
                                        1L,
                                        1L,
                                        1L
                                )
                        ),
                        request -> Mono.just(
                                McpApprovalPort.Result.APPROVED
                        ),
                        new ObjectMapper()
                )
        );
        Map<String, Object> arguments = Map.of(
                "customerId", "customer-7",
                "lines", List.of(Map.of("sku", "sku-1", "quantity", 2))
        );

        Mono.from(handler.handle(
                request(3L, "tools/call", Map.of(
                        "name", "find_invoice",
                        "arguments", arguments
                )),
                context()
        )).block();

        assertEquals(arguments, invocation.get().call().body());
        assertEquals(Map.of(), invocation.get().call().pathArguments());
        assertEquals(Map.of(), invocation.get().call().queryArguments());
    }

    private McpRuleContent rules(String protocol) {
        return new McpRuleContent(
                List.of(server()),
                List.of(new McpRuntimeTool(
                        "tool-1",
                        "billing",
                        "find_invoice",
                        "Find an invoice",
                        "LOCAL_OPERATION",
                        "operation-42",
                        protocol,
                        null,
                        "{\"type\":\"object\"}",
                        "{\"type\":\"object\"}",
                        "HTTP".equals(protocol)
                                ? Map.of(
                                        "invoiceId", "PATH",
                                        "includeLines", "QUERY",
                                        "request", "BODY"
                                )
                                : Map.of(),
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
                Map.ofEntries(
                        Map.entry(
                                "originalBearerToken",
                                "Bearer local-jwt"
                        ),
                        Map.entry("callerId", "user-7"),
                        Map.entry("tenantId", "tenant-a"),
                        Map.entry("clientIp", "127.0.0.1"),
                        Map.entry("traceparent", "00-trace-parent"),
                        Map.entry("idp.issuer", "https://idp.internal"),
                        Map.entry("idp.session-id", "session-1"),
                        Map.entry("idp.client-id", "finance-web"),
                        Map.entry("idp.token-id", "token-1"),
                        Map.entry("idp.token-version", "2"),
                        Map.entry("idp.audience", "gateway-mcp"),
                        Map.entry(
                                "idp.issued-at",
                                "2026-08-02T04:59:30Z"
                        ),
                        Map.entry(
                                "idp.expires-at",
                                "2026-08-02T05:05:00Z"
                        )
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
