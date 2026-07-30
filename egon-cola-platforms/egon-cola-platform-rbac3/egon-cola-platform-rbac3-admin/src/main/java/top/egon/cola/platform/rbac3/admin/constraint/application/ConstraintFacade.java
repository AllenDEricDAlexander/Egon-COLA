package top.egon.cola.platform.rbac3.admin.constraint.application;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Enforces the different qualification boundaries of SSD and DSD sets.
 */
public final class ConstraintFacade {

    private final RoleFactSource roleFactSource;
    private final ConstraintStore constraintStore;

    public ConstraintFacade(RoleFactSource roleFactSource) {
        this(roleFactSource, null);
    }

    public ConstraintFacade(
            RoleFactSource roleFactSource,
            ConstraintStore constraintStore) {
        this.roleFactSource = Objects.requireNonNull(roleFactSource, "roleFactSource");
        this.constraintStore = constraintStore;
    }

    public void validate(SodCommand command) {
        Objects.requireNonNull(command, "command");
        if (command.roleIds().isEmpty()
                || new HashSet<>(command.roleIds()).size() != command.roleIds().size()) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        if (command.maximumActiveRoles() < 1
                || command.maximumActiveRoles() >= command.roleIds().size()) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        if (command.constraintType() == ConstraintType.DSD) {
            if (command.applicationId() == null) {
                throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
            }
            for (String roleId : command.roleIds()) {
                RoleFact role = roleFactSource.require(roleId);
                if (!role.applicationId().equals(command.applicationId())) {
                    throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
                }
                if (!role.activationRoot()) {
                    throw new Rbac3RuleViolation("DSD_MEMBER_NOT_ACTIVATION_ROOT");
                }
            }
        } else if (command.applicationId() != null) {
            for (String roleId : command.roleIds()) {
                if (!roleFactSource.require(roleId).applicationId()
                        .equals(command.applicationId())) {
                    throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
                }
            }
        }
    }

    public List<SodView> sodSets(String tenantId) {
        return List.copyOf(requiredStore().sodSets(tenantId));
    }

    public MutationResult saveSod(SaveSodCommand command) {
        validate(new SodCommand(
                command.constraintType(),
                command.applicationId(),
                command.roleIds(),
                command.maximumActiveRoles()));
        return requiredStore().saveSod(command);
    }

    public MutationResult savePrerequisites(PrerequisiteGroupCommand command) {
        if (command.prerequisiteRoleIds().isEmpty()
                || command.prerequisiteRoleIds().contains(command.targetRoleId())) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        RoleFact target = roleFactSource.require(command.targetRoleId());
        for (String prerequisiteRoleId : command.prerequisiteRoleIds()) {
            if (!target.applicationId().equals(
                    roleFactSource.require(prerequisiteRoleId).applicationId())) {
                throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
            }
        }
        return requiredStore().savePrerequisites(command);
    }

    public MutationResult saveCardinality(CardinalityCommand command) {
        if (command.maximumActive() < 1) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        roleFactSource.require(command.roleId());
        return requiredStore().saveCardinality(command);
    }

    public List<DataRuleView> dataRules(String tenantId) {
        return List.copyOf(requiredStore().dataRules(tenantId));
    }

    public MutationResult saveDataRule(DataRuleCommand command) {
        roleFactSource.require(command.roleId());
        return requiredStore().saveDataRule(command);
    }

    public List<FieldRuleView> fieldRules(String tenantId) {
        return List.copyOf(requiredStore().fieldRules(tenantId));
    }

    public MutationResult saveFieldRule(FieldRuleCommand command) {
        roleFactSource.require(command.roleId());
        return requiredStore().saveFieldRule(command);
    }

    public List<OperationSodRuleView> operationSodRules(String tenantId) {
        return List.copyOf(requiredStore().operationSodRules(tenantId));
    }

    public MutationResult saveOperationSodRule(OperationSodRuleCommand command) {
        if (command.priorActionCode().equals(command.forbiddenLaterActionCode())) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        return requiredStore().saveOperationSodRule(command);
    }

    private ConstraintStore requiredStore() {
        if (constraintStore == null) {
            throw new IllegalStateException("constraint store is not configured");
        }
        return constraintStore;
    }

    @FunctionalInterface
    public interface RoleFactSource {

        RoleFact require(String roleId);
    }

    public interface ConstraintStore {

        List<SodView> sodSets(String tenantId);

