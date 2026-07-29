package top.egon.cola.component.accessguard.execution;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.GuardEntryType;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeLimiterContractTest {

    @Test
    void callerThreadIsObserveOnlyAndExecutesInline() throws Throwable {
        CallerThreadTimeLimiter limiter = new CallerThreadTimeLimiter();
        AtomicInteger calls = new AtomicInteger();

        Object value = limiter.execute(
                invocation(() -> {
                    calls.incrementAndGet();
                    return "ok";
                }),
                config(TimeLimitMode.OBSERVE_ONLY, TimeLimiterType.CALLER_THREAD, Duration.ofMillis(1)));

        assertThat(value).isEqualTo("ok");
        assertThat(calls).hasValue(1);
        assertThatThrownBy(() -> limiter.execute(
                invocation(() -> "never"),
                config(TimeLimitMode.ENFORCE, TimeLimiterType.CALLER_THREAD, Duration.ofMillis(1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void boundedThreadPoolReportsTimeoutAndSaturation() throws Exception {
        try (ThreadPoolTimeLimiter limiter = new ThreadPoolTimeLimiter(
                "guard-test", 1, 1, Duration.ofSeconds(1), 0)) {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            CompletableFuture<Object> running = CompletableFuture.supplyAsync(() -> {
                try {
                    return limiter.execute(
                            invocation(() -> {
                                started.countDown();
                                release.await();
                                return "first";
                            }),
                            config(TimeLimitMode.ENFORCE, TimeLimiterType.THREAD_POOL, Duration.ofSeconds(5)));
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> limiter.execute(
                    invocation(() -> "second"),
                    config(TimeLimitMode.ENFORCE, TimeLimiterType.THREAD_POOL, Duration.ofSeconds(1))))
                    .isInstanceOf(ExecutorRejectedException.class);
            release.countDown();
            assertThat(running.get(5, TimeUnit.SECONDS)).isEqualTo("first");
        }
        try (ThreadPoolTimeLimiter limiter = new ThreadPoolTimeLimiter(
                "guard-timeout-test", 1, 1, Duration.ofSeconds(1), 0)) {
            assertThatThrownBy(() -> limiter.execute(
                    invocation(() -> {
                        new CountDownLatch(1).await();
                        return null;
                    }),
                    config(TimeLimitMode.ENFORCE, TimeLimiterType.THREAD_POOL, Duration.ofMillis(10))))
                    .isInstanceOf(TimeLimitExceededException.class);
        }
    }

    @Test
    void virtualThreadExecutorHasAnExplicitLifecycle() throws Throwable {
        VirtualThreadTimeLimiter limiter = new VirtualThreadTimeLimiter();
        assertThat(limiter.execute(
                invocation(() -> "ok"),
                config(TimeLimitMode.ENFORCE, TimeLimiterType.VIRTUAL_THREAD, Duration.ofSeconds(1))))
                .isEqualTo("ok");

        limiter.close();

        assertThatThrownBy(() -> limiter.execute(
                invocation(() -> "closed"),
                config(TimeLimitMode.ENFORCE, TimeLimiterType.VIRTUAL_THREAD, Duration.ofSeconds(1))))
                .isInstanceOf(ExecutorRejectedException.class);
    }

    private static ExecutionConfig.TimeLimitConfig config(
            TimeLimitMode mode,
            TimeLimiterType type,
            Duration timeout
    ) {
        return new ExecutionConfig.TimeLimitConfig(true, mode, type, timeout, true);
    }

    private static GuardInvocation invocation(top.egon.cola.component.accessguard.api.GuardedOperation<?> operation) {
        return new GuardInvocation(
                "draw", null, Object.class, null, new Object[0], Map.of(),
                GuardEntryType.PROGRAMMATIC, GuardInvocationKind.OPERATION, operation);
    }
}
