package ${package}.starter;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = OrganizationApplication.class,
        properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class OrganizationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void completesUserAndTeachingFlowThroughRealHttpBoundary() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        MvcResult createdUser = mockMvc.perform(post("/api/v1/users")
                        .headers(adminHeaders("create-user-" + suffix))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Mario\",\"email\":\"mario-" + suffix + "@example.com\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = JsonPath.read(createdUser.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/users/{id}/roles", userId)
                        .headers(adminHeaders("assign-role-" + suffix))
                        .contentType(APPLICATION_JSON)
                        .content("{\"roleCode\":\"STUDENT\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/roles/STUDENT/permissions")
                        .headers(adminHeaders("grant-permission-" + suffix))
                        .contentType(APPLICATION_JSON)
                        .content("{\"permissionCode\":\"CLASS_READ\"}"))
                .andExpect(status().isNoContent());

        String gradeCode = "GRADE_" + suffix.toUpperCase();
        MvcResult createdGrade = mockMvc.perform(post("/api/v1/grades")
                        .headers(adminHeaders("create-grade-" + suffix))
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"" + gradeCode + "\",\"name\":\"Grade One\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String gradeId = JsonPath.read(createdGrade.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/grades/{id}", gradeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(gradeCode));

        MvcResult createdClass = mockMvc.perform(post("/api/v1/school-classes")
                        .headers(adminHeaders("create-class-" + suffix))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Class A\",\"gradeCode\":\"" + gradeCode + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String schoolClassId = JsonPath.read(createdClass.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/school-classes/{id}", schoolClassId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gradeCode").value(gradeCode));

        mockMvc.perform(post("/api/v1/school-classes/{id}/users", schoolClassId)
                        .headers(adminHeaders("assign-class-" + suffix))
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleCodes[0]").value("STUDENT"));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from user_roles where user_id = ?", Integer.class, userId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from role_permissions rp join roles r on r.id = rp.role_id where r.code = ?",
                Integer.class, "STUDENT")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from school_class_users where user_id = ? and school_class_id = ?",
                Integer.class, userId, schoolClassId)).isEqualTo(1);
    }

    private static HttpHeaders adminHeaders(String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Actor-Id", "admin-1");
        headers.set("X-Actor-Roles", "ORGANIZATION_ADMIN,TEACHING_ADMIN");
        headers.set("X-Trace-Id", "flow-test");
        headers.set("Idempotency-Key", idempotencyKey);
        return headers;
    }
}
