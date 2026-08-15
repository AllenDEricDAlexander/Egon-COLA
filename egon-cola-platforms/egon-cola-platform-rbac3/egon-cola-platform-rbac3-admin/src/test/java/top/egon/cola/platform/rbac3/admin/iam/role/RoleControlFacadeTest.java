package top.egon.cola.platform.rbac3.admin.iam.role;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.role.service.RoleFacade;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import top.egon.cola.platform.rbac3.admin.iam.role.inheritance.repository.RoleHierarchyRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.repository.RoleControlRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.dto.CreateRoleCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.dto.AssignPermissionCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.dto.AssignPermissionsCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.vo.RoleMutationResultVO;

class RoleControlFacadeTest {

    @Test
    void permissionBindingPreservesOneAtomicBatchAtTheStoreBoundary() {
        RecordingControlStore controlStore = new RecordingControlStore();
        RoleFacade facade = new RoleFacade(new EmptyHierarchyStore(), controlStore);
        AssignPermissionsCommandDTO command = new AssignPermissionsCommandDTO(
                "10001",
                "71001",
                "50001",
                List.of("72001", "72002"),
                Instant.EPOCH,
                null,
                3L,
                "20001");

        RoleMutationResultVO result = facade.assignPermissions(command, Instant.EPOCH);

        assertEquals(List.of("72001", "72002"), controlStore.lastCommand.permissionIds());
        assertEquals("50001", result.resourceId());
    }

    private static final class RecordingControlStore implements RoleControlRepository {

        private AssignPermissionsCommandDTO lastCommand;

        @Override
        public RoleMutationResultVO create(
                CreateRoleCommandDTO command,
                Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RoleMutationResultVO assignPermission(
                AssignPermissionCommandDTO command,
                Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RoleMutationResultVO assignPermissions(
                AssignPermissionsCommandDTO command,
                Instant now) {
            lastCommand = command;
            return new RoleMutationResultVO("50001", 4L, "event-1", true);
        }
    }

    private static final class EmptyHierarchyStore implements RoleHierarchyRepository {

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
