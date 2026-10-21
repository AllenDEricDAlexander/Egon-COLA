package top.egon.cola.component.dtp.config;

import io.micrometer.context.ThreadLocalAccessor;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.dtp.context.DtpTaskContextManager;
import top.egon.cola.component.dtp.domain.DynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ThreadPoolCreateCommand;
import top.egon.cola.component.dtp.exception.DtpTaskExceptionHandler;
import top.egon.cola.component.dtp.exception.DtpTaskFailureBO;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;
import top.egon.cola.component.dtp.executor.ThreadManage;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

class DtpAsyncIntegrationTest {
    private static final ThreadLocal<String> USER = new ThreadLocal<>();
    private static final List<DtpTaskFailureBO> FAILURES = new CopyOnWriteArrayList<>();
    private static final RuntimeException FAILURE = new IllegalStateException("async business error");
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DtpTaskContextAutoConfiguration.class))
            .withUserConfiguration(AsyncApplication.class);

    @Test
    void actualSpringProxyPreservesContextAndObservesVoidFutureAndCompletableFailures() {
        FAILURES.clear();
        runner.run(context -> {
            assertThat(context).hasSingleBean(AsyncConfigurer.class);
            USER.set("request-user");
            MDC.put("traceId", "request-trace");
            try {
                AsyncService service = context.getBean(AsyncService.class);
                assertThat(service.context().get(2, TimeUnit.SECONDS)).isEqualTo("request-user:request-trace");
                service.voidFailure();
                Future<String> future = service.futureFailure();
                CompletableFuture<String> stage = service.stageFailure();
                assertThatThrownBy(() -> future.get(2, TimeUnit.SECONDS)).hasCause(FAILURE);
                assertThatThrownBy(() -> stage.get(2, TimeUnit.SECONDS)).hasCause(FAILURE);
                await().untilAsserted(() -> assertThat(FAILURES).hasSize(3));
                assertThat(FAILURES).allMatch(failure -> failure.getCause() == FAILURE
                        && "request-trace".equals(failure.getTraceId()));
                assertThat(FAILURES).anyMatch(failure -> "ASYNC_VOID".equals(failure.getSource()));
                var observer = context.getBean(ManagedExecutorRegistry.class)
                        .get("applicationTaskExecutor").orElseThrow().taskObserver();
                assertThat(observer.failedTasks()).isEqualTo(3);
                assertThat(observer.succeededTasks()).isEqualTo(1);
                assertThat(USER.get()).isEqualTo("request-user");
                assertThat(MDC.get("traceId")).isEqualTo("request-trace");
            } finally { USER.remove(); MDC.clear(); }
        });
    }

    @Test
    void disabledMonitoringStillPropagatesContextAndReportsErrors() {
        FAILURES.clear();
        runner.withPropertyValues("egon.cola.component.dtp.observation.enabled=false").run(context -> {
            USER.set("user");
            MDC.put("traceId", "trace-disabled-metrics");
            try {
                AsyncService service = context.getBean(AsyncService.class);
                assertThat(service.context().get(2, TimeUnit.SECONDS)).isEqualTo("user:trace-disabled-metrics");
                service.voidFailure();
                await().untilAsserted(() -> assertThat(FAILURES).hasSize(1));
                assertThat(context.getBean(ManagedExecutorRegistry.class).get("applicationTaskExecutor")
                        .orElseThrow().taskObserver().submittedTasks()).isZero();
            } finally { USER.remove(); MDC.clear(); }
        });
    }

    @Test
    void unmanagedVoidReportsEachFailureButRestoresCallbackMdcAndLimitsOnlyDefaultLogging() throws Exception {
        var factory = new org.springframework.beans.factory.support.StaticListableBeanFactory();
        var reports = new CopyOnWriteArrayList<DtpTaskFailureBO>();
        factory.addBean("handler", (DtpTaskExceptionHandler) failure -> {
            reports.add(failure);
            MDC.put("traceId", "handler-mutation");
        });
        var properties = new DynamicThreadPoolAutoProperties();
        properties.getExceptionHandling().setLogInterval(java.time.Duration.ofMinutes(5));
        var handler = new top.egon.cola.component.dtp.exception.DtpAsyncUncaughtExceptionHandler(
                factory.getBeanProvider(DtpTaskExceptionHandler.class),
                factory.getBeanProvider(ValidationUtils.class), properties);
        MDC.put("traceId", "caller-trace");
        try {
            var method = AsyncService.class.getMethod("voidFailure");
            handler.handleUncaughtException(FAILURE, method, "private-argument");
            handler.handleUncaughtException(FAILURE, method, "private-argument");
            assertThat(reports).hasSize(2);
            assertThat(reports.getFirst().isLogEligible()).isTrue();
            assertThat(reports.getLast().isLogEligible()).isFalse();
            assertThat(reports).allMatch(failure -> failure.getExecutorName() == null
                    && failure.getTraceId().equals("caller-trace") && failure.getCause() == FAILURE);
            assertThat(MDC.get("traceId")).isEqualTo("caller-trace");
        } finally { MDC.clear(); }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    static class AsyncApplication {
        @Bean("userAccessor")
        @Qualifier("dtpContextAccessor")
        ThreadLocalAccessor<String> userAccessor() {
            return new ThreadLocalAccessor<>() {
                public Object key() { return "async.user"; }
                public String getValue() { return USER.get(); }
                public void setValue(String value) { USER.set(value); }
                public void setValue() { USER.remove(); }
            };
        }

        @Bean("dtpTaskExceptionHandler")
        DtpTaskExceptionHandler handler() { return FAILURES::add; }

        @Bean("dtpValidator")
        LocalValidatorFactoryBean validator() { return new LocalValidatorFactoryBean(); }

        @Bean("dtpValidationUtils")
        ValidationUtils validation(@Qualifier("dtpValidator") jakarta.validation.Validator validator) {
            return new ValidationUtils(validator);
        }

        @Bean("managedExecutorRegistry")
        ManagedExecutorRegistry registry(DynamicThreadPoolAutoProperties properties,
                @Qualifier("dtpTaskContextManager") ObjectProvider<DtpTaskContextManager> contexts,
                @Qualifier("dtpTaskExceptionHandler") ObjectProvider<DtpTaskExceptionHandler> handlers,
                @Qualifier("dtpValidationUtils") ObjectProvider<ValidationUtils> validation) {
            return new ManagedExecutorRegistry("test", "one", properties, contexts::getObject,
                    handlers::getObject, validation::getObject);
        }

        @Bean("threadManage")
        ThreadManage threadManage(@Qualifier("managedExecutorRegistry") ManagedExecutorRegistry registry,
                                 @Qualifier("dtpValidationUtils") ValidationUtils validation) {
            return new ThreadManage(registry, new DynamicThreadPoolService(registry, validation), validation);
        }

        @Bean("applicationTaskExecutor")
        ThreadPoolTaskExecutor executor(@Qualifier("threadManage") ThreadManage manager) {
            return manager.newThreadPool(new ThreadPoolCreateCommand().setExecutorName("applicationTaskExecutor")
                    .setCorePoolSize(1).setMaximumPoolSize(1).setQueueCapacity(8));
        }

        @Bean("asyncService")
        AsyncService service() { return new AsyncService(); }
    }

    static class AsyncService {
        @Async
        public Future<String> context() {
            return CompletableFuture.completedFuture(USER.get() + ":" + MDC.get("traceId"));
        }
        @Async
        public void voidFailure() { throw FAILURE; }
        @Async
        public Future<String> futureFailure() { throw FAILURE; }
        @Async
        public CompletableFuture<String> stageFailure() { throw FAILURE; }
    }
}
