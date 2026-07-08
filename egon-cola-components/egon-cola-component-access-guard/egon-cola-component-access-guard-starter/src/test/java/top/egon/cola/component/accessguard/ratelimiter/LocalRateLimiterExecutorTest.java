package top.egon.cola.component.accessguard.ratelimiter;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static top.egon.cola.component.accessguard.annotation.FailStrategy.FAIL_OPEN;
import static top.egon.cola.component.accessguard.annotation.TimeoutExecutorType.THREAD_POOL;
import static top.egon.cola.component.accessguard.annotation.WhiteListMode.GATEKEEPER;

class LocalRateLimiterExecutorTest {

    private final AtomicLong now = new AtomicLong();

    private final LocalRateLimiterExecutor executor = new LocalRateLimiterExecutor(now::get);

    @Test
    void shouldAllowFirstRequestAndRejectSecondWhenCapacityExhausted() {
        AccessGuardRule rule = rule();
        AccessGuardContext context = context();

        assertThat(executor.tryAcquire(rule, context).allowed()).isTrue();
        assertThat(executor.tryAcquire(rule, context).allowed()).isFalse();
    }

    @Test
    void shouldRefillAfterConfiguredInterval() {
        AccessGuardRule rule = rule();
        AccessGuardContext context = context();

        executor.tryAcquire(rule, context);
        now.addAndGet(TimeUnit.SECONDS.toNanos(1));

        assertThat(executor.tryAcquire(rule, context).allowed()).isTrue();
    }

    private AccessGuardContext context() {
        AccessGuardContext context = new AccessGuardContext();
        context.setRuleName("draw-api");
        context.setAccessKeyHash("hash001");
        return context;
    }

    private AccessGuardRule rule() {
        return new AccessGuardRule(
                "draw-api",
                "userId",
                "",
                false,
                List.of(),
                GATEKEEPER,
                true,
                1L,
                1L,
                TimeUnit.SECONDS,
                false,
                0L,
                Duration.ofHours(24),
                false,
                false,
                Duration.ofMillis(350),
                THREAD_POOL,
                false,
                true,
                "",
                "",
                FAIL_OPEN
        );
    }
}
