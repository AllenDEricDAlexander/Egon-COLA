package top.egon.cola.component.accessguard.observability;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.core.plan.GuardPlan;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.core.plan.GuardPlanResolver;
import top.egon.cola.component.accessguard.core.plan.GuardPlanSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntSupplier;

@Endpoint(id = "accessguard")
public final class AccessGuardEndpoint {

    private final AccessGuardProperties properties;
    private final GuardPlanResolver resolver;
    private final IntSupplier penaltyEntries;
    private final IntSupplier rateLimitEntries;

    public AccessGuardEndpoint(
            AccessGuardProperties properties,
            GuardPlanResolver resolver,
            IntSupplier penaltyEntries,
            IntSupplier rateLimitEntries
    ) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.penaltyEntries = Objects.requireNonNull(penaltyEntries, "penaltyEntries");
        this.rateLimitEntries = Objects.requireNonNull(rateLimitEntries, "rateLimitEntries");
    }

    @ReadOperation
    public Map<String, Object> accessguard() {
        List<Map<String, Object>> rules = new ArrayList<>();
        int failures = 0;
        for (Map.Entry<String, AccessGuardProperties.Rule> configured : properties.getRules().entrySet()) {
            if (!configured.getValue().getObservability().isEndpoint()) {
                continue;
            }
            boolean failed = false;
            try {
                GuardPlanSnapshot snapshot = resolver.resolve(configured.getKey());
                rules.add(ruleSummary(snapshot));
            } catch (RuntimeException exception) {
                failed = true;
                rules.add(Map.of("ruleId", configured.getKey(), "status", "UNAVAILABLE"));
            }
            if (resolver.lastFailure(configured.getKey()).isPresent()) {
                failed = true;
            }
            if (failed) {
                failures++;
            }
        }
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", failures == 0 ? "UP" : "DEGRADED");
        health.put("planFailures", failures);
        health.put("localEntries", Map.of(
                "penalty-box", Math.max(0, penaltyEntries.getAsInt()),
                "rate-limit", Math.max(0, rateLimitEntries.getAsInt())));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("storage", properties.getStorage().name());
        response.put("health", Map.copyOf(health));
        response.put("rules", List.copyOf(rules));
        return Map.copyOf(response);
    }

    private static Map<String, Object> ruleSummary(GuardPlanSnapshot snapshot) {
        GuardPlan plan = snapshot.plan();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("ruleId", snapshot.ruleId());
        summary.put("planVersion", snapshot.version());
        summary.put("source", snapshot.source());
        summary.put("enabled", plan.enabled());
        summary.put("policies", enabledPolicies(plan.admission()));
        return Map.copyOf(summary);
    }

    private static List<String> enabledPolicies(AdmissionConfig admission) {
        List<String> policies = new ArrayList<>();
        if (admission.denyList().enabled()) {
            policies.add("deny-list");
        }
        if (admission.allowList().enabled()) {
            policies.add("allow-list");
        }
        if (admission.penaltyBox().enabled()) {
            policies.add("penalty-box");
        }
        if (admission.rateLimit().enabled()) {
            policies.add("rate-limit");
        }
        return List.copyOf(policies);
    }
}
