package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.engine.balance.LoadBalancerType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GatewayProviderPolicyCompiler {

    public Map<String, RuntimeProviderPolicy> compile(
            List<GatewayRuntimePolicy> policies) {
        Map<String, RuntimeProviderPolicy> compiled = new LinkedHashMap<>();
        for (GatewayRuntimePolicy policy : policies) {
            RuntimeProviderPolicy runtime = compile(policy);
            if (runtime != null
                    && compiled.putIfAbsent(
                    runtime.policyId(),
                    runtime
            ) != null) {
                throw new IllegalArgumentException(
                        "duplicate provider policy " + runtime.policyId()
                );
            }
        }
        return Map.copyOf(compiled);
    }

    private RuntimeProviderPolicy compile(GatewayRuntimePolicy source) {
        if ("LOAD_BALANCE".equals(source.type())) {
            String algorithm = string(
                    source.configuration(),
                    "algorithm",
                    "ROUND_ROBIN"
            );
            return new RuntimeProviderPolicy(
                    source.policyId(),
                    RuntimeProviderPolicy.Type.LOAD_BALANCE,
                    LoadBalancerType.valueOf(algorithm),
                    null
            );
        }
        if (!"PROVIDER_OVERRIDE".equals(source.type())) {
            return null;
        }
        Map<String, Object> config = source.configuration();
        return new RuntimeProviderPolicy(
                source.policyId(),
                RuntimeProviderPolicy.Type.PROVIDER_OVERRIDE,
                null,
                new ProviderSelectionPolicy(
                        bool(config, "serviceEnabled", true),
                        nullableBoolean(config.get("secureRequired")),
                        string(config, "requiredZone", null),
                        string(config, "requiredRegion", null),
                        strings(config.get("requiredTags")),
                        override(map(config.get("serviceOverride"))),
                        instanceOverrides(map(config.get("instanceOverrides")))
                )
        );
    }

    private Map<String, ProviderPolicyOverride> instanceOverrides(
            Map<String, Object> source) {
        Map<String, ProviderPolicyOverride> result = new LinkedHashMap<>();
        source.forEach((instanceId, value) ->
                result.put(instanceId, override(map(value))));
        return Map.copyOf(result);
    }

    private ProviderPolicyOverride override(Map<String, Object> source) {
        return new ProviderPolicyOverride(
                nullableBoolean(source.get("enabled")),
                nullableInteger(source.get("weight")),
                string(source, "zone", null),
                string(source, "region", null),
                source.containsKey("tags")
                        ? strings(source.get("tags"))
                        : null
        );
    }

    private Map<String, Object> map(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> values)) {
            throw new IllegalArgumentException(
                    "provider override must be an object"
            );
        }
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, item) -> result.put(key.toString(), item));
        return Map.copyOf(result);
    }

    private Set<String> strings(Object value) {
        if (value == null) {
            return Set.of();
        }
        if (!(value instanceof Iterable<?> values)) {
            throw new IllegalArgumentException(
                    "provider tags must be an array"
            );
        }
        Set<String> result = new LinkedHashSet<>();
        values.forEach(item -> result.add(item.toString()));
        return Set.copyOf(result);
    }

    private String string(
            Map<String, Object> source,
            String key,
            String defaultValue) {
        Object value = source.get(key);
        return value == null ? defaultValue : value.toString();
    }

    private boolean bool(
            Map<String, Object> source,
            String key,
            boolean defaultValue) {
        Boolean value = nullableBoolean(source.get(key));
        return value == null ? defaultValue : value;
    }

    private Boolean nullableBoolean(Object value) {
        return value == null
                ? null
                : value instanceof Boolean bool
                ? bool
                : Boolean.valueOf(value.toString());
    }

    private Integer nullableInteger(Object value) {
        return value == null
                ? null
                : value instanceof Number number
                ? number.intValue()
                : Integer.valueOf(value.toString());
    }
}
