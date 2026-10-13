package top.egon.cola.archetype.source.light.adapter.user.controller;

import jakarta.servlet.Filter;
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
import top.egon.cola.archetype.source.light.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.light.application.user.pojo.command.AssignRoleCommand;
import top.egon.cola.archetype.source.light.application.user.pojo.result.UserResult;
import top.egon.cola.archetype.source.light.start.config.NativeHttpSecurityConfiguration;
import top.egon.cola.archetype.source.light.start.config.NativeSecurityContractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@TestPropertySource(properties = "TIANQUAN_SHOUBING_RESOURCE_URI=https://light.example.test")
@Import(NativeSecurityContractTest.SecurityTestBeans.class)
@WebMvcTest(RoleController.class)
@ContextConfiguration(classes = {
        NativeHttpSecurityConfiguration.class,
        RoleController.class,
        UserAdapterConvertorImpl.class,
        TraceIdFilter.class,
        RequestContextFilter.class
})
class RoleControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "roleManageImpl")
    private RoleManage roleManage;

    @Autowired private WebApplicationContext web;
    @Autowired private NativeSecurityContractTest.SecurityFixture security;
    @BeforeEach void verifiedRequestDefaults() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(web)
                .addFilter(web.getBean(TraceIdFilter.class))
                .addFilter(web.getBean("springSecurityFilterChain", Filter.class))
                .defaultRequest(MockMvcRequestBuilders.get("/")
                        .header("Authorization", "Bearer " + security.token("1001", Set.of("egon-cola-source-light:role:assign"))))
                .build();
    }

    @Test
    void assigns_role() throws Exception {
        when(roleManage.assignRole(any())).thenReturn(new UserResult(1001L, "Mario", "mario@example.com", "ACTIVE"));
        mockMvc.perform(post("/api/users/1001/roles")
                        .header("X-Operator-Id", "operator-1")
                        .header("X-Request-Id", "request-1")
                        .contentType("application/json")
                        .content("{\"roleCode\":\"teacher\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<AssignRoleCommand> captor = ArgumentCaptor.forClass(AssignRoleCommand.class);
        verify(roleManage).assignRole(captor.capture());
        assertThat(captor.getValue().roleCode()).isEqualTo("teacher");
        assertThat(captor.getValue().operatorId()).isEqualTo("subject-1001");
    }

    @Test
    void rejects_missing_role_code() throws Exception {
        mockMvc.perform(post("/api/users/1001/roles")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(roleManage, never()).assignRole(any());
    }
}
