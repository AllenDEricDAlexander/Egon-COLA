package top.egon.cola.platform.rbac3.admin.role.application;

import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchyValidator;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.util.function.Function;

/**
 * Serializes role graph changes and rebuilds the materialized closure atomically.
 */
public final class RoleFacade {

    private final HierarchyStore hierarchyStore;
    private final RoleControlStore roleControlStore;
    private final RoleHierarchyValidator validator = new RoleHierarchyValidator();

    public RoleFacade(HierarchyStore hierarchyStore) {
        this(hierarchyStore, null);
    }

    public RoleFacade(HierarchyStore hierarchyStore, RoleControlStore roleControlStore) {
        this.hierarchyStore = Objects.requireNonNull(hierarchyStore, "hierarchyStore");
        this.roleControlStore = roleControlStore;
    }

    public RoleMutationResult createRole(CreateRoleCommand command, Instant now) {
        return requiredControlStore().create(command, now);
    }

    public RoleMutationResult assignPermission(
            AssignPermissionCommand command,
            Instant now) {
        return requiredControlStore().assignPermission(command, now);
    }

    public RoleMutationResult assignPermissions(
            AssignPermissionsCommand command,
            Instant now) {
        return requiredControlStore().assignPermissions(command, now);
    }

    public RoleMutationResult removePermission(
            RemovePermissionCommand command,
            Instant now) {
        return requiredControlStore().removePermission(command, now);
    }

    public RoleMutationResult updateRole(UpdateRoleCommand command, Instant now) {
        return requiredControlStore().update(command, now);
    }

    public List<RoleView> roles(String tenantId, String applicationId) {
        return List.copyOf(requiredControlStore().roles(tenantId, applicationId));
    }

    public RoleImpactView impact(String tenantId, String roleId) {
        return requiredControlStore().impact(tenantId, roleId);
    }

    public void addInheritance(
            String tenantId,
            String applicationId,
            String seniorRoleId,
            String juniorRoleId) {
        addInheritance(new InheritanceCommand(
                tenantId, applicationId, seniorRoleId, juniorRoleId,
                -1L, "role-control-plane"));
    }

    public void addInheritance(InheritanceCommand command) {
        mutateInheritance(command, true);
    }

    private void mutateInheritance(InheritanceCommand command, boolean add) {
        String tenantId = command.tenantId();
        String applicationId = command.applicationId();
        String seniorRoleId = command.seniorRoleId();
        String juniorRoleId = command.juniorRoleId();
        RoleEdge edge = new RoleEdge(seniorRoleId, juniorRoleId);
        hierarchyStore.withGraphLock(tenantId, applicationId, current -> {
            hierarchyStore.assertRoleVersion(
                    tenantId, seniorRoleId, command.expectedRoleVersion());
            if (add && current.edges().contains(edge)
                    || !add && !current.edges().contains(edge)) {
                return null;
            }
            var edges = new ArrayList<>(current.edges());
            if (add) {
                edges.add(edge);
            } else {
                edges.remove(edge);
            }
            RoleHierarchy next = new RoleHierarchy(current.nodes().values(), edges);
            validate(next);
            if (add) {
                hierarchyStore.addEdge(tenantId, applicationId, edge);
            } else {
                hierarchyStore.removeEdge(tenantId, applicationId, edge);
            }
            hierarchyStore.rebuildClosure(tenantId, applicationId);
            hierarchyStore.recordGraphMutation(
                    tenantId, applicationId, edge, add, command.actorId());
            return null;
        });
    }

    public void removeInheritance(
            String tenantId,
            String applicationId,
            String seniorRoleId,
            String juniorRoleId) {
        removeInheritance(new InheritanceCommand(
                tenantId, applicationId, seniorRoleId, juniorRoleId,
                -1L, "role-control-plane"));
    }

    public void removeInheritance(InheritanceCommand command) {
        mutateInheritance(command, false);
    }

    private void validate(RoleHierarchy hierarchy) {
        validator.validate(hierarchy);
        for (String roleId : hierarchy.nodes().keySet()) {
            if (hierarchy.rootsOf(roleId).size() != 1) {
                throw new Rbac3RuleViolation(
                        "ROLE_ACTIVATION_ROOT_AMBIGUOUS", java.util.List.of(roleId));
            }
        }
    }

    private RoleControlStore requiredControlStore() {
        if (roleControlStore == null) {
            throw new IllegalStateException("role control store is not configured");
        }
        return roleControlStore;
    }

