package top.egon.cola.platform.rbac3.core.delegation;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class ManagementPolicyDecisionService {

    private final ManagementPolicySpecification specification;

    public ManagementPolicyDecisionService() {
        this(new ManagementPolicySpecification());
    }

    public ManagementPolicyDecisionService(ManagementPolicySpecification specification) {
        this.specification = specification;
    }

    public ManagementDecision decide(ManagementDecisionInput input) {
        String denial = "MANAGEMENT_POLICY_DENIED";
        for (ManagementPolicyFact policy : input.policies().stream()
                .sorted(java.util.Comparator.comparing(ManagementPolicyFact::id))
                .toList()) {
            var result = specification.evaluate(input, policy);
            if (result.allowed()) {
                return new ManagementDecision(true, result.reasonCode(), policy.id());
            }
            if (priority(result.reasonCode()) > priority(denial)) {
                denial = result.reasonCode();
            }
        }
        return new ManagementDecision(false, denial, null);
    }

    private int priority(String reasonCode) {
        return switch (reasonCode) {
            case "STEP_UP_REQUIRED" -> 5;
            case "MANAGEMENT_OPERATION_DENIED" -> 4;
            case "MANAGED_ROLE_SCOPE_DENIED" -> 3;
            case "MANAGED_USER_SCOPE_DENIED" -> 2;
            default -> 1;
        };
    }

    public record ManagementDecisionInput(
            String subjectId,
            String targetUserId,
            String activationRootRoleId,
            String operation,
            String authStrength,
            String roleRisk,
            int assignmentDays,
            boolean reasonPresent,
            boolean ticketPresent,
            Instant databaseNow,
            List<ManagementPolicyFact> policies
    ) {
        public ManagementDecisionInput {
            policies = List.copyOf(policies);
            if (assignmentDays < 0 || databaseNow == null) {
                throw new IllegalArgumentException("valid assignment days and database time are required");
            }
        }
    }

    public record ManagementPolicyFact(
            String id,
            Set<String> subjectIds,
            Set<String> targetUserIds,
            Set<String> activationRootRoleIds,
            Set<String> operations,
            Restrictions restrictions,
            Instant validFrom,
            Instant validTo,
            boolean active
    ) {
        public ManagementPolicyFact {
            subjectIds = Set.copyOf(subjectIds);
            targetUserIds = Set.copyOf(targetUserIds);
            activationRootRoleIds = Set.copyOf(activationRootRoleIds);
            operations = Set.copyOf(operations);
        }
    }

    public record Restrictions(
            int maxAssignmentDays,
            String maxRiskLevel,
            String requiredAuthStrength,
            boolean requireReason,
            boolean requireTicket
    ) {
        public Restrictions {
            if (maxAssignmentDays < 1) {
                throw new IllegalArgumentException("maxAssignmentDays must be positive");
            }
        }
    }

    public record ManagementDecision(boolean allowed, String reasonCode, String policyId) {
    }
}
