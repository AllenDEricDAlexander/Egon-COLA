package top.egon.cola.platform.rbac3.admin.role;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.role.application.RoleFacade;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleControlFacadeTest {

    @Test
    void permissionBindingPreservesOneAtomicBatchAtTheStoreBoundary() {
        RecordingControlStore controlStore = new RecordingControlStore();
        RoleFacade facade = new RoleFacade(new EmptyHierarchyStore(), controlStore);
        RoleFacade.AssignPermissionsCommand command = new RoleFacade.AssignPermissionsCommand(
                "10001",
                "71001",
                "50001",
                List.of("72001", "72002"),
                Instant.EPOCH,
                null,
                3L,
                "20001");

        RoleFacade.RoleMutationResult result = facade.assignPermissions(command, Instant.EPOCH);

        assertEquals(List.of("72001", "72002"), controlStore.lastCommand.permissionIds());
        assertEquals("50001", result.resourceId());
    }

    private static final class RecordingControlStore implements RoleFacade.RoleControlStore {

        private RoleFacade.AssignPermissionsCommand lastCommand;

        @Override
        public RoleFacade.RoleMutationResult create(
                RoleFacade.CreateRoleCommand command,
                Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RoleFacade.RoleMutationResult assignPermission(
                RoleFacade.AssignPermissionCommand command,
                Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RoleFacade.RoleMutationResult assignPermissions(
                RoleFacade.AssignPermissionsCommand command,
                Instant now) {
            lastCommand = command;
            return new RoleFacade.RoleMutationResult("50001", 4L, "event-1", true);
        }
    }

    private static final class EmptyHierarchyStore implements RoleFacade.HierarchyStore {

        @Override
        public <T> T withGraphLock(
                String tenantId,
                String applicationId,
                Function<RoleHierarchy, T> action) {
            return action.apply(new RoleHierarchy(List.of(), List.of()));
        }

        @Override
        public void addEdge(String tenantId, String applicationId,
                            top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge edge) {
        }

        @Override
        public void removeEdge(String tenantId, String applicationId,
                               top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge edge) {
        }

        @Override
        public void rebuildClosure(String tenantId, String applicationId) {
        }
    }
}
