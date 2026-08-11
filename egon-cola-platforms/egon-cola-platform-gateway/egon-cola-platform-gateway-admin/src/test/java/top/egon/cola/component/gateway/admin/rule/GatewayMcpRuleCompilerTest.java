package top.egon.cola.component.gateway.admin.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayMcpRuleCompilerTest {

    private final GatewayRuleCompiler compiler = new GatewayRuleCompiler(
            new GatewayRuleCanonicalizer()
    );

    @Test
    void publishesMcpInsideTheCanonicalGatewaySnapshot() {
        McpRuleContent mcp = mcp("orders");

        CompiledGatewayRelease release = compiler.compile(
                "release-mcp",
                Instant.parse("2026-08-02T00:00:00Z"),
                content(mcp)
        );

        assertEquals(mcp, release.snapshot().content().mcp());
        assertTrue(release.snapshotJson().contains("\"mcp\""));
        assertTrue(release.snapshotJson().contains("\"orders.get\""));
    }

    @Test
    void rejectsLocalMcpToolThatReferencesAnUnknownOperation() {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> compiler.compile(
                        "release-invalid",
                        Instant.parse("2026-08-02T00:00:00Z"),
                        content(mcp("missing-operation"))
                )
        );

        assertTrue(failure.getMessage().contains(
                "MCP tool references unknown operation"
        ));
    }

    private GatewayRuleContent content(McpRuleContent mcp) {
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "orders",
                "orders",
                GatewayProtocol.HTTP,
                "GET /orders",
                "{}",
                "{}",
                true,
                new GatewayProviderServiceRef(
                        "test-biz",
                        "test-app",
                        "local",
                        "default",
                        GatewayProtocol.HTTP,
                        "orders",
                        "default",
                        "v1",
                        "http"
                ),
                "TRANSPARENT",
                Set.of(),
                Map.of(),
                false
        );
        return new GatewayRuleContent(
                "group-1",
                "orders",
                "local",
                "default",
                List.of(operation),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                mcp
        );
    }

    private McpRuleContent mcp(String operationId) {
        return new McpRuleContent(
                List.of(new McpRuntimeServer(
                        "server-1",
                        "orders",
                        "Orders",
                        "Order capabilities",
                        "Use approved order operations.",
                        Set.of(McpProtocolDialect.STABLE_2025_11_25),
                        "https://resource.egon.top/gateway-mcp",
                        30,
                        true
                )),
                List.of(new McpRuntimeTool(
                        "tool-1",
                        "orders",
                        "orders.get",
                        "Get an order",
                        "LOCAL_OPERATION",
                        operationId,
                        "HTTP",
                        null,
                        "{}",
                        "{}",
                        Map.of(),
                        Set.of("mcp:orders:tool:orders.get:call"),
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
}
