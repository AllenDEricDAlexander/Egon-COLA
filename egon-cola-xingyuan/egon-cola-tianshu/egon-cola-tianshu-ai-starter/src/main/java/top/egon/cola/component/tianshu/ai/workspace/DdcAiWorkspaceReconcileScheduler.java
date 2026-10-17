package top.egon.cola.component.tianshu.ai.workspace;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.tianshu.ai.properties.DdcAiWatchedResourceDTO;
import top.egon.cola.component.tianshu.ai.properties.DdcAiWorkspaceProperties;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 周期对账兜底：使用自管调度线程，不依赖宿主的 @EnableScheduling。
 * / Periodic reconcile fallback on a self-managed scheduler thread, independent of the host's @EnableScheduling.
 */
@Slf4j
public class DdcAiWorkspaceReconcileScheduler {

    private final DdcAiSkillWorkspaceService workspace;
    private final DdcAiWorkspaceProperties properties;
    private final ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> task;

    public DdcAiWorkspaceReconcileScheduler(DdcAiSkillWorkspaceService workspace, DdcAiWorkspaceProperties properties,
            ScheduledExecutorService scheduler) {
        this.workspace = Objects.requireNonNull(workspace, "workspace");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    public void start() {
        long interval = Math.max(1L, properties.getReconcileInterval().toMillis());
        task = scheduler.scheduleWithFixedDelay(this::reconcileOnce, 0L, interval, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        ScheduledFuture<?> current = task;
        if (current != null) current.cancel(false);
    }

    /** 逐资源对账，单个失败不影响其他资源。 / Reconciles every resource; one failure never stops the rest. */
    public int reconcileOnce() {
        int reconciled = 0;
        for (DdcAiWatchedResourceDTO resource : properties.getResources()) {
            try {
                workspace.reconcile(resource);
                reconciled++;
            } catch (RuntimeException failure) {
                log.warn("AI workspace reconcile failed for {}", resource.watchKey(), failure);
            }
        }
        return reconciled;
    }
}
