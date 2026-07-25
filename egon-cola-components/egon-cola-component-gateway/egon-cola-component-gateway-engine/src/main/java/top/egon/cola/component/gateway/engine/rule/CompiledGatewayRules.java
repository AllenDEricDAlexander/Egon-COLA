package top.egon.cola.component.gateway.engine.rule;

import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.CompiledHttpRouteIndex;
import top.egon.cola.component.gateway.engine.rpc.RpcMethodIndex;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.engine.traffic.RuntimeTrafficPolicy;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record CompiledGatewayRules(
        GatewayRuleSnapshot snapshot,
        CompiledHttpRouteIndex httpRoutes,
        RpcMethodIndex rpcMethods,
        Set<ProviderServiceKey> providerServices,
        Map<String, RuntimeTrafficPolicy> trafficPolicies,
        Map<String, GatewaySecurityPolicy> securityPolicies
) {

    public CompiledGatewayRules {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        httpRoutes = Objects.requireNonNull(httpRoutes, "httpRoutes");
        rpcMethods = Objects.requireNonNull(rpcMethods, "rpcMethods");
        providerServices = Set.copyOf(Objects.requireNonNull(
                providerServices,
                "providerServices"
        ));
        trafficPolicies = Map.copyOf(Objects.requireNonNull(
                trafficPolicies,
                "trafficPolicies"
        ));
        securityPolicies = Map.copyOf(Objects.requireNonNull(
                securityPolicies,
                "securityPolicies"
        ));
    }
}
