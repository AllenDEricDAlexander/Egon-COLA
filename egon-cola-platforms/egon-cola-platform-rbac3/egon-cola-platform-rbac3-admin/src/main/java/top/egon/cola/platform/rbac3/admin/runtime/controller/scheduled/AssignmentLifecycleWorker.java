package top.egon.cola.platform.rbac3.admin.runtime.controller.scheduled;

import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.service.AssignmentLifecycleService;

/**
 * 触发到期授权分配的单批次处理。
 * Triggers one batch of due authorization-assignment processing.
 */
public final class AssignmentLifecycleWorker {

    /** 生命周期编排服务。Lifecycle orchestration service. */
    private final AssignmentLifecycleService service;

    /**
     * 创建调度入口。
     * Creates the scheduled entry point.
     *
     * @param service 生命周期编排服务；lifecycle orchestration service
     */
    public AssignmentLifecycleWorker(AssignmentLifecycleService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    /**
     * 处理一个有界批次。
     * Processes one bounded batch.
     *
     * @return 已处理数量；number of processed assignments
     */
    public int runOnce() {
        return service.runOnce();
    }
}
