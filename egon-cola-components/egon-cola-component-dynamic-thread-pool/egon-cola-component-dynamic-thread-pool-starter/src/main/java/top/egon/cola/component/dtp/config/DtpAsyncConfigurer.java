package top.egon.cola.component.dtp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.core.env.Environment;
import java.util.concurrent.Executor;

/** 只提供异常策略，默认执行器解析继续交给Spring。 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DtpAsyncConfigurer implements AsyncConfigurer {
    @Qualifier("dtpAsyncUncaughtExceptionHandler")
    private final ObjectProvider<AsyncUncaughtExceptionHandler> handler;
    @Qualifier("applicationTaskExecutor")
    private final ObjectProvider<Executor> applicationExecutor;
    @Qualifier("environment")
    private final Environment environment;

    @Override
    public Executor getAsyncExecutor() {
        // Boot force模式原本明确选applicationTaskExecutor，不能退回Spring的多池猜测。
        return "force".equals(environment.getProperty("spring.task.execution.mode"))
                ? applicationExecutor.getIfAvailable() : null;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (failure, method, params) -> handler.getObject().handleUncaughtException(failure, method, params);
    }
}
