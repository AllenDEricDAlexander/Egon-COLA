package top.egon.cola.platform.rbac3.core.delegation;

import top.egon.cola.platform.rbac3.core.rule.RuleResult;

import java.util.List;

public final class ManagementPolicySpecification {

    public RuleResult evaluate(
            ManagementPolicyDecisionService.ManagementDecisionInput input,
            ManagementPolicyDecisionService.ManagementPolicyFact policy
    ) {
        if (!policy.active()
                || input.databaseNow().isBefore(policy.validFrom())
                || !input.databaseNow().isBefore(policy.validTo())) {
            return denied(policy.id(), "MANAGEMENT_POLICY_INACTIVE");
        }
        if (!policy.subjectIds().contains(input.subjectId())) {
            return denied(policy.id(), "MANAGEMENT_SUBJECT_OUT_OF_SCOPE");
        }
        if (!policy.targetUserIds().contains(input.targetUserId())) {
            return denied(policy.id(), "MANAGEMENT_TARGET_OUT_OF_SCOPE");
        }
        if (!policy.activationRootRoleIds().contains(input.activationRootRoleId())) {
            return denied(policy.id(), "MANAGEMENT_ROLE_OUT_OF_SCOPE");
        }
        if (!policy.operations().contains(input.operation())) {
            return denied(policy.id(), "MANAGEMENT_OPERATION_OUT_OF_SCOPE");
        }
        ManagementPolicyDecisionService.Restrictions restrictions = policy.restrictions();
        if (input.assignmentDays() > restrictions.maxAssignmentDays()
                || risk(input.roleRisk()) > risk(restrictions.maxRiskLevel())
                || strength(input.authStrength()) < strength(restrictions.requiredAuthStrength())
                || (restrictions.requireReason() && !input.reasonPresent())
                || (restrictions.requireTicket() && !input.ticketPresent())) {
            return denied(policy.id(), "MANAGEMENT_RESTRICTION_VIOLATION");
        }
        return RuleResult.allow("MANAGEMENT_POLICY_ALLOWED");
    }

    private RuleResult denied(String policyId, String reasonCode) {
        return RuleResult.deny(reasonCode, List.of(policyId));
    }

    private int risk(String value) {
        return switch (value) {
            case "LOW" -> 0;
            case "MEDIUM" -> 1;
            case "HIGH" -> 2;
            case "CRITICAL" -> 3;
            default -> throw new IllegalArgumentException("unknown risk level");
        };
    }

    private int strength(String value) {
        return switch (value) {
            case "PASSWORD" -> 0;
            case "MFA" -> 1;
            case "STRONG" -> 2;
            default -> throw new IllegalArgumentException("unknown auth strength");
        };
    }
}
