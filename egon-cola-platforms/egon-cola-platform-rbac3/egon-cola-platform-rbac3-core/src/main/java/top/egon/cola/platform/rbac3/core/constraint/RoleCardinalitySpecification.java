package top.egon.cola.platform.rbac3.core.constraint;

import top.egon.cola.platform.rbac3.core.rule.RuleResult;

import java.util.List;

public final class RoleCardinalitySpecification {

    public RuleResult evaluate(long activeAssignments, long maximumActive) {
        if (activeAssignments < 0 || maximumActive < 1) {
            throw new IllegalArgumentException("valid cardinality counts are required");
        }
        return activeAssignments < maximumActive
                ? RuleResult.allow("ROLE_CARDINALITY_AVAILABLE")
                : RuleResult.deny("ROLE_CARDINALITY_EXCEEDED", List.of());
    }
}
