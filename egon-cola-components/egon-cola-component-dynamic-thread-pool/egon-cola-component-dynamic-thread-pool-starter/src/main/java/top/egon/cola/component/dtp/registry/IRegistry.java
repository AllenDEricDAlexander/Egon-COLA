package top.egon.cola.component.dtp.registry;

import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ThreadPoolConfigEntity;
import top.egon.cola.component.dtp.registry.model.DtpAuditEvent;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;

import java.util.List;

/**
 * @ClassName: IRegistry
 * @description: 注册中心接口
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-14Day-下午8:14
 * @Version: 1.0
 */
public interface IRegistry {

    void reportSnapshot(ExecutorSnapshot snapshot);

    void reportSnapshots(List<ExecutorSnapshot> snapshots);

    List<ExecutorSnapshot> querySnapshots(String appName, String instanceId);

    ExecutorSnapshot querySnapshot(String appName, String instanceId, String executorName);

    void publishConfigChange(DtpConfigChangeMessage message);

    void recordAuditEvent(DtpAuditEvent event);

    List<DtpAuditEvent> queryAuditEvents(String appName, String date);

    @Deprecated
    default void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolEntities) {
        throw new UnsupportedOperationException("reportThreadPool is deprecated; implement reportSnapshots instead");
    }

    @Deprecated
    default void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity) {
        throw new UnsupportedOperationException("reportThreadPoolConfigParameter is deprecated; implement reportSnapshot instead");
    }

}
