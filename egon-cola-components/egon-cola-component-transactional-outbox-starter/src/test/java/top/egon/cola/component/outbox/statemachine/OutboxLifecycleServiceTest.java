package top.egon.cola.component.outbox.statemachine;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.autoconfigure.OutboxStateMachineAutoConfiguration;
import top.egon.cola.component.outbox.autoconfigure.OutboxStateMachineProperties;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxAutoConfiguration;
import top.egon.cola.component.outbox.common.exception.OutboxStateMachineException;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.component.outbox.store.OutboxStatus;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxLifecycleServiceTest {

    private static ValidatorFactory validatorFactory;
    private static ValidationUtils validationUtils;

    @BeforeAll
    static void createValidationFacade() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validationUtils = new ValidationUtils(validatorFactory.getValidator());
    }

    @AfterAll
    static void closeValidationFactory() {
        validatorFactory.close();
    }

    @Test
    void enqueueUsesTheStateMachineTarget() {
        OutboxLifecycleService service = service();

        assertThat(service.evaluate(
                OutboxLifecycleStateMachineFactory.NEW_STATE,
                OutboxLifecycleSignalEnum.ENQUEUE,
                0,
                3
        )).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void retriesAreSeparatedByTheConfiguredAttemptBudget() {
        OutboxLifecycleService service = service();

        assertThat(service.evaluate(
                OutboxStatus.PROCESSING.getMessage(),
                OutboxLifecycleSignalEnum.DELIVERY_RETRYABLE,
                2,
                3
        )).isEqualTo(OutboxStatus.RETRY_WAIT);
        assertThat(service.evaluate(
                OutboxStatus.PROCESSING.getMessage(),
                OutboxLifecycleSignalEnum.DELIVERY_RETRYABLE,
                3,
                3
        )).isEqualTo(OutboxStatus.DEAD);
    }

    @Test
    void everyConfiguredLifecycleSignalUsesTheExpectedStateMachineTarget() {
        OutboxLifecycleService service = service();

        assertThat(service.evaluate(
                OutboxStatus.PENDING.getMessage(), OutboxLifecycleSignalEnum.CLAIM, 0, 3
        )).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(service.evaluate(
                OutboxStatus.RETRY_WAIT.getMessage(), OutboxLifecycleSignalEnum.CLAIM, 1, 3
        )).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(service.evaluate(
                OutboxStatus.PROCESSING.getMessage(), OutboxLifecycleSignalEnum.RECLAIM, 1, 3
        )).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(service.evaluate(
                OutboxStatus.PROCESSING.getMessage(), OutboxLifecycleSignalEnum.DELIVERY_SUCCEEDED, 3, 3
        )).isEqualTo(OutboxStatus.SUCCEEDED);
        assertThat(service.evaluate(
                OutboxStatus.PROCESSING.getMessage(), OutboxLifecycleSignalEnum.SCHEDULE_RETRY, 1, 3
        )).isEqualTo(OutboxStatus.RETRY_WAIT);
        assertThat(service.evaluate(
                OutboxStatus.PROCESSING.getMessage(), OutboxLifecycleSignalEnum.DELIVERY_PERMANENT, 1, 3
        )).isEqualTo(OutboxStatus.DEAD);
    }

    @Test
    void anEventCannotResetAnUnknownOrTerminalSourceState() {
        OutboxLifecycleService service = service();

        assertThatThrownBy(() -> service.evaluate("UNKNOWN", OutboxLifecycleSignalEnum.CLAIM, 0, 3))
                .isInstanceOfSatisfying(OutboxStateMachineException.class,
                        failure -> assertThat(failure.getReason()).isEqualTo("OUTBOX_FSM_REJECTED"));
        assertThatThrownBy(() -> service.evaluate(
                OutboxStatus.SUCCEEDED.getMessage(),
                OutboxLifecycleSignalEnum.CLAIM,
                1,
                3
        )).isInstanceOfSatisfying(OutboxStateMachineException.class,
                failure -> assertThat(failure.getReason()).isEqualTo("OUTBOX_FSM_REJECTED"));
    }

    @Test
    void retryBudgetMustBePositive() {
        assertThatThrownBy(() -> service().evaluate(
                OutboxStatus.PROCESSING.getMessage(),
                OutboxLifecycleSignalEnum.DELIVERY_RETRYABLE,
                -1,
                0
        )).isInstanceOfSatisfying(OutboxStateMachineException.class,
                        failure -> assertThat(failure.getReason()).isEqualTo("OUTBOX_FSM_INPUT_INVALID"));
    }

    @Test
    void autoConfigurationBindsNamedPropertiesAndCreatesTrueSharedAliases() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ConfigurationPropertiesAutoConfiguration.class,
                        ValidationAutoConfiguration.class,
                        TransactionalOutboxAutoConfiguration.class,
                        OutboxStateMachineAutoConfiguration.class
                ))
                .withUserConfiguration(
                        CoreDependencies.class
                )
                .withPropertyValues(
                        "egon.cola.component.transactional-outbox.storage.validate-schema=false",
                        "egon.cola.component.transactional-outbox.polling.enabled=false",
                        "egon.cola.component.transactional-outbox.cleanup.enabled=false",
                        OutboxStateMachineProperties.PREFIX + ".execution-timeout=50ms",
                        OutboxStateMachineProperties.PREFIX + ".business.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("outboxStateMachineExecutionService");
                    assertThat(context).hasBean("outboxLifecycleStateMachineFactory");
                    assertThat(context.getBean("outboxLifecycleService")).isInstanceOf(OutboxLifecycleService.class);
                    assertThat(context.getBean(OutboxStateMachineProperties.class).getExecutionTimeout())
                            .isEqualTo(Duration.ofMillis(50));
                    assertThat(context.getBean("outboxStateMachineClock"))
                            .isSameAs(context.getBean("egonColaMybatisPlusClock"));
                    assertThat(context.getBean("outboxStateMachineObjectMapper"))
                            .isSameAs(context.getBean("objectMapper"));
                });
    }

    @Test
    void autoConfigurationRejectsClaimBudgetLongerThanTheDeliveryLease() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ConfigurationPropertiesAutoConfiguration.class,
                        ValidationAutoConfiguration.class,
                        TransactionalOutboxAutoConfiguration.class,
                        OutboxStateMachineAutoConfiguration.class
                ))
                .withUserConfiguration(CoreDependencies.class)
                .withPropertyValues(
                        "egon.cola.component.transactional-outbox.storage.validate-schema=false",
                        "egon.cola.component.transactional-outbox.polling.enabled=false",
                        "egon.cola.component.transactional-outbox.cleanup.enabled=false",
                        OutboxStateMachineProperties.PREFIX + ".claim-evaluation-timeout=61s"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    private static OutboxLifecycleService service() {
        return new OutboxLifecycleService(
                new OutboxLifecycleStateMachineFactory(),
                new StateMachineExecutionService(validationUtils),
                OutboxStateMachineProperties.builder().build(),
                validationUtils
        );
    }

    @Configuration(proxyBeanMethods = false)
    static class CoreDependencies {

        @Bean
        DataSource dataSource() {
            return org.mockito.Mockito.mock(DataSource.class);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean("egonColaMybatisPlusClock")
        Clock egonColaMybatisPlusClock() {
            return Clock.systemUTC();
        }

        @Bean
        com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper();
        }

        @Bean
        OutboxStore outboxStore() {
            return org.mockito.Mockito.mock(OutboxStore.class);
        }
    }

}
