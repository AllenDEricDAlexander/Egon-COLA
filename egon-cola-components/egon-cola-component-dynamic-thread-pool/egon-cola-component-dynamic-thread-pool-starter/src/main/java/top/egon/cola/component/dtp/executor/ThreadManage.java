package top.egon.cola.component.dtp.executor;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.ThreadPoolCreateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.executor.managed.DtpThreadPoolTaskExecutor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import top.egon.cola.component.dtp.context.DtpTaskObserver;

/**
 * @description 统一命名管理入口，复用现有注册表及调参服务
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ThreadManage implements DisposableBean {

    @Qualifier("managedExecutorRegistry")
    private final ManagedExecutorRegistry registry;
    @Qualifier("dynamicThreadPollService")
    private final IDynamicThreadPoolService service;
    @Qualifier("dtpValidationUtils")
    private final ValidationUtils validation;

    public ThreadPoolTaskExecutor newThreadPool(ThreadPoolCreateCommand command) {
        Objects.requireNonNull(command, "command");
        ThreadPoolCreateCommand configuration = command.toBuilder().build();
        try {
            validation.validate(configuration, ThreadPoolCreateCommand.Create.class);
        } catch (ConstraintViolationException e) {
            throw new IllegalArgumentException("invalid thread pool creation configuration", e);
        }
        return registry.create(configuration, observer -> {
            DtpThreadPoolTaskExecutor pool = new DtpThreadPoolTaskExecutor(observer);
            try {
                pool.setCorePoolSize(configuration.getCorePoolSize());
                pool.setMaxPoolSize(configuration.getMaximumPoolSize());
                pool.setQueueCapacity(configuration.getQueueCapacity());
                pool.setKeepAliveSeconds(Math.toIntExact(configuration.getKeepAlive().getSeconds()));
                pool.setAllowCoreThreadTimeOut(configuration.isAllowCoreThreadTimeOut());
                pool.setThreadNamePrefix(configuration.getExecutorName() + "-");
                if (configuration.getThreadFactory() != null) { pool.setThreadFactory(configuration.getThreadFactory()); }
                if (configuration.getRejectedExecutionHandler() != null) { pool.setRejectedExecutionHandler(configuration.getRejectedExecutionHandler()); }
                pool.initialize();
                log.info("动态线程池创建完成 executor={} core={} max={} queueCapacity={}",
                        configuration.getExecutorName(), configuration.getCorePoolSize(),
                        configuration.getMaximumPoolSize(), configuration.getQueueCapacity());
                return pool;
            } catch (RuntimeException | Error e) {
                pool.shutdown();
                throw e;
            }
        });
    }

    public ManagedExecutor executor(String executorName) {
        if (executorName == null || executorName.isBlank()) {
            throw new IllegalArgumentException("executorName must not be blank");
        }
        return registry.get(executorName)
                .orElseThrow(() -> new IllegalArgumentException("executor not found: " + executorName));
    }

    public UpdateResult changeThreadPool(ExecutorUpdateCommand command) { return service.updateExecutor(command); }

    public CompletableFuture<Void> runAsync(String executorName, Runnable task) {
        Objects.requireNonNull(task, "task");
        ManagedExecutor executor = executor(executorName);
        return observer(executor).completable(executor, task, () -> { task.run(); return null; }, registry.createsRoot(executor));
    }

    public <T> CompletableFuture<T> supplyAsync(String executorName, Supplier<T> task) {
        Objects.requireNonNull(task, "task");
        ManagedExecutor executor = executor(executorName);
        return observer(executor).completable(executor, task, task::get, registry.createsRoot(executor));
    }

    /** 请求/异步链局部视图：绑定调用时点的上下文，不应注册为跨请求单例。 */
    public Executor contextualExecutor(String executorName) {
        ManagedExecutor executor = executor(executorName);
        return observer(executor).contextualExecutor(executor, registry.createsRoot(executor));
    }

    private DtpTaskObserver observer(ManagedExecutor executor) {
        DtpTaskObserver observer = executor.taskObserver();
        if (observer == null) { throw new UnsupportedOperationException("executor does not support managed task observation"); }
        return observer;
    }

    @Override
    public void destroy() { registry.closeOwned(); }
}
