package top.egon.cola.component.gateway.engine.rule;

import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.CompiledHttpRouteIndex;
import top.egon.cola.component.gateway.engine.rpc.RpcMethodIndex;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record CompiledGatewayRules(
        GatewayRuleSnapshot snapshot,
        CompiledHttpRouteIndex httpRoutes,
        RpcMethodIndex rpcMethods,
        Set<ProviderServiceKey> providerServices,
        Map<String, Map<String, Object>> policies
) {

    public CompiledGatewayRules {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        httpRoutes = Objects.requireNonNull(httpRoutes, "httpRoutes");
        rpcMethods = Objects.requireNonNull(rpcMethods, "rpcMethods");
        providerServices = Set.copyOf(Objects.requireNonNull(
                providerServices,
                "providerServices"
        ));
        policies = Map.copyOf(Objects.requireNonNull(policies, "policies"));
    }
}
