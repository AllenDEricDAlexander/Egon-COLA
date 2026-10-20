package top.egon.cola.component.dtp.executor.adapter;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.dtp.context.DtpRunnable;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.executor.ManagedExecutor;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.dtp.context.DtpTaskObserver;
import top.egon.cola.component.dtp.executor.support.ResizableCapacityBlockingQueue;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.Collection;
import top.egon.cola.component.dtp.executor.support.ThreadPoolResizeSupport;
import top.egon.cola.component.dtp.executor.managed.DtpThreadPoolTaskExecutor;

/**
 * @author      有罗敷的马同学
 * @description Spring 线程池托管执行器
 * @Date        下午11:15 2026/6/29
 **/
@Slf4j
public class ThreadPoolTaskExecutorManagedExecutor extends AbstractExecutorService implements ManagedExecutor {

    private final String appName;

    private final String instanceId;

    private final String executorName;

    private final ThreadPoolTaskExecutor executor;

    private final DtpTaskObserver observer;

    public ThreadPoolTaskExecutorManagedExecutor(String appName, String instanceId, String executorName, ThreadPoolTaskExecutor executor) {
        this(appName, instanceId, executorName, executor, executor instanceof DtpThreadPoolTaskExecutor pool ? pool.taskObserver() : null);
    }

    public ThreadPoolTaskExecutorManagedExecutor(String appName, String instanceId, String executorName, ThreadPoolTaskExecutor executor, DtpTaskObserver observer) {
        this.appName = appName;
        this.instanceId = instanceId;
        this.executorName = executorName;
        this.executor = Objects.requireNonNull(executor, "executor");
        this.observer = observer;
    }

    @Override
    public DtpTaskObserver taskObserver() { return observer; }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> task) {
        return observer == null ? super.newTaskFor(task) : observer.future(task, executor instanceof DtpThreadPoolTaskExecutor);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable task, T value) {
        return observer == null ? super.newTaskFor(task, value) : observer.future(task, value, executor instanceof DtpThreadPoolTaskExecutor);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        if (observer == null) { return super.invokeAny(tasks); }
        try { return observer.invokeAny(this, tasks, false, 0, executor instanceof DtpThreadPoolTaskExecutor); }
        catch (TimeoutException e) { throw new AssertionError(e); }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return observer == null ? super.invokeAny(tasks, timeout, unit)
                : observer.invokeAny(this, tasks, true, unit.toNanos(timeout), executor instanceof DtpThreadPoolTaskExecutor);
    }

    @Override
    public void execute(Runnable command) {
        if (executor instanceof DtpThreadPoolTaskExecutor) { executor.execute(command); }
        else if (observer == null) { executor.execute(DtpRunnable.wrap(command)); }
        else { observer.dispatch(observer.decorate(command, false), executor); }
    }

    @Override
    public void shutdown() {
        if (executor instanceof DtpThreadPoolTaskExecutor) { executor.shutdown(); }
        else { executor.getThreadPoolExecutor().shutdown(); }
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executor.getThreadPoolExecutor().shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executor.getThreadPoolExecutor().isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.getThreadPoolExecutor().isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.getThreadPoolExecutor().awaitTermination(timeout, unit);
    }

    @Override
    public String appName() {
        return appName;
    }

    @Override
    public String instanceId() {
        return instanceId;
    }

    @Override
    public String executorName() {
        return executorName;
    }

    @Override
    public ExecutorKind kind() {
        return ExecutorKind.SPRING_THREAD_POOL_TASK_EXECUTOR;
    }

    @Override
    public ExecutorSnapshot snapshot() {
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
        BlockingQueue<Runnable> queue = threadPoolExecutor.getQueue();
        ExecutorSnapshot snapshot = new ExecutorSnapshot();
        snapshot.setAppName(appName);
        snapshot.setInstanceId(instanceId);
        snapshot.setExecutorName(executorName);
        snapshot.setExecutorKind(kind());
        snapshot.setVirtual(false);
        snapshot.setResizable(true);
        snapshot.setCorePoolSize(threadPoolExecutor.getCorePoolSize());
        snapshot.setMaximumPoolSize(threadPoolExecutor.getMaximumPoolSize());
        snapshot.setActiveCount(threadPoolExecutor.getActiveCount());
        snapshot.setPoolSize(threadPoolExecutor.getPoolSize());
        snapshot.setTaskCount(threadPoolExecutor.getTaskCount());
        snapshot.setCompletedTaskCount(threadPoolExecutor.getCompletedTaskCount());
        ThreadPoolResizeSupport.queueSnapshot(queue, snapshot);
        if (taskObserver() != null) {
            snapshot.setSubmittedTasks(taskObserver().submittedTasks());
            snapshot.setFailedTasks(taskObserver().failedTasks());
            snapshot.setRejectedTasks(taskObserver().rejectedTasks());
        }
        snapshot.setReportTime(Instant.now());
        return snapshot;
    }

    @Override
    public UpdateResult update(ExecutorUpdateCommand command) {
        synchronized (executor.getThreadPoolExecutor()) {
            UpdateResult result = new UpdateResult();
            result.setBefore(snapshot());
            try {
                ThreadPoolResizeSupport.update(executor.getThreadPoolExecutor(), executor, command);
                result.setSuccess(true);
                result.setMessage("success");
            } catch (RuntimeException e) {
                result.setSuccess(false);
                result.setMessage(e.getMessage());
            }
            result.setAfter(snapshot());
            return result;
        }
    }

    @Override
    public boolean supportsQueueResize() {
        return executor.getThreadPoolExecutor().getQueue() instanceof ResizableCapacityBlockingQueue<?>;
    }

    @Override
    public boolean supportsResize() {
        return true;
    }

    @Override
    public boolean supportsVirtualThread() {
        return false;
    }

    @Override
    public boolean supportsQueueMetrics() {
        return true;
    }

}
