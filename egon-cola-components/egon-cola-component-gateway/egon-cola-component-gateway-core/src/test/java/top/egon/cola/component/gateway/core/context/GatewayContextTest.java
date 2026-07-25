package top.egon.cola.component.gateway.core.context;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayContextTest {

    @Test
    void contextCarriesTypedRequestRoutingAndExecutionState() {
        Instant startedAt = Instant.parse("2026-07-25T01:00:00Z");
        GatewayPrincipal principal = new GatewayPrincipal(
                "account-1",
                Map.of("tenantId", "tenant-a")
        );
        GatewayProviderSelection provider = new GatewayProviderSelection(
                "order-provider",
                "provider-1",
                "lease-1",
                "10.0.0.10",
                8080,
                Map.of("gateway.zone", "cn-east")
        );
        GatewayContext context = new GatewayContext(
                "request-1",
                "trace-1",
                "00-trace-1-span-1-01",
                null,
                AccessZone.INTERNAL,
                "gateway-group-1",
                "engine-node-1",
                "operation-1",
                "route-1",
                "release-1",
                principal,
                provider,
                startedAt.plusSeconds(2),
                startedAt,
                GatewayStage.PROVIDER_SELECTED,
                List.of(new GatewayGovernanceDecision(
                        "policy-1",
                        "ALLOWED",
                        true
                )),
                List.of(new GatewayDiagnostic(
                        "PROVIDER_SELECTED",
                        GatewayStage.PROVIDER_SELECTED,
                        startedAt.plusMillis(5)
                ))
        );

        assertEquals("request-1", context.requestId());
        assertEquals("trace-1", context.traceId());
        assertEquals(AccessZone.INTERNAL, context.accessZone());
        assertEquals("gateway-group-1", context.gatewayGroupId());
        assertEquals("engine-node-1", context.engineNodeId());
        assertEquals(
                "operation-1",
                context.operationId().orElseThrow()
        );
        assertEquals("route-1", context.routeId().orElseThrow());
        assertEquals("release-1", context.releaseId().orElseThrow());
        assertEquals(principal, context.principal().orElseThrow());
        assertEquals(provider, context.providerSelection().orElseThrow());
        assertEquals(
                GatewayStage.PROVIDER_SELECTED,
                context.stage()
        );
        assertTrue(context.tracestate().isEmpty());
    }

    @Test
    void nestedAttributesAndContextCollectionsAreDefensivelyCopied() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("tenantId", "tenant-a");
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("gateway.weight", "100");
        List<GatewayGovernanceDecision> decisions = new ArrayList<>();
        decisions.add(new GatewayGovernanceDecision(
                "policy-1",
                "ALLOWED",
                true
        ));
        List<GatewayDiagnostic> diagnostics = new ArrayList<>();
        diagnostics.add(new GatewayDiagnostic(
                "ROUTE_MATCHED",
                GatewayStage.ROUTE_MATCHED,
                Instant.parse("2026-07-25T01:00:00.001Z")
        ));
        GatewayPrincipal principal = new GatewayPrincipal(
                "account-1",
                attributes
        );
        GatewayProviderSelection provider = new GatewayProviderSelection(
                "order-provider",
                "provider-1",
                "lease-1",
                "10.0.0.10",
                8080,
                metadata
        );
        GatewayContext context = context(
                principal,
                provider,
                decisions,
                diagnostics
        );

        attributes.put("role", "admin");
        metadata.put("gateway.zone", "cn-east");
        decisions.clear();
        diagnostics.clear();

        assertEquals(
                Map.of("tenantId", "tenant-a"),
                context.principal().orElseThrow().attributes()
        );
        assertEquals(
                Map.of("gateway.weight", "100"),
                context.providerSelection().orElseThrow().metadata()
        );
        assertEquals(1, context.governanceDecisions().size());
        assertEquals(1, context.diagnostics().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> context.governanceDecisions().clear()
        );
    }

    @Test
    void deadlineBeforeRequestStartIsRejected() {
        Instant startedAt = Instant.parse("2026-07-25T01:00:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayContext(
                        "request-1",
                        "trace-1",
                        null,
                        null,
                        AccessZone.PUBLIC,
                        "gateway-group-1",
                        "engine-node-1",
                        null,
                        null,
                        null,
                        null,
                        null,
                        startedAt.minusMillis(1),
                        startedAt,
                        GatewayStage.RECEIVED,
                        List.of(),
                        List.of()
                )
        );
    }

    private GatewayContext context(
            GatewayPrincipal principal,
            GatewayProviderSelection provider,
            List<GatewayGovernanceDecision> decisions,
            List<GatewayDiagnostic> diagnostics) {
        Instant startedAt = Instant.parse("2026-07-25T01:00:00Z");
        return new GatewayContext(
                "request-1",
                "trace-1",
                null,
                null,
                AccessZone.INTERNAL,
                "gateway-group-1",
                "engine-node-1",
                null,
                null,
                null,
                principal,
                provider,
                startedAt.plusSeconds(2),
                startedAt,
                GatewayStage.PROVIDER_SELECTED,
                decisions,
                diagnostics
        );
    }
}
