package top.egon.cola.component.dtp.executor.virtual;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.executor.adapter.BoundedVirtualThreadManagedExecutor;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @ClassName: BoundedVirtualThreadExecutorTest
 * @description: 有界虚拟线程执行器测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-06Month-29Day
 * @Version: 1.0
 */
public class BoundedVirtualThreadExecutorTest {

    @AfterEach
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void test_executeShouldRunTaskWithCapturedMdc() throws Exception {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 1);
        try {
            CountDownLatch done = new CountDownLatch(1);
            AtomicReference<String> traceId = new AtomicReference<>();
            MDC.put("traceId", "trace-virtual-001");

            executor.execute(() -> {
                traceId.set(MDC.get("traceId"));
                done.countDown();
            });

            assertThat(done.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(traceId.get()).isEqualTo("trace-virtual-001");
            assertThat(executor.submittedTasks()).isEqualTo(1L);
            assertThat(executor.completedTasks()).isEqualTo(1L);
            assertThat(executor.failedTasks()).isEqualTo(0L);
            assertThat(executor.runningTasks()).isEqualTo(0L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_submitCallableShouldRunTaskWithCapturedMdcAndIncrementCompletedOnce() throws Exception {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 1);
        try {
            MDC.put("traceId", "trace-virtual-submit-001");

            Future<String> future = executor.submit(() -> MDC.get("traceId"));

            assertThat(future.get()).isEqualTo("trace-virtual-submit-001");
            assertThat(executor.submittedTasks()).isEqualTo(1L);
            assertThat(executor.completedTasks()).isEqualTo(1L);
            assertThat(executor.failedTasks()).isEqualTo(0L);
            assertThat(executor.runningTasks()).isEqualTo(0L);
            assertThat(executor.availablePermits()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_executeShouldRejectWhenConcurrencyLimitExceeded() throws Exception {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            executor.execute(() -> {
                started.countDown();
                await(release);
            });
            assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> executor.execute(() -> {
            }))
                    .isInstanceOf(RejectedExecutionException.class)
                    .hasMessage("Virtual executor concurrency limit exceeded");

            assertThat(executor.submittedTasks()).isEqualTo(2L);
            assertThat(executor.rejectedTasks()).isEqualTo(1L);
            assertThat(executor.runningTasks()).isEqualTo(1L);
            assertThat(executor.availablePermits()).isEqualTo(0);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void test_submitCallableFailureShouldIncrementFailedOnly() {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 1);
        try {
            Future<String> future = executor.submit(() -> {
                throw new IllegalStateException("submit failure");
            });

            assertThatThrownBy(future::get)
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("submit failure");
            assertThat(executor.submittedTasks()).isEqualTo(1L);
            assertThat(executor.completedTasks()).isEqualTo(0L);
            assertThat(executor.failedTasks()).isEqualTo(1L);
            assertThat(executor.runningTasks()).isEqualTo(0L);
            assertThat(executor.availablePermits()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_submitRunnableFailureShouldIncrementFailedOnly() {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 1);
        try {
            Future<?> future = executor.submit(() -> {
                throw new IllegalStateException("submit runnable failure");
            });

            assertThatThrownBy(future::get)
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("submit runnable failure");
            assertThat(executor.submittedTasks()).isEqualTo(1L);
            assertThat(executor.completedTasks()).isEqualTo(0L);
            assertThat(executor.failedTasks()).isEqualTo(1L);
            assertThat(executor.runningTasks()).isEqualTo(0L);
            assertThat(executor.availablePermits()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_invokeAllFailureShouldIncrementFailedOnly() throws Exception {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 1);
        try {
            Future<String> future = executor.invokeAll(Collections.<Callable<String>>singletonList(() -> {
                throw new IllegalStateException("invoke failure");
            })).getFirst();

            assertThatThrownBy(future::get)
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("invoke failure");
            assertThat(executor.submittedTasks()).isEqualTo(1L);
            assertThat(executor.completedTasks()).isEqualTo(0L);
            assertThat(executor.failedTasks()).isEqualTo(1L);
            assertThat(executor.runningTasks()).isEqualTo(0L);
            assertThat(executor.availablePermits()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_invokeAnyFailureShouldIncrementFailedOnly() {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 1);
        try {
            assertThatThrownBy(() -> executor.invokeAny(Collections.<Callable<String>>singletonList(() -> {
                throw new IllegalStateException("invoke any failure");
            })))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("invoke any failure");
            assertThat(executor.submittedTasks()).isEqualTo(1L);
            assertThat(executor.completedTasks()).isEqualTo(0L);
            assertThat(executor.failedTasks()).isEqualTo(1L);
            assertThat(executor.runningTasks()).isEqualTo(0L);
            assertThat(executor.availablePermits()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_updateConcurrencyLimit() {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 1);
        try {
            executor.updateConcurrencyLimit(3);

            assertThat(executor.concurrencyLimit()).isEqualTo(3);
            assertThat(executor.availablePermits()).isEqualTo(3);

            executor.updateConcurrencyLimit(2);

            assertThat(executor.concurrencyLimit()).isEqualTo(2);
            assertThat(executor.availablePermits()).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_constructorShouldRejectInvalidLimit() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BoundedVirtualThreadExecutor("dtp-virtual", 0))
                .withMessage("concurrencyLimit must be positive");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BoundedVirtualThreadExecutor("dtp-virtual", -1))
                .withMessage("concurrencyLimit must be positive");
    }

    @Test
    public void test_executeShouldRejectNullSynchronously() {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 1);
        try {
            assertThatNullPointerException().isThrownBy(() -> executor.execute(null));
            assertThat(executor.submittedTasks()).isEqualTo(0L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_adapterSnapshotAndUpdate() {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 2);
        BoundedVirtualThreadManagedExecutor managedExecutor = new BoundedVirtualThreadManagedExecutor(
                "test-app", "instance-01", "virtualExecutor", executor
        );
        try {
            ExecutorSnapshot snapshot = managedExecutor.snapshot();

            assertThat(snapshot.getAppName()).isEqualTo("test-app");
            assertThat(snapshot.getInstanceId()).isEqualTo("instance-01");
            assertThat(snapshot.getExecutorName()).isEqualTo("virtualExecutor");
            assertThat(snapshot.getExecutorKind()).isEqualTo(ExecutorKind.VIRTUAL_THREAD_PER_TASK);
            assertThat(snapshot.isVirtual()).isTrue();
            assertThat(snapshot.isResizable()).isFalse();
            assertThat(snapshot.getConcurrencyLimit()).isEqualTo(2);
            assertThat(snapshot.getRunningTasks()).isEqualTo(0L);
            assertThat(snapshot.getSubmittedTasks()).isEqualTo(0L);
            assertThat(snapshot.getCompletedTaskCount()).isEqualTo(0L);
            assertThat(snapshot.getFailedTasks()).isEqualTo(0L);
            assertThat(snapshot.getRejectedTasks()).isEqualTo(0L);
            assertThat(snapshot.getAvailablePermits()).isEqualTo(2);
            assertThat(snapshot.getCorePoolSize()).isNull();
            assertThat(snapshot.getQueueSize()).isNull();
            assertThat(snapshot.getReportTime()).isNotNull();
            assertThat(managedExecutor.supportsResize()).isFalse();
            assertThat(managedExecutor.supportsVirtualThread()).isTrue();
            assertThat(managedExecutor.supportsQueueMetrics()).isFalse();

            ExecutorUpdateCommand command = new ExecutorUpdateCommand();
            command.setConcurrencyLimit(4);
            UpdateResult result = managedExecutor.update(command);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("success");
            assertThat(result.getBefore().getConcurrencyLimit()).isEqualTo(2);
            assertThat(result.getAfter().getConcurrencyLimit()).isEqualTo(4);
            assertThat(executor.concurrencyLimit()).isEqualTo(4);

            ExecutorUpdateCommand invalidCommand = new ExecutorUpdateCommand();
            UpdateResult failedResult = managedExecutor.update(invalidCommand);

            assertThat(failedResult.isSuccess()).isFalse();
            assertThat(failedResult.getMessage()).isEqualTo("concurrencyLimit must not be null");
            assertThat(failedResult.getAfter().getConcurrencyLimit()).isEqualTo(4);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
