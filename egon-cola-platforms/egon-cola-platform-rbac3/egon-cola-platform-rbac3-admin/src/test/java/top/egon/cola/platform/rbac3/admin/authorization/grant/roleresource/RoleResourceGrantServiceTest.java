package top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleResourcesCommandDTO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleResourcesRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.vo.RoleResourceGrantMutationVO;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleResourceGrantServiceTest {

    @Test
    void requestAcceptsUniqueResourceIdsAndRejectsDuplicates() {
        ReplaceRoleResourcesRequestDTO request = new ReplaceRoleResourcesRequestDTO(
                List.of("501", "502"), null, null, 8L);
        assertEquals(List.of("501", "502"), request.resourceIds());
        assertThrows(IllegalArgumentException.class,
                () -> new ReplaceRoleResourcesRequestDTO(
                        List.of("501", "501"), null, null, 8L));
    }

    @Test
    void commandAndMutationNeverCarryPermissionCharacters() throws Exception {
        ReplaceRoleResourcesCommandDTO command = new ReplaceRoleResourcesCommandDTO(
                200L, 71L, 301L, Set.of(501L),
                Instant.parse("2026-08-25T00:00:00Z"), null, 8L, "actor");
        RoleResourceGrantMutationVO mutation = RoleResourceGrantMutationVO.success(
                command.roleId(), 9L, Set.of(501L), Set.of(701L),
                Set.of(501L, 701L), 1L, 0L, 2L, 12L, 4L);

        assertEquals("301", mutation.roleId());
        assertEquals(List.of("501"), mutation.directResourceIds());
        assertEquals(List.of("701"), mutation.derivedApiResourceIds());
        assertThrows(NoSuchMethodException.class,
                () -> command.getClass().getDeclaredMethod("permissionCode"));
    }
}
