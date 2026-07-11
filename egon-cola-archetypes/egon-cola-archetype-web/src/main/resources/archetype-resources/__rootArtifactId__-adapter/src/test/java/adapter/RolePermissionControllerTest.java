package ${package}.adapter;

import ${package}.adapter.controller.user.PermissionController;
import ${package}.adapter.controller.user.RoleController;
import ${package}.adapter.converter.PermissionAdapterConverter;
import ${package}.adapter.converter.RoleAdapterConverter;
import ${package}.application.manage.user.PermissionManage;
import ${package}.application.manage.user.RoleManage;
import ${package}.application.result.user.PermissionTreeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RolePermissionControllerTest {

    @Mock RoleManage roleManage;
    @Mock PermissionManage permissionManage;

    @Test
    void exposesRoleAndPermissionHttpContracts() throws Exception {
        when(permissionManage.getPermissionTree(any()))
            .thenReturn(new PermissionTreeResult("u-1", List.of("CLASS_READ")));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new RoleController(roleManage, new RoleAdapterConverter()),
            new PermissionController(permissionManage, new PermissionAdapterConverter())).build();

        mockMvc.perform(post("/api/v1/users/u-1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleCode\":\"STUDENT\"}"))
            .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/roles/STUDENT/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"permissionCode\":\"CLASS_READ\"}"))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/users/u-1/permissions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permissionCodes[0]").value("CLASS_READ"));
    }
}
