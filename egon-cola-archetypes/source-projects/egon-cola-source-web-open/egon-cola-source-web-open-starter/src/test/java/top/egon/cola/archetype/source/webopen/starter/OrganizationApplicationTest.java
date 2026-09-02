package top.egon.cola.archetype.source.webopen.starter;

import top.egon.cola.archetype.source.webopen.starter.config.async.AsyncConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.AopTestUtils;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.dtp.context.DtpTaskDecorator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = OrganizationApplication.class,
        properties = "spring.profiles.active=test")
class OrganizationApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private ThreadPoolTaskExecutor applicationTaskExecutor;

    @Autowired
    private AsyncConfiguration asyncConfiguration;

    @Autowired
    private DtpTaskDecorator dtpTaskDecorator;

    @Autowired
    private LongIdGenerator idGenerator;

    @Test
    void exposesOneBoundedDtpGovernedExecutorAndMachineId() {
        assertThat(context.getBeansOfType(ThreadPoolTaskExecutor.class))
                .containsOnlyKeys("applicationTaskExecutor");
        assertThat(applicationTaskExecutor.getThreadPoolExecutor().getCorePoolSize())
                .isEqualTo(8);
        assertThat(applicationTaskExecutor.getThreadPoolExecutor().getMaximumPoolSize())
                .isEqualTo(32);
        assertThat(applicationTaskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity())
                .isEqualTo(1000);
        assertThat((ThreadPoolTaskExecutor) AopTestUtils.getTargetObject(
                asyncConfiguration.getAsyncExecutor()))
                .isSameAs(applicationTaskExecutor);
        assertThat(context.getBeansOfType(DtpTaskDecorator.class)).containsOnlyKeys("dtpTaskDecorator");
        assertThat(environment.getProperty("egon.cola.component.id.machine-id", Long.class))
                .isEqualTo(0L);
        assertThat(idGenerator.nextLongId()).isPositive();
        assertThat(environment.getProperty("egon.cola.component.dtp.enabled", Boolean.class))
                .isFalse();
    }
}
