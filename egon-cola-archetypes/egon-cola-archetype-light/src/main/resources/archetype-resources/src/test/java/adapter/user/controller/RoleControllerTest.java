package ${package}.adapter.user.controller;

import ${package}.adapter.user.convertor.UserAdapterConvertorImpl;
import ${package}.application.user.command.AssignRoleCommand;
import ${package}.application.user.manage.RoleManage;
import ${package}.application.user.result.UserResult;
import ${package}.adapter.filter.RequestContextFilter;
import ${package}.adapter.filter.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleController.class)
@ContextConfiguration(classes = {
        RoleController.class,
        UserAdapterConvertorImpl.class,
        TraceIdFilter.class,
        RequestContextFilter.class
})
class RoleControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleManage roleManage;

    @Test
    void assigns_role() throws Exception {
        when(roleManage.assignRole(any())).thenReturn(new UserResult("user-1", "Mario", "mario@example.com", "ACTIVE"));
        mockMvc.perform(post("/api/users/user-1/roles")
                        .header("X-Operator-Id", "operator-1")
                        .header("X-Request-Id", "request-1")
                        .contentType("application/json")
                        .content("{\"roleCode\":\"teacher\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<AssignRoleCommand> captor = ArgumentCaptor.forClass(AssignRoleCommand.class);
        verify(roleManage).assignRole(captor.capture());
        assertThat(captor.getValue().roleCode()).isEqualTo("teacher");
        assertThat(captor.getValue().operatorId()).isEqualTo("operator-1");
    }

    @Test
    void rejects_missing_role_code() throws Exception {
        mockMvc.perform(post("/api/users/user-1/roles")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(roleManage, never()).assignRole(any());
    }
}
