package top.egon.cola.component.outbox.statemachine;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.StateMachineBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.common.exception.OutboxStateMachineException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class StateMachineExecutionServiceTest {

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
    void returnsOnlyAfterOneAcceptedTransitionHasEnded() throws Exception {
        StateMachine<String, String> machine = machine(context -> true, context -> { });
        StateMachineExecutionService service = new StateMachineExecutionService(validationUtils);

        String target = service.execute(
                machine,
                "SOURCE",
                MessageBuilder.withPayload("MOVE").build(),
                Duration.ofSeconds(1)
        );

        assertThat(target).isEqualTo("TARGET");
    }

    @Test
    void acceptedEventWithRejectedGuardDoesNotReturnThePersistedSourceAsSuccess() throws Exception {
        StateMachine<String, String> machine = machine(context -> false, context -> { });
        StateMachineExecutionService service = new StateMachineExecutionService(validationUtils);

        assertThatThrownBy(() -> service.execute(
                machine,
                "SOURCE",
                MessageBuilder.withPayload("MOVE").build(),
                Duration.ofSeconds(1)
        )).isInstanceOfSatisfying(OutboxStateMachineException.class,
                failure -> {
                    assertThat(failure.getReason()).isEqualTo("OUTBOX_FSM_REJECTED");
                    assertThat(failure.isRetryable()).isFalse();
                });
    }

    @Test
    void actionErrorIsNotACompletedTransition() throws Exception {
        AtomicBoolean actionStarted = new AtomicBoolean();
        StateMachine<String, String> machine = machine(
                context -> true,
                context -> {
                    actionStarted.set(true);
                    throw new IllegalStateException("action failed");
                }
        );
        StateMachineExecutionService service = new StateMachineExecutionService(validationUtils);

        assertThatThrownBy(() -> service.execute(
                machine,
                "SOURCE",
                MessageBuilder.withPayload("MOVE").build(),
                Duration.ofSeconds(1)
        )).isInstanceOfSatisfying(OutboxStateMachineException.class,
                failure -> {
                    assertThat(failure.getReason()).isEqualTo("OUTBOX_FSM_EXECUTION_FAILED");
                    assertThat(failure.isRetryable()).isTrue();
                });
        assertThat(actionStarted.get()).isTrue();
    }

    @Test
    void acceptedResultCompletionErrorIsNotACompletedTransition() throws Exception {
        StateMachine<String, String> machine = spy(machine(context -> true, context -> { }));
        var message = MessageBuilder.withPayload("MOVE").build();
        StateMachineEventResult<String, String> result = StateMachineEventResult.from(
                machine,
                message,
                StateMachineEventResult.ResultType.ACCEPTED,
                Mono.error(new IllegalStateException("completion failed"))
        );
        doReturn(Flux.just(result)).when(machine).sendEvent(any(Mono.class));
        StateMachineExecutionService service = new StateMachineExecutionService(validationUtils);

        assertThatThrownBy(() -> service.execute(machine, "SOURCE", message, Duration.ofSeconds(1)))
                .isInstanceOfSatisfying(OutboxStateMachineException.class,
                        failure -> {
                            assertThat(failure.getReason()).isEqualTo("OUTBOX_FSM_EXECUTION_FAILED");
                            assertThat(failure.isRetryable()).isTrue();
                        });
    }

    @Test
    void stopFailureCannotTurnACompletedTransitionIntoSuccess() throws Exception {
        StateMachine<String, String> machine = machine(context -> true, context -> { });
        AtomicBoolean stopAttempted = new AtomicBoolean();
        StateMachine<String, String> stopFailingMachine = failStop(machine, stopAttempted);
        StateMachineExecutionService service = new StateMachineExecutionService(validationUtils);

        try {
            assertThatThrownBy(() -> service.execute(
                    stopFailingMachine,
                    "SOURCE",
                    MessageBuilder.withPayload("MOVE").build(),
                    Duration.ofSeconds(1)
            )).isInstanceOfSatisfying(OutboxStateMachineException.class,
                    failure -> {
                        assertThat(failure.getReason()).isEqualTo("OUTBOX_FSM_EXECUTION_FAILED");
                        assertThat(failure.isRetryable()).isTrue();
                    });
            assertThat(stopAttempted.get()).isTrue();
        } finally {
            machine.stopReactively().block(Duration.ofSeconds(1));
        }
    }

    @Test
    void timedOutPureActionCannotReturnSuccess() throws Exception {
        CountDownLatch actionStarted = new CountDownLatch(1);
        CountDownLatch releaseAction = new CountDownLatch(1);
        AtomicBoolean actionCompleted = new AtomicBoolean();
        StateMachine<String, String> machine = machine(context -> true, context -> {
            actionStarted.countDown();
            try {
                if (!releaseAction.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("test action release timed out");
                }
                actionCompleted.set(true);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("test action interrupted", exception);
            }
        });
        StateMachineExecutionService service = new StateMachineExecutionService(validationUtils);

        try {
            assertThatThrownBy(() -> service.execute(
                    machine,
                    "SOURCE",
                    MessageBuilder.withPayload("MOVE").build(),
                    Duration.ofSeconds(1)
            )).isInstanceOfSatisfying(OutboxStateMachineException.class,
                    failure -> {
                        assertThat(failure.getReason()).isEqualTo("OUTBOX_FSM_TIMEOUT");
                        assertThat(failure.isRetryable()).isTrue();
                    });
            assertThat(actionStarted.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(actionCompleted).isFalse();
        } finally {
            releaseAction.countDown();
        }
    }

    private static StateMachine<String, String> machine(
            java.util.function.Predicate<org.springframework.statemachine.StateContext<String, String>> guard,
            Action<String, String> action
    ) throws Exception {
        StateMachineBuilder.Builder<String, String> builder = StateMachineBuilder.builder();
        builder.configureConfiguration().withConfiguration().autoStartup(false);
        builder.configureStates()
                .withStates()
                .initial("SOURCE")
                .states(Set.of("SOURCE", "TARGET"));
        builder.configureTransitions()
                .withExternal()
                .source("SOURCE")
                .target("TARGET")
                .event("MOVE")
                .guard(context -> guard.test(context))
                .action(action);
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static StateMachine<String, String> failStop(
            StateMachine<String, String> machine,
            AtomicBoolean stopAttempted
    ) {
        return (StateMachine<String, String>) Proxy.newProxyInstance(
                StateMachine.class.getClassLoader(),
                new Class<?>[]{StateMachine.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("stopReactively")) {
                        stopAttempted.set(true);
                        return Mono.error(new IllegalStateException("stop failed"));
                    }
                    try {
                        return method.invoke(machine, arguments);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }
        );
    }
}
