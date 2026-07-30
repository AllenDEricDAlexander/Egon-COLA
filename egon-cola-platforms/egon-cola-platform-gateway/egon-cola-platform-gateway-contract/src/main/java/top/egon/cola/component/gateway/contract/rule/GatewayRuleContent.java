package top.egon.cola.component.gateway.contract.rule;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record GatewayRuleContent(
        String gatewayGroupId,
        String gatewayGroupCode,
        String env,
        String namespace,
        List<GatewayRuntimeOperation> operations,
        List<GatewayRuntimeRoute> routes,
        List<GatewayRuntimePolicy> providerPolicies,
        List<GatewayRuntimePolicy> trafficPolicies,
        List<GatewayRuntimePolicy> securityPolicies,
        List<GatewayRuntimePolicy> corsPolicies,
        List<GatewayRpcDescriptor> rpcDescriptors
) {

    public GatewayRuleContent {
        gatewayGroupId = required(gatewayGroupId, "gatewayGroupId");
        gatewayGroupCode = required(gatewayGroupCode, "gatewayGroupCode");
        env = required(env, "env");
        namespace = required(namespace, "namespace");
        operations = sorted(
                operations,
                Comparator.comparing(GatewayRuntimeOperation::operationId)
        );
        routes = sorted(
                routes,
                Comparator.comparing(GatewayRuntimeRoute::routeId)
        );
        providerPolicies = sorted(
                providerPolicies,
                Comparator.comparing(GatewayRuntimePolicy::policyId)
        );
        trafficPolicies = sorted(
                trafficPolicies,
                Comparator.comparing(GatewayRuntimePolicy::policyId)
        );
        securityPolicies = sorted(
                securityPolicies,
                Comparator.comparing(GatewayRuntimePolicy::policyId)
        );
        corsPolicies = sorted(
                corsPolicies,
                Comparator.comparing(GatewayRuntimePolicy::policyId)
        );
        rpcDescriptors = sorted(
                rpcDescriptors,
                Comparator.comparing(GatewayRpcDescriptor::descriptorId)
        );
    }

    private static <T> List<T> sorted(
            List<T> source,
            Comparator<T> comparator) {
        return Objects.requireNonNull(source, "rule list")
                .stream()
                .sorted(comparator)
                .toList();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
