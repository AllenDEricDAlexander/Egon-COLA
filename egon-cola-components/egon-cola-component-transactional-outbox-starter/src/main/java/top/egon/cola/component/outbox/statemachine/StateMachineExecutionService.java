package top.egon.cola.component.outbox.statemachine;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.access.StateMachineAccess;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.support.DefaultExtendedState;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.transition.Transition;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.common.exception.OutboxStateMachineException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Executes one fresh state machine and returns only after its trigger has completed. */
@Slf4j
@Validated
@RequiredArgsConstructor
public class StateMachineExecutionService {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    public <S, E> S execute(
            @NotNull StateMachine<S, E> machine,
            @NotNull S persistedState,
            @NotNull Message<E> message,
            @NotNull Duration timeout
    ) {
        if (persistedState == null || message == null || message.getPayload() == null
                || machine == null || timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw rejected("OUTBOX_FSM_INPUT_INVALID", "Invalid outbox state-machine execution input");
        }
        validationUtils.validate(machine, jakarta.validation.groups.Default.class);
        long timeoutNanos;
        try {
            timeoutNanos = timeout.toNanos();
        } catch (ArithmeticException exception) {
            throw rejected("OUTBOX_FSM_INPUT_INVALID", "Outbox state-machine timeout is too large");
        }
        long startedAt = System.nanoTime();
        long totalDeadline = saturatingAdd(startedAt, timeoutNanos);
        long cleanupBudget = Math.min(Duration.ofMillis(25).toNanos(), Math.max(1L, timeoutNanos / 10));
        long operationDeadline = Math.max(startedAt, totalDeadline - cleanupBudget);

        AtomicInteger completedTransitions = new AtomicInteger();
        AtomicInteger startedTransitions = new AtomicInteger();
        AtomicReference<TransitionResult<S, E>> completedTransition = new AtomicReference<>();
        AtomicReference<Exception> stateMachineFailure = new AtomicReference<>();
        StateMachineListenerAdapter<S, E> listener = new StateMachineListenerAdapter<>() {
            @Override
            public void transitionStarted(Transition<S, E> transition) {
                if (matches(transition, message)) {
                    startedTransitions.incrementAndGet();
                }
            }

            @Override
            public void transitionEnded(Transition<S, E> transition) {
                if (!matches(transition, message)) {
                    return;
                }
                completedTransitions.incrementAndGet();
                completedTransition.set(new TransitionResult<>(
                        transition.getSource().getId(),
                        transition.getTarget().getId()
                ));
            }

            @Override
            public void stateMachineError(StateMachine<S, E> stateMachine, Exception exception) {
                stateMachineFailure.compareAndSet(null, exception);
            }
        };
        boolean listenerAttached = false;
        boolean startAttempted = false;
        S finalState = null;
        RuntimeException failure = null;
        try {
            DefaultStateMachineContext<S, E> stateContext = new DefaultStateMachineContext<>(
                    persistedState,
                    null,
                    Map.copyOf(message.getHeaders()),
                    new DefaultExtendedState(Map.of())
            );
            List<StateMachineAccess<S, E>> regions = machine.getStateMachineAccessor().withAllRegions();
            if (regions == null || regions.size() != 1) {
                throw rejected("OUTBOX_FSM_REGION_INVALID", "Outbox state machine must have one region");
            }
            for (StateMachineAccess<S, E> region : regions) {
                block(region.resetStateMachineReactively(stateContext), operationDeadline);
            }
            startAttempted = true;
            block(machine.startReactively(), operationDeadline);
            machine.addStateListener(listener);
            listenerAttached = true;

            List<StateMachineEventResult<S, E>> results = block(
                    machine.sendEvent(Mono.just(message)).collectList(),
                    operationDeadline
            );
            if (results == null || results.isEmpty()) {
                throw rejected("OUTBOX_FSM_REJECTED", "Outbox state-machine event was not accepted");
            }
            for (StateMachineEventResult<S, E> result : results) {
                block(result.complete(), operationDeadline);
            }
            if (machine.hasStateMachineError() || stateMachineFailure.get() != null) {
                throw failed("Outbox state machine entered an error state", stateMachineFailure.get());
            }
            if (results.size() != 1
                    || results.getFirst().getResultType() != StateMachineEventResult.ResultType.ACCEPTED) {
                if (startedTransitions.get() > 0) {
                    throw failed("Outbox state machine failed after starting a transition", stateMachineFailure.get());
                }
                throw rejected("OUTBOX_FSM_REJECTED", "Outbox state-machine event was denied or deferred");
            }
            TransitionResult<S, E> transition = completedTransition.get();
            if (completedTransitions.get() == 0 || transition == null) {
                throw rejected("OUTBOX_FSM_REJECTED", "Outbox state machine completed no matching transition");
            }
            if (completedTransitions.get() != 1
                    || startedTransitions.get() != 1
                    || !persistedState.equals(transition.source())
                    || machine.getState() == null
                    || !machine.getState().getId().equals(transition.target())) {
                throw rejected("OUTBOX_FSM_EXECUTION_FAILED", "Outbox state machine completed an inconsistent transition");
            }
            finalState = transition.target();
        } catch (RuntimeException exception) {
            failure = classify(exception, operationDeadline);
        } finally {
            if (listenerAttached) {
                machine.removeStateListener(listener);
            }
            if (startAttempted) {
                try {
                    block(machine.stopReactively(), totalDeadline);
                } catch (RuntimeException stopFailure) {
                    OutboxStateMachineException classifiedStop = classify(stopFailure, totalDeadline);
                    if (failure == null) {
                        failure = classifiedStop;
                    } else {
                        failure.addSuppressed(classifiedStop);
                    }
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        if (log.isTraceEnabled()) {
            log.trace("Outbox state-machine transition completed: {} -> {}", persistedState, finalState);
        }
        return finalState;
    }

    private static <T> T block(Mono<T> publisher, long deadline) {
        return publisher.subscribeOn(Schedulers.boundedElastic()).block(remaining(deadline));
    }

    private static <S, E> boolean matches(Transition<S, E> transition, Message<E> message) {
        return transition.getTrigger() != null
                && message.getPayload().equals(transition.getTrigger().getEvent());
    }

    private static Duration remaining(long deadline) {
        long nanos = deadline - System.nanoTime();
        if (nanos <= 0) {
            throw new OutboxStateMachineException(
                    "OUTBOX_FSM_TIMEOUT",
                    true,
                    "Outbox state-machine execution timed out"
            );
        }
        return Duration.ofNanos(nanos);
    }

    private static long saturatingAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    private static OutboxStateMachineException classify(RuntimeException exception, long deadline) {
        if (exception instanceof OutboxStateMachineException stateMachineException) {
            return stateMachineException;
        }
        if (deadline - System.nanoTime() <= 0) {
            return new OutboxStateMachineException(
                    "OUTBOX_FSM_TIMEOUT",
                    true,
                    "Outbox state-machine execution timed out",
                    exception
            );
        }
        return new OutboxStateMachineException(
                "OUTBOX_FSM_EXECUTION_FAILED",
                true,
                "Outbox state-machine execution failed",
                exception
        );
    }

    private static OutboxStateMachineException rejected(String reason, String message) {
        return new OutboxStateMachineException(reason, false, message);
    }

    private static OutboxStateMachineException failed(String message, Throwable cause) {
        return new OutboxStateMachineException("OUTBOX_FSM_EXECUTION_FAILED", true, message, cause);
    }

    private record TransitionResult<S, E>(S source, S target) {
    }
}
