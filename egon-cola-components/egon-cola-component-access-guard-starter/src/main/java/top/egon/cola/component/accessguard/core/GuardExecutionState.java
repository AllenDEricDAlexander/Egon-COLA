package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.accessguard.core.plan.GuardPlanSnapshot;
import top.egon.cola.component.accessguard.policy.GuardContext;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record GuardExecutionState(
        GuardPlanSnapshot snapshot,
        GuardContext context,
        Set<String> bypassedPolicies,
        GuardDecision degradedDecision,
        GuardResolution degradedResolution,
        String degradedPolicy,
        GuardFailure failure
) {

    public GuardExecutionState {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        context = Objects.requireNonNull(context, "context");
        bypassedPolicies = bypassedPolicies == null ? Set.of() : Set.copyOf(bypassedPolicies);
        degradedPolicy = degradedPolicy == null ? "" : degradedPolicy;
        if ((degradedDecision == null) != (degradedResolution == null)) {
            throw new IllegalArgumentException("degraded decision and resolution must be set together");
        }
    }

    public static GuardExecutionState initial(GuardPlanSnapshot snapshot, GuardContext context) {
        return new GuardExecutionState(snapshot, context, Set.of(), null, null, "", null);
    }

    public GuardExecutionState withBypassedPolicies(Set<String> policies) {
        if (policies == null || policies.isEmpty()) {
            return this;
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>(bypassedPolicies);
        merged.addAll(policies);
        Set<String> bypasses = Set.copyOf(merged);
        return new GuardExecutionState(
                snapshot,
                context.withBypassedPolicies(bypasses),
                bypasses,
                degradedDecision,
                degradedResolution,
                degradedPolicy,
                failure);
    }

    public GuardExecutionState degraded(
            GuardDecision decision,
            GuardResolution resolution,
            String policy,
            GuardFailure guardFailure
    ) {
        if (degradedDecision != null) {
            return this;
        }
        return new GuardExecutionState(
                snapshot,
                context,
                bypassedPolicies,
                Objects.requireNonNull(decision, "decision"),
                Objects.requireNonNull(resolution, "resolution"),
                policy,
                Objects.requireNonNull(guardFailure, "guardFailure"));
    }

    public boolean isDegraded() {
        return degradedDecision != null;
    }
}
