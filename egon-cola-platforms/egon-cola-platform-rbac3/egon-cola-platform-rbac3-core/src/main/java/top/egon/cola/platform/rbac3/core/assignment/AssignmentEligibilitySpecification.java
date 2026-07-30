package top.egon.cola.platform.rbac3.core.assignment;

import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.rule.RuleResult;

import java.time.Instant;
import java.util.List;

public final class AssignmentEligibilitySpecification {

    public RuleResult evaluate(EligibleAssignmentFact assignment, Instant databaseNow) {
        if (assignment.eligibleAt(databaseNow)) {
            return RuleResult.allow("ASSIGNMENT_ELIGIBLE");
        }
        return RuleResult.deny("ASSIGNMENT_NOT_ELIGIBLE", List.of(assignment.id()));
    }
}
