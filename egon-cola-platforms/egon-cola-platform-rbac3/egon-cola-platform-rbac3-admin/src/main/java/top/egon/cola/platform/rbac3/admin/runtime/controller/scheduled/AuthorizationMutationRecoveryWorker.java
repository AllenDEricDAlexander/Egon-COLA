package top.egon.cola.platform.rbac3.admin.runtime.controller.scheduled;

import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationMutationRecoveryService;

/**
 * 触发待恢复授权变更的单批次处理。
 * Triggers one batch of pending authorization-mutation recovery.
 */
public final class AuthorizationMutationRecoveryWorker {

    /** 授权变更恢复服务。Authorization-mutation recovery service. */
    private final AuthorizationMutationRecoveryService service;

    /**
     * 创建调度入口。
     * Creates the scheduled entry point.
     *
     * @param service 授权变更恢复服务；authorization-mutation recovery service
     */
    public AuthorizationMutationRecoveryWorker(
            AuthorizationMutationRecoveryService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    /**
     * 处理一个有界恢复批次。
     * Processes one bounded recovery batch.
     *
     * @return 已完成数量；number of completed mutations
     */
    public int runOnce() {
        return service.runOnce();
    }
}
