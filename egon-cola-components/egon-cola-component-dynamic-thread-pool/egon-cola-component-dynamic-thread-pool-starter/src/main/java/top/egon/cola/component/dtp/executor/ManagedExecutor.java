package top.egon.cola.component.dtp.executor;

import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;

/**
 * @author      有罗敷的马同学
 * @description 托管执行器接口
 * @Date        上午8:55 2026/6/29
 **/
public interface ManagedExecutor {

    String appName();

    String instanceId();

    String executorName();

    ExecutorKind kind();

    ExecutorSnapshot snapshot();

    UpdateResult update(ExecutorUpdateCommand command);

    boolean supportsResize();

    boolean supportsVirtualThread();

    boolean supportsQueueMetrics();

}
