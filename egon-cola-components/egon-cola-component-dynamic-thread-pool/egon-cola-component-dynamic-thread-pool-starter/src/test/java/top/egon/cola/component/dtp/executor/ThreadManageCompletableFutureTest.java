package top.egon.cola.component.dtp.executor;

import io.micrometer.context.ThreadLocalAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoProperties;
import top.egon.cola.component.dtp.context.DtpTaskContextManager;
import top.egon.cola.component.dtp.domain.DynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ThreadPoolCreateCommand;
import top.egon.cola.component.dtp.exception.DtpContextPropagationException;
import top.egon.cola.component.dtp.exception.DtpTaskFailureBO;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

class ThreadManageCompletableFutureTest {
    private final ThreadLocal<String> user = new ThreadLocal<>();
    private final List<DtpTaskFailureBO> failures = new CopyOnWriteArrayList<>();
    private final AtomicBoolean brokenInstall = new AtomicBoolean();
    private LocalValidatorFactoryBean validator;
    private ManagedExecutorRegistry registry;
    private ThreadManage manager;
    private org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor pool;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        ValidationUtils validation = new ValidationUtils(validator);
        DtpTaskContextManager contexts = new DtpTaskContextManager(List.of(new ThreadLocalAccessor<String>() {
            public Object key() { return "test.user"; }
            public String getValue() { return user.get(); }
            public void setValue(String value) {
                user.set(value);
                if (brokenInstall.get()) { throw new IllegalStateException("install failed"); }
            }
            public void setValue() { user.remove(); }
        }));
        registry = new ManagedExecutorRegistry("app", "one", new DynamicThreadPoolAutoProperties(),
                () -> contexts, () -> failures::add, () -> validation);
        manager = new ThreadManage(registry, new DynamicThreadPoolService(registry, validation), validation);
        pool = manager.newThreadPool(new ThreadPoolCreateCommand().setExecutorName("work")
                .setCorePoolSize(1).setMaximumPoolSize(1).setQueueCapacity(8));
    }

    @AfterEach
    void close() {
        manager.destroy();
        validator.close();
        user.remove();
        MDC.clear();
    }

    @Test
    void rootHelpersUseRealResultsAndReportFailureOnce() throws Exception {
        user.set("A");
        MDC.put("traceId", "trace-A");
        assertThat(manager.supplyAsync("work", user::get).get(2, TimeUnit.SECONDS)).isEqualTo("A");
        assertThat(manager.runAsync("work", () -> assertThat(user.get()).isEqualTo("A")).get(2, TimeUnit.SECONDS)).isNull();
        RuntimeException original = new IllegalStateException("business");
        var failed = manager.supplyAsync("work", () -> { throw original; });
        await().untilAsserted(() -> assertThat(failures).hasSize(1));
        assertThat(failures.getFirst().getCause()).isSameAs(original);
        assertThat(failures.getFirst().getTraceId()).isEqualTo("trace-A");
        assertThatThrownBy(failed::get).hasCause(original);
        assertThat(registry.get("work").orElseThrow().taskObserver().failedTasks()).isEqualTo(1);
    }

    @Test
    void returnedObjectIsTheObservedRootEvenWhenExternallyCompleted() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        user.set("A");
        MDC.put("traceId", "trace-A");
        var root = manager.supplyAsync("work", () -> {
            started.countDown();
            try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return "business";
        });
        try {
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            user.set("B");
            MDC.put("traceId", "trace-B");
            assertThat(root.complete("external")).isTrue();
            assertThat(root.get()).isEqualTo("external");
            assertThat(registry.get("work").orElseThrow().taskObserver().succeededTasks()).isEqualTo(1);
            assertThat(user.get()).isEqualTo("B");
            assertThat(MDC.get("traceId")).isEqualTo("trace-B");
        } finally { release.countDown(); }
    }

    @Test
    void installationFailureCompletesRootWithoutInvokingBusiness() {
        user.set("A");
        brokenInstall.set(true);
        AtomicBoolean invoked = new AtomicBoolean();
        var root = manager.supplyAsync("work", () -> { invoked.set(true); return "wrong"; });
        assertThatThrownBy(() -> root.get(2, TimeUnit.SECONDS)).hasCauseInstanceOf(DtpContextPropagationException.class);
        assertThat(invoked).isFalse();
        assertThat(registry.get("work").orElseThrow().taskObserver().contextFailures("INSTALL")).isEqualTo(1);
    }

    @Test
    void boundExecutorUsesRegistrationContextWhenAnotherThreadCompletesUpstream() throws Exception {
        user.set("A");
        MDC.put("traceId", "trace-A");
        Executor bound = manager.contextualExecutor("work");
        CompletableFuture<String> upstream = new CompletableFuture<>();
        var next = upstream.thenApplyAsync(value -> user.get() + ":" + MDC.get("traceId"), bound);
        try (var other = Executors.newSingleThreadExecutor()) {
            other.submit(() -> {
                user.set("B");
                MDC.put("traceId", "trace-B");
                try { upstream.complete("done"); }
                finally {
                    assertThat(user.get()).isEqualTo("B");
                    assertThat(MDC.get("traceId")).isEqualTo("trace-B");
                    user.remove();
                    MDC.clear();
                }
            }).get(2, TimeUnit.SECONDS);
        }
        assertThat(next.get(2, TimeUnit.SECONDS)).isEqualTo("A:trace-A");
        assertThat(registry.get("work").orElseThrow().taskObserver().isComplete()).isFalse();
    }

    @Test
    void cancellingRootDoesNotInterruptOrClearRunningWorkerContext() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<String> workerSaw = new CompletableFuture<>();
        user.set("A");
        var root = manager.supplyAsync("work", () -> {
            started.countDown();
            try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            workerSaw.complete(user.get());
            return "ignored";
        });
        try {
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            user.set("caller-B");
            assertThat(root.cancel(true)).isTrue();
            assertThat(user.get()).isEqualTo("caller-B");
        } finally { release.countDown(); }
        assertThat(workerSaw.get(2, TimeUnit.SECONDS)).isEqualTo("A");
        assertThat(pool.getThreadPoolExecutor().submit(() -> user.get() == null
                && (MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty()))
                .get(2, TimeUnit.SECONDS)).isTrue();
        assertThat(registry.get("work").orElseThrow().taskObserver().cancelledTasks()).isEqualTo(1);
        assertThat(registry.get("work").orElseThrow().taskObserver().failedTasks()).isZero();
    }

    @Test
    void unknownLegacyTraceWrapperNeverMisattributesFailureToTheSubmittingRequest() {
        RuntimeException original = new IllegalStateException("legacy");
        MDC.put("traceId", "wrapped-A");
        var task = new top.egon.cola.component.common.trace.thread.TraceRouteCallable<String>() {
            @Override
            protected String doCall() {
                assertThat(MDC.get("traceId")).isEqualTo("wrapped-A");
                throw original;
            }
        };
        MDC.put("traceId", "submitting-B");
        var future = manager.executor("work").submit(task);
        assertThatThrownBy(future::get).hasCause(original);
        await().untilAsserted(() -> assertThat(failures).hasSize(1));
        assertThat(failures.getFirst().getTraceId()).isNull();
        assertThat(MDC.get("traceId")).isEqualTo("submitting-B");
        assertThat(manager.executor("work").taskObserver().isComplete()).isFalse();
    }
}
