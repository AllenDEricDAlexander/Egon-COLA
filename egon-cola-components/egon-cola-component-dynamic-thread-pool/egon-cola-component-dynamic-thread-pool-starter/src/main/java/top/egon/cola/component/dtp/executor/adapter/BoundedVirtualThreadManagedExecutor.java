package top.egon.cola.component.dtp.executor.adapter;

import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.executor.ManagedExecutor;
import top.egon.cola.component.dtp.executor.virtual.BoundedVirtualThreadExecutor;

import java.time.Instant;

/**
 * @author      有罗敷的马同学
 * @description 有界虚拟线程托管执行器
 * @Date        下午9:52 2026/6/29
 **/
public class BoundedVirtualThreadManagedExecutor implements ManagedExecutor {

    private final String appName;

    private final String instanceId;

    private final String executorName;

    private final BoundedVirtualThreadExecutor executor;

    public BoundedVirtualThreadManagedExecutor(String appName, String instanceId, String executorName,
                                               BoundedVirtualThreadExecutor executor) {
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
