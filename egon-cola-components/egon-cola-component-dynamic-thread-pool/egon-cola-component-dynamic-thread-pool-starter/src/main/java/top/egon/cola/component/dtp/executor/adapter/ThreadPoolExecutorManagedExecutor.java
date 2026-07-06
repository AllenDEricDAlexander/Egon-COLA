package top.egon.cola.component.dtp.executor.adapter;

import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.executor.ManagedExecutor;
import top.egon.cola.component.dtp.executor.support.ThreadPoolResizeSupport;

import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author      有罗敷的马同学
 * @description 平台线程池托管执行器
 * @Date        上午8:55 2026/6/29
 **/
public class ThreadPoolExecutorManagedExecutor implements ManagedExecutor {

    private final String appName;

    private final String instanceId;

    private final String executorName;

    private final ThreadPoolExecutor executor;

    public ThreadPoolExecutorManagedExecutor(String appName, String instanceId, String executorName, ThreadPoolExecutor executor) {
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
        snapshot.setQueueType(queue.getClass().getSimpleName());
        snapshot.setQueueSize(queue.size());
        snapshot.setRemainingCapacity(queue.remainingCapacity());
        snapshot.setReportTime(Instant.now());
        return snapshot;
    }

    @Override
    public UpdateResult update(ExecutorUpdateCommand command) {
        ExecutorSnapshot before = snapshot();
        UpdateResult result = new UpdateResult();
        result.setBefore(before);
        try {
            String validationMessage = validate(command);
            if (validationMessage != null) {
                result.setSuccess(false);
                result.setMessage(validationMessage);
                result.setAfter(snapshot());
                return result;
            }
            if (command.getCorePoolSize() != null && command.getMaximumPoolSize() != null) {
                ThreadPoolResizeSupport.resize(executor, command.getCorePoolSize(), command.getMaximumPoolSize());
            }
            if (Boolean.FALSE.equals(command.getAllowCoreThreadTimeOut())) {
                executor.allowCoreThreadTimeOut(false);
            }
            if (command.getKeepAliveSeconds() != null) {
                executor.setKeepAliveTime(command.getKeepAliveSeconds(), TimeUnit.SECONDS);
            }
            if (Boolean.TRUE.equals(command.getAllowCoreThreadTimeOut())) {
                executor.allowCoreThreadTimeOut(true);
            }
            result.setSuccess(true);
            result.setMessage("success");
        } catch (RuntimeException e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
        }
        result.setAfter(snapshot());
        return result;
    }

    private String validate(ExecutorUpdateCommand command) {
        boolean hasCorePoolSize = command.getCorePoolSize() != null;
        boolean hasMaximumPoolSize = command.getMaximumPoolSize() != null;
        if (hasCorePoolSize != hasMaximumPoolSize) {
            return "corePoolSize and maximumPoolSize must be provided together";
        }
        if (hasCorePoolSize) {
            if (command.getCorePoolSize() <= 0 || command.getMaximumPoolSize() <= 0) {
                return "corePoolSize and maximumPoolSize must be positive";
            }
            if (command.getCorePoolSize() > command.getMaximumPoolSize()) {
                return "corePoolSize must <= maximumPoolSize";
            }
        }
        boolean targetAllowCoreThreadTimeOut = command.getAllowCoreThreadTimeOut() != null
                ? command.getAllowCoreThreadTimeOut()
                : executor.allowsCoreThreadTimeOut();
        long targetKeepAliveSeconds = command.getKeepAliveSeconds() != null
                ? command.getKeepAliveSeconds()
                : executor.getKeepAliveTime(TimeUnit.SECONDS);
        if (targetKeepAliveSeconds < 0) {
            return "keepAliveSeconds must be >= 0";
        }
        if (targetAllowCoreThreadTimeOut && targetKeepAliveSeconds <= 0) {
            return "keepAliveSeconds must be > 0 when allowCoreThreadTimeOut is true";
        }
        return null;
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
