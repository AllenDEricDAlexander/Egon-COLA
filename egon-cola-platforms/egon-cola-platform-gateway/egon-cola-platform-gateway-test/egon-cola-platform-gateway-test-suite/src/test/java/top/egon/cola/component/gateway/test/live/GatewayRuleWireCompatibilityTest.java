package top.egon.cola.component.gateway.test.live;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCompiler;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteTransportPolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeParameter;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleJsonCodec;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
                        "test-biz",
                        "test-app",
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

        assertNull(engineSnapshot.content().routes().getFirst()
                .transportPolicy());
        assertEquals(
                "b0d278bfeaa4e5582ed57580135cb4cf9e4b3d89d365416b2250b53f8da96dca",
                engineSnapshot.ruleContentSha256()
        );
        engineCodec.verify(engineSnapshot);
    }

    @Test
    void engineVerifiesAdminSnapshotWithOpenAiTransportPolicy() {
        GatewayRouteTransportPolicy transport =
                new GatewayRouteTransportPolicy(
                        GatewayRouteProfile.OPENAI_HTTP,
                        GatewayTransportProtocol.HTTP,
                        GatewayRequestBodyMode.STREAMING,
                        GatewayTransportResponseMode.SSE,
                        536_870_912L,
                        10_000L,
                        120_000L,
                        90_000L,
                        1_800_000L,
                        null,
                        null,
                        false,
                        false
                );
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "openai-responses",
                "POST /v1/responses",
                GatewayProtocol.HTTP,
                "POST /v1/responses",
                "{}",
                "{}",
                List.of(),
                true,
                new GatewayProviderServiceRef(
                        "test-biz",
                        "test-app",
                        "test",
                        "gateway-live",
                        GatewayProtocol.HTTP,
                        "openai-compatible-provider",
                        "default",
                        "v1",
                        "https"
                ),
                "TRANSPARENT",
                Set.of(),
                Map.of("framework", "webflux"),
                false
        );
        GatewayRuntimeRoute route = new GatewayRuntimeRoute(
                "openai-responses",
                operation.operationId(),
                "api.openai.example",
                "POST",
                "/v1/responses",
                Set.of(AccessZone.PUBLIC),
                0,
                true,
                transport
        );
        GatewayRuleContent content = new GatewayRuleContent(
                "group-openai",
                "default",
                "test",
                "gateway-live",
                List.of(operation),
                List.of(route),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        var release = new GatewayRuleCompiler(
                new GatewayRuleCanonicalizer()
        ).compile(
                "release-openai",
                Instant.parse("2026-07-30T00:00:00Z"),
                content
        );
        GatewayRuleJsonCodec engineCodec = new GatewayRuleJsonCodec();
        var engineSnapshot = engineCodec.readSnapshot(
                release.snapshotJson().getBytes(StandardCharsets.UTF_8)
        );

        assertEquals(
                transport,
                engineSnapshot.content().routes().getFirst()
                        .transportPolicy()
        );
        engineCodec.verify(engineSnapshot);
    }
}
