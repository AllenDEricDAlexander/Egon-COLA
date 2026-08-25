package ${package}.start.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import ${package}.start.StudentManagementApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(
        classes = StudentManagementApplication.class,
        properties = {
                "app.integrations.rabbitmq.enabled=false",
                "app.integrations.redis.enabled=false",
                "app.integrations.external-http.enabled=false",
                "dubbo.protocol.port=-1",
                "dubbo.application.qos-enable=false"
        })
class RuntimeConfigurationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exposesOpenApiAndHealthEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Student Management"));
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void serializesInstantAsIsoText() throws Exception {
        assertThat(objectMapper.writeValueAsString(Instant.parse("2026-07-10T00:00:00Z")))
                .isEqualTo("\"2026-07-10T00:00:00Z\"");
    }
}
