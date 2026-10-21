package top.egon.cola.archetype.source.light.start.config.async;

import top.egon.cola.component.common.core.exception.ExceptionLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@EnableAsync
@Configuration("asyncConfiguration")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AsyncConfiguration implements AsyncConfigurer {

    @Qualifier("dtpAsyncUncaughtExceptionHandler")
    private final ObjectProvider<AsyncUncaughtExceptionHandler> dtpExceptionHandler;

    /** 保留未安装DTP组件时的手工构造及原日志策略。 */
    public AsyncConfiguration() {
        this(null);
    }

    @Override
    public Executor getAsyncExecutor() {
        return null;
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
