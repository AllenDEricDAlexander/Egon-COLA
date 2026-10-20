package top.egon.cola.component.dtp.executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoProperties;
import top.egon.cola.component.dtp.context.DtpCallable;
import top.egon.cola.component.dtp.context.DtpRunnable;
import top.egon.cola.component.dtp.domain.DynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.ThreadPoolCreateCommand;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

class ThreadManageTest {
    private LocalValidatorFactoryBean validator;
    private ManagedExecutorRegistry registry;
    private ThreadManage manager;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        ValidationUtils validation = new ValidationUtils(validator);
        registry = new ManagedExecutorRegistry("app", "one", new DynamicThreadPoolAutoProperties());
        manager = new ThreadManage(registry, new DynamicThreadPoolService(registry, validation), validation);
    }

    @AfterEach
    void close() {
        manager.destroy();
        validator.close();
        MDC.clear();
    }

    private ThreadPoolCreateCommand command() {
        return new ThreadPoolCreateCommand().setExecutorName("email").setCorePoolSize(1).setMaximumPoolSize(2).setQueueCapacity(8);
    }

    @Test
    void concurrentCreationIsUniqueAndDifferentDefinitionsCannotReplaceIt() throws Exception {
        try (var creators = Executors.newFixedThreadPool(4)) {
            var requests = java.util.stream.IntStream.range(0, 12)
                    .mapToObj(i -> (Callable<ThreadPoolTaskExecutor>) () -> manager.newThreadPool(command())).toList();
            List<Future<ThreadPoolTaskExecutor>> results = creators.invokeAll(requests);
            ThreadPoolTaskExecutor pool = results.getFirst().get();
            for (Future<ThreadPoolTaskExecutor> result : results) { assertThat(result.get()).isSameAs(pool); }
            assertThat(registry.list()).hasSize(1);
            assertThatThrownBy(() -> manager.newThreadPool(command().setQueueCapacity(9))).isInstanceOf(IllegalStateException.class);
            assertThat(manager.newThreadPool(command())).isSameAs(pool);
        }
    }

    @Test
    void directAndManagedSubmissionsPreserveTraceAndCreateBackgroundRoots() throws Exception {
        ThreadPoolTaskExecutor pool = manager.newThreadPool(command());
        assertThat(pool.submit(TraceContext::getTraceId).get()).matches("[0-9a-f]{32}");
        assertThat(MDC.get(TraceContext.TRACE_ID)).isNull();
        TraceContext original = TraceContext.root("old");
        Callable<String> old;
        Runnable oldRunnable;
        java.util.concurrent.atomic.AtomicReference<String> seen = new java.util.concurrent.atomic.AtomicReference<>();
        try (var ignored = original.open()) {
            old = DtpCallable.wrap(TraceContext::getTraceId);
            oldRunnable = DtpRunnable.wrap(() -> seen.set(TraceContext.getTraceId()));
        }
        try (var ignored = TraceContext.root("new").open()) {
            assertThat(manager.executor("email").submit(old).get()).isEqualTo(original.traceId());
            pool.submit(oldRunnable).get();
            assertThat(seen.get()).isEqualTo(original.traceId());
        }
        Callable<String> empty = DtpCallable.wrap(TraceContext::getTraceId);
        assertThat(pool.submit(empty).get()).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void failuresAndQueuedCancellationAreNotCountedAsSuccess() throws Exception {
        manager.newThreadPool(command());
        ManagedExecutor executor = manager.executor("email");
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> blocker = executor.submit(() -> { running.countDown(); release.await(); return null; });
        try {
            assertThat(running.await(2, TimeUnit.SECONDS)).isTrue();
            Future<?> cancelled = executor.submit(() -> { throw new AssertionError("cancelled body must not run"); });
            assertThat(cancelled.cancel(false)).isTrue();
        } finally { release.countDown(); }
        blocker.get(2, TimeUnit.SECONDS);
        Future<?> failed = executor.submit((Callable<Object>) () -> { throw new IllegalStateException("expected"); });
        assertThatThrownBy(() -> failed.get(2, TimeUnit.SECONDS)).hasCauseInstanceOf(IllegalStateException.class);
        executor.submit(() -> { }).get(2, TimeUnit.SECONDS);
        await().untilAsserted(() -> {
            assertThat(executor.taskObserver().submittedTasks()).isEqualTo(4);
            assertThat(executor.taskObserver().succeededTasks()).isEqualTo(2);
            assertThat(executor.taskObserver().failedTasks()).isEqualTo(1);
            assertThat(executor.taskObserver().cancelledTasks()).isEqualTo(1);
        });
    }

    @Test
    void callerRunsRestoresCallerContextAndRecordsBothRejectionAndSuccess() throws Exception {
        manager.newThreadPool(command().setMaximumPoolSize(1).setQueueCapacity(0)
                .setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()));
        var executor = manager.executor("email");
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> blocker = executor.submit(() -> { running.countDown(); release.await(); return null; });
        try {
            assertThat(running.await(2, TimeUnit.SECONDS)).isTrue();
            TraceContext caller = TraceContext.root("caller");
            try (var ignored = caller.open()) {
                assertThat(executor.submit(Thread::currentThread).get()).isSameAs(Thread.currentThread());
                assertThat(TraceContext.getTraceId()).isEqualTo(caller.traceId());
            }
            assertThat(executor.taskObserver().rejectedTasks()).isEqualTo(1);
        } finally { release.countDown(); }
        blocker.get(2, TimeUnit.SECONDS);
        await().untilAsserted(() -> assertThat(executor.taskObserver().succeededTasks()).isEqualTo(2));
    }

    @Test
    void resizeDoesNotChangeCreationIdentityAndSpringOwnershipIsRespected() {
        ThreadPoolTaskExecutor pool = manager.newThreadPool(command());
        ExecutorUpdateCommand update = new ExecutorUpdateCommand();
        update.setAppName("app"); update.setInstanceId("one"); update.setExecutorName("email");
        update.setExecutorKind(manager.executor("email").kind());
        update.setCorePoolSize(2); update.setMaximumPoolSize(4); update.setQueueCapacity(16);
        update.setKeepAliveSeconds(2L); update.setAllowCoreThreadTimeOut(true);
        assertThat(manager.changeThreadPool(update).isSuccess()).isTrue();
        assertThat(pool.getThreadPoolExecutor().allowsCoreThreadTimeOut()).isTrue();
        assertThat(pool.getQueueCapacity()).isEqualTo(16);
        assertThat(manager.newThreadPool(command())).isSameAs(pool);
        registry.registerBean("email", pool);
        manager.destroy();
        assertThat(pool.getThreadPoolExecutor().isShutdown()).isFalse();
        pool.shutdown();
        assertThatThrownBy(() -> manager.newThreadPool(command())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void invalidCreationCannotRegisterOrAllocateAPool() {
        assertThatThrownBy(() -> manager.newThreadPool(command().setCorePoolSize(5))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> manager.newThreadPool(command().setQueueCapacity(-1))).isInstanceOf(IllegalArgumentException.class);
        assertThat(registry.list()).isEmpty();
    }
    @Test
    void constructorInjectionAndLegacySetterDescriptorsRemainCompatible() throws Exception {
        var constructor = ThreadManage.class.getDeclaredConstructors()[0];
        assertThat(constructor.isAnnotationPresent(org.springframework.beans.factory.annotation.Autowired.class)).isTrue();
        assertThat(java.util.Arrays.stream(constructor.getParameters())
                .map(parameter -> parameter.getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value()))
                .containsExactly("managedExecutorRegistry", "dynamicThreadPollService", "dtpValidationUtils");
        assertThat(ExecutorUpdateCommand.class.getMethod("setCorePoolSize", Integer.class).getReturnType()).isEqualTo(void.class);
        assertThat(ExecutorUpdateCommand.class.getMethod("setMaximumPoolSize", Integer.class).getReturnType()).isEqualTo(void.class);
    }

    @Test
    void registryPreservesLegacyInsertionOrderAndDeduplicatesTheSameManagedObject() {
        var first = org.mockito.Mockito.mock(ManagedExecutor.class);
        var second = org.mockito.Mockito.mock(ManagedExecutor.class);
        org.mockito.Mockito.when(first.executorName()).thenReturn("z-first");
        org.mockito.Mockito.when(second.executorName()).thenReturn("a-second");
        var legacy = new ManagedExecutorRegistry(List.of(first, second, first));
        assertThat(legacy.list()).containsExactly(first, second);
        assertThatThrownBy(() -> legacy.list().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

}
