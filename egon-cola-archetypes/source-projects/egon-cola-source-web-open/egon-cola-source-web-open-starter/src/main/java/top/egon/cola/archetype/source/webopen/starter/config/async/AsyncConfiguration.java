package top.egon.cola.archetype.source.webopen.starter.config.async;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.dtp.context.DtpTaskDecorator;

@Slf4j
@EnableAsync
@Configuration
public class AsyncConfiguration implements AsyncConfigurer {

    private final Executor applicationTaskExecutor;

    public AsyncConfiguration(
            @Lazy @Qualifier("applicationTaskExecutor") Executor applicationTaskExecutor) {
        this.applicationTaskExecutor = applicationTaskExecutor;
    }

    @Bean
    DtpTaskDecorator dtpTaskDecorator() {
        return new DtpTaskDecorator();
    }

    @Bean(name = "applicationTaskExecutor")
    ThreadPoolTaskExecutor applicationTaskExecutor(
            ThreadPoolTaskExecutorBuilder builder, DtpTaskDecorator decorator) {
        ThreadPoolTaskExecutor executor = builder.build();
        executor.setTaskDecorator(decorator);
        return executor;
    }

    @Bean
    ThreadPoolTaskExecutorCustomizer dtpTaskExecutorCustomizer(
            DtpTaskDecorator decorator) {
        return executor -> executor.setTaskDecorator(decorator);
    }

    @Override
    public Executor getAsyncExecutor() {
        return applicationTaskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
            log.error("Async method failed: {}", method.toGenericString(), ex);
    }
}
