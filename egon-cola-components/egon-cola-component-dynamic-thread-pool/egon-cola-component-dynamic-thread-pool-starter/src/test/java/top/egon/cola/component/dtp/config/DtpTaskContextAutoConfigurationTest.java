package top.egon.cola.component.dtp.config;

import io.micrometer.context.ThreadLocalAccessor;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.dtp.context.DtpTaskContextManager;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class DtpTaskContextAutoConfigurationTest {
    private static final ThreadLocal<String> USER = new ThreadLocal<>();
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DtpTaskContextAutoConfiguration.class, TaskExecutionAutoConfiguration.class))
            .withUserConfiguration(AccessorConfiguration.class);

    @Test
    void bootDefaultExecutorReceivesDecoratorBeforeItIsBuilt() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(TaskDecorator.class);
            assertThat(context).doesNotHaveBean("dtpAsyncConfigurer");
            assertThat(context).doesNotHaveBean("org.springframework.context.annotation.internalAsyncAnnotationProcessor");
            USER.set("request");
            MDC.put("traceId", "request-trace");
            try {
                var executor = context.getBean("applicationTaskExecutor", ThreadPoolTaskExecutor.class);
                assertThat(executor.submit(() -> USER.get() + ":" + MDC.get("traceId")).get(2, TimeUnit.SECONDS))
                        .isEqualTo("request:request-trace");
            } finally { USER.remove(); MDC.clear(); }
        });
    }

    @Test
    void userDecoratorIsPreservedAndUnqualifiedAccessorsAreNotScanned() {
        runner.withBean("userDecorator", TaskDecorator.class, () -> task -> task)
                .withBean("unqualified", ThreadLocalAccessor.class, () -> new ThreadLocalAccessor<String>() {
                    public Object key() { return "must-not-be-used"; }
                    public String getValue() { throw new AssertionError("unqualified accessor was used"); }
                    public void setValue(String value) { }
                    public void setValue() { }
                }).run(context -> {
                    assertThat(context).hasSingleBean(TaskDecorator.class).doesNotHaveBean("dtpTaskDecorator");
                    assertThatCode(() -> context.getBean(DtpTaskContextManager.class).capture()).doesNotThrowAnyException();
                });
    }

    @Test
    void disablingExtraThreadLocalsKeepsTracePropagation() {
        runner.withPropertyValues("egon.cola.component.dtp.context-propagation.custom-thread-locals-enabled=false")
                .run(context -> {
                    USER.set("request");
                    MDC.put("traceId", "trace-only");
                    try {
                        var executor = context.getBean("applicationTaskExecutor", ThreadPoolTaskExecutor.class);
                        assertThat(executor.submit(USER::get).get(2, TimeUnit.SECONDS)).isNull();
                        assertThat(executor.submit(() -> MDC.get("traceId")).get(2, TimeUnit.SECONDS)).isEqualTo("trace-only");
                    } finally { USER.remove(); MDC.clear(); }
                });
    }

    @Test
    void forcedBootExecutorSelectionAndExistingConfigurerArePreserved() {
        runner.withUserConfiguration(EnabledAsync.class)
                .withPropertyValues("spring.task.execution.mode=force")
                .run(context -> {
                    assertThat(context).hasSingleBean(AsyncConfigurer.class);
                    assertThat(context.getBean(AsyncConfigurer.class).getAsyncExecutor())
                            .isSameAs(context.getBean("applicationTaskExecutor"));
                });
        runner.withUserConfiguration(UserAsyncConfigurer.class)
                .run(context -> assertThat(context).hasSingleBean(AsyncConfigurer.class).doesNotHaveBean("dtpAsyncConfigurer"));
    }

    @Test
    void contextWorksWithoutTheOptionalMetricsBackend() {
        runner.withClassLoader(new org.springframework.boot.test.context.FilteredClassLoader("io.micrometer.core.instrument"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(TaskDecorator.class).hasSingleBean(DtpTaskContextManager.class);
                });
    }

    @Test
    void decoratorNeverCancelsTheForkJoinCarrierAsIfItWereTheBusinessStage() {
        runner.withBean("dtpTaskContextManager", DtpTaskContextManager.class, () -> new DtpTaskContextManager(
                java.util.List.of(new ThreadLocalAccessor<String>() {
                    public Object key() { return "broken"; }
                    public String getValue() { return USER.get(); }
                    public void setValue(String value) { throw new IllegalStateException("install"); }
                    public void setValue() { USER.remove(); }
                }))).run(context -> {
                    var captured = new java.util.concurrent.atomic.AtomicReference<Runnable>();
                    var root = java.util.concurrent.CompletableFuture.supplyAsync(() -> "never", captured::set);
                    USER.set("request");
                    try {
                        var decorated = context.getBean(TaskDecorator.class).decorate(captured.get());
                        assertThatThrownBy(decorated::run)
                                .isInstanceOf(top.egon.cola.component.dtp.exception.DtpContextPropagationException.class);
                        assertThat(((java.util.concurrent.Future<?>) captured.get()).isCancelled()).isFalse();
                        assertThat(root.isDone()).isFalse();
                    } finally { root.cancel(false); USER.remove(); MDC.clear(); }
                });
    }

    @Configuration(proxyBeanMethods = false)
    @org.springframework.scheduling.annotation.EnableAsync
    static class EnabledAsync {
        @Bean("otherExecutor")
        org.springframework.core.task.SyncTaskExecutor otherExecutor() {
            return new org.springframework.core.task.SyncTaskExecutor();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @org.springframework.scheduling.annotation.EnableAsync
    static class UserAsyncConfigurer implements AsyncConfigurer { }

    @Configuration(proxyBeanMethods = false)
    static class AccessorConfiguration {
        @Bean("userAccessor")
        @Qualifier("dtpContextAccessor")
        ThreadLocalAccessor<String> userAccessor() {
            return new ThreadLocalAccessor<>() {
                public Object key() { return "test.user"; }
                public String getValue() { return USER.get(); }
                public void setValue(String value) { USER.set(value); }
                public void setValue() { USER.remove(); }
            };
        }
    }
}
