package top.egon.cola.archetype.source.serviceopen.starter;

import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import top.egon.cola.component.dtp.context.DtpTaskDecorator;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"dubbo.protocol.port=-1", "dubbo.application.qos-enable=false"})
class EvaluationServiceApplicationTest {

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private ThreadPoolTaskExecutor applicationTaskExecutor;

    @Autowired
    private Executor asyncExecutor;

    @Autowired
    private DtpTaskDecorator dtpTaskDecorator;

    @Autowired
    private Environment environment;

    @Test
    void shouldExposeOneBoundedDtpGovernedApplicationExecutor() {
        assertThat(applicationTaskExecutor.getCorePoolSize()).isEqualTo(8);
        assertThat(applicationTaskExecutor.getMaxPoolSize()).isEqualTo(32);
        assertThat(applicationTaskExecutor.getQueueCapacity()).isEqualTo(1000);
        assertThat(asyncExecutor).isSameAs(applicationTaskExecutor);
        assertThat(dtpTaskDecorator).isNotNull();
        assertThat(environment.getProperty("egon.cola.component.id.machine-id", Integer.class))
                .isEqualTo(0);
        assertThat(environment.getProperty("egon.cola.component.dtp.enabled", Boolean.class))
                .isFalse();
    }
}
