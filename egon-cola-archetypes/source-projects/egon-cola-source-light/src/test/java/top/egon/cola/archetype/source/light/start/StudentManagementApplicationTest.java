package top.egon.cola.archetype.source.light.start;

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
                "egon.cola.component.rpc.enabled=false",
                "egon.cola.component.ddc.enabled=false"
        })
class StudentManagementApplicationTest {
    @Test
    void contextLoads() {
    }
}