    public interface HierarchyStore {

        <T> T withGraphLock(
                String tenantId,
                String applicationId,
                Function<RoleHierarchy, T> action);

        void addEdge(String tenantId, String applicationId, RoleEdge edge);

        void removeEdge(String tenantId, String applicationId, RoleEdge edge);

        void rebuildClosure(String tenantId, String applicationId);

        default void assertRoleVersion(
                String tenantId,
                String roleId,
                long expectedRoleVersion) {
        }

        default void recordGraphMutation(
                String tenantId,
                String applicationId,
                RoleEdge edge,
                boolean added,
                String actorId) {
        }
    }

    public interface RoleControlStore {

        RoleMutationResult create(CreateRoleCommand command, Instant now);

        RoleMutationResult assignPermission(AssignPermissionCommand command, Instant now);

        default RoleMutationResult assignPermissions(
                AssignPermissionsCommand command,
                Instant now) {
            RoleMutationResult result = null;
            for (String permissionId : command.permissionIds()) {
                result = assignPermission(new AssignPermissionCommand(
                        command.tenantId(),
                        command.applicationId(),
                        command.roleId(),
                        permissionId,
                        command.validFrom(),
                        command.validTo(),
                        command.actorId()), now);
            }
            return result;
        }

        default RoleMutationResult removePermission(
                RemovePermissionCommand command,
                Instant now) {
            throw new UnsupportedOperationException("permission removal is not configured");
        }

        default RoleMutationResult update(UpdateRoleCommand command, Instant now) {
            throw new UnsupportedOperationException("role update is not configured");
        }

        default List<RoleView> roles(String tenantId, String applicationId) {
            throw new UnsupportedOperationException("role query is not configured");
        }

        default RoleImpactView impact(String tenantId, String roleId) {
            throw new UnsupportedOperationException("role impact is not configured");
        }
    }

    public record CreateRoleCommand(
            String tenantId,
            String applicationId,
            String roleCode,
            String roleName,
            String roleType,
            String riskLevel,
            boolean privileged,
            String landingRouteId,
            int landingPriority,
            Integer maximumAssignmentDays,
            String actorId
    ) {
    }

    public record AssignPermissionCommand(
            String tenantId,
            String applicationId,
            String roleId,
            String permissionId,
            Instant validFrom,
            Instant validTo,
            String actorId
    ) {
    }

    public record AssignPermissionsCommand(
            String tenantId,
            String applicationId,
            String roleId,
            List<String> permissionIds,
            Instant validFrom,
            Instant validTo,
            long expectedRoleVersion,
            String actorId
    ) {

        public AssignPermissionsCommand {
            permissionIds = List.copyOf(Objects.requireNonNull(
                    permissionIds, "permissionIds"));
            if (permissionIds.isEmpty()
                    || permissionIds.stream().distinct().count() != permissionIds.size()) {
                throw new IllegalArgumentException("permissionIds must be a non-empty set");
            }
            if (expectedRoleVersion < 0L) {
                throw new IllegalArgumentException("expectedRoleVersion must not be negative");
            }
        }
    }

    public record RemovePermissionCommand(
            String tenantId,
            String applicationId,
            String roleId,
            String permissionId,
            long expectedRoleVersion,
            String actorId
    ) {
    }

    public record UpdateRoleCommand(
            String tenantId,
            String roleId,
            String roleName,
            String status,
            String landingRouteId,
            int landingPriority,
            Integer maximumAssignmentDays,
            long expectedRoleVersion,
            String actorId
    ) {
    }

    public record InheritanceCommand(
            String tenantId,
            String applicationId,
            String seniorRoleId,
            String juniorRoleId,
            long expectedRoleVersion,
            String actorId
    ) {
    }

    public record RoleView(
            String roleId,
            String applicationId,
            String roleCode,
            String roleName,
            String roleType,
            String riskLevel,
            boolean privileged,
            String status,
            long version
    ) {
    }

    public record RoleImpactView(
            String roleId,
            List<String> activationRoots,
            List<String> roleFamily,
            String effectiveFamilyRisk,
            long permissionCount,
            List<String> conflicts
    ) {

        public RoleImpactView {
            activationRoots = List.copyOf(activationRoots);
            roleFamily = List.copyOf(roleFamily);
            conflicts = List.copyOf(conflicts);
        }
    }

    public record RoleMutationResult(
            String resourceId,
            long policyVersion,
            String propagationId,
            boolean propagationPending
    ) {
    }
}
