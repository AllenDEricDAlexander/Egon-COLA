package top.egon.cola.component.gateway.engine.security;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.core.security.AuthenticationMode;
import top.egon.cola.component.gateway.core.security.AuthorizationDecisionMode;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.core.security.SecurityFailureMode;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GatewaySecurityPolicyCompiler {

    private final GatewaySecurityCapabilityRegistry capabilities;

    public GatewaySecurityPolicyCompiler(
            GatewaySecurityCapabilityRegistry capabilities) {
        this.capabilities = capabilities;
    }

    public Map<String, GatewaySecurityPolicy> compile(
            List<GatewayRuntimePolicy> policies,
            Map<String, Set<GatewayProtocol>> policyProtocols) {
        Map<String, GatewaySecurityPolicy> result = new LinkedHashMap<>();
        policies.stream()
                .filter(policy -> "SECURITY".equals(policy.type()))
                .forEach(source -> {
                    GatewaySecurityPolicy policy = policy(source);
                    Set<GatewayProtocol> protocols = policyProtocols.getOrDefault(
                            policy.policyId(),
                            Set.of()
                    );
                    capabilities.validate(policy, protocols);
                    if (result.putIfAbsent(
                            policy.policyId(),
                            policy
                    ) != null) {
                        throw new IllegalArgumentException(
                                "duplicate security policy "
                                        + policy.policyId()
                        );
                    }
                });
        return Map.copyOf(result);
    }

    private GatewaySecurityPolicy policy(GatewayRuntimePolicy source) {
        Map<String, Object> config = source.configuration();
        return new GatewaySecurityPolicy(
                source.policyId(),
                AuthenticationMode.valueOf(string(
                        config,
                        "authenticationMode",
                        "NONE"
                )),
                strings(config.get("credentialExtractorIds")),
                strings(config.get("authenticationProviderIds")),
                strings(config.get("authorizationProviderIds")),
                AuthorizationDecisionMode.valueOf(string(
                        config,
                        "decisionMode",
                        "ALL_ALLOW"
                )),
                string(config, "identityMapperId", null),
                Duration.ofMillis(number(
                        config,
                        "providerTimeoutMs",
                        1000
                )),
                SecurityFailureMode.valueOf(string(
                        config,
                        "failureMode",
                        "FAIL_CLOSED"
                ))
        );
    }

    private List<String> strings(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(Object::toString).toList();
        }
        String text = value.toString();
        return text.isBlank()
                ? List.of()
                : java.util.Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private long number(
            Map<String, Object> source,
            String field,
            long defaultValue) {
        Object value = source.get(field);
        if (value == null) {
            return defaultValue;
        }
        return value instanceof Number number
                ? number.longValue()
                : Long.parseLong(value.toString());
    }

    private String string(
            Map<String, Object> source,
            String field,
            String defaultValue) {
        Object value = source.get(field);
        return value == null ? defaultValue : value.toString();
    }
}
