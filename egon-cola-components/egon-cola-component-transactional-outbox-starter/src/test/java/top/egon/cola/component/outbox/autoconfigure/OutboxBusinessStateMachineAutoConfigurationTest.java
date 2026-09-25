package top.egon.cola.component.outbox.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.transaction.PlatformTransactionManager;
import top.egon.cola.component.outbox.common.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.statemachine.BusinessStateMachineDeliveryHandler;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineContextExecutor;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineDefinitionStrategy;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineRepository;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineService;
import top.egon.cola.component.outbox.store.OutboxStore;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxBusinessStateMachineAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    OutboxMetricsAutoConfiguration.class,
                    TransactionalOutboxAutoConfiguration.class,
                    OutboxStateMachineAutoConfiguration.class,
                    OutboxBusinessStateMachineAutoConfiguration.class))
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean("egonColaMybatisPlusClock", Clock.class, Clock::systemUTC)
            .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(OutboxStore.class, () -> mock(OutboxStore.class))
            .withPropertyValues(
                    "egon.cola.component.transactional-outbox.storage.validate-schema=false",
                    "egon.cola.component.transactional-outbox.polling.enabled=false"
            );

    @Test
    void businessAdapterIsAbsentByDefault() {
        contextRunner.run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(BusinessStateMachineService.class)
                .doesNotHaveBean(BusinessStateMachineDeliveryHandler.class));
    }

    @Test
    void enabledBusinessAdapterFailsFastWithoutDefinitionOrContextExecutor() {
        contextRunner
                .withPropertyValues("egon.cola.component.transactional-outbox.state-machine.business.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(OutboxConfigurationException.class);
                });
    }

    @Test
    void enabledBusinessAdapterRejectsMultipleContextExecutors() {
        contextRunner
                .withPropertyValues("egon.cola.component.transactional-outbox.state-machine.business.enabled=true")
                .withBean("orderPaymentDefinition", BusinessStateMachineDefinitionStrategy.class,
                        TestDefinitionStrategy::new)
                .withBean("firstContextExecutor", BusinessStateMachineContextExecutor.class,
                        () -> mock(BusinessStateMachineContextExecutor.class))
                .withBean("secondContextExecutor", BusinessStateMachineContextExecutor.class,
                        () -> mock(BusinessStateMachineContextExecutor.class))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(OutboxConfigurationException.class);
                });
    }

    @Test
    void enabledBusinessAdapterRegistersTheNamedServiceAndHandlerForValidSpi() {
        BusinessStateMachineDefinitionStrategy definition = new TestDefinitionStrategy();
        contextRunner
                .withPropertyValues("egon.cola.component.transactional-outbox.state-machine.business.enabled=true")
                .withBean("orderPaymentDefinition", BusinessStateMachineDefinitionStrategy.class, () -> definition)
                .withBean("outboxBusinessStateMachineContextExecutor", BusinessStateMachineContextExecutor.class,
                        () -> mock(BusinessStateMachineContextExecutor.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasBean("outboxBusinessStateMachineDefinitions")
                        .hasBean("outboxBusinessStateMachineTransaction")
                        .hasBean("outboxBusinessStateMachineService")
                        .hasSingleBean(BusinessStateMachineDeliveryHandler.class)
                        .hasSingleBean(DeliveryHandler.class));
    }

    @Test
    void enabledBusinessAdapterRejectsDuplicateDestinations() {
        contextRunner
                .withPropertyValues("egon.cola.component.transactional-outbox.state-machine.business.enabled=true")
                .withBean("firstDefinition", BusinessStateMachineDefinitionStrategy.class,
                        TestDefinitionStrategy::new)
                .withBean("secondDefinition", BusinessStateMachineDefinitionStrategy.class,
                        TestDefinitionStrategy::new)
                .withBean("outboxBusinessStateMachineContextExecutor", BusinessStateMachineContextExecutor.class,
                        () -> mock(BusinessStateMachineContextExecutor.class))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(OutboxConfigurationException.class);
                });
    }

    @Test
    void enabledBusinessAdapterRejectsAFactoryThatReusesItsMachine() throws Exception {
        TestDefinitionStrategy definition = new TestDefinitionStrategy(true);
        contextRunner
                .withPropertyValues("egon.cola.component.transactional-outbox.state-machine.business.enabled=true")
                .withBean("reusedDefinition", BusinessStateMachineDefinitionStrategy.class, () -> definition)
                .withBean("outboxBusinessStateMachineContextExecutor", BusinessStateMachineContextExecutor.class,
                        () -> mock(BusinessStateMachineContextExecutor.class))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(OutboxConfigurationException.class);
                });
    }

    private static final class TestDefinitionStrategy implements BusinessStateMachineDefinitionStrategy {

        private final StateMachineFactory<String, String> factory;
        private final String destination;

        private TestDefinitionStrategy() {
            this("order-payment:1", false);
        }

        private TestDefinitionStrategy(boolean reuseMachine) throws Exception {
            this("order-payment:1", reuseMachine);
        }

        private TestDefinitionStrategy(String destination, boolean reuseMachine) {
            this.destination = destination;
            try {
                factory = mock(StateMachineFactory.class);
                if (reuseMachine) {
                    var machine = testMachine();
                    when(factory.getStateMachine()).thenReturn(machine);
                } else {
                    when(factory.getStateMachine()).thenAnswer(invocation -> testMachine());
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to create test business state machine", exception);
            }
        }

        private static org.springframework.statemachine.StateMachine<String, String> testMachine() throws Exception {
            StateMachineBuilder.Builder<String, String> builder = StateMachineBuilder.builder();
            builder.configureConfiguration().withConfiguration().autoStartup(false);
            builder.configureStates().withStates().initial("WAITING").states(Set.of("WAITING", "DONE"));
            builder.configureTransitions().withExternal().source("WAITING").target("DONE").event("COMPLETE");
            return builder.build();
        }

        @Override
        public String destination() {
            return destination;
        }

        @Override
        public StateMachineFactory<String, String> stateMachineFactory() {
            return factory;
        }

        @Override
        public BusinessStateMachineRepository repository() {
            return mock(BusinessStateMachineRepository.class);
        }

        @Override
        public void validateEvent(top.egon.cola.component.outbox.statemachine.BusinessStateMachineEvent event) {
        }
    }
}
