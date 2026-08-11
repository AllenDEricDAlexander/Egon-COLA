package top.egon.cola.component.gateway.engine.rpc;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleBackedRpcGatewaySecurityProcessorTest {

    @Test
    void bindsIdpResourceScopeToResolvedRpcTarget() {
        RuntimeRpcRoute route = new RuntimeRpcRoute(
                "route-1", "invoice.query", "billing.Invoice/Query",
                new ProviderServiceKey(
                        "finance", "billing", "prod", "default",
                        ProviderProtocolType.RPC, "billing.Invoice", "default",
                        "v1", "grpc"),
                "QueryRequest", "QueryResponse", "sha256",
                Set.of("identity"), GatewayResponseMode.TRANSPARENT,
                true, Duration.ofSeconds(3)
        );

        Map<String, String> attributes =
                RuleBackedRpcGatewaySecurityProcessor.securityAttributes(route);

        assertEquals(Map.of(
                "idp.biz-code", "finance",
                "idp.app-code", "billing",
                "idp.env", "prod"
        ), attributes);
    }
}
