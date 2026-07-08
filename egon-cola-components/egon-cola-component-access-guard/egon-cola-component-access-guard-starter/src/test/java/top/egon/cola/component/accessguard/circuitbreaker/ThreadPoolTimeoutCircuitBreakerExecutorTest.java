package top.egon.cola.component.accessguard.circuitbreaker;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.reject.RejectResponseInvoker;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static top.egon.cola.component.accessguard.annotation.FailStrategy.FAIL_OPEN;
import static top.egon.cola.component.accessguard.annotation.TimeoutExecutorType.THREAD_POOL;
import static top.egon.cola.component.accessguard.annotation.WhiteListMode.GATEKEEPER;

class ThreadPoolTimeoutCircuitBreakerExecutorTest {

    private final RejectResponseInvoker rejectResponseInvoker = (joinPoint, rule, context, args) -> "timeout";

    @Test
    void shouldReturnBusinessResultBeforeTimeout() throws Throwable {
        ThreadPoolTimeoutCircuitBreakerExecutor executor = new ThreadPoolTimeoutCircuitBreakerExecutor(
                Executors.newSingleThreadExecutor(),
                rejectResponseInvoker
        );

        Object result = executor.execute(joinPoint(() -> "ok"), rule(Duration.ofSeconds(1), false), new AccessGuardContext());

        assertThat(result).isEqualTo("ok");
        executor.shutdown();
    }

    @Test
    void shouldReturnRejectResponseAfterTimeout() throws Throwable {
        ThreadPoolTimeoutCircuitBreakerExecutor executor = new ThreadPoolTimeoutCircuitBreakerExecutor(
                Executors.newSingleThreadExecutor(),
                rejectResponseInvoker
        );
        CountDownLatch latch = new CountDownLatch(1);

        Object result = executor.execute(joinPoint(() -> {
            latch.await();
            return "ok";
        }), rule(Duration.ofMillis(10), false), new AccessGuardContext());

        assertThat(result).isEqualTo("timeout");
        latch.countDown();
        executor.shutdown();
    }

    @Test
    void shouldReturnRejectResponseWhenExecutorRejects() throws Throwable {
        ThreadPoolExecutor rejectedExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        rejectedExecutor.shutdownNow();
        ThreadPoolTimeoutCircuitBreakerExecutor executor = new ThreadPoolTimeoutCircuitBreakerExecutor(
                rejectedExecutor,
                rejectResponseInvoker
        );

        Object result = executor.execute(joinPoint(() -> "ok"), rule(Duration.ofSeconds(1), false), new AccessGuardContext());

        assertThat(result).isEqualTo("timeout");
    }

    @Test
    void shouldPropagateBusinessExceptionsWhenFallbackOnExceptionDisabled() {
        ThreadPoolTimeoutCircuitBreakerExecutor executor = new ThreadPoolTimeoutCircuitBreakerExecutor(
                Executors.newSingleThreadExecutor(),
                rejectResponseInvoker
        );

        assertThatThrownBy(() -> executor.execute(joinPoint(() -> {
            throw new IllegalStateException("boom");
        }), rule(Duration.ofSeconds(1), false), new AccessGuardContext()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        executor.shutdown();
    }

    private AccessGuardRule rule(Duration timeout, boolean fallbackOnException) {
        return new AccessGuardRule(
                "draw-api",
                "userId",
                "",
                false,
                List.of(),
                GATEKEEPER,
                false,
                1L,
                1L,
                TimeUnit.SECONDS,
                false,
                0L,
                Duration.ofHours(24),
                false,
                true,
                timeout,
                THREAD_POOL,
                fallbackOnException,
                true,
                "fallback",
                "",
                FAIL_OPEN
        );
    }

    private ProceedingJoinPoint joinPoint(ThrowingSupplier supplier) {
        return new TestProceedingJoinPoint(supplier);
    }

    interface ThrowingSupplier {

        Object get() throws Throwable;
    }

    static class TestProceedingJoinPoint implements ProceedingJoinPoint {

        private final ThrowingSupplier supplier;

        TestProceedingJoinPoint(ThrowingSupplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public Object proceed() throws Throwable {
            return supplier.get();
        }

        @Override
        public Object proceed(Object[] args) throws Throwable {
            return supplier.get();
        }

        @Override
        public void set$AroundClosure(AroundClosure arc) {
        }

        @Override
        public String toShortString() {
            return "test";
        }

        @Override
        public String toLongString() {
            return "test";
        }

        @Override
        public Object getThis() {
            return this;
        }

        @Override
        public Object getTarget() {
            return this;
        }

        @Override
        public Object[] getArgs() {
            return new Object[0];
        }

        @Override
        public Signature getSignature() {
            return null;
        }

        @Override
        public SourceLocation getSourceLocation() {
            return null;
        }

        @Override
        public String getKind() {
            return Ordered.class.getName();
        }

        @Override
        public StaticPart getStaticPart() {
            return null;
        }
    }
}
