package top.egon.cola.component.dtp.executor.adapter;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.executor.ManagedExecutor;

import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author      有罗敷的马同学
 * @description Spring 线程池托管执行器
 * @Date        下午11:15 2026/6/29
 **/
public class ThreadPoolTaskExecutorManagedExecutor implements ManagedExecutor {

    private final String appName;

    private final String instanceId;

    private final String executorName;

    private final ThreadPoolTaskExecutor executor;

    public ThreadPoolTaskExecutorManagedExecutor(String appName, String instanceId, String executorName,
                                                 ThreadPoolTaskExecutor executor) {
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
                resize(command.getCorePoolSize(), command.getMaximumPoolSize());
            }
            ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
            if (Boolean.FALSE.equals(command.getAllowCoreThreadTimeOut())) {
                executor.setAllowCoreThreadTimeOut(false);
            }
            if (command.getKeepAliveSeconds() != null) {
                executor.setKeepAliveSeconds(command.getKeepAliveSeconds().intValue());
            }
            if (Boolean.TRUE.equals(command.getAllowCoreThreadTimeOut())) {
                executor.setAllowCoreThreadTimeOut(true);
            }
            if (command.getKeepAliveSeconds() != null) {
                threadPoolExecutor.setKeepAliveTime(command.getKeepAliveSeconds(), TimeUnit.SECONDS);
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

    private void resize(int newCore, int newMax) {
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
        if (newMax < threadPoolExecutor.getCorePoolSize()) {
            executor.setCorePoolSize(newCore);
            executor.setMaxPoolSize(newMax);
            return;
        }
        executor.setMaxPoolSize(newMax);
        executor.setCorePoolSize(newCore);
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
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
        boolean targetAllowCoreThreadTimeOut = command.getAllowCoreThreadTimeOut() != null
                ? command.getAllowCoreThreadTimeOut()
                : threadPoolExecutor.allowsCoreThreadTimeOut();
        long targetKeepAliveSeconds = command.getKeepAliveSeconds() != null
                ? command.getKeepAliveSeconds()
                : threadPoolExecutor.getKeepAliveTime(TimeUnit.SECONDS);
        if (targetKeepAliveSeconds < 0) {
            return "keepAliveSeconds must be >= 0";
        }
        if (targetAllowCoreThreadTimeOut && targetKeepAliveSeconds <= 0) {
            return "keepAliveSeconds must be > 0 when allowCoreThreadTimeOut is true";
        }
        if (targetKeepAliveSeconds > Integer.MAX_VALUE) {
            return "keepAliveSeconds must be <= " + Integer.MAX_VALUE;
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
