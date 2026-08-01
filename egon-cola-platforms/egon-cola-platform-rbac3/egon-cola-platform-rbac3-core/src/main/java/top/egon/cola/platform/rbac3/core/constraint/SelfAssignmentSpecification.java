package top.egon.cola.platform.rbac3.core.constraint;

import top.egon.cola.platform.rbac3.core.rule.RuleResult;

import java.util.List;
import java.util.Set;

public final class SelfAssignmentSpecification {

    private static final Set<String> MUTATING_OPERATIONS = Set.of(
            "ASSIGN_ROLE", "TEMPORARY_ASSIGN", "RESUME_ROLE"
    );

    public RuleResult evaluate(String operatorUserId, String targetUserId, String operation) {
        if (operatorUserId.equals(targetUserId) && MUTATING_OPERATIONS.contains(operation)) {
            return RuleResult.deny("SELF_ASSIGNMENT_FORBIDDEN", List.of(targetUserId));
        }
        return RuleResult.allow("SELF_ASSIGNMENT_RULE_SATISFIED");
    }
}
