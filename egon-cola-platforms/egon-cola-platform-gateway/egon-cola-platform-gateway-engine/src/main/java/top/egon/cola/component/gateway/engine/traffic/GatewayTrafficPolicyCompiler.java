package top.egon.cola.component.gateway.engine.traffic;

import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GatewayTrafficPolicyCompiler {

    private final GatewayPolicyKeyCompiler keyCompiler =
            new GatewayPolicyKeyCompiler();

    public Map<String, RuntimeTrafficPolicy> compile(
            List<GatewayRuntimePolicy> policies) {
        Map<String, RuntimeTrafficPolicy> result = new LinkedHashMap<>();
        for (GatewayRuntimePolicy source : policies.stream()
                .filter(this::trafficPolicy)
                .toList()) {
            Map<String, Object> config = source.configuration();
            TrafficPolicyType type = TrafficPolicyType.valueOf(source.type());
            String keyExpression = string(config, "keyExpression", null);
            if (type == TrafficPolicyType.RATE_LIMIT) {
                keyCompiler.compile(keyExpression);
            }
            if ((type == TrafficPolicyType.REQUEST_SIZE
                    || type == TrafficPolicyType.RESPONSE_SIZE)
                    && longValue(config, "maxBytes", 0) <= 0) {
                throw new IllegalArgumentException(
                        type + " maxBytes must be positive"
                );
            }
            RuntimeTrafficPolicy policy = new RuntimeTrafficPolicy(
                    source.policyId(),
                    type,
                    TrafficPolicyScope.valueOf(source.scope()),
                    bool(config, "enabled", true),
                    integer(config, "priority", 0),
                    keyExpression,
                    RateLimitFailureMode.valueOf(string(
                            config,
                            "failureMode",
                            "LOCAL_FALLBACK"
                    )),
                    config,
                    longValue(config, "stateEpoch", 0),
                    longValue(config, "policyVersion", 1)
            );
            if (result.putIfAbsent(policy.policyId(), policy) != null) {
                throw new IllegalArgumentException(
                        "duplicate traffic policy " + policy.policyId()
                );
            }
        }
        return Map.copyOf(result);
    }

    private boolean trafficPolicy(GatewayRuntimePolicy policy) {
        try {
            TrafficPolicyType.valueOf(policy.type());
            return true;
        } catch (IllegalArgumentException unsupported) {
            return false;
        }
    }

    private String string(
            Map<String, Object> source,
            String key,
            String defaultValue) {
        Object value = source.get(key);
        return value == null ? defaultValue : value.toString();
    }

    private int integer(
            Map<String, Object> source,
            String key,
            int defaultValue) {
        return Math.toIntExact(longValue(source, key, defaultValue));
    }

    private long longValue(
            Map<String, Object> source,
            String key,
            long defaultValue) {
        Object value = source.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private boolean bool(
            Map<String, Object> source,
            String key,
            boolean defaultValue) {
        Object value = source.get(key);
        return value == null
                ? defaultValue
                : Boolean.parseBoolean(value.toString());
    }
}
