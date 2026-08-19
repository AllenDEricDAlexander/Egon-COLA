package top.egon.cola.component.gateway.engine.http.security;

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
    void usesOnlyServerRouteBizAppIdentityForAuthorization() {
        RuntimeHttpRoute route = new RuntimeHttpRoute(
                "route-1", "orders.get", "group-1",
                Set.of(AccessZone.INTERNAL), "api.internal",
                Set.of("GET"), "/orders/{id}", false,
                new ProviderServiceKey(
                        "commerce", "orders", "prod", "default",
                        ProviderProtocolType.HTTP, "orders-api", "default",
                        "v1", "http"),
                Set.of("identity"), 0, GatewayResponseMode.TRANSPARENT,
                Map.of(
                        "applicationCode", "orders-admin",
                        "definitionSetId", "definition-1",
                        "mappingVersion", "5")
        );

        Map<String, String> attributes =
                RuleBackedHttpGatewaySecurityProcessor.securityAttributes(
                        new HttpRouteMatch(route, Map.of("id", "1")));

        assertEquals(Map.of(
                "idp.biz-code", "commerce",
                "idp.app-code", "orders",
                "idp.env", "prod"), attributes);
    }
}
