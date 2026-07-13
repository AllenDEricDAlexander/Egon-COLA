package ${package}.adapter;

import ${package}.adapter.controller.user.UserController;
import ${package}.adapter.converter.UserAdapterConverter;
import ${package}.application.user.manage.UserManage;
import ${package}.application.user.result.UserDetailResult;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserManage userManage;

    @Test
    void createsUserWithDirectVoAndLocation() throws Exception {
        when(userManage.createUser(any())).thenReturn(
            new UserDetailResult("u-1", "Mario", "mario@example.com", "ACTIVE", List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new UserController(userManage, new UserAdapterConverter())).build();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Mario\",\"email\":\"mario@example.com\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/users/u-1"))
            .andExpect(jsonPath("$.email").value("mario@example.com"));
    }
}
