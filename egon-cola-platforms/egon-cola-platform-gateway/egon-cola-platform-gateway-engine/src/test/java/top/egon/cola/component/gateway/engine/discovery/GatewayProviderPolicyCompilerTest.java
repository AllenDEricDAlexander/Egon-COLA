package top.egon.cola.component.gateway.engine.discovery;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.engine.balance.LoadBalancerType;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayProviderPolicyCompilerTest {

    @Test
    void compilesLoadBalanceAndProviderOverridePolicies() {
        Map<String, RuntimeProviderPolicy> policies =
                new GatewayProviderPolicyCompiler().compile(List.of(
                        new GatewayRuntimePolicy(
                                "weighted",
                                "LOAD_BALANCE",
                                "PROVIDER_SERVICE",
                                Map.of(
                                        "algorithm",
                                        "SMOOTH_WEIGHTED_ROUND_ROBIN"
                                )
                        ),
                        new GatewayRuntimePolicy(
                                "zone-a",
                                "PROVIDER_OVERRIDE",
                                "PROVIDER_SERVICE",
                                Map.of(
                                        "requiredZone",
                                        "zone-a",
                                        "requiredTags",
                                        List.of("stable"),
                                        "serviceOverride",
                                        Map.of("weight", 250)
                                )
                        )
                ));

        assertEquals(
                LoadBalancerType.SMOOTH_WEIGHTED_ROUND_ROBIN,
                policies.get("weighted").loadBalancer()
        );
        ProviderSelectionPolicy override =
                policies.get("zone-a").selectionPolicy();
        assertEquals("zone-a", override.requiredZone());
        assertTrue(override.requiredTags().contains("stable"));
        assertEquals(250, override.serviceOverride().weight());
    }
}
