package top.egon.cola.platform.rbac3.core.impact;

import top.egon.cola.platform.rbac3.core.rule.RuleResult;

import java.util.Comparator;
import java.util.List;

public final class RoleChangeImpactAnalyzer {

    private static final List<String> REASON_PRIORITY = List.of(
            "SELF_ASSIGNMENT_FORBIDDEN",
            "MANAGEMENT_POLICY_NOT_FOUND",
            "SSD_CONSTRAINT_VIOLATION",
            "PREREQUISITE_ROLE_MISSING",
            "ROLE_CARDINALITY_EXCEEDED"
    );

    public Analysis analyze(List<RuleResult> results) {
        List<RuleResult> failures = results.stream()
                .filter(result -> !result.allowed())
                .sorted(Comparator.<RuleResult>comparingInt(
                                result -> priority(result.reasonCode()))
                        .thenComparing(RuleResult::reasonCode))
                .toList();
        return new Analysis(failures.isEmpty(),
                failures.isEmpty() ? null : failures.getFirst().reasonCode(), failures);
    }

    private int priority(String reasonCode) {
        int index = REASON_PRIORITY.indexOf(reasonCode);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    public record Analysis(
            boolean allowed,
            String primaryReasonCode,
            List<RuleResult> failures
    ) {
        public Analysis {
            failures = List.copyOf(failures);
        }
    }
}
