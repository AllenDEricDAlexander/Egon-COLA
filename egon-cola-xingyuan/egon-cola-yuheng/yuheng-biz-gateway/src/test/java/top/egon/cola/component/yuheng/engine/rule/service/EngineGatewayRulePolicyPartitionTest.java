package top.egon.cola.component.yuheng.engine.rule.service;

import top.egon.cola.component.yuheng.engine.rule.domain.ApiRpcGatewayCompiledRulesDTO;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.yuheng.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.yuheng.contract.rule.GatewayRouteTransportPolicy;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleContent;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.yuheng.runtime.traffic.domain.TrafficPolicyType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineGatewayRulePolicyPartitionTest {

    @Test
    void ignoresMcpProjectionWithoutAddingMcpStateToApiRules() {
        var invalidTool = new top.egon.cola.component.yuheng.contract.mcp.rule.McpRuntimeTool(
                "tool-1", "missing-server", "tool", "invalid MCP reference",
                "LOCAL_OPERATION", "missing-operation", "HTTP", null, "{}", "{}",
                Map.of(), Set.of(), "LOW", true, true);
        var mcp = new top.egon.cola.component.yuheng.contract.mcp.rule.McpRuleContent(
                List.of(), List.of(invalidTool), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
        var content = new GatewayRuleContent("group-1", "orders", "test", "default",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), mcp);
        var snapshot = new GatewayRuleSnapshot("v1", "release-1", Instant.EPOCH,
                "content-sha", "artifact-sha", content);
        var compiled = new ApiRpcGatewayRuleCompilerStrategy().compile(snapshot);
        assertEquals(snapshot, compiled.snapshot());
        assertEquals("artifact-sha", compiled.ruleChecksum());
        assertTrue(java.util.Arrays.stream(ApiRpcGatewayCompiledRulesDTO.class.getRecordComponents())
                .noneMatch(field -> field.getName().equals("mcpRules")));
    }

    @Test
    void compilesSecurityPoliciesFromDedicatedSnapshotSection() {
        GatewayRuntimePolicy security = new GatewayRuntimePolicy(
                "security-1",
                "SECURITY",
                "GLOBAL",
                Map.of(
                        "routeSecurityType", "PUBLIC_PROTOCOL",
                        "authenticationMode", "NONE",
                        "failureMode", "FAIL_CLOSED"
                )
        );
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "operation-1",
                "orders:http:GET:/orders",
                GatewayProtocol.HTTP,
                "GET /orders",
                "{}",
                "{}",
                true,
                new GatewayProviderServiceRef(
                        "test-biz",
                        "test-app",
                        "test",
                        "default",
                        GatewayProtocol.HTTP,
                        "orders",
                        "default",
                        "1.0.0",
                        "HTTP"
                ),
                "TRANSPARENT",
                Set.of("security-1"),
                Map.of(),
                false
        );
        GatewayRuleContent content = new GatewayRuleContent(
                "group-1",
                "orders",
                "test",
                "default",
                List.of(operation),
                List.of(new GatewayRuntimeRoute(
                        "route-1",
                        "operation-1",
                        "api.example.com",
                        "GET",
                        "/orders",
                        Set.of(AccessZone.PUBLIC),
                        0,
                        true
                )),
                List.of(),
                List.of(),
                List.of(security),
                List.of(),
                List.of()
        );
        GatewayRuleSnapshot snapshot = new GatewayRuleSnapshot(
                "v1",
                "release-1",
                Instant.parse("2026-07-25T00:00:00Z"),
                "content-sha",
                "artifact-sha",
                content
        );

        ApiRpcGatewayCompiledRulesDTO compiled =
                new ApiRpcGatewayRuleCompilerStrategy().compile(snapshot);

        assertTrue(compiled.securityPolicies().containsKey("security-1"));
    }

    @Test
    void httpEffectiveTimeoutCanExceedLegacyFallbackWithoutCreatingRetry() {
        GatewayRuntimePolicy timeout = new GatewayRuntimePolicy(
                "timeout-1",
                "TIMEOUT",
                "ROUTE",
                Map.of("timeout", "PT1M")
        );
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "operation-1",
                "orders:http:GET:/orders",
                GatewayProtocol.HTTP,
                "GET /orders",
                "{}",
                "{}",
                true,
                new GatewayProviderServiceRef(
                        "test-biz",
                        "test-app",
                        "test",
                        "default",
                        GatewayProtocol.HTTP,
                        "orders",
                        "default",
                        "1.0.0",
                        "HTTP"
                ),
                "TRANSPARENT",
                Set.of("timeout-1"),
                Map.of(),
                false
        );
        GatewayRouteTransportPolicy transport =
                new GatewayRouteTransportPolicy(
                        GatewayRouteProfile.DEFAULT,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        true
                );
        GatewayRuleContent content = new GatewayRuleContent(
                "group-1",
                "orders",
                "test",
                "default",
                List.of(operation),
                List.of(new GatewayRuntimeRoute(
                        "route-1",
                        "operation-1",
                        "api.example.com",
                        "GET",
                        "/orders",
                        Set.of(AccessZone.PUBLIC),
                        0,
                        true,
                        transport
                )),
                List.of(),
                List.of(timeout),
                List.of(),
                List.of(),
                List.of()
        );
        GatewayRuleSnapshot snapshot = new GatewayRuleSnapshot(
                "v1",
                "release-1",
                Instant.parse("2026-07-25T00:00:00Z"),
                "content-sha",
                "artifact-sha",
                content
        );
        ApiRpcGatewayCompiledRulesDTO compiled =
                new ApiRpcGatewayRuleCompilerStrategy().compile(snapshot);

        assertFalse(compiled.trafficPolicies().values().stream()
                .anyMatch(policy -> policy.type()
                        == TrafficPolicyType.RETRY));
        var route = compiled.httpRoutes().match(
                "api.example.com",
                "GET",
                "/orders",
                AccessZone.PUBLIC
        ).orElseThrow().route();
        assertTrue(route.transportPolicy().retryAllowed());
        assertEquals(
                Duration.ofMinutes(1),
                route.transportPolicy().totalTimeout().orElseThrow()
        );
    }
}
