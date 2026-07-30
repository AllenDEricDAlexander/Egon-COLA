package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.gateway.engine.balance.LoadBalancerType;

import java.util.Objects;

public record RuntimeProviderPolicy(
        String policyId,
        Type type,
        LoadBalancerType loadBalancer,
        ProviderSelectionPolicy selectionPolicy
) {

    public RuntimeProviderPolicy {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        type = Objects.requireNonNull(type, "type");
        if (type == Type.LOAD_BALANCE && loadBalancer == null) {
            throw new IllegalArgumentException(
                    "load balance policy requires an algorithm"
            );
        }
        if (type == Type.PROVIDER_OVERRIDE && selectionPolicy == null) {
            throw new IllegalArgumentException(
                    "provider override requires selection policy"
            );
        }
    }

    public enum Type {
        LOAD_BALANCE,
        PROVIDER_OVERRIDE
    }
}
