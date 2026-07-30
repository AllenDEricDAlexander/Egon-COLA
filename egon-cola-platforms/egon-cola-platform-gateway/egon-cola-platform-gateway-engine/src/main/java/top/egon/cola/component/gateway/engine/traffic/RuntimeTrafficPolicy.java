package top.egon.cola.component.gateway.engine.traffic;

import java.util.Map;
import java.util.Objects;

public record RuntimeTrafficPolicy(
        String policyId,
        TrafficPolicyType type,
        TrafficPolicyScope scope,
        boolean enabled,
        int priority,
        String keyExpression,
        RateLimitFailureMode failureMode,
        Map<String, Object> parameters,
        long stateEpoch,
        long policyVersion
) {

    public RuntimeTrafficPolicy {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        type = Objects.requireNonNull(type, "type");
        scope = Objects.requireNonNull(scope, "scope");
        failureMode = Objects.requireNonNull(failureMode, "failureMode");
        parameters = Map.copyOf(Objects.requireNonNull(
                parameters,
                "parameters"
        ));
        if (stateEpoch < 0 || policyVersion < 1) {
            throw new IllegalArgumentException(
                    "stateEpoch and policyVersion are invalid"
            );
        }
    }
}
