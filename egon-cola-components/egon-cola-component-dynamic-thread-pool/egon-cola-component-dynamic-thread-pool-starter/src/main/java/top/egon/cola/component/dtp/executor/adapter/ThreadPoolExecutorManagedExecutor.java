package top.egon.cola.component.dtp.executor.adapter;

import top.egon.cola.component.dtp.context.DtpContextAwareExecutorService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.executor.ManagedExecutor;
import top.egon.cola.component.dtp.executor.support.ThreadPoolResizeSupport;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.dtp.context.DtpTaskObserver;
import top.egon.cola.component.dtp.executor.support.ResizableCapacityBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author      有罗敷的马同学
 * @description 平台线程池托管执行器
 * @Date        上午8:55 2026/6/29
 **/
@Slf4j
public class ThreadPoolExecutorManagedExecutor extends DtpContextAwareExecutorService implements ManagedExecutor {

    private final String appName;

    private final String instanceId;

    private final String executorName;

    private final ThreadPoolExecutor executor;

    public ThreadPoolExecutorManagedExecutor(String appName, String instanceId, String executorName, ThreadPoolExecutor executor) {
        this(appName, instanceId, executorName, executor, null);
    }

    public ThreadPoolExecutorManagedExecutor(String appName, String instanceId, String executorName, ThreadPoolExecutor executor, DtpTaskObserver observer) {
        super(executor, observer);
        this.appName = appName;
        this.instanceId = instanceId;
        this.executorName = executorName;
        this.executor = executor;
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
        return ExecutorKind.PLATFORM_THREAD_POOL;
    }

    @Override
    public ExecutorSnapshot snapshot() {
        BlockingQueue<Runnable> queue = executor.getQueue();
        ExecutorSnapshot snapshot = new ExecutorSnapshot();
        snapshot.setAppName(appName);
        snapshot.setInstanceId(instanceId);
        snapshot.setExecutorName(executorName);
        snapshot.setExecutorKind(kind());
        snapshot.setVirtual(false);
        snapshot.setResizable(true);
        snapshot.setCorePoolSize(executor.getCorePoolSize());
        snapshot.setMaximumPoolSize(executor.getMaximumPoolSize());
        snapshot.setActiveCount(executor.getActiveCount());
        snapshot.setPoolSize(executor.getPoolSize());
        snapshot.setTaskCount(executor.getTaskCount());
        snapshot.setCompletedTaskCount(executor.getCompletedTaskCount());
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
        synchronized (executor) {
            UpdateResult result = new UpdateResult();
            result.setBefore(snapshot());
            try {
                ThreadPoolResizeSupport.update(executor, null, command);
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
        return executor.getQueue() instanceof ResizableCapacityBlockingQueue<?>;
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
