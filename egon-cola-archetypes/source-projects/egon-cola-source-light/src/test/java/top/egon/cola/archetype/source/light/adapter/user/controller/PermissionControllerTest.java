package top.egon.cola.archetype.source.light.adapter.user.controller;

import jakarta.servlet.Filter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContextFilter;
import top.egon.cola.archetype.source.light.adapter.filter.TraceIdFilter;
import top.egon.cola.archetype.source.light.adapter.user.pojo.convertor.UserAdapterConvertorImpl;
import top.egon.cola.archetype.source.light.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.light.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.light.application.user.pojo.result.PermissionDetailResult;
import top.egon.cola.archetype.source.light.application.user.pojo.result.PermissionResult;
import top.egon.cola.archetype.source.light.start.config.NativeHttpSecurityConfiguration;
import top.egon.cola.archetype.source.light.start.config.NativeSecurityContractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@TestPropertySource(properties = "TIANQUAN_SHOUBING_RESOURCE_URI=https://light.example.test")
@Import(NativeSecurityContractTest.SecurityTestBeans.class)
@WebMvcTest(PermissionController.class)
@ContextConfiguration(classes = {
        NativeHttpSecurityConfiguration.class,
        PermissionController.class,
        UserAdapterConvertorImpl.class,
        TraceIdFilter.class,
        RequestContextFilter.class
})
class PermissionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "permissionManageImpl")
    private PermissionManage permissionManage;

    @Autowired private WebApplicationContext web;
    @Autowired private NativeSecurityContractTest.SecurityFixture security;
    @BeforeEach void verifiedRequestDefaults() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(web)
                .addFilter(web.getBean(TraceIdFilter.class))
                .addFilter(web.getBean("springSecurityFilterChain", Filter.class))
                .defaultRequest(MockMvcRequestBuilders.get("/")
                        .header("Authorization", "Bearer " + security.token("1001", Set.of("egon-cola-source-light:permission:grant", "egon-cola-source-light:permission:read"))))
                .build();
    }

    @Test
    void grants_permission() throws Exception {
        when(permissionManage.grantPermission(any())).thenReturn(new PermissionResult("teacher", "course:read", "GRANTED"));
        mockMvc.perform(post("/api/roles/teacher/permissions")
                        .header("X-Operator-Id", "operator-1")
                        .header("X-Request-Id", "request-1")
                        .contentType("application/json")
                        .content("{\"permissionCode\":\"course:read\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<GrantPermissionCommand> captor = ArgumentCaptor.forClass(GrantPermissionCommand.class);
        verify(permissionManage).grantPermission(captor.capture());
        assertThat(captor.getValue().permissionCode()).isEqualTo("course:read");
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("request-1");
    }

    @Test
    void rejects_missing_permission_code() throws Exception {
        mockMvc.perform(post("/api/roles/teacher/permissions")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(permissionManage, never()).grantPermission(any());
    }

    @Test
    void lists_permissions_for_user() throws Exception {
        when(permissionManage.getByUser(any())).thenReturn(List.of(
                new PermissionDetailResult("course:read", "Read courses")));

        mockMvc.perform(get("/api/users/1001/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("course:read"));
    }
}
