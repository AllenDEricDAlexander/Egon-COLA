package top.egon.cola.archetype.source.webopen.adapter.user.controller;

import top.egon.cola.archetype.source.webopen.adapter.user.controller.PermissionController;
import top.egon.cola.archetype.source.webopen.adapter.user.controller.RoleController;
import top.egon.cola.archetype.source.webopen.adapter.user.converter.PermissionAdapterConverter;
import top.egon.cola.archetype.source.webopen.adapter.user.converter.RoleAdapterConverter;
import top.egon.cola.archetype.source.webopen.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.webopen.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.webopen.application.user.result.PermissionTreeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
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
import top.egon.cola.component.common.id.generator.LongIdGenerator;

@ExtendWith(MockitoExtension.class)
class RolePermissionControllerTest {

    @Mock RoleManage roleManage;
    @Mock PermissionManage permissionManage;

    @Test
    void exposesRoleAndPermissionHttpContracts() throws Exception {
        when(permissionManage.getPermissionTree(any()))
            .thenReturn(new PermissionTreeResult(1001L, List.of("CLASS_READ")));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new RoleController(roleManage, Mappers.getMapper(RoleAdapterConverter.class),
                (LongIdGenerator) () -> 9001L),
            new PermissionController(
                permissionManage, Mappers.getMapper(PermissionAdapterConverter.class),
                (LongIdGenerator) () -> 9002L)).build();

        mockMvc.perform(post("/api/v1/users/1001/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleCode\":\"STUDENT\"}"))
            .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/roles/STUDENT/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"permissionCode\":\"CLASS_READ\"}"))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/users/1001/permissions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permissionCodes[0]").value("CLASS_READ"));
    }
}
