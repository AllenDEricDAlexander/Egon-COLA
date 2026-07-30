package top.egon.cola.component.gateway.contract.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GatewayRouteTransportPolicyTest {

    @Test
    void legacyRouteConstructorLeavesTransportPolicyAbsent() {
        GatewayRuntimeRoute route = new GatewayRuntimeRoute(
                "route",
                "operation",
                "api.example.com",
                "post",
                "/v1/responses",
                Set.of(AccessZone.PUBLIC),
                0,
                true
        );

        assertNull(route.transportPolicy());
    }

    @Test
    void routeRetainsNullableWireOverridesWithoutAddingDefaults() {
        GatewayRouteTransportPolicy policy = new GatewayRouteTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
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
                false,
                false
        );

        GatewayRuntimeRoute route = new GatewayRuntimeRoute(
                "route",
                "operation",
                "api.example.com",
                "POST",
                "/v1/responses",
                Set.of(AccessZone.PUBLIC),
                0,
                true,
                policy
        );

        assertSame(policy, route.transportPolicy());
        assertNull(policy.transportProtocol());
        assertNull(policy.requestBodyMode());
        assertNull(policy.responseMode());
        assertNull(policy.maxRequestBodyBytes());
    }

    @Test
    void wirePolicyExposesOnlyFrozenTransportOverrideFields() {
        List<String> components = Arrays.stream(
                        GatewayRouteTransportPolicy.class.getRecordComponents()
                )
                .map(component -> component.getName())
                .toList();

        assertEquals(List.of(
                "profile",
                "transportProtocol",
                "requestBodyMode",
                "responseMode",
                "maxRequestBodyBytes",
                "connectTimeoutMs",
                "responseHeaderTimeoutMs",
                "streamIdleTimeoutMs",
                "totalTimeoutMs",
                "websocketIdleTimeoutMs",
                "websocketMaxFrameBytes",
                "bodyLogEnabled",
                "retryEnabled"
        ), components);
    }
}
