package top.egon.cola.component.gateway.core.route;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRouteCompilerTest {

    @Test
    void selectsExactThenVariableThenCatchAllWithoutScanningAllRoutes() {
        CompiledHttpRouteIndex index = new HttpRouteCompiler().compile(List.of(
                route("exact", "api.example.com", "/orders/current", 0),
                route("variable", "api.example.com", "/orders/{id}", 0),
                route("assets", "*.example.com", "/assets/**", 0)
        ));

        assertEquals("exact", index.match(
                "api.example.com",
                "GET",
                "/orders/current",
                AccessZone.PUBLIC
        ).orElseThrow().route().routeId());
        HttpRouteMatch variable = index.match(
                "api.example.com",
                "GET",
                "/orders/42",
                AccessZone.PUBLIC
        ).orElseThrow();
        assertEquals("variable", variable.route().routeId());
        assertEquals("42", variable.pathVariables().get("id"));
        assertEquals("assets", index.match(
                "cdn.example.com",
                "GET",
                "/assets/a/b",
                AccessZone.PUBLIC
        ).orElseThrow().route().routeId());
    }

    @Test
    void rejectsSameStructuralPatternWithSamePriority() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpRouteCompiler().compile(List.of(
                        route("one", "api.example.com", "/orders/{id}", 1),
                        route("two", "api.example.com", "/orders/{key}", 1)
                ))
        );
    }

    @Test
    void preservesAccessZoneAsTrustedRouteDimension() {
        RuntimeHttpRoute internal = new RuntimeHttpRoute(
                "internal",
                "operation",
                "group",
                Set.of(AccessZone.INTERNAL),
                "api.example.com",
                Set.of("GET"),
                "/internal",
                false,
                service(),
                Set.of(),
                0,
                GatewayResponseMode.TRANSPARENT,
                Map.of()
        );
        CompiledHttpRouteIndex index = new HttpRouteCompiler()
                .compile(List.of(internal));

        assertTrue(index.match(
                "api.example.com",
                "GET",
                "/internal",
                AccessZone.PUBLIC
        ).isEmpty());
        assertTrue(index.match(
                "api.example.com",
                "GET",
                "/internal",
                AccessZone.INTERNAL
        ).isPresent());
    }

    @Test
    void preservesEffectiveTransportPolicyOnCompiledRoute() {
        EffectiveGatewayTransportPolicy policy =
                new EffectiveGatewayTransportPolicy(
                        GatewayRouteProfile.OPENAI_HTTP,
                        GatewayTransportProtocol.HTTP,
                        GatewayRequestBodyMode.STREAMING,
                        GatewayTransportResponseMode.AUTO_STREAM,
                        512L * 1024L * 1024L,
                        OptionalLong.empty(),
                        Duration.ofSeconds(10),
                        Duration.ofSeconds(120),
                        Duration.ofSeconds(90),
                        Optional.of(Duration.ofMinutes(30)),
                        Optional.empty(),
                        OptionalLong.empty(),
                        false,
                        false,
                        true
                );
        RuntimeHttpRoute route = new RuntimeHttpRoute(
                "openai",
                "responses",
                "group",
                Set.of(AccessZone.PUBLIC),
                "ai.example.com",
                Set.of("POST"),
                "/v1/responses",
                true,
                service(),
                Set.of(),
                0,
                GatewayResponseMode.TRANSPARENT,
                Map.of(),
                policy
        );

        RuntimeHttpRoute matched = new HttpRouteCompiler()
                .compile(List.of(route))
                .match(
                        "ai.example.com",
                        "POST",
                        "/v1/responses",
                        AccessZone.PUBLIC
                )
                .orElseThrow()
                .route();

        assertSame(policy, matched.transportPolicy());
    }

    @Test
    void oldRuntimeRouteConstructorUsesLegacyTransportDefaults() {
        RuntimeHttpRoute route = route(
                "legacy",
                "api.example.com",
                "/orders",
                0
        );

        assertEquals(GatewayRouteProfile.DEFAULT,
                route.transportPolicy().profile());
        assertEquals(GatewayRequestBodyMode.AGGREGATED,
                route.transportPolicy().requestBodyMode());
        assertEquals(GatewayTransportResponseMode.STANDARD,
                route.transportPolicy().responseMode());
        assertEquals(2L * 1024L * 1024L,
                route.transportPolicy().maxRequestBodyBytes());
        assertEquals(OptionalLong.of(4L * 1024L * 1024L),
                route.transportPolicy().maxResponseBodyBytes());
        assertEquals(Duration.ofSeconds(5),
                route.transportPolicy().responseHeaderTimeout());
        assertTrue(route.transportPolicy().totalTimeout().isEmpty());
    }

    private RuntimeHttpRoute route(
            String id,
            String host,
            String path,
            int priority) {
        return new RuntimeHttpRoute(
                id,
                id + "-operation",
                "group",
                Set.of(AccessZone.PUBLIC, AccessZone.INTERNAL),
                host,
                Set.of("GET"),
                path,
                true,
                service(),
                Set.of(),
                priority,
                GatewayResponseMode.TRANSPARENT,
                Map.of()
        );
    }

    private ProviderServiceKey service() {
        return new ProviderServiceKey(
                "local",
                "default",
                ProviderProtocolType.HTTP,
                "orders",
                "default",
                "v1",
                "http"
        );
    }
}
