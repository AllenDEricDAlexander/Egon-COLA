package ${package}.start;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(
        classes = StudentManagementApplication.class,
        properties = {
                "app.integrations.rabbitmq.enabled=false",
                "app.integrations.redis.enabled=false",
                "app.integrations.external-http.enabled=false",
                "dubbo.protocol.port=-1",
                "dubbo.application.qos-enable=false"
        })
class StudentManagementApplicationTest {
    @Test
    void contextLoads() {
    }
}
