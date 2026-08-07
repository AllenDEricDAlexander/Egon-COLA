package top.egon.cola.component.dtp.executor.adapter;

import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.executor.ManagedExecutor;
import top.egon.cola.component.dtp.executor.virtual.BoundedVirtualThreadExecutor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author      有罗敷的马同学
 * @description 有界虚拟线程托管执行器
 * @Date        下午9:52 2026/6/29
 **/
public class BoundedVirtualThreadManagedExecutor extends AbstractExecutorService implements ManagedExecutor {

    private final String appName;

    private final String instanceId;

    private final String executorName;

    private final BoundedVirtualThreadExecutor executor;

    public BoundedVirtualThreadManagedExecutor(String appName, String instanceId, String executorName,
                                               BoundedVirtualThreadExecutor executor) {
        this.appName = appName;
        this.instanceId = instanceId;
        this.executorName = executorName;
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(task, result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executor.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return executor.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return executor.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return executor.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
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
        return ExecutorKind.VIRTUAL_THREAD_PER_TASK;
    }

    @Override
    public ExecutorSnapshot snapshot() {
        ExecutorSnapshot snapshot = new ExecutorSnapshot();
        snapshot.setAppName(appName);
        snapshot.setInstanceId(instanceId);
        snapshot.setExecutorName(executorName);
        snapshot.setExecutorKind(kind());
        snapshot.setVirtual(true);
        snapshot.setResizable(false);
        snapshot.setConcurrencyLimit(executor.concurrencyLimit());
        snapshot.setRunningTasks(executor.runningTasks());
        snapshot.setSubmittedTasks(executor.submittedTasks());
        snapshot.setCompletedTaskCount(executor.completedTasks());
        snapshot.setFailedTasks(executor.failedTasks());
        snapshot.setRejectedTasks(executor.rejectedTasks());
        snapshot.setAvailablePermits(executor.availablePermits());
        snapshot.setReportTime(Instant.now());
        return snapshot;
    }

    @Override
    public UpdateResult update(ExecutorUpdateCommand command) {
        ExecutorSnapshot before = snapshot();
        UpdateResult result = new UpdateResult();
        result.setBefore(before);
        try {
            if (command.getConcurrencyLimit() == null) {
                result.setSuccess(false);
                result.setMessage("concurrencyLimit must not be null");
                result.setAfter(snapshot());
                return result;
            }
            executor.updateConcurrencyLimit(command.getConcurrencyLimit());
            result.setSuccess(true);
            result.setMessage("success");
        } catch (RuntimeException e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
        }
        result.setAfter(snapshot());
        return result;
    }

    @Override
    public boolean supportsResize() {
        return false;
    }

    @Override
    public boolean supportsVirtualThread() {
        return true;
    }

    @Override
    public boolean supportsQueueMetrics() {
        return false;
    }

}
