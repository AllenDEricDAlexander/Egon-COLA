package top.egon.cola.archetype.source.light.start;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import top.egon.cola.archetype.source.light.start.config.NativeSecurityContractTest;

@ActiveProfiles("test")
@Import(NativeSecurityContractTest.SecurityTestBeans.class)
@SpringBootTest(
        classes = StudentManagementApplication.class,
        properties = {
                "app.integrations.rabbitmq.enabled=false",
                "TIANQUAN_SHOUBING_RESOURCE_URI=https://light.example.test",
                "app.integrations.redis.enabled=false",
                "app.integrations.external-http.enabled=false",
                "egon.cola.component.rpc.enabled=false",
                "egon.cola.component.tianshu.enabled=false"
        })
class StudentManagementApplicationTest extends top.egon.cola.archetype.source.light.support.PersistenceTestSupport {
    @Test
    void contextLoads() {
    }
}
