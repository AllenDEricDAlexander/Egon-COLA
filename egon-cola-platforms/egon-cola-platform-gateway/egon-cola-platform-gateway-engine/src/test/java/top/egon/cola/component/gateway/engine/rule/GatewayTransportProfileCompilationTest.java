package top.egon.cola.component.gateway.engine.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteTransportPolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.core.route.RuntimeHttpRoute;
import top.egon.cola.component.gateway.core.transport.GatewayTransportDefaults;
import top.egon.cola.component.gateway.core.transport.GatewayTransportSafetyLimits;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityCapabilityRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayTransportProfileCompilationTest {

    private static final long MIB = 1024L * 1024L;

    @Test
    void legacyRouteKeepsAggregatedStandardTransportDefaults() {
        RuntimeHttpRoute route = compile(
                null,
                Set.of(),
                List.of(),
                legacyCompiler()
        );

        assertEquals(GatewayRouteProfile.DEFAULT,
                route.transportPolicy().profile());
        assertEquals(GatewayRequestBodyMode.AGGREGATED,
                route.transportPolicy().requestBodyMode());
        assertEquals(GatewayTransportResponseMode.STANDARD,
                route.transportPolicy().responseMode());
        assertEquals(2L * MIB,
                route.transportPolicy().maxRequestBodyBytes());
        assertEquals(OptionalLong.of(4L * MIB),
                route.transportPolicy().maxResponseBodyBytes());
        assertEquals(Duration.ofSeconds(30),
                route.transportPolicy().connectTimeout());
        assertEquals(Duration.ofSeconds(5),
                route.transportPolicy().responseHeaderTimeout());
        assertEquals(Duration.ofSeconds(5),
                route.transportPolicy().streamIdleTimeout());
        assertTrue(route.transportPolicy().totalTimeout().isEmpty());
    }

    @Test
    void openAiProfileCompilesStreamingLongConnectionDefaults() {
        RuntimeHttpRoute route = compile(
                profile(GatewayRouteProfile.OPENAI_HTTP),
                Set.of(),
                List.of(),
                legacyCompiler()
        );

        assertEquals(GatewayRequestBodyMode.STREAMING,
                route.transportPolicy().requestBodyMode());
        assertEquals(GatewayTransportResponseMode.AUTO_STREAM,
                route.transportPolicy().responseMode());
        assertEquals(512L * MIB,
                route.transportPolicy().maxRequestBodyBytes());
        assertTrue(route.transportPolicy().maxResponseBodyBytes().isEmpty());
        assertEquals(Duration.ofSeconds(10),
                route.transportPolicy().connectTimeout());
        assertEquals(Duration.ofSeconds(120),
                route.transportPolicy().responseHeaderTimeout());
        assertEquals(Duration.ofSeconds(90),
                route.transportPolicy().streamIdleTimeout());
        assertEquals(Duration.ofMinutes(30),
                route.transportPolicy().totalTimeout().orElseThrow());
        assertFalse(route.transportPolicy().bodyLogEnabled());
        assertFalse(route.transportPolicy().retryAllowed());
    }

    @Test
    void routeOverridesWinWhileSizePoliciesRemainRestrictive() {
        GatewayRouteTransportPolicy routePolicy = new GatewayRouteTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.WEBSOCKET,
                GatewayRequestBodyMode.AGGREGATED,
                GatewayTransportResponseMode.STANDARD,
                64L * MIB,
                20_000L,
                180_000L,
                240_000L,
                2_700_000L,
                1_800_000L,
                32L * MIB,
                true,
                true
        );
        List<GatewayRuntimePolicy> traffic = List.of(
                policy("request-size", "REQUEST_SIZE",
                        Map.of("maxBytes", 32L * MIB)),
                policy("response-size", "RESPONSE_SIZE",
                        Map.of("maxBytes", 8L * MIB)),
                policy("timeout", "TIMEOUT",
                        Map.of("timeout", "PT10M"))
        );

        RuntimeHttpRoute route = compile(
                routePolicy,
                Set.of("request-size", "response-size", "timeout"),
                traffic,
                legacyCompiler()
        );

        assertEquals(32L * MIB,
                route.transportPolicy().maxRequestBodyBytes());
        assertEquals(OptionalLong.of(8L * MIB),
                route.transportPolicy().maxResponseBodyBytes());
        assertEquals(Duration.ofMinutes(45),
                route.transportPolicy().totalTimeout().orElseThrow());
        assertEquals(Duration.ofMinutes(30),
                route.transportPolicy().websocketIdleTimeout().orElseThrow());
        assertEquals(OptionalLong.of(32L * MIB),
                route.transportPolicy().websocketMaxFrameBytes());
        assertTrue(route.transportPolicy().bodyLogEnabled());
        assertTrue(route.transportPolicy().retryAllowed());
    }

    @Test
    void trafficTimeoutAndResponseLimitApplyWhenRouteDoesNotOverrideThem() {
        List<GatewayRuntimePolicy> traffic = List.of(
                policy("response-size", "RESPONSE_SIZE",
                        Map.of("maxBytes", 12L * MIB)),
                policy("timeout", "TIMEOUT",
                        Map.of("timeoutMillis", 360_000L))
        );

        RuntimeHttpRoute route = compile(
                profile(GatewayRouteProfile.OPENAI_HTTP),
                Set.of("response-size", "timeout"),
                traffic,
                legacyCompiler()
        );

        assertEquals(OptionalLong.of(12L * MIB),
                route.transportPolicy().maxResponseBodyBytes());
        assertEquals(Duration.ofMinutes(6),
                route.transportPolicy().totalTimeout().orElseThrow());
    }

    @Test
    void compileRejectsRouteOutsideNodeSafetyLimits() {
        GatewayTransportSafetyLimits localSafety =
                new GatewayTransportSafetyLimits(
                        256L * MIB,
                        Duration.ofSeconds(9),
                        Duration.ofMinutes(10),
                        Duration.ofMinutes(30),
                        Duration.ofHours(2),
                        Duration.ofHours(2),
                        64L * MIB
                );
        EngineGatewayRuleCompiler compiler = new EngineGatewayRuleCompiler(
                GatewaySecurityCapabilityRegistry.empty(),
                GatewayTransportDefaults.legacy(),
                localSafety
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> compile(
                        profile(GatewayRouteProfile.OPENAI_HTTP),
                        Set.of(),
                        List.of(),
                        compiler
                )
        );
    }

    private RuntimeHttpRoute compile(
            GatewayRouteTransportPolicy routePolicy,
            Set<String> policyRefs,
            List<GatewayRuntimePolicy> trafficPolicies,
            EngineGatewayRuleCompiler compiler) {
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "operation-1",
                "openai:http:POST:/v1/responses",
                GatewayProtocol.HTTP,
                "POST /v1/responses",
                "{}",
                "{}",
                true,
                new GatewayProviderServiceRef(
                        "test",
                        "default",
                        GatewayProtocol.HTTP,
                        "openai",
                        "default",
                        "1.0.0",
                        "HTTP"
                ),
                "TRANSPARENT",
                policyRefs,
                Map.of(),
                false
        );
        GatewayRuntimeRoute route = new GatewayRuntimeRoute(
                "route-1",
                "operation-1",
                "api.example.com",
                "POST",
                "/v1/responses",
                Set.of(AccessZone.PUBLIC),
                0,
                true,
                routePolicy
        );
        GatewayRuleContent content = new GatewayRuleContent(
                "group-1",
                "openai",
                "test",
                "default",
                List.of(operation),
                List.of(route),
                List.of(),
                trafficPolicies,
                List.of(),
                List.of(),
                List.of()
        );
        GatewayRuleSnapshot snapshot = new GatewayRuleSnapshot(
                "v1",
                "release-1",
                Instant.parse("2026-07-30T00:00:00Z"),
                "content-sha",
                "artifact-sha",
                content
        );

        return compiler.compile(snapshot).httpRoutes().match(
                "api.example.com",
                "POST",
                "/v1/responses",
                AccessZone.PUBLIC
        ).orElseThrow().route();
    }

    private EngineGatewayRuleCompiler legacyCompiler() {
        return new EngineGatewayRuleCompiler();
    }

    private GatewayRouteTransportPolicy profile(GatewayRouteProfile profile) {
        return new GatewayRouteTransportPolicy(
                profile,
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
                null
        );
    }

    private GatewayRuntimePolicy policy(
            String policyId,
            String type,
            Map<String, Object> configuration) {
        return new GatewayRuntimePolicy(
                policyId,
                type,
                "ROUTE",
                configuration
        );
    }
}
