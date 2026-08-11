package top.egon.cola.component.gateway.engine.http;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;
import top.egon.cola.component.gateway.core.route.HttpRouteMatch;
import top.egon.cola.component.gateway.core.route.RuntimeHttpRoute;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleBackedHttpGatewaySecurityProcessorTest {

    @Test
    void bindsIdpResourceScopeToMatchedUpstream() {
        RuntimeHttpRoute route = new RuntimeHttpRoute(
                "route-1", "orders.get", "group-1",
                Set.of(AccessZone.INTERNAL), "api.internal",
                Set.of("GET"), "/orders/{id}", false,
                new ProviderServiceKey(
                        "commerce", "orders", "prod", "default",
                        ProviderProtocolType.HTTP, "orders-api", "default",
                        "v1", "http"),
                Set.of("identity"), 0, GatewayResponseMode.TRANSPARENT,
                Map.of("applicationCode", "orders-admin")
        );

        Map<String, String> attributes =
                RuleBackedHttpGatewaySecurityProcessor.securityAttributes(
                        new HttpRouteMatch(route, Map.of("id", "1")));

        assertEquals("commerce", attributes.get("idp.biz-code"));
        assertEquals("orders", attributes.get("idp.app-code"));
        assertEquals("prod", attributes.get("idp.env"));
        assertEquals("orders-admin", attributes.get("rbac3.application-code"));
    }
}
