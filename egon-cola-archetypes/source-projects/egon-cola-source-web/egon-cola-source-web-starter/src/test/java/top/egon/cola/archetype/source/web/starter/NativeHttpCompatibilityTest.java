package top.egon.cola.archetype.source.web.starter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
import top.egon.cola.archetype.source.web.application.user.result.UserDetailResult;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = OrganizationApplication.class)
class NativeHttpCompatibilityTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserManage userManage;

    @Test
    void retains_created_status_location_and_response_fields_without_an_added_login_requirement() throws Exception {
        when(userManage.createUser(any())).thenReturn(new UserDetailResult(1001L, "Mario", "mario@example.com", "ACTIVE", List.of("STUDENT")));
        mockMvc.perform(post("/api/v1/users").header("Idempotency-Key", "native-http-check")
                        .contentType("application/json").content("{\"name\":\"Mario\",\"email\":\"mario@example.com\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/users/1001"))
                .andExpect(jsonPath("$.id").value("1001")).andExpect(jsonPath("$.roleCodes[0]").value("STUDENT"));
        mockMvc.perform(post("/api/v1/users").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void keeps_openapi_info_and_public_business_paths() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Student Management Organization API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1/users'].post").exists());
    }
}
