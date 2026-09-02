package top.egon.cola.archetype.source.lightopen.start.config.async;

import top.egon.cola.archetype.source.lightopen.start.StudentManagementApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(
        classes = StudentManagementApplication.class,
        properties = {
                "app.integrations.rabbitmq.enabled=false",
                "app.integrations.redis.enabled=false",
                "app.integrations.external-http.enabled=false",
                "egon.cola.component.id.machine-id=0",
                "egon.cola.component.dtp.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false"
        })
class AsyncConfigurationTest {

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private ThreadPoolTaskExecutor executor;

    @Test
    void uses_the_single_bounded_application_executor() {
        assertThat(executor).isNotNull();
        assertThat(executor.getThreadPoolExecutor().getCorePoolSize()).isEqualTo(8);
        assertThat(executor.getThreadPoolExecutor().getMaximumPoolSize()).isEqualTo(32);
        assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity())
                .isEqualTo(1000);
    }
}
