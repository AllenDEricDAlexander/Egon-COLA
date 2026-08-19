package top.egon.cola.component.outbox.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;
import top.egon.cola.component.outbox.aop.TransactionalMessageAop;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.dispatch.OutboxDispatcher;
import top.egon.cola.component.outbox.dispatch.OutboxPoller;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.component.outbox.transaction.OutboxAfterCommitBuffer;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TransactionalOutboxAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    OutboxMetricsAutoConfiguration.class,
                    TransactionalOutboxAutoConfiguration.class,
                    OutboxHttpAutoConfiguration.class,
                    OutboxRabbitAutoConfiguration.class
            ));

    @Test
    void shouldBackOffWithoutDataSource() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(TransactionalOutbox.class)
                .doesNotHaveBean(OutboxDispatcher.class));
    }

    @Test
    void shouldCreateOneCoreRuntimeForUniqueInfrastructure() {
        configuredContext()
                .run(context -> assertThat(context)
                        .hasSingleBean(TransactionalOutbox.class)
                        .hasSingleBean(OutboxDispatcher.class)
                        .hasSingleBean(OutboxAfterCommitBuffer.class)
                        .hasSingleBean(TransactionalMessageAop.class));
    }

    @Test
    void shouldDisableEveryRuntimeBean() {
        configuredContext()
                .withPropertyValues(
                        "egon.cola.component.transactional-outbox.enabled=false"
                )
                .run(context -> assertThat(context)
                        .doesNotHaveBean(TransactionalOutbox.class)
                        .doesNotHaveBean(OutboxDispatcher.class)
                        .doesNotHaveBean(OutboxPoller.class));
    }

    @Test
    void shouldBackOffAnnotationAdvisorOnly() {
        configuredContext()
                .withPropertyValues(
                        "egon.cola.component.transactional-outbox.annotation.enabled=false"
                )
                .run(context -> assertThat(context)
                        .hasSingleBean(TransactionalOutbox.class)
                        .doesNotHaveBean(TransactionalMessageAop.class));
    }

    @Test
    void shouldBackOffForCustomTransactionalOutbox() {
        TransactionalOutbox custom = mock(TransactionalOutbox.class);
        configuredContext()
                .withBean(TransactionalOutbox.class, () -> custom)
                .run(context -> assertThat(context)
                        .getBean(TransactionalOutbox.class)
                        .isSameAs(custom));
    }

    private ApplicationContextRunner configuredContext() {
        return contextRunner
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .withBean(
                        PlatformTransactionManager.class,
                        () -> mock(PlatformTransactionManager.class)
                )
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(OutboxStore.class, () -> mock(OutboxStore.class))
                .withPropertyValues(
                        "egon.cola.component.transactional-outbox.storage.validate-schema=false",
                        "egon.cola.component.transactional-outbox.polling.enabled=false"
                );
    }
}
