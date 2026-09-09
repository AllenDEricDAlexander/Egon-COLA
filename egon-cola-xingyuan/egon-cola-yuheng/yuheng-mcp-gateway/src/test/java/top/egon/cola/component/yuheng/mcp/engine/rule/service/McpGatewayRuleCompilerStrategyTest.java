package top.egon.cola.component.yuheng.mcp.engine.rule.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.yuheng.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.yuheng.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.yuheng.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.yuheng.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleContent;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.yuheng.mcp.engine.rule.domain.McpGatewayCompiledRulesDTO;
import top.egon.cola.component.yuheng.mcp.rule.domain.CompiledMcpRules;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class McpGatewayRuleCompilerStrategyTest {

    private final McpGatewayRuleCompilerStrategy compiler = new McpGatewayRuleCompilerStrategy();

    @Test
    void compilesPublishedLocalToolsAndSharedPolicyViewsWithOneIdentity() {
        GatewayRuleSnapshot snapshot = snapshot(tool("LOCAL_OPERATION", "HTTP", "operation-1"),
                List.of(operation()), List.of());
        McpGatewayCompiledRulesDTO compiled = compiler.compile(snapshot);

        assertSame(snapshot, compiled.snapshot());
        assertEquals(snapshot.releaseId(), compiled.releaseId());
        assertEquals(snapshot.artifactSha256(), compiled.ruleChecksum());
        assertTrue(compiled.mcpRules().tool("orders", "orders.get").isPresent());
        assertEquals(1, compiled.providerServices().size());
        assertTrue(compiled.providerPolicies().containsKey("balance"));
        assertTrue(compiled.trafficPolicies().containsKey("timeout"));
        assertEquals(Set.of("snapshot", "providerServices", "providerPolicies", "trafficPolicies", "mcpRules"),
                Arrays.stream(McpGatewayCompiledRulesDTO.class.getRecordComponents())
                        .map(component -> component.getName()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void ignoresApiIngressRoutesInsteadOfBuildingHttpOrRpcIndexes() {
        GatewayRuntimeRoute apiRoute = new GatewayRuntimeRoute("api-route", "missing-api-operation",
                "api.example.test", "GET", "/api-only", Set.of(AccessZone.PUBLIC), 0, true);
        var compiled = compiler.compile(snapshot(tool("LOCAL_OPERATION", "HTTP", "operation-1"),
                List.of(operation()), List.of(apiRoute)));

        assertTrue(compiled.mcpRules().tool("orders", "orders.get").isPresent());
        assertFalse(Arrays.stream(compiled.getClass().getMethods())
                .anyMatch(method -> Set.of("httpRoutes", "rpcMethods").contains(method.getName())));
    }

    @Test
    void rejectsMissingLocalOperationWithoutAnyDraftFallback() {
        assertThrows(IllegalArgumentException.class, () -> compiler.compile(
                snapshot(tool("LOCAL_OPERATION", "HTTP", "missing"), List.of(operation()), List.of())));
    }

    @Test
    void rejectsUnsupportedDraftSourceAndMismatchedProtocol() {
        assertThrows(IllegalArgumentException.class, () -> compiler.compile(
                snapshot(tool("MANUAL_DRAFT", "HTTP", "operation-1"), List.of(operation()), List.of())));
        assertThrows(IllegalArgumentException.class, () -> compiler.compile(
                snapshot(tool("LOCAL_OPERATION", "RPC", "operation-1"), List.of(operation()), List.of())));
    }

    @Test
    void copiesCollectionsAndRejectsDetachedMcpProjection() {
        var compiled = compiler.compile(snapshot(tool("LOCAL_OPERATION", "HTTP", "operation-1"),
                List.of(operation()), List.of()));
        var services = new HashSet<>(compiled.providerServices());
        var policies = new HashMap<>(compiled.providerPolicies());
        var copy = new McpGatewayCompiledRulesDTO(compiled.snapshot(), services, policies,
                compiled.trafficPolicies(), compiled.mcpRules());
        services.clear();
        policies.clear();

        assertEquals(1, copy.providerServices().size());
        assertTrue(copy.providerPolicies().containsKey("balance"));
        assertThrows(UnsupportedOperationException.class, () -> copy.providerServices().clear());
        assertThrows(IllegalArgumentException.class, () -> new McpGatewayCompiledRulesDTO(
                compiled.snapshot(), Set.of(), Map.of(), Map.of(), CompiledMcpRules.empty()));
        assertThrows(NullPointerException.class, () -> compiler.compile(null));
    }

    @Test
    void exposesExactlyTheNamedMcpStrategyBean() {
        try (var context = new AnnotationConfigApplicationContext(McpGatewayRuleCompilerStrategy.class)) {
            assertSame(context.getBean(McpGatewayRuleCompilerStrategy.class),
                    context.getBean("mcpGatewayRuleCompilerStrategy"));
        }
    }

    private GatewayRuleSnapshot snapshot(McpRuntimeTool tool,
                                        List<GatewayRuntimeOperation> operations,
                                        List<GatewayRuntimeRoute> routes) {
        McpRuntimeServer server = new McpRuntimeServer("server-1", "orders", "Orders",
                "Order capabilities", "Use approved operations.",
                Set.of(McpProtocolDialect.STABLE_2025_11_25),
                "https://resource.egon.top/gateway-mcp", 30, true);
        McpRuleContent mcp = new McpRuleContent(List.of(server), List.of(tool),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        GatewayRuleContent content = new GatewayRuleContent("group-1", "orders", "local", "default",
                operations, routes,
                List.of(new GatewayRuntimePolicy("balance", "LOAD_BALANCE", "GLOBAL",
                        Map.of("algorithm", "ROUND_ROBIN"))),
                List.of(new GatewayRuntimePolicy("timeout", "TIMEOUT", "OPERATION",
                        Map.of("timeout", "PT10S"))),
                List.of(), List.of(), List.of(), mcp);
        return new GatewayRuleSnapshot("v1", "release-1", Instant.EPOCH,
                "content-sha", "artifact-sha", content);
    }

    private McpRuntimeTool tool(String source, String protocol, String operationId) {
        return new McpRuntimeTool("tool-1", "orders", "orders.get", "Get orders",
                source, operationId, protocol, null, "{\"type\":\"object\"}", "{}",
                Map.of(), Set.of("orders.read"), "LOW", true, true);
    }

    private GatewayRuntimeOperation operation() {
        return new GatewayRuntimeOperation("operation-1", "orders:get", GatewayProtocol.HTTP,
                "GET /orders", "{}", "{}", true,
                new GatewayProviderServiceRef("business", "orders-app", "local", "default",
                        GatewayProtocol.HTTP, "orders", "default", "v1", "http"),
                "TRANSPARENT", Set.of("balance", "timeout"), Map.of(), false);
    }
}
