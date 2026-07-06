package top.egon.cola.component.dtp.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @ClassName: DtpContextPropagationTest
 * @description: DTP 上下文传递测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-06Month-29Day
 * @Version: 1.0
 */
public class DtpContextPropagationTest {

    @AfterEach
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void test_runnableShouldRestoreCapturedMdcAndPreviousWorkerMdc() {
        MDC.put("traceId", "trace-001");
        Runnable wrapped = DtpRunnable.wrap(() -> assertThat(MDC.get("traceId")).isEqualTo("trace-001"));

        MDC.put("traceId", "worker-old");
        wrapped.run();

        assertThat(MDC.get("traceId")).isEqualTo("worker-old");
    }

    @Test
    public void test_callableShouldReturnCapturedMdcAndClearPreviousMdc() throws Exception {
        MDC.put("traceId", "trace-002");
        Callable<String> wrapped = DtpCallable.wrap(() -> MDC.get("traceId"));

        MDC.clear();

        assertThat(wrapped.call()).isEqualTo("trace-002");
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    public void test_supplierShouldReturnCapturedMdc() {
        MDC.put("traceId", "trace-003");
        Supplier<String> wrapped = DtpSupplier.wrap(() -> MDC.get("traceId"));

        MDC.clear();

        assertThat(wrapped.get()).isEqualTo("trace-003");
    }

    @Test
    public void test_executorServiceSubmitCallableShouldPropagateMdcOnAnotherThread() throws Exception {
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ExecutorService executor = new DtpContextAwareExecutorService(delegate);
        try {
            MDC.put("traceId", "trace-004");
            Future<String> future = executor.submit(() -> MDC.get("traceId"));

            assertThat(future.get()).isEqualTo("trace-004");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_executorServiceShouldNotLeakMdcBetweenTasksOnSameThread() throws Exception {
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ExecutorService executor = new DtpContextAwareExecutorService(delegate);
        try {
            MDC.put("traceId", "trace-005");
            Future<String> first = executor.submit(() -> MDC.get("traceId"));
            assertThat(first.get()).isEqualTo("trace-005");

            MDC.clear();
            Future<String> second = executor.submit(() -> MDC.get("traceId"));

            assertThat(second.get()).isNull();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_runnableWrapShouldRejectNullRunnable() {
        assertThatNullPointerException().isThrownBy(() -> DtpRunnable.wrap(null));
    }

    @Test
    public void test_callableWrapShouldRejectNullCallable() {
        assertThatNullPointerException().isThrownBy(() -> DtpCallable.wrap(null));
    }

    @Test
    public void test_supplierWrapShouldRejectNullSupplier() {
        assertThatNullPointerException().isThrownBy(() -> DtpSupplier.wrap(null));
    }

    @Test
    public void test_executorServiceShouldRejectNullDelegate() {
        assertThatNullPointerException().isThrownBy(() -> new DtpContextAwareExecutorService(null));
    }

    @Test
    public void test_executorServiceSubmitCallableShouldRejectNullSynchronously() {
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ExecutorService executor = new DtpContextAwareExecutorService(delegate);
        try {
            assertThatNullPointerException().isThrownBy(() -> executor.submit((Callable<?>) null));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_executorServiceInvokeAllShouldRejectNullCallableElementSynchronously() {
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ExecutorService executor = new DtpContextAwareExecutorService(delegate);
        try {
            assertThatNullPointerException().isThrownBy(() -> executor.invokeAll(Collections.singletonList(null)));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_threadsShouldRejectNullTaskAndName() {
        assertThatNullPointerException().isThrownBy(() -> DtpThreads.startVirtualThread(null));
        assertThatNullPointerException().isThrownBy(() -> DtpThreads.newPlatformThread(null, () -> {
        }));
        assertThatNullPointerException().isThrownBy(() -> DtpThreads.newPlatformThread("dtp-test", null));
        assertThatNullPointerException().isThrownBy(() -> DtpThreads.newVirtualThread(null, () -> {
        }));
        assertThatNullPointerException().isThrownBy(() -> DtpThreads.newVirtualThread("dtp-test", null));
    }

}
