package ${package}.adapter.user.controller;

import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.user.manage.PermissionManage;
import ${package}.application.user.result.PermissionResult;
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

@WebMvcTest(PermissionController.class)
@ContextConfiguration(classes = {PermissionController.class, TraceIdFilter.class, RequestContextFilter.class})
class PermissionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PermissionManage permissionManage;

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
}
