package top.egon.cola.component.accessguard.execution.reactive;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.core.GuardEntryType;
import top.egon.cola.component.accessguard.core.GuardExecutionResult;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.accessguard.core.PreparedGuardExecution;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReactorGuardExecutorTest {

    @Test
    void monoDoesNotEvaluateOrInvokeBeforeSubscription() throws Throwable {
        GuardEngine engine = mock(GuardEngine.class);
        PreparedGuardExecution prepared = admitted(engine, disabledTimeLimit());
        GuardInvocation invocation = invocation(Mono.class, () -> Mono.just("ok"));
        ReactorGuardExecutor executor = new ReactorGuardExecutor(engine);

        Mono<String> result = cast(executor.guard(invocation, Mono.class));

        verifyNoInteractions(engine);
        StepVerifier.create(result).expectNext("ok").verifyComplete();
        verify(engine).prepare(any());
        verify(prepared).complete("ok");
    }

    @Test
    void reactiveTimeoutUsesOperatorAndPreservesDecision() {
        GuardEngine engine = mock(GuardEngine.class);
        PreparedGuardExecution prepared = admitted(engine, enforceTimeout(Duration.ofSeconds(1)));
        GuardOutcome timedOut = GuardOutcome.rejected(
                "draw", GuardDecision.TIME_LIMIT_EXCEEDED, "execution", 1L);
        try {
            when(prepared.resolveFailure(GuardDecision.TIME_LIMIT_EXCEEDED, "TIME_LIMIT_EXCEEDED"))
                    .thenThrow(new AccessGuardRejectedException(timedOut));
        } catch (Throwable throwable) {
            throw new AssertionError(throwable);
        }
        ReactorGuardExecutor executor = new ReactorGuardExecutor(engine);

        StepVerifier.withVirtualTime(() -> cast(executor.guard(
                        invocation(Mono.class, Mono::never), Mono.class)))
                .thenAwait(Duration.ofSeconds(1))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(AccessGuardRejectedException.class);
                    assertThat(((AccessGuardRejectedException) error).outcome().decision())
                            .isEqualTo(GuardDecision.TIME_LIMIT_EXCEEDED);
                })
                .verify();
    }

    @Test
    void rejectedFluxKeepsFallbackShapeAndDoesNotInvokeBusiness() throws Throwable {
        GuardEngine engine = mock(GuardEngine.class);
        PreparedGuardExecution prepared = mock(PreparedGuardExecution.class);
        when(prepared.admitted()).thenReturn(false);
        when(prepared.resolveAdmission()).thenReturn(new GuardExecutionResult<>(
                Flux.just("fallback-1", "fallback-2"), GuardOutcome.allowed("draw", 1L)));
        when(engine.prepare(any())).thenReturn(prepared);
        GuardInvocation invocation = invocation(Flux.class, () -> {
            throw new AssertionError("business must not run");
        });
        ReactorGuardExecutor executor = new ReactorGuardExecutor(engine);

        Flux<String> result = cast(executor.guard(invocation, Flux.class));

        StepVerifier.create(result).expectNext("fallback-1", "fallback-2").verifyComplete();
    }

    @Test
    void businessExceptionIsResolvedWithItsOriginalDecision() {
        GuardEngine engine = mock(GuardEngine.class);
        PreparedGuardExecution prepared = admitted(engine, disabledTimeLimit());
        GuardOutcome failed = GuardOutcome.rejected(
                "draw", GuardDecision.BUSINESS_EXCEPTION, "execution", 1L);
        try {
            when(prepared.resolveFailure(GuardDecision.BUSINESS_EXCEPTION, "BUSINESS_EXCEPTION"))
                    .thenThrow(new AccessGuardRejectedException(failed));
        } catch (Throwable throwable) {
            throw new AssertionError(throwable);
        }
        ReactorGuardExecutor executor = new ReactorGuardExecutor(engine);

        Mono<String> result = cast(executor.guard(
                invocation(Mono.class, () -> Mono.error(new IllegalStateException("boom"))), Mono.class));

        StepVerifier.create(result)
                .expectErrorSatisfies(error -> assertThat(((AccessGuardRejectedException) error)
                        .outcome().decision()).isEqualTo(GuardDecision.BUSINESS_EXCEPTION))
                .verify();
    }

    @Test
    void fluxCancellationFinalizesOnlyAtTheTerminalSignal() {
        GuardEngine engine = mock(GuardEngine.class);
        PreparedGuardExecution prepared = admitted(engine, disabledTimeLimit());
        ReactorGuardExecutor executor = new ReactorGuardExecutor(engine);
        Flux<Long> result = cast(executor.guard(
                invocation(Flux.class, () -> Flux.interval(Duration.ofSeconds(1))), Flux.class));

        verify(prepared, never()).cancel();
        StepVerifier.withVirtualTime(() -> result)
                .thenAwait(Duration.ofSeconds(1))
                .expectNext(0L)
                .thenCancel()
                .verify();
        verify(prepared).cancel();
    }

    private static PreparedGuardExecution admitted(GuardEngine engine, ExecutionConfig.TimeLimitConfig timeLimit) {
        PreparedGuardExecution prepared = mock(PreparedGuardExecution.class);
        when(prepared.admitted()).thenReturn(true);
        when(prepared.execution()).thenReturn(new ExecutionConfig(
                timeLimit,
                new ExecutionConfig.RejectionConfig(RejectionMode.THROW, "", "")));
        when(prepared.complete(any())).thenAnswer(invocation -> new GuardExecutionResult<>(
                invocation.getArgument(0), GuardOutcome.allowed("draw", 1L)));
        when(engine.prepare(any())).thenReturn(prepared);
        return prepared;
    }

    private static ExecutionConfig.TimeLimitConfig disabledTimeLimit() {
        return new ExecutionConfig.TimeLimitConfig(
                false, TimeLimitMode.DISABLED, TimeLimiterType.CALLER_THREAD, Duration.ofSeconds(1), true);
    }

    private static ExecutionConfig.TimeLimitConfig enforceTimeout(Duration timeout) {
        return new ExecutionConfig.TimeLimitConfig(
                true, TimeLimitMode.ENFORCE, TimeLimiterType.THREAD_POOL, timeout, true);
    }

    private static GuardInvocation invocation(
            Class<?> returnType,
            top.egon.cola.component.accessguard.api.GuardedOperation<?> operation
    ) {
        return new GuardInvocation(
                "draw",
                null,
                returnType,
                null,
                new Object[0],
                Map.of(),
                GuardEntryType.PROGRAMMATIC,
                GuardInvocationKind.OPERATION,
                operation);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }
}
