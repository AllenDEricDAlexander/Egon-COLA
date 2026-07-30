package top.egon.cola.component.gateway.engine.traffic;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.engine.rpc.RpcMethodIndex;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayTrafficGovernanceTest {

    @Test
    void enforcesReferencedLocalRateLimit() {
        RuntimeTrafficPolicy rateLimit = runtime(new GatewayRuntimePolicy(
                "one-request",
                "RATE_LIMIT",
                "OPERATION",
                Map.of(
                        "keyExpression",
                        "${operationId}",
                        "capacity",
                        1,
                        "initialTokens",
                        1,
                        "refillTokens",
                        1,
                        "refillPeriod",
                        "PT1H"
                )
        ));
        GatewayTrafficGovernance governance = governance(rateLimit);

        governance.acquire(
                Set.of("one-request"),
                context(),
                Duration.ofSeconds(2)
        ).block().close();

        GatewayTrafficRejectedException rejected = assertThrows(
                GatewayTrafficRejectedException.class,
                () -> governance.acquire(
                        Set.of("one-request"),
                        context(),
                        Duration.ofSeconds(2)
                ).block()
        );
        assertEquals("GATEWAY_RATE_LIMITED", rejected.code());
    }

    @Test
    void releasesNonBlockingBulkheadPermit() {
        RuntimeTrafficPolicy bulkhead = runtime(new GatewayRuntimePolicy(
                "single-flight",
                "BULKHEAD",
                "OPERATION",
                Map.of("maxConcurrent", 1)
        ));
        GatewayTrafficGovernance governance = governance(bulkhead);
        GatewayTrafficGovernance.RequestPermit first =
                governance.acquire(
                        Set.of("single-flight"),
                        context(),
                        Duration.ofSeconds(2)
                ).block();

        GatewayTrafficRejectedException rejected = assertThrows(
                GatewayTrafficRejectedException.class,
                () -> governance.acquire(
                        Set.of("single-flight"),
                        context(),
                        Duration.ofSeconds(2)
                ).block()
        );
        assertEquals("GATEWAY_CONCURRENCY_REJECTED", rejected.code());

        first.close();
        governance.acquire(
                Set.of("single-flight"),
                context(),
                Duration.ofSeconds(2)
        ).block().close();
    }

    @Test
    void selectsMostRestrictiveReferencedBodySizePolicies() {
        RuntimeTrafficPolicy requestSize = runtime(new GatewayRuntimePolicy(
                "request-size",
                "REQUEST_SIZE",
                "ROUTE",
                Map.of("maxBytes", 512)
        ));
        RuntimeTrafficPolicy responseSize = runtime(new GatewayRuntimePolicy(
                "response-size",
                "RESPONSE_SIZE",
                "ROUTE",
                Map.of("maxBytes", 1024)
        ));
        GatewayTrafficGovernance governance = governance(
                requestSize,
                responseSize
        );

        GatewayTrafficGovernance.RequestPermit permit =
                governance.acquire(
                        Set.of("request-size", "response-size"),
                        context(),
                        Duration.ofSeconds(2)
                ).block();

        assertEquals(512, permit.requestSizeLimit(2048));
        assertEquals(1024, permit.responseSizeLimit(4096));
        permit.close();
    }

    @Test
    void rpcRouteDeadlineRemainsShorterThanTimeoutPolicy() {
        RuntimeTrafficPolicy timeout = runtime(new GatewayRuntimePolicy(
                "timeout",
                "TIMEOUT",
                "ROUTE",
                Map.of("timeout", "PT1M")
        ));
        GatewayTrafficGovernance governance = governance(timeout);

        GatewayTrafficGovernance.RequestPermit permit = governance.acquire(
                Set.of("timeout"),
                context(),
                Duration.ofSeconds(3)
        ).block();

        assertEquals(Duration.ofSeconds(3), permit.timeout());
        permit.close();
    }

    private RuntimeTrafficPolicy runtime(GatewayRuntimePolicy source) {
        return new GatewayTrafficPolicyCompiler()
                .compile(List.of(source))
                .get(source.policyId());
    }

    private GatewayTrafficGovernance governance(
            RuntimeTrafficPolicy... policies) {
        GatewayRuleContent content = new GatewayRuleContent(
                "group-id",
                "group",
                "test",
                "default",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        GatewayRuleSnapshot snapshot = new GatewayRuleSnapshot(
                "v1",
                "release-1",
                Instant.EPOCH,
                "content",
                "artifact",
                content
        );
        CompiledGatewayRules rules = new CompiledGatewayRules(
                snapshot,
                new HttpRouteCompiler().compile(List.of()),
                RpcMethodIndex.empty(),
                Set.of(),
                Map.of(),
                java.util.Arrays.stream(policies).collect(
                        java.util.stream.Collectors.toUnmodifiableMap(
                                RuntimeTrafficPolicy::policyId,
                                java.util.function.Function.identity()
                        )
                ),
                Map.of(),
                Map.of()
        );
        return new GatewayTrafficGovernance(() -> rules, null);
    }

    private GatewayTrafficContext context() {
        return new GatewayTrafficContext(
                "operation",
                "route",
                "application",
                "caller",
                "127.0.0.1",
                "provider",
                null,
                Map.of(),
                Map.of(),
                Map.of()
        );
    }
}
