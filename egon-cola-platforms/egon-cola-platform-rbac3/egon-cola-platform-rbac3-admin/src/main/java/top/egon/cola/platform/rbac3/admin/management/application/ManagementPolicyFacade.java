package top.egon.cola.platform.rbac3.admin.management.application;

import top.egon.cola.platform.rbac3.core.delegation.ManagementPolicyDecisionService;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Requires one complete management policy to authorize one operation.
 */
public final class ManagementPolicyFacade {

    private final ManagementPolicyDecisionService decisionService;
    private final PolicyFactSource factSource;
    private final ControlStore controlStore;

    public ManagementPolicyFacade(
            ManagementPolicyDecisionService decisionService,
            PolicyFactSource factSource
    ) {
        this.decisionService = Objects.requireNonNull(decisionService, "decisionService");
        this.factSource = Objects.requireNonNull(factSource, "factSource");
        this.controlStore = factSource instanceof ControlStore store ? store : null;
    }

    public String authorize(Request request) {
        var decision = decisionService.decide(
                new ManagementPolicyDecisionService.ManagementDecisionInput(
                        request.subjectId(), request.targetUserId(),
                        request.activationRootRoleId(), request.operation(),
                        request.authenticationStrength(), request.roleRisk(),
                        request.assignmentDays(), request.reasonPresent(),
                        request.ticketPresent(), request.databaseNow(),
                        factSource.policies(
                                request.tenantId(), request.subjectId(),
                                request.targetUserId(), request.databaseNow())));
        if (!decision.allowed()) {
            throw new Rbac3RuleViolation(decision.reasonCode());
        }
        return decision.policyId();
    }

    public List<PolicyView> policies(String tenantId) {
        return List.copyOf(store().policies(required(tenantId, "tenantId")));
    }

    public PolicyView policy(String tenantId, String policyId) {
        return store().policy(
                required(tenantId, "tenantId"), required(policyId, "policyId"));
    }

    public PolicyView save(SaveCommand command) {
        return store().save(Objects.requireNonNull(command, "command"));
    }

    public PolicyView disable(
            String tenantId,
            String policyId,
            long expectedVersion,
            String actorId
    ) {
        return store().disable(
                required(tenantId, "tenantId"), required(policyId, "policyId"),
                expectedVersion, required(actorId, "actorId"));
    }

    public CapabilityView capabilities(
            String tenantId,
            String subjectUserId,
            Instant databaseNow
    ) {
        return store().capabilities(
                required(tenantId, "tenantId"),
                required(subjectUserId, "subjectUserId"),
                Objects.requireNonNull(databaseNow, "databaseNow"));
    }

    public List<ManagedUserView> manageableUsers(
            String tenantId,
            String subjectUserId,
            String query,
            Instant databaseNow
    ) {
        return List.copyOf(store().manageableUsers(
                required(tenantId, "tenantId"),
                required(subjectUserId, "subjectUserId"), query,
                Objects.requireNonNull(databaseNow, "databaseNow")));
    }

    public List<ManagedRoleView> manageableRoles(
            String tenantId,
            String subjectUserId,
            String query,
            Instant databaseNow
    ) {
        return List.copyOf(store().manageableRoles(
                required(tenantId, "tenantId"),
                required(subjectUserId, "subjectUserId"), query,
                Objects.requireNonNull(databaseNow, "databaseNow")));
    }

    @FunctionalInterface
    public interface PolicyFactSource {
        List<ManagementPolicyDecisionService.ManagementPolicyFact> policies(
                String tenantId,
                String subjectId,
                String targetUserId,
                Instant databaseNow);
    }

    public interface ControlStore {
        List<PolicyView> policies(String tenantId);

        PolicyView policy(String tenantId, String policyId);

        PolicyView save(SaveCommand command);

        PolicyView disable(
                String tenantId,
                String policyId,
                long expectedVersion,
                String actorId);

        CapabilityView capabilities(
                String tenantId,
                String subjectUserId,
                Instant databaseNow);

        List<ManagedUserView> manageableUsers(
                String tenantId,
                String subjectUserId,
                String query,
                Instant databaseNow);

        List<ManagedRoleView> manageableRoles(
                String tenantId,
                String subjectUserId,
                String query,
                Instant databaseNow);
    }

    public record Request(
            String tenantId,
            String subjectId,
            String targetUserId,
            String activationRootRoleId,
            String operation,
            String authenticationStrength,
            String roleRisk,
            int assignmentDays,
            boolean reasonPresent,
            boolean ticketPresent,
            Instant databaseNow
    ) {
    }

    public record SaveCommand(
            String tenantId,
            String policyId,
            String policyCode,
            String name,
            Instant validFrom,
            Instant validTo,
            Restrictions restrictions,
            List<Subject> subjects,
            List<Scope> scopes,
            List<String> activationRootRoleIds,
            Set<String> operations,
            long expectedVersion,
            String actorId
    ) {
        public SaveCommand {
            restrictions = Objects.requireNonNull(restrictions, "restrictions");
            subjects = List.copyOf(subjects);
            scopes = List.copyOf(scopes);
            activationRootRoleIds = List.copyOf(activationRootRoleIds);
            operations = Set.copyOf(operations);
            if (subjects.isEmpty() || scopes.isEmpty()
                    || activationRootRoleIds.isEmpty() || operations.isEmpty()) {
                throw new IllegalArgumentException(
                        "management policy subjects, scopes, roles and operations are required");
            }
        }
    }

    public record PolicyView(
            String policyId,
            String policyCode,
            String name,
            String status,
            Instant validFrom,
            Instant validTo,
            Restrictions restrictions,
            List<Subject> subjects,
            List<Scope> scopes,
            List<String> activationRootRoleIds,
            Set<String> operations,
            long version
    ) {
        public PolicyView {
            subjects = List.copyOf(subjects);
            scopes = List.copyOf(scopes);
            activationRootRoleIds = List.copyOf(activationRootRoleIds);
            operations = Set.copyOf(operations);
        }
    }

    public record Restrictions(
            Integer maximumAssignmentDays,
            String maximumRiskLevel,
            String requiredAuthenticationStrength,
            boolean requireReason,
            boolean requireTicket,
            boolean includeInheritedSubjectRoles,
            boolean requireAllAffiliationsInScope
    ) {
    }

    public record Subject(String type, String id) {
    }

    public record Scope(String type, String referenceId) {
    }

    public record CapabilityView(
            List<String> policyIds,
            Set<String> operations,
            List<String> activationRootRoleIds
    ) {
        public CapabilityView {
            policyIds = List.copyOf(policyIds);
            operations = Set.copyOf(operations);
            activationRootRoleIds = List.copyOf(activationRootRoleIds);
        }
    }

    public record ManagedUserView(
            String userId,
            String username,
            String displayName
    ) {
    }

    public record ManagedRoleView(
            String roleId,
            String roleCode,
            String roleName,
            String riskLevel,
            boolean privileged
    ) {
    }

    private ControlStore store() {
        if (controlStore == null) {
            throw new IllegalStateException("management policy control store is not configured");
        }
        return controlStore;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
