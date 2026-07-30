package top.egon.cola.platform.rbac3.admin.assignment.application;

import top.egon.cola.platform.rbac3.admin.management.application.ManagementPolicyFacade;
import top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationMutationCoordinator;
import top.egon.cola.platform.rbac3.core.constraint.PrerequisiteRoleSpecification;
import top.egon.cola.platform.rbac3.core.constraint.RoleCardinalitySpecification;
import top.egon.cola.platform.rbac3.core.constraint.SsdSpecification;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import top.egon.cola.platform.rbac3.core.rule.RuleResult;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Orchestrates delegated role assignment in the fixed security-check order.
 */
public final class AssignmentFacade {

    private final ManagementPolicyFacade managementPolicyFacade;
    private final AssignmentFactSource factSource;
    private final AssignmentLock assignmentLock;
    private final AssignmentStore assignmentStore;
    private final AuthorizationMutationCoordinator mutationCoordinator;
    private final SsdSpecification ssdSpecification = new SsdSpecification();
    private final PrerequisiteRoleSpecification prerequisiteSpecification =
            new PrerequisiteRoleSpecification();
    private final RoleCardinalitySpecification cardinalitySpecification =
            new RoleCardinalitySpecification();

    public AssignmentFacade(
            ManagementPolicyFacade managementPolicyFacade,
            AssignmentFactSource factSource,
            AssignmentLock assignmentLock,
            AssignmentStore assignmentStore,
            AuthorizationMutationCoordinator mutationCoordinator
    ) {
        this.managementPolicyFacade = Objects.requireNonNull(
                managementPolicyFacade, "managementPolicyFacade");
        this.factSource = Objects.requireNonNull(factSource, "factSource");
        this.assignmentLock = Objects.requireNonNull(assignmentLock, "assignmentLock");
        this.assignmentStore = Objects.requireNonNull(assignmentStore, "assignmentStore");
        this.mutationCoordinator = Objects.requireNonNull(
                mutationCoordinator, "mutationCoordinator");
    }

    public AssignmentResult assign(AssignRequest request) {
        AssignmentFacts initial = factSource.load(request);
        int assignmentDays = assignmentDays(request.validFrom(), request.validTo());
        String managementPolicyId = managementPolicyFacade.authorize(
                new ManagementPolicyFacade.Request(
                request.tenantId(), request.actorId(), request.targetUserId(),
                initial.activationRootRoleId(), operation(request.assignmentType()),
                request.authenticationStrength(), initial.roleRisk(), assignmentDays,
                hasText(request.reason()), hasText(request.ticketNo()), request.databaseNow()));
        if (initial.privileged() && !request.platformAdministrator()) {
            throw new Rbac3RuleViolation("PRIVILEGED_ROLE_MANAGEMENT_DENIED");
        }
        if (request.actorId().equals(request.targetUserId())) {
            throw new Rbac3RuleViolation("SELF_PRIVILEGE_ESCALATION_DENIED");
        }
        if (initial.maximumAssignmentDays() != null
                && assignmentDays > initial.maximumAssignmentDays()) {
            throw new Rbac3RuleViolation("MANAGEMENT_POLICY_DENIED");
        }
        validateRules(initial, request.roleId());

        Cardinality cardinality = initial.cardinality();
        @SuppressWarnings("unchecked")
        AuthorizationMutationCoordinator.MutationResult<String> mutation =
                (AuthorizationMutationCoordinator.MutationResult<String>)
                        assignmentLock.withLock(new LockExecution(
                                request.tenantId(), initial.activationRootRoleId(),
                                cardinality.scopeType(), cardinality.scopeId(), () -> {
                                    AssignmentFacts locked = factSource.load(request);
                                    validateRules(locked, request.roleId());
                                    return mutationCoordinator.execute(
                                            new AuthorizationMutationCoordinator.MutationScope(
                                                    request.tenantId(), "USER",
                                                    request.targetUserId(), request.commandId(),
                                                    request.actorId()),
                                            request.targetUserId(),
                                            new AuthorizationMutationCoordinator.ExpectedVersions(
                                                    null, null,
                                                    request.expectedUserAuthVersion(),
                                                    request.validFrom().isAfter(
                                                            request.databaseNow())
                                                            ? request.expectedUserAuthVersion()
                                                            : Math.incrementExact(request
                                                                    .expectedUserAuthVersion()),
                                                    null, null),
                                            () -> assignmentStore.assign(new AssignmentCommand(
                                                    request, initial.activationRootRoleId(),
                                                    managementPolicyId)));
                                }));
        return new AssignmentResult(
                mutation.value(), mutation.mutationId(), mutation.completed(),
                mutation.reasonCode(), mutation.versions().newAuthVersion());
    }

