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
                || (policy.validTo() != null
                && !input.databaseNow().isBefore(policy.validTo()))) {
            return denied(policy.id(), "MANAGEMENT_POLICY_DENIED");
        }
        if (!policy.subjectIds().contains(input.subjectId())) {
            return denied(policy.id(), "MANAGEMENT_POLICY_DENIED");
        }
        if (!policy.targetUserIds().contains(input.targetUserId())) {
            return denied(policy.id(), "MANAGED_USER_SCOPE_DENIED");
        }
        if (!policy.activationRootRoleIds().contains(input.activationRootRoleId())) {
            return denied(policy.id(), "MANAGED_ROLE_SCOPE_DENIED");
        }
        if (!policy.operations().contains(input.operation())) {
            return denied(policy.id(), "MANAGEMENT_OPERATION_DENIED");
        }
        ManagementPolicyDecisionService.Restrictions restrictions = policy.restrictions();
        if (strength(input.authStrength()) < strength(restrictions.requiredAuthStrength())) {
            return denied(policy.id(), "STEP_UP_REQUIRED");
        }
        if (input.assignmentDays() > restrictions.maxAssignmentDays()
                || risk(input.roleRisk()) > risk(restrictions.maxRiskLevel())
                || (restrictions.requireReason() && !input.reasonPresent())
                || (restrictions.requireTicket() && !input.ticketPresent())) {
            return denied(policy.id(), "MANAGEMENT_POLICY_DENIED");
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
