package top.egon.cola.component.dtp.context;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoProperties;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

class DtpTaskObserverTest {
    @Test
    void forkJoinCarrierCancellationIsNotTheCompletableFutureBusinessOutcome() throws Exception {
        var observer = new DtpTaskObserver("app", "one", "opaque-carrier", new DynamicThreadPoolAutoProperties.Observation(), true);
        try (var executor = new DtpContextAwareExecutorService(Executors.newSingleThreadExecutor(), observer)) {
            var root = java.util.concurrent.CompletableFuture.supplyAsync(() -> "business", command -> {
                assertThat(command).isInstanceOf(java.util.concurrent.CompletableFuture.AsynchronousCompletionTask.class);
                // JDK21 AsyncSupply的ForkJoinTask状态与它承载的CF root相互独立。
                ((java.util.concurrent.Future<?>) command).cancel(false);
                executor.execute(command);
            });
            assertThat(root.get(2, TimeUnit.SECONDS)).isEqualTo("business");
        }
        assertThat(observer.cancelledTasks()).isZero();
        assertThat(observer.succeededTasks()).isZero();
        assertThat(observer.failedTasks()).isZero();
        assertThat(observer.isComplete()).isFalse();
    }

    @Test
    void opaqueStageInstallFailureNeverInventsACancelledOrFailedBusinessStage() throws Exception {
        ThreadLocal<String> local = new ThreadLocal<>();
        var context = new DtpTaskContextManager(List.of(new io.micrometer.context.ThreadLocalAccessor<String>() {
            public Object key() { return "opaque"; }
            public String getValue() { return local.get(); }
            public void setValue(String value) { throw new IllegalStateException("install"); }
            public void setValue() { local.remove(); }
        }));
        var reports = new java.util.concurrent.atomic.AtomicInteger();
        var observer = new DtpTaskObserver("app", "one", "opaque-install", new DynamicThreadPoolAutoProperties.Observation(), true,
                () -> context, () -> failure -> reports.incrementAndGet(), () -> null,
                new DynamicThreadPoolAutoProperties.ExceptionHandling());
        CountDownLatch uncaught = new CountDownLatch(1);
        var factory = observer.threadFactory(task -> {
            Thread thread = new Thread(task, "opaque-worker");
            thread.setUncaughtExceptionHandler((worker, failure) -> uncaught.countDown());
            return thread;
        });
        local.set("caller");
        java.util.concurrent.CompletableFuture<String> root = null;
        try (var executor = new DtpContextAwareExecutorService(Executors.newSingleThreadExecutor(factory), observer)) {
            root = java.util.concurrent.CompletableFuture.supplyAsync(() -> "never", executor);
            assertThat(uncaught.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(root.isDone()).isFalse();
            assertThat(observer.cancelledTasks()).isZero();
            assertThat(observer.failedTasks()).isZero();
            assertThat(observer.contextFailures("INSTALL")).isEqualTo(1);
            assertThat(reports).hasValue(0);
            assertThat(observer.isComplete()).isFalse();
        } finally {
            if (root != null) { root.cancel(false); }
            local.remove();
        }
    }

    @Test
    void bulkFailuresAndCancellationHaveExactlyOneTerminalOutcome() throws Exception {
        var observer = new DtpTaskObserver("app", "one", "bulk", new DynamicThreadPoolAutoProperties.Observation(), false);
        try (var executor = new DtpContextAwareExecutorService(Executors.newFixedThreadPool(2), observer)) {
            var results = executor.invokeAll(List.of((Callable<String>) () -> "ok", () -> { throw new IllegalStateException("expected"); }));
            assertThat(results.getFirst().get()).isEqualTo("ok");
            assertThatThrownBy(() -> results.getLast().get()).hasCauseInstanceOf(IllegalStateException.class);
            CountDownLatch started = new CountDownLatch(1);
            String winner = executor.invokeAny(List.of(() -> { started.countDown(); new CountDownLatch(1).await(); return "never"; },
                    () -> { assertThat(started.await(2, TimeUnit.SECONDS)).isTrue(); return "winner"; }));
            assertThat(winner).isEqualTo("winner");
            await().untilAsserted(() -> {
                assertThat(observer.submittedTasks()).isEqualTo(4);
                assertThat(observer.succeededTasks()).isEqualTo(2);
                assertThat(observer.failedTasks()).isEqualTo(1);
                assertThat(observer.cancelledTasks()).isEqualTo(1);
            });
        }
    }

    @Test
    void monitoringCallbackFailureDoesNotChangeBusinessResult() throws Exception {
        var observer = new DtpTaskObserver("app", "one", "callback", new DynamicThreadPoolAutoProperties.Observation(), false);
        observer.recordTimings((kind, nanos) -> { throw new IllegalStateException("monitor failure"); });
        try (var executor = new DtpContextAwareExecutorService(Executors.newSingleThreadExecutor(), observer)) {
            assertThat(executor.submit(() -> "business-result").get(2, TimeUnit.SECONDS)).isEqualTo("business-result");
            await().untilAsserted(() -> assertThat(observer.succeededTasks()).isEqualTo(1));
        }
    }
    @Test
    void opaqueCompletableFutureResultsAreNeverMistakenForBusinessSuccess() throws Exception {
        var observer = new DtpTaskObserver("app", "one", "stage", new DynamicThreadPoolAutoProperties.Observation(), true);
        try (var executor = new DtpContextAwareExecutorService(Executors.newSingleThreadExecutor(), observer)) {
            var stage = java.util.concurrent.CompletableFuture.supplyAsync(() -> { throw new IllegalStateException("stage failure"); }, executor);
            assertThatThrownBy(stage::join).hasCauseInstanceOf(IllegalStateException.class);
        }
        assertThat(observer.submittedTasks()).isEqualTo(1);
        assertThat(observer.succeededTasks()).isZero();
        assertThat(observer.failedTasks()).isZero();
        assertThat(observer.isComplete()).isFalse();
    }

    @Test
    void cancellingARunningFutureKeepsCancellationSeparateFromInterruptedBodyFailure() throws Exception {
        var observer = new DtpTaskObserver("app", "one", "cancel", new DynamicThreadPoolAutoProperties.Observation(), true);
        try (var executor = new DtpContextAwareExecutorService(Executors.newSingleThreadExecutor(), observer)) {
            for (int i = 0; i < 30; i++) {
                CountDownLatch started = new CountDownLatch(1);
                var task = executor.submit(() -> { started.countDown(); new CountDownLatch(1).await(); return null; });
                assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
                assertThat(task.cancel(true)).isTrue();
            }
        }
        assertThat(observer.submittedTasks()).isEqualTo(30);
        assertThat(observer.cancelledTasks()).isEqualTo(30);
        assertThat(observer.failedTasks()).isZero();
        assertThat(observer.succeededTasks()).isZero();
    }

}
