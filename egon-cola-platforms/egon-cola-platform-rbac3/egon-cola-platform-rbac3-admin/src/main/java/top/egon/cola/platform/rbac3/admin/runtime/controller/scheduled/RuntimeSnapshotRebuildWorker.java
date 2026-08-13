package top.egon.cola.platform.rbac3.admin.runtime.controller.scheduled;

import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.EventEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeProjectionService;

/**
 * 接收运行时快照重建请求并交给投影服务处理。
 * Receives runtime snapshot rebuild requests and delegates them to the projection service.
 */
public final class RuntimeSnapshotRebuildWorker {

    /** 运行时投影服务。Runtime projection service. */
    private final RuntimeProjectionService service;

    /**
     * 创建重建入口。
     * Creates the rebuild entry point.
     *
     * @param service 运行时投影服务；runtime projection service
     */
    public RuntimeSnapshotRebuildWorker(RuntimeProjectionService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    /**
     * 投影一个授权事件。
     * Projects one authorization event.
     *
     * @param event 事件信封；event envelope
     * @return 投影结果；projection outcome
     */
    public Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum project(
            EventEnvelopeVO event) {
        return service.project(event);
    }
}