    public List<AssignmentView> assignments(
            String tenantId,
            String userId,
            Instant databaseNow
    ) {
        return List.copyOf(assignmentStore.assignments(
                tenantId, userId, databaseNow));
    }

    public AssignmentResult change(ChangeRequest request) {
        AssignmentChangeFacts facts = factSource.loadChange(request);
        String operation = request.operation().name() + "_ROLE";
        boolean permittedSelfRevoke = false;
        if (request.actorId().equals(request.targetUserId())
                && request.operation() == ChangeOperation.REVOKE
                && "LOW".equals(facts.roleRisk())) {
            operation = "SELF_REVOKE_LOW_RISK";
            permittedSelfRevoke = true;
        }
        managementPolicyFacade.authorize(new ManagementPolicyFacade.Request(
                request.tenantId(), request.actorId(), request.targetUserId(),
                facts.activationRootRoleId(), operation,
                request.authenticationStrength(), facts.roleRisk(), 1,
                hasText(request.reason()), hasText(request.ticketNo()),
                request.databaseNow()));
        if (facts.privileged() && !request.platformAdministrator()) {
            throw new Rbac3RuleViolation("PRIVILEGED_ROLE_MANAGEMENT_DENIED");
        }
        if (request.actorId().equals(request.targetUserId()) && !permittedSelfRevoke) {
            throw new Rbac3RuleViolation("SELF_PRIVILEGE_ESCALATION_DENIED");
        }
        @SuppressWarnings("unchecked")
        AuthorizationMutationCoordinator.MutationResult<String> mutation =
                (AuthorizationMutationCoordinator.MutationResult<String>)
                        assignmentLock.withLock(new LockExecution(
                                request.tenantId(), facts.activationRootRoleId(),
                                "TENANT", request.tenantId(),
                                () -> mutationCoordinator.execute(
                                        new AuthorizationMutationCoordinator.MutationScope(
                                                request.tenantId(), "USER",
                                                request.targetUserId(), request.commandId(),
                                                request.actorId()),
                                        request.targetUserId(),
                                        new AuthorizationMutationCoordinator.ExpectedVersions(
                                                null, null,
                                                request.expectedUserAuthVersion(),
                                                Math.incrementExact(
                                                        request.expectedUserAuthVersion()),
                                                null, null),
                                        () -> assignmentStore.change(request))));
        return new AssignmentResult(
                mutation.value(), mutation.mutationId(), mutation.completed(),
                mutation.reasonCode(), mutation.versions().newAuthVersion());
    }

    private void validateRules(AssignmentFacts facts, String requestedRoleId) {
        Set<String> resulting = new HashSet<>(facts.currentRoleIds());
        resulting.add(requestedRoleId);
        require(ssdSpecification.evaluate(resulting, facts.ssdSets()));
        for (PrerequisiteRoleSpecification.PrerequisiteGroup group
                : facts.prerequisiteGroups()) {
            require(prerequisiteSpecification.evaluate(resulting, group));
        }
        require(cardinalitySpecification.evaluate(
                facts.cardinality().activeAssignments(),
                facts.cardinality().maximumActive()));
    }

