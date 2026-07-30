package top.egon.cola.component.ddc.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;

import java.time.LocalDateTime;

@Service
public class PublishFailureRecorder {

    private final DdcPublishTaskRepository publishTaskRepository;

    public PublishFailureRecorder(DdcPublishTaskRepository publishTaskRepository) {
        this.publishTaskRepository = publishTaskRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String changeId, String appCode, String env, String namespace, String configKey, String errorMessage) {
        DdcPublishTaskEntity task = publishTaskRepository.findByChangeId(changeId)
                .orElseGet(() -> newFailedTask(changeId, appCode, env, namespace, configKey));
        task.setStatus(PublishStatus.FAILED.name());
        task.setErrorMessage(errorMessage);
        task.setUpdatedAt(LocalDateTime.now());
        publishTaskRepository.save(task);
    }

    private DdcPublishTaskEntity newFailedTask(String changeId, String appCode, String env, String namespace, String configKey) {
        LocalDateTime now = LocalDateTime.now();
        DdcPublishTaskEntity task = new DdcPublishTaskEntity();
        task.setId(UuidV7.simpleString());
        task.setChangeId(changeId);
        task.setAppCode(appCode);
        task.setEnv(env);
        task.setNamespace(namespace);
        task.setConfigKey(configKey);
        task.setStatus(PublishStatus.FAILED.name());
        task.setTargetCount(0);
        task.setAckCount(0);
        task.setFailedCount(0);
        task.setIgnoredCount(0);
        task.setTimeoutCount(0);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }
}
