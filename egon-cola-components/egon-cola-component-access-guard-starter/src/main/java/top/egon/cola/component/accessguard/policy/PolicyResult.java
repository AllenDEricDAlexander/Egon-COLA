package top.egon.cola.component.accessguard.policy;

import top.egon.cola.component.accessguard.core.GuardDecision;

import java.time.Duration;
import java.util.Set;

public record PolicyResult(
        boolean allowed,
        GuardDecision decision,
        Set<String> bypassedPolicies,
        Duration retryAfter,
        long remainingTokens
) {

    public PolicyResult {
        if (decision == null) {
            throw new IllegalArgumentException("decision is required");
        }
        if (allowed != (decision == GuardDecision.PASS)) {
            throw new IllegalArgumentException("PASS must be allowed and rejections must not be allowed");
        }
        bypassedPolicies = bypassedPolicies == null ? Set.of() : Set.copyOf(bypassedPolicies);
        if (bypassedPolicies.contains("deny-list")) {
            throw new IllegalArgumentException("deny-list cannot be bypassed");
        }
        retryAfter = retryAfter == null ? Duration.ZERO : retryAfter;
        if (retryAfter.isNegative()) {
            throw new IllegalArgumentException("retryAfter must not be negative");
        }
    }

    public static PolicyResult pass() {
        return new PolicyResult(true, GuardDecision.PASS, Set.of(), Duration.ZERO, -1L);
    }

    public static PolicyResult passWithBypass(Set<String> bypassedPolicies) {
        return new PolicyResult(true, GuardDecision.PASS, bypassedPolicies, Duration.ZERO, -1L);
    }

    public static PolicyResult reject(GuardDecision decision) {
        return new PolicyResult(false, decision, Set.of(), Duration.ZERO, -1L);
    }

    public static PolicyResult reject(GuardDecision decision, Duration retryAfter, long remainingTokens) {
        return new PolicyResult(false, decision, Set.of(), retryAfter, remainingTokens);
    }
}
