package top.egon.cola.component.accessguard.execution.async;

import org.junit.jupiter.api.Test;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompletionStageGuardExecutorTest {

    @Test
    void pendingStageIsReturnedWithoutBlockingAndCompletesLater() throws Exception {
        GuardEngine engine = mock(GuardEngine.class);
        PreparedGuardExecution prepared = admitted(engine, disabledTimeLimit());
        CompletableFuture<String> pending = new CompletableFuture<>();
        AtomicInteger calls = new AtomicInteger();
        GuardInvocation invocation = invocation(() -> {
            calls.incrementAndGet();
            return pending;
        });
        CompletionStageGuardExecutor executor = new CompletionStageGuardExecutor(engine);

        CompletionStage<String> result = executor.guard(invocation);

        assertThat(result.toCompletableFuture()).isNotDone();
        assertThat(calls).hasValue(1);
        verify(prepared, never()).complete(any());
        pending.complete("ok");
        assertThat(result.toCompletableFuture().get(1, TimeUnit.SECONDS)).isEqualTo("ok");
        verify(prepared).complete("ok");
    }

    @Test
    void timeoutIsComposedAndResolvedWithoutAPlatformTimeLimiter() throws Throwable {
        GuardEngine engine = mock(GuardEngine.class);
        PreparedGuardExecution prepared = admitted(engine, enforceTimeout(Duration.ofMillis(20)));
        GuardOutcome timedOut = GuardOutcome.rejected(
                "draw", GuardDecision.TIME_LIMIT_EXCEEDED, "execution", 1L);
        when(prepared.resolveFailure(GuardDecision.TIME_LIMIT_EXCEEDED, "TIME_LIMIT_EXCEEDED"))
                .thenReturn(new GuardExecutionResult<>(CompletableFuture.completedFuture("fallback"), timedOut));
        CompletionStageGuardExecutor executor = new CompletionStageGuardExecutor(engine);

        CompletionStage<String> result = executor.guard(invocation(CompletableFuture::new));

        assertThat(result.toCompletableFuture().get(1, TimeUnit.SECONDS)).isEqualTo("fallback");
        verify(prepared).resolveFailure(GuardDecision.TIME_LIMIT_EXCEEDED, "TIME_LIMIT_EXCEEDED");
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

    private static GuardInvocation invocation(top.egon.cola.component.accessguard.api.GuardedOperation<?> operation) {
        return new GuardInvocation(
                "draw",
                null,
                CompletionStage.class,
                null,
                new Object[0],
                Map.of(),
                GuardEntryType.PROGRAMMATIC,
                GuardInvocationKind.OPERATION,
                operation);
    }
}
