package top.egon.cola.component.dtp.config;

import io.micrometer.context.ThreadLocalAccessor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.dtp.context.DtpTaskContextManager;
import top.egon.cola.component.dtp.context.DtpTaskDecorator;
import top.egon.cola.component.dtp.exception.DtpAsyncUncaughtExceptionHandler;
import top.egon.cola.component.dtp.exception.DtpTaskExceptionHandler;
import top.egon.cola.component.dtp.exception.LoggingDtpTaskExceptionHandler;

import java.util.List;

/** 在Boot构建默认池前提供缺省装饰器，不替换用户执行器或已配置的装饰器。 */
@AutoConfiguration(before = TaskExecutionAutoConfiguration.class)
@EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
@ConditionalOnProperty(prefix = "egon.cola.component.dtp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DtpTaskContextAutoConfiguration {
    @Bean("dtpTaskContextManager")
    @ConditionalOnMissingBean(name = "dtpTaskContextManager")
    public DtpTaskContextManager dtpTaskContextManager(
            @Qualifier("dtpContextAccessor") ObjectProvider<ThreadLocalAccessor<?>> accessors,
            DynamicThreadPoolAutoProperties properties) {
        return new DtpTaskContextManager(properties.getContextPropagation().isCustomThreadLocalsEnabled()
                ? accessors.orderedStream().toList() : List.of(), properties.getExceptionHandling().getLogInterval());
    }

    @Bean("dtpTaskDecorator")
    @ConditionalOnMissingBean(TaskDecorator.class)
    public DtpTaskDecorator dtpTaskDecorator(@Qualifier("dtpTaskContextManager") ObjectProvider<DtpTaskContextManager> contexts) {
        return new DtpTaskDecorator(contexts);
    }

    @Bean("dtpTaskExceptionHandler")
    @ConditionalOnMissingBean(name = "dtpTaskExceptionHandler")
    public DtpTaskExceptionHandler dtpTaskExceptionHandler() {
        return new LoggingDtpTaskExceptionHandler();
    }

    @Bean("dtpAsyncUncaughtExceptionHandler")
    @ConditionalOnMissingBean(name = "dtpAsyncUncaughtExceptionHandler")
    public AsyncUncaughtExceptionHandler dtpAsyncUncaughtExceptionHandler(
            @Qualifier("dtpTaskExceptionHandler") ObjectProvider<DtpTaskExceptionHandler> handlers,
            @Qualifier("dtpValidationUtils") ObjectProvider<ValidationUtils> validation,
            DynamicThreadPoolAutoProperties properties) {
        return new DtpAsyncUncaughtExceptionHandler(handlers, validation, properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(name = "org.springframework.context.annotation.internalAsyncAnnotationProcessor")
    static class EnabledAsyncConfiguration {
        @Bean("dtpAsyncConfigurer")
        @ConditionalOnMissingBean(AsyncConfigurer.class)
        DtpAsyncConfigurer dtpAsyncConfigurer(
                @Qualifier("dtpAsyncUncaughtExceptionHandler") ObjectProvider<AsyncUncaughtExceptionHandler> handler,
                @Qualifier("applicationTaskExecutor") ObjectProvider<java.util.concurrent.Executor> executor,
                org.springframework.core.env.Environment environment) {
            return new DtpAsyncConfigurer(handler, executor, environment);
        }
    }
}
