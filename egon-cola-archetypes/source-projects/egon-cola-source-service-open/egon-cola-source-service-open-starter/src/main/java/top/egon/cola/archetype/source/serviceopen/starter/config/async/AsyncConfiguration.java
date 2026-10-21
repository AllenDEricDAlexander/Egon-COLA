package top.egon.cola.archetype.source.serviceopen.starter.config.async;

import top.egon.cola.component.common.core.exception.ExceptionLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
import top.egon.cola.component.dtp.context.DtpTaskContextManager;

@Slf4j
@EnableAsync
@Configuration("asyncConfiguration")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AsyncConfiguration implements AsyncConfigurer {

    @Lazy
    @Qualifier("applicationTaskExecutor")
    private final Executor applicationTaskExecutor;

    @Qualifier("dtpAsyncUncaughtExceptionHandler")
    private final ObjectProvider<AsyncUncaughtExceptionHandler> dtpExceptionHandler;


    /** 保留既有手工构造入口；Spring使用完整的限定构造器。 */
    public AsyncConfiguration(Executor applicationTaskExecutor) {
        this(applicationTaskExecutor, null);
    }

    @Bean("dtpTaskDecorator")
    DtpTaskDecorator dtpTaskDecorator(
            @Qualifier("dtpTaskContextManager") ObjectProvider<DtpTaskContextManager> contexts) {
        return new DtpTaskDecorator(contexts);
    }

    @Bean(name = "applicationTaskExecutor")
    ThreadPoolTaskExecutor applicationTaskExecutor(
            ThreadPoolTaskExecutorBuilder builder, DtpTaskDecorator decorator) {
        ThreadPoolTaskExecutor executor = builder.build();
        executor.setTaskDecorator(decorator);
        return executor;
    }

    @Bean("dtpTaskExecutorCustomizer")
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
        if (dtpExceptionHandler != null) {
            AsyncUncaughtExceptionHandler handler = dtpExceptionHandler.getIfAvailable();
            if (handler != null) { return handler; }
        }
        return (Throwable ex, Method method, Object... params) ->
            ExceptionLogger.log(log, ex);
    }
}