    private void require(RuleResult result) {
        if (!result.allowed()) {
            String reasonCode = "PREREQUISITE_ROLE_MISSING".equals(result.reasonCode())
                    ? "ROLE_PREREQUISITE_NOT_MET"
                    : result.reasonCode();
            throw new Rbac3RuleViolation(reasonCode, result.evidenceIds());
        }
    }

    private int assignmentDays(Instant from, Instant to) {
        if (to == null) {
            return Integer.MAX_VALUE;
        }
        long seconds = Duration.between(from, to).toSeconds();
        return Math.toIntExact(Math.max(1L, (seconds + 86_399L) / 86_400L));
    }

    private String operation(String assignmentType) {
        return "TEMPORARY".equals(assignmentType)
                ? "TEMPORARY_ASSIGN"
                : "ASSIGN_ROLE";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @FunctionalInterface
    public interface AssignmentFactSource {
        AssignmentFacts load(AssignRequest request);

        default AssignmentChangeFacts loadChange(ChangeRequest request) {
            throw new UnsupportedOperationException("assignment change facts are not configured");
        }
    }

    @FunctionalInterface
    public interface AssignmentLock {
        Object withLock(LockExecution scope);
    }

    @FunctionalInterface
    public interface AssignmentStore {
        String assign(AssignmentCommand command);

        default List<AssignmentView> assignments(
                String tenantId,
                String userId,
                Instant databaseNow
        ) {
            throw new UnsupportedOperationException("assignment query is not configured");
        }

        default String change(ChangeRequest request) {
            throw new UnsupportedOperationException("assignment change is not configured");
        }
    }

    public record LockExecution(
            String tenantId,
            String activationRootRoleId,
            String scopeType,
            String scopeId,
            Supplier<Object> action
    ) {
    }

    public record AssignRequest(
            String tenantId,
            String actorId,
            String targetUserId,
            String roleId,
            String assignmentType,
            Instant validFrom,
            Instant validTo,
            String reason,
            String ticketNo,
            String authenticationStrength,
            boolean platformAdministrator,
            long expectedUserAuthVersion,
            String commandId,
            Instant databaseNow
    ) {
    }

    public record AssignmentCommand(
            AssignRequest request,
            String activationRootRoleId,
            String managementPolicyId
    ) {
    }

    public record ChangeRequest(
            String tenantId,
            String actorId,
            String targetUserId,
            String assignmentId,
            ChangeOperation operation,
            String reason,
            String ticketNo,
            String authenticationStrength,
            boolean platformAdministrator,
            long expectedAssignmentVersion,
            long expectedUserAuthVersion,
            String commandId,
            Instant databaseNow
    ) {
    }

    public record AssignmentChangeFacts(
            String activationRootRoleId,
            String roleRisk,
            boolean privileged
    ) {
    }

    public record AssignmentFacts(
            String activationRootRoleId,
            String roleRisk,
            boolean privileged,
            String roleType,
            Integer maximumAssignmentDays,
            Set<String> currentRoleIds,
            List<SsdSpecification.SsdSet> ssdSets,
            List<PrerequisiteRoleSpecification.PrerequisiteGroup> prerequisiteGroups,
            Cardinality cardinality
    ) {
        public AssignmentFacts {
            currentRoleIds = Set.copyOf(currentRoleIds);
            ssdSets = List.copyOf(ssdSets);
            prerequisiteGroups = List.copyOf(prerequisiteGroups);
        }
    }

    public record Cardinality(
            String scopeType,
            String scopeId,
            long maximumActive,
            long activeAssignments
    ) {
    }

    public record AssignmentResult(
            String assignmentId,
            String mutationId,
            boolean completed,
            String reasonCode,
            Long authVersion
    ) {
    }

    public record AssignmentView(
            String assignmentId,
            String roleId,
            String assignmentType,
            String status,
            Instant validFrom,
            Instant validTo,
            String sourceType,
            String sourceId,
            long version
    ) {
    }

    public enum ChangeOperation {
        REVOKE,
        SUSPEND,
        RESUME
    }
}
