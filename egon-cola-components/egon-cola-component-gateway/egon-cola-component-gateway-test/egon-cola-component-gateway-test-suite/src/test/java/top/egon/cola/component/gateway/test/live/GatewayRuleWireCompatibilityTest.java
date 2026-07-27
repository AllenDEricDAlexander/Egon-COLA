package top.egon.cola.component.gateway.test.live;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCompiler;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeParameter;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleJsonCodec;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

class GatewayRuleWireCompatibilityTest {

    @Test
    void engineVerifiesAdminSnapshotWithMultiZoneRouteAndTrafficPolicy() {
        GatewayRuntimePolicy rateLimit = new GatewayRuntimePolicy(
                "live-http-rate",
                "RATE_LIMIT",
                "OPERATION",
                Map.of(
                        "operationIds", List.of("orders"),
                        "keyExpression", "${operationId}",
                        "capacity", 1,
                        "initialTokens", 1,
                        "refillTokens", 1,
                        "refillPeriod", "PT1H",
                        "mode", "DISTRIBUTED"
                )
        );
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "orders",
                "GET /api/orders/{id}",
                GatewayProtocol.HTTP,
                "GET /api/orders/{id}",
                "{}",
                "{}",
                List.of(new GatewayRuntimeParameter(
                        "id",
                        "PATH",
                        true,
                        "java.lang.String",
                        null,
                        "order identifier"
                )),
                true,
                new GatewayProviderServiceRef(
                        "test",
                        "gateway-live",
                        GatewayProtocol.HTTP,
                        "orders-http-provider",
                        "default",
                        "1.0.0-live",
                        "http"
                ),
                "TRANSPARENT",
                Set.of(rateLimit.policyId()),
                Map.of("framework", "mvc"),
                false
        );
        GatewayRuntimeRoute route = new GatewayRuntimeRoute(
                "live-http-order",
                operation.operationId(),
                "api.gateway.test",
                "GET",
                "/api/orders/{id}",
                Set.of(AccessZone.PUBLIC, AccessZone.INTERNAL),
                0,
                true
        );
        GatewayRuleContent content = new GatewayRuleContent(
                "group-1",
                "default",
                "test",
                "gateway-live",
                List.of(operation),
                List.of(route),
                List.of(),
                List.of(rateLimit),
                List.of(),
                List.of(),
                List.of()
        );
        var release = new GatewayRuleCompiler(
                new GatewayRuleCanonicalizer()
        ).compile(
                "release-1",
                Instant.parse("2026-07-27T00:00:00Z"),
                content
        );
        GatewayRuleJsonCodec engineCodec = new GatewayRuleJsonCodec();
        var engineSnapshot = engineCodec.readSnapshot(
                release.snapshotJson().getBytes(StandardCharsets.UTF_8)
        );

        engineCodec.verify(engineSnapshot);
    }
}
