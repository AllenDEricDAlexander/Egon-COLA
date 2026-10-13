package top.egon.cola.archetype.source.web.adapter.user.controller;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.archetype.source.web.adapter.graphql.NativeGraphQlSecurityContractTest.SecurityFixture;
import top.egon.cola.archetype.source.web.adapter.user.controller.PermissionController;
import top.egon.cola.archetype.source.web.adapter.user.controller.RoleController;
import top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.PermissionAdapterConverter;
import top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.RoleAdapterConverter;
import top.egon.cola.archetype.source.web.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.web.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.web.application.user.pojo.result.PermissionTreeResult;
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

@ExtendWith(MockitoExtension.class)
class RolePermissionControllerTest {
    @TempDir Path temporary;
    private SecurityFixture security;

    @BeforeEach
    void bindVerifiedUser() throws Exception {
        security = new SecurityFixture(temporary);
        SecurityContextHolder.getContext().setAuthentication(security.authenticatedUser("1001", Set.copyOf(security.permissions)));
    }

    @AfterEach
    void closeVerifiedUser() { security.close(); }


    @Mock RoleManage roleManage;
    @Mock PermissionManage permissionManage;

    @Test
    void exposesRoleAndPermissionHttpContracts() throws Exception {
        when(permissionManage.getPermissionTree(any()))
            .thenReturn(new PermissionTreeResult(1001L, List.of("CLASS_READ")));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new RoleController(security.fields, security.properties, roleManage, Mappers.getMapper(RoleAdapterConverter.class)),
            new PermissionController(security.fields, security.properties,
                permissionManage, Mappers.getMapper(PermissionAdapterConverter.class))).build();

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
