package top.egon.cola.component.dtp.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.thread.TraceRouteCallable;
import top.egon.cola.component.common.trace.thread.TraceRouteRunnable;
import top.egon.cola.component.common.trace.thread.TraceRouteSupplier;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @ClassName: DtpContextPropagationTest
 * @description: DTP 上下文传递测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-06Month-29Day
 * @Version: 1.0
 */
public class DtpContextPropagationTest {

    private static final String BUSINESS_KEY = "businessId";

    @AfterEach
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void test_wrappersShouldReuseCommonTraceTemplates() {
        assertThat(DtpRunnable.wrap(() -> {
        })).isInstanceOf(TraceRouteRunnable.class);
        assertThat(DtpCallable.wrap(() -> null)).isInstanceOf(TraceRouteCallable.class);
        assertThat(DtpSupplier.wrap(() -> null)).isInstanceOf(TraceRouteSupplier.class);
    }

    @Test
    public void test_runnableShouldPropagateCompleteContextAndRestoreWorkerMdc() {
        TraceContext expected = TraceContext.root("request-runnable")
                .withSource("order-service", "order-01");
        AtomicReference<TraceContext> actual = new AtomicReference<>();
        Runnable wrapped;
        try (TraceContext.Scope ignored = expected.open()) {
            MDC.put(BUSINESS_KEY, "order-001");
            wrapped = DtpRunnable.wrap(() -> actual.set(TraceContext.capture()));
        }

        MDC.put("workerKey", "worker-value");
        wrapped.run();

        assertCompleteContext(actual.get(), expected, "order-001");
        assertThat(MDC.get("workerKey")).isEqualTo("worker-value");
        assertThat(TraceContext.current()).isEmpty();
    }

    @Test
    public void test_callableAndSupplierShouldPropagateCompleteContext() throws Exception {
        TraceContext expected = TraceContext.root("request-functions");
        Callable<TraceContext> callable;
        Supplier<TraceContext> supplier;
        try (TraceContext.Scope ignored = expected.open()) {
            MDC.put(BUSINESS_KEY, "function-001");
            callable = DtpCallable.wrap(TraceContext::capture);
            supplier = DtpSupplier.wrap(TraceContext::capture);
        }

        assertCompleteContext(callable.call(), expected, "function-001");
        assertCompleteContext(supplier.get(), expected, "function-001");
        assertThat(TraceContext.current()).isEmpty();
    }

    @Test
    public void test_executorServiceShouldPropagateContextAndRestorePlatformWorker() throws Exception {
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ExecutorService executor = new DtpContextAwareExecutorService(delegate);
        try {
            delegate.submit(() -> MDC.put("workerKey", "worker-value")).get();
            TraceContext expected = TraceContext.root("request-executor");
            Future<TraceContext> future;
            try (TraceContext.Scope ignored = expected.open()) {
                MDC.put(BUSINESS_KEY, "executor-001");
                future = executor.submit(TraceContext::capture);
            }

            assertCompleteContext(future.get(), expected, "executor-001");
            assertThat(delegate.submit(() -> MDC.get("workerKey")).get())
                    .isEqualTo("worker-value");
            assertThat(delegate.submit(TraceContext::getTraceId).get()).isNull();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_threadsShouldPropagatePlatformAndVirtualContext() throws Exception {
        TraceContext expected = TraceContext.root("request-threads");
        AtomicReference<ThreadContext> platform = new AtomicReference<>();
        AtomicReference<ThreadContext> virtual = new AtomicReference<>();
        Thread platformThread;
        Thread virtualThread;
        try (TraceContext.Scope ignored = expected.open()) {
            MDC.put(BUSINESS_KEY, "thread-001");
            platformThread = DtpThreads.newPlatformThread(
                    "dtp-platform-test",
                    () -> platform.set(currentThreadContext())
            );
            virtualThread = DtpThreads.newVirtualThread(
                    "dtp-virtual-test",
                    () -> virtual.set(currentThreadContext())
            );
        }

        platformThread.start();
        virtualThread.start();
        platformThread.join();
        virtualThread.join();

        assertCompleteContext(platform.get().traceContext(), expected, "thread-001");
        assertThat(platform.get().virtual()).isFalse();
        assertCompleteContext(virtual.get().traceContext(), expected, "thread-001");
        assertThat(virtual.get().virtual()).isTrue();
    }

    @Test
    public void test_runnableShouldRestoreWorkerMdcWhenDelegateFails() {
        TraceContext expected = TraceContext.root("request-failure");
        Runnable wrapped;
        try (TraceContext.Scope ignored = expected.open()) {
            wrapped = DtpRunnable.wrap(() -> {
                throw new IllegalStateException("task failed");
            });
        }
        MDC.put("workerKey", "worker-value");

        assertThatThrownBy(wrapped::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("task failed");
        assertThat(MDC.get("workerKey")).isEqualTo("worker-value");
        assertThat(TraceContext.current()).isEmpty();
    }

    @Test
    public void test_nullArgumentsShouldFailSynchronously() {
        assertThatNullPointerException().isThrownBy(() -> DtpRunnable.wrap(null));
        assertThatNullPointerException().isThrownBy(() -> DtpCallable.wrap(null));
        assertThatNullPointerException().isThrownBy(() -> DtpSupplier.wrap(null));
        assertThatNullPointerException().isThrownBy(() -> new DtpContextAwareExecutorService(null));
        assertThatNullPointerException().isThrownBy(() -> DtpThreads.startVirtualThread(null));
        assertThatNullPointerException().isThrownBy(() -> DtpThreads.newPlatformThread(null, () -> {
        }));
        assertThatNullPointerException().isThrownBy(() -> DtpThreads.newVirtualThread("dtp-test", null));

        ExecutorService executor = new DtpContextAwareExecutorService(Executors.newSingleThreadExecutor());
        try {
            assertThatNullPointerException().isThrownBy(() -> executor.submit((Callable<?>) null));
            assertThatNullPointerException().isThrownBy(
                    () -> executor.invokeAll(Collections.singletonList(null))
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private static ThreadContext currentThreadContext() {
        return new ThreadContext(TraceContext.capture(), Thread.currentThread().isVirtual());
    }

    private static void assertCompleteContext(TraceContext actual,
                                              TraceContext expected,
                                              String businessId) {
        assertThat(actual).isNotNull();
        assertThat(actual.traceId()).isEqualTo(expected.traceId());
        assertThat(actual.spanId()).isEqualTo(expected.spanId());
        assertThat(actual.parentSpanId()).isEqualTo(expected.parentSpanId());
        assertThat(actual.requestId()).isEqualTo(expected.requestId());
        assertThat(actual.traceFlags()).isEqualTo(expected.traceFlags());
        assertThat(actual.tracestate()).isEqualTo(expected.tracestate());
        assertThat(actual.sourceApp()).isEqualTo(expected.sourceApp());
        assertThat(actual.sourceInstance()).isEqualTo(expected.sourceInstance());
        assertThat(actual.mdcContext()).containsEntry(BUSINESS_KEY, businessId);
    }

    private record ThreadContext(TraceContext traceContext, boolean virtual) {
    }
}
