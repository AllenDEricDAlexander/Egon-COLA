package top.egon.cola.component.gateway.engine.cors;

import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class GatewayCorsPolicyCompiler {

    public Map<String, RuntimeCorsPolicy> compile(
            List<GatewayRuntimePolicy> policies) {
        Map<String, RuntimeCorsPolicy> result = new LinkedHashMap<>();
        policies.stream()
                .filter(policy -> "CORS".equals(policy.type()))
                .forEach(source -> {
                    Map<String, Object> config = source.configuration();
                    RuntimeCorsPolicy policy = new RuntimeCorsPolicy(
                            source.policyId(),
                            values(config, "allowedOrigins", false),
                            values(config, "allowedMethods", true),
                            values(config, "allowedHeaders", false),
                            values(config, "exposedHeaders", false),
                            bool(config, "allowCredentials", false),
                            number(config, "maxAgeSeconds", 0),
                            bool(config, "enabled", true)
                    );
                    if (result.putIfAbsent(
                            policy.policyId(),
                            policy
                    ) != null) {
                        throw new IllegalArgumentException(
                                "duplicate CORS policy " + policy.policyId()
                        );
                    }
                });
        return Map.copyOf(result);
    }

    private Set<String> values(
            Map<String, Object> source,
            String key,
            boolean upperCase) {
        Object raw = source.get(key);
        if (raw == null) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (raw instanceof Collection<?> collection) {
            collection.forEach(value -> add(
                    result,
                    value.toString(),
                    upperCase
            ));
        } else {
            for (String value : raw.toString().split(",")) {
                add(result, value, upperCase);
            }
        }
        return Set.copyOf(result);
    }

    private void add(
            Set<String> values,
            String value,
            boolean upperCase) {
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "CORS list must not contain blank values"
            );
        }
        values.add(upperCase
                ? normalized.toUpperCase(Locale.ROOT)
                : normalized);
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

    private long number(
            Map<String, Object> source,
            String key,
            long defaultValue) {
        Object value = source.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value instanceof Number number
                ? number.longValue()
                : Long.parseLong(value.toString());
    }
}
