package ${package}.adapter.user.controller;

import ${package}.adapter.user.convertor.UserAdapterConvertorImpl;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.manage.UserManage;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {
        UserController.class,
        UserAdapterConvertorImpl.class,
        TraceIdFilter.class,
        RequestContextFilter.class
})
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserManage userManage;

    @Test
    void creates_user_with_request_context() throws Exception {
        when(userManage.create(any())).thenReturn(new UserResult("user-1", "Mario", "mario@example.com", "ACTIVE"));

        mockMvc.perform(post("/api/users")
                        .header("X-Operator-Id", "operator-1")
                        .header("X-Request-Id", "request-1")
                        .contentType("application/json")
                        .content("{\"externalId\":\"external-1\",\"name\":\"Mario\",\"email\":\"mario@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"));

        ArgumentCaptor<CreateUserCommand> captor = ArgumentCaptor.forClass(CreateUserCommand.class);
        verify(userManage).create(captor.capture());
        assertThat(captor.getValue().operatorId()).isEqualTo("operator-1");
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("request-1");
    }

    @Test
    void rejects_invalid_email_before_application() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"externalId\":\"external-1\",\"name\":\"Mario\",\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest());

        verify(userManage, never()).create(any());
    }
}
