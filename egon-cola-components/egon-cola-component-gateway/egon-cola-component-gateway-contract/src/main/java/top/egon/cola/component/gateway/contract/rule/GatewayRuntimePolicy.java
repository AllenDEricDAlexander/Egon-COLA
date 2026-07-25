package top.egon.cola.component.gateway.contract.rule;

import java.util.Map;
import java.util.Objects;

public record GatewayRuntimePolicy(
        String policyId,
        String type,
        String scope,
        Map<String, Object> configuration
) {

    public GatewayRuntimePolicy {
        policyId = required(policyId, "policyId");
        type = required(type, "type");
        scope = required(scope, "scope");
        configuration = Map.copyOf(Objects.requireNonNull(
                configuration,
                "configuration"
        ));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
