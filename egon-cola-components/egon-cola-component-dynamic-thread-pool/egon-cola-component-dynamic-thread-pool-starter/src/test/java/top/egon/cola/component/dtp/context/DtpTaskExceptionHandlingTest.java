package top.egon.cola.component.dtp.context;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoProperties;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

class DtpTaskExceptionHandlingTest {

    @Test
    void ownedWorkerReportsOnceAndStillInvokesTheOriginalHandler() throws Exception {
        var reports = new java.util.concurrent.CopyOnWriteArrayList<top.egon.cola.component.dtp.exception.DtpTaskFailureBO>();
        var context = new DtpTaskContextManager();
        var observer = new DtpTaskObserver("app", "one", "owned", new DynamicThreadPoolAutoProperties.Observation(), true,
                () -> context, () -> reports::add, () -> null, new DynamicThreadPoolAutoProperties.ExceptionHandling());
        var originalCalled = new java.util.concurrent.CountDownLatch(1);
        var originalFailure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        var attributes = new java.util.concurrent.atomic.AtomicBoolean();
        var pool = new top.egon.cola.component.dtp.executor.managed.DtpThreadPoolTaskExecutor(observer);
        pool.setCorePoolSize(1);
        pool.setMaxPoolSize(1);
        pool.setThreadFactory(task -> {
            Thread thread = new Thread(task, "original-worker");
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((worker, failure) -> {
                attributes.set(worker.isDaemon() && worker.getName().equals("original-worker"));
                originalFailure.set(failure);
                originalCalled.countDown();
            });
            return thread;
        });
        pool.initialize();
        RuntimeException original = new IllegalStateException("worker-failure");
        try {
            MDC.put("traceId", "owned-trace");
            pool.execute(() -> { throw original; });
            assertThat(originalCalled.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(reports).hasSize(1);
            assertThat(reports.getFirst().getTraceId()).isEqualTo("owned-trace");
            assertThat(originalFailure.get()).isSameAs(original);
            assertThat(attributes).isTrue();
            assertThat(observer.failedTasks()).isEqualTo(1);
            assertThat(observer.uncaughtFailures()).isEqualTo(1);
        } finally { pool.shutdown(); MDC.clear(); }
    }

    @Test
    void reporterFailureDoesNotMutateTheOriginalFutureFailure() throws Exception {
        RuntimeException original = new IllegalArgumentException("business");
        var context = new DtpTaskContextManager();
        var observer = new DtpTaskObserver("app", "one", "reporter", new DynamicThreadPoolAutoProperties.Observation(), true,
                () -> context, () -> failure -> { throw new IllegalStateException("reporter"); },
                () -> null, new DynamicThreadPoolAutoProperties.ExceptionHandling());
        try (var executor = new DtpContextAwareExecutorService(Executors.newSingleThreadExecutor(), observer)) {
            var future = executor.submit(() -> { throw original; });
            assertThatThrownBy(future::get).hasCause(original);
            await().untilAsserted(() -> assertThat(observer.reporterFailures()).isEqualTo(1));
            assertThat(original.getSuppressed()).isEmpty();
            assertThat(observer.failedTasks()).isEqualTo(1);
        }
    }

    @Test
    void systemErrorsBusinessErrorsAndSlowDiagnosticsHaveSeparateBudgets() throws Exception {
        var reports = new java.util.concurrent.CopyOnWriteArrayList<top.egon.cola.component.dtp.exception.DtpTaskFailureBO>();
        var context = new DtpTaskContextManager();
        var properties = new DynamicThreadPoolAutoProperties.Observation();
        properties.setSlowTaskThreshold(java.time.Duration.ofNanos(1));
        var observer = new DtpTaskObserver("app", "one", "budgets", properties, true,
                () -> context, () -> reports::add, () -> null, new DynamicThreadPoolAutoProperties.ExceptionHandling());
        try (var executor = new DtpContextAwareExecutorService(Executors.newSingleThreadExecutor(), observer)) {
            for (int index = 0; index < 2; index++) {
                var future = executor.submit(() -> {
                    throw new top.egon.cola.component.common.core.exception.BusinessException(
                            top.egon.cola.component.common.core.enums.BusinessExceptionEnum.INVALID_PARAM);
                });
                assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class);
            }
            var system = executor.submit(() -> { throw new IllegalStateException("system"); });
            assertThatThrownBy(system::get).isInstanceOf(ExecutionException.class);
            await().untilAsserted(() -> assertThat(reports).hasSize(3));
            assertThat(reports.get(0).isLogEligible()).isTrue();
            assertThat(reports.get(1).isLogEligible()).isFalse();
            assertThat(reports.get(2).isLogEligible()).isTrue();
            assertThat(observer.failedTasks()).isEqualTo(3);
        }
    }

