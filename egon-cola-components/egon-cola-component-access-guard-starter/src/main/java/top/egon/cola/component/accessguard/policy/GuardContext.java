package top.egon.cola.component.accessguard.policy;

import java.util.Set;

public record GuardContext(
        String ruleId,
        long planVersion,
        String stateVersion,
        String keyHash,
        Set<String> bypassedPolicies
) {

    public GuardContext {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId must not be blank");
        }
        if (planVersion < 0) {
            throw new IllegalArgumentException("planVersion must not be negative");
        }
        if (stateVersion == null || stateVersion.isBlank()) {
            throw new IllegalArgumentException("stateVersion must not be blank");
        }
        if (keyHash == null || !keyHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("keyHash must be a lowercase SHA-256 value");
        }
        ruleId = ruleId.trim();
        stateVersion = stateVersion.trim();
        bypassedPolicies = bypassedPolicies == null ? Set.of() : Set.copyOf(bypassedPolicies);
    }

    public static GuardContext forPolicy(String ruleId, long planVersion, String stateVersion, String keyHash) {
        return new GuardContext(ruleId, planVersion, stateVersion, keyHash, Set.of());
    }

    public GuardContext withBypassedPolicies(Set<String> policies) {
        return new GuardContext(ruleId, planVersion, stateVersion, keyHash, policies);
    }

    @Override
    public String toString() {
        return "GuardContext[ruleId=" + ruleId
                + ", planVersion=" + planVersion
                + ", stateVersion=" + stateVersion
                + ", keyHash=<redacted>, bypassedPolicies=" + bypassedPolicies + "]";
    }
}