        MutationResult saveSod(SaveSodCommand command);

        MutationResult savePrerequisites(PrerequisiteGroupCommand command);

        MutationResult saveCardinality(CardinalityCommand command);

        List<DataRuleView> dataRules(String tenantId);

        MutationResult saveDataRule(DataRuleCommand command);

        List<FieldRuleView> fieldRules(String tenantId);

        MutationResult saveFieldRule(FieldRuleCommand command);

        List<OperationSodRuleView> operationSodRules(String tenantId);

        MutationResult saveOperationSodRule(OperationSodRuleCommand command);
    }

    public record RoleFact(String roleId, String applicationId, boolean activationRoot) {
    }

    public record SodCommand(
            ConstraintType constraintType,
            String applicationId,
            List<String> roleIds,
            int maximumActiveRoles
    ) {

        public SodCommand {
            constraintType = Objects.requireNonNull(constraintType, "constraintType");
            roleIds = List.copyOf(Objects.requireNonNull(roleIds, "roleIds"));
        }
    }

    public record SaveSodCommand(
            String tenantId,
            String setId,
            String setCode,
            ConstraintType constraintType,
            String applicationId,
            int maximumActiveRoles,
            List<String> roleIds,
            java.time.Instant validFrom,
            java.time.Instant validTo,
            long expectedVersion,
            String actorId
    ) {

        public SaveSodCommand {
            roleIds = List.copyOf(roleIds);
        }
    }

    public record PrerequisiteGroupCommand(
            String tenantId,
            String targetRoleId,
            String groupCode,
            String matchMode,
            List<String> prerequisiteRoleIds,
            long expectedRoleVersion,
            String actorId
    ) {

        public PrerequisiteGroupCommand {
            prerequisiteRoleIds = List.copyOf(prerequisiteRoleIds);
        }
    }

    public record CardinalityCommand(
            String tenantId,
            String roleId,
            String scopeType,
            int maximumActive,
            java.time.Instant validFrom,
            java.time.Instant validTo,
            long expectedVersion,
            String actorId
    ) {
    }

    public record DataRuleCommand(
            String tenantId,
            String ruleId,
            String applicationId,
            String roleId,
            String permissionId,
            String scopeType,
            Long directorySnapshotVersion,
            List<RuleReference> references,
            java.time.Instant validFrom,
            java.time.Instant validTo,
            long expectedVersion,
            String actorId
    ) {

        public DataRuleCommand {
            references = List.copyOf(references);
        }
    }

    public record FieldRuleCommand(
            String tenantId,
            String ruleId,
            String applicationId,
            String roleId,
            String permissionId,
            String fieldDefinitionId,
            String accessLevel,
            java.time.Instant validFrom,
            java.time.Instant validTo,
            long expectedVersion,
            String actorId
    ) {
    }

    public record OperationSodRuleCommand(
            String tenantId,
            String ruleId,
            String applicationCode,
            String businessResource,
            String priorActionCode,
            String forbiddenLaterActionCode,
            java.time.Instant lookbackFrom,
            java.time.Instant validFrom,
            java.time.Instant validTo,
            long expectedVersion,
            String actorId
    ) {
    }

    public record RuleReference(String referenceType, String referenceId) {
    }

    public record MutationResult(
            String resourceId,
            long policyVersion,
            String propagationId,
            boolean propagationPending
    ) {
    }

    public record SodView(
            String setId,
            String setCode,
            String constraintType,
            String applicationId,
            int maximumActiveRoles,
            List<String> roleIds,
            String status,
            long version
    ) {

        public SodView {
            roleIds = List.copyOf(roleIds);
        }
    }

    public record DataRuleView(
            String ruleId,
            String applicationId,
            String roleId,
            String permissionId,
            String scopeType,
            List<RuleReference> references,
            String status,
            long version
    ) {

        public DataRuleView {
            references = List.copyOf(references);
        }
    }

    public record FieldRuleView(
            String ruleId,
            String applicationId,
            String roleId,
            String permissionId,
            String fieldDefinitionId,
            String accessLevel,
            String status,
            long version
    ) {
    }

    public record OperationSodRuleView(
            String ruleId,
            String applicationCode,
            String businessResource,
            String priorActionCode,
            String forbiddenLaterActionCode,
            String status,
            long version
    ) {
    }

    public enum ConstraintType {
        SSD,
        DSD
    }
}
