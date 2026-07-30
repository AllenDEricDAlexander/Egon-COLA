package top.egon.cola.component.gateway.engine.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineGatewayRulePolicyPartitionTest {

    @Test
    void compilesSecurityPoliciesFromDedicatedSnapshotSection() {
        GatewayRuntimePolicy security = new GatewayRuntimePolicy(
                "security-1",
                "SECURITY",
                "GLOBAL",
                Map.of(
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

        CompiledGatewayRules compiled =
                new EngineGatewayRuleCompiler().compile(snapshot);

        assertTrue(compiled.securityPolicies().containsKey("security-1"));
    }
}
