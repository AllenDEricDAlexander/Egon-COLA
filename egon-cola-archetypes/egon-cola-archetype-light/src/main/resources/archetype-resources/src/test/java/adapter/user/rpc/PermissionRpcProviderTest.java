package ${package}.adapter.user.rpc;

import ${package}.facade.user.PermissionFacade;
import ${package}.facade.user.dto.PermissionDetailDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionRpcProviderTest {
    @Test
    void delegates_permission_query_to_facade() {
        PermissionFacade facade = mock(PermissionFacade.class);
        List<PermissionDetailDTO> expected = List.of(
                new PermissionDetailDTO("course:read", "Read courses", List.of()));
        when(facade.getUserPermissions("u-1")).thenReturn(expected);

        assertThat(new PermissionRpcProvider(facade).getUserPermissions("u-1"))
                .isSameAs(expected);
    }
}