    @Test
    void technicalMetricsUseFixedDimensionsAndAreRemovedWithOwnedPool() throws Exception {
        var validator = new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        var validation = new top.egon.cola.component.common.core.validation.ValidationUtils(validator);
        ThreadLocal<String> user = new ThreadLocal<>();
        var contexts = new DtpTaskContextManager(java.util.List.of(new io.micrometer.context.ThreadLocalAccessor<String>() {
            public Object key() { return "metrics.user"; }
            public String getValue() { return user.get(); }
            public void setValue(String value) { throw new IllegalStateException("install"); }
            public void setValue() { user.remove(); }
        }));
        var registry = new top.egon.cola.component.dtp.executor.ManagedExecutorRegistry("app", "one",
                new DynamicThreadPoolAutoProperties(), () -> contexts,
                () -> failure -> { throw new IllegalStateException("reporter"); }, () -> validation);
        var manager = new top.egon.cola.component.dtp.executor.ThreadManage(registry,
                new top.egon.cola.component.dtp.domain.DynamicThreadPoolService(registry, validation), validation);
        var meters = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        var binder = new top.egon.cola.component.dtp.metrics.DtpMeterBinder(registry);
        try {
            manager.newThreadPool(new top.egon.cola.component.dtp.domain.model.entity.ThreadPoolCreateCommand()
                    .setExecutorName("metrics").setCorePoolSize(1).setMaximumPoolSize(1).setQueueCapacity(4));
            binder.bindTo(meters);
            user.set("must-not-be-a-tag");
            assertThatThrownBy(() -> manager.supplyAsync("metrics", () -> "never").get(2, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(top.egon.cola.component.dtp.exception.DtpContextPropagationException.class);
            await().untilAsserted(() -> {
                assertThat(meters.get("dtp.context.propagation.failures").tag("phase", "INSTALL").functionCounter().count()).isEqualTo(1);
                assertThat(meters.get("dtp.task.exception.reporter.failures").functionCounter().count()).isEqualTo(1);
            });
            assertThat(meters.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags())
                    .noneMatch(tag -> tag.getKey().equals("traceId") || tag.getKey().equals("userId")
                            || tag.getKey().equals("accessorKey") || tag.getValue().equals("must-not-be-a-tag")));
            manager.destroy();
            assertThat(meters.getMeters()).isEmpty();
        } finally {
            manager.destroy();
            binder.destroy();
            meters.close();
            validator.close();
            user.remove();
        }
    }

    @Test
    void failedSubmitLogsSafeErrorWithoutRequiringBusinessGet() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger("top.egon.cola.component.dtp");
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        DtpTaskObserver observer = new DtpTaskObserver("app", "instance", "orders",
                new DynamicThreadPoolAutoProperties.Observation(), true);
        RuntimeException original = new RuntimeException("credential=must-not-appear");
        try (var executor = new DtpContextAwareExecutorService(Executors.newSingleThreadExecutor(), observer)) {
            MDC.put("traceId", "task-trace");
            var future = executor.submit(() -> { throw original; });
            MDC.clear();
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(events.list).anyMatch(event -> event.getLevel() == Level.ERROR));
            assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class).hasCause(original);
            ILoggingEvent error = events.list.stream().filter(event -> event.getLevel() == Level.ERROR).findFirst().orElseThrow();
            assertThat(error.getThrowableProxy()).isNotNull();
            assertThat(error.getThrowableProxy().getMessage()).doesNotContain("credential=");
            assertThat(error.getKeyValuePairs()).anyMatch(pair -> pair.key.equals("traceId") && pair.value.equals("task-trace"));
            assertThat(observer.failedTasks()).isEqualTo(1);
            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        } finally {
            logger.detachAppender(events);
            events.stop();
            MDC.clear();
        }
    }
}
