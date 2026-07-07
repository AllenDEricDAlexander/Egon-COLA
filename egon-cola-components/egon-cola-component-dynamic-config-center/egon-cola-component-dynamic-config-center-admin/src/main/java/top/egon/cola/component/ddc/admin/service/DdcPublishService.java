package top.egon.cola.component.ddc.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.model.dto.DdcPublishRequest;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcOperationLogEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.ChangeType;
import top.egon.cola.component.ddc.admin.model.enums.InstanceStatus;
import top.egon.cola.component.ddc.admin.model.enums.PublishMode;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.ddc.admin.repository.DdcOperationLogRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.admin.service.policy.PublishConsistencyPolicyFactory;
import top.egon.cola.component.ddc.admin.service.policy.PublishDecision;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
public class DdcPublishService {

    private final DdcConfigItemRepository configItemRepository;

    private final DdcConfigVersionRepository versionRepository;

    private final DdcPublishTaskRepository publishTaskRepository;

    private final DdcPublishAckRepository publishAckRepository;

    private final DdcInstanceRepository instanceRepository;

    private final DdcOperationLogRepository operationLogRepository;

    private final DdcRedisRepository redisRepository;

    private final PublishConsistencyPolicyFactory policyFactory;

    private final PublishFailureRecorder failureRecorder;

    private final TransactionTemplate transactionTemplate;

    public DdcPublishService(DdcConfigItemRepository configItemRepository,
                             DdcConfigVersionRepository versionRepository,
                             DdcPublishTaskRepository publishTaskRepository,
                             DdcPublishAckRepository publishAckRepository,
                             DdcInstanceRepository instanceRepository,
                             DdcOperationLogRepository operationLogRepository,
                             DdcRedisRepository redisRepository,
                             PublishConsistencyPolicyFactory policyFactory,
                             PublishFailureRecorder failureRecorder,
                             PlatformTransactionManager transactionManager) {
        this.configItemRepository = configItemRepository;
        this.versionRepository = versionRepository;
        this.publishTaskRepository = publishTaskRepository;
        this.publishAckRepository = publishAckRepository;
        this.instanceRepository = instanceRepository;
        this.operationLogRepository = operationLogRepository;
        this.redisRepository = redisRepository;
        this.policyFactory = policyFactory;
        this.failureRecorder = failureRecorder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public DdcPublishResultVO publish(DdcPublishRequest request, String operator) {
        String changeId = UuidV7.simpleString();
        try {
            PublishPrepareResult prepareResult = transactionTemplate.execute(status -> transactionalPublishPrepare(changeId, request, operator));
            if (prepareResult == null) {
                throw new DdcAdminException("publish prepare failed");
            }
            DdcPublishMessage message = prepareResult.message();
            redisRepository.writeConfig(message.getAppCode(), message.getEnv(), message.getNamespace(),
                    message.getConfigKey(), message.getConfigValue(), message.getTargetVersion());
            redisRepository.publish(message);
            PublishDecision decision = policyFactory.get(prepareResult.publishMode()).afterMessagePublished();
            if (decision.completed()) {
                completeAfterMessagePublished(changeId, decision);
            }
            return DdcPublishResultVO.from(publishTaskRepository.findByChangeId(changeId).orElse(prepareResult.task()));
        } catch (Exception e) {
            failureRecorder.recordFailure(changeId, request.getAppCode(), request.getEnv(), request.getNamespace(),
                    request.getConfigKey(), e.getMessage());
            throw new DdcAdminException("publish config failed", e);
        }
    }

    @Transactional
    public DdcPublishResultVO ack(DdcAckRequest request) {
        DdcPublishTaskEntity task = publishTaskRepository.findByChangeId(request.getChangeId())
                .orElseThrow(() -> new DdcAdminException("publish task not found"));
        DdcPublishAckEntity ack = publishAckRepository.findByChangeIdAndInstanceId(request.getChangeId(), request.getInstanceId())
                .orElseGet(() -> newPublishAck(task, request.getInstanceId()));
        ack.setAppCode(request.getAppCode());
        ack.setEnv(request.getEnv());
        ack.setNamespace(request.getNamespace());
        ack.setConfigKey(request.getConfigKey());
        ack.setTargetVersion(request.getTargetVersion());
        ack.setCurrentVersion(request.getCurrentVersion());
        ack.setAckStatus(request.getStatus().name());
        ack.setErrorMessage(request.getErrorMessage());
        ack.setAckAt(toAckAt(request.getAckTime()));
        publishAckRepository.save(ack);

        List<DdcPublishAckEntity> acks = publishAckRepository.findByChangeId(request.getChangeId());
        int successCount = count(acks, DdcAckStatus.SUCCESS);
        int failedCount = count(acks, DdcAckStatus.FAILED);
        int ignoredCount = count(acks, DdcAckStatus.IGNORED);
        task.setAckCount(successCount);
        task.setFailedCount(failedCount);
        task.setIgnoredCount(ignoredCount);
        task.setUpdatedAt(LocalDateTime.now());
        PublishMode publishMode = parsePublishMode(task.getPublishMode());
        PublishDecision decision = policyFactory.get(publishMode)
                .decide(nullToZero(task.getTargetCount()), successCount, failedCount, nullToZero(task.getTimeoutCount()));
        if (decision.completed()) {
            task.setStatus(decision.status().name());
        }
        publishTaskRepository.save(task);
        return DdcPublishResultVO.from(task);
    }

    private PublishPrepareResult transactionalPublishPrepare(String changeId, DdcPublishRequest request, String operator) {
        validatePublishRequest(request);
        DdcConfigItemEntity config = configItemRepository.findByAppCodeAndEnvAndNamespaceAndConfigKey(
                        request.getAppCode(), request.getEnv(), request.getNamespace(), request.getConfigKey())
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .orElseThrow(() -> new DdcAdminException("config item not found"));
        if (request.getExpectedVersion() != null && !Objects.equals(request.getExpectedVersion(), config.getCurrentVersion())) {
            throw new DdcAdminException("config version changed");
        }

        String oldValue = config.getConfigValue();
        boolean valueChanged = request.getConfigValue() != null && !Objects.equals(request.getConfigValue(), oldValue);
        if (valueChanged) {
            config.setConfigValue(request.getConfigValue());
            config.setCurrentVersion(config.getCurrentVersion() + 1);
            config.setUpdatedAt(LocalDateTime.now());
            configItemRepository.save(config);
            saveVersion(config, oldValue, config.getConfigValue(), operator);
        }

        PublishMode publishMode = request.getPublishMode() == null ? PublishMode.ASYNC : request.getPublishMode();
        List<DdcInstanceEntity> targets = instanceRepository.findByAppCodeAndEnvAndNamespaceAndStatus(
                request.getAppCode(), request.getEnv(), request.getNamespace(), InstanceStatus.ONLINE.name());
        DdcPublishTaskEntity task = newPublishTask(changeId, config, publishMode, targets.size(), request.getTimeoutMs(), operator);
        publishTaskRepository.save(task);
        targets.forEach(instance -> publishAckRepository.save(newPublishAck(task, instance.getInstanceId())));
        savePublishOperation(config, changeId, operator);
        DdcPublishMessage message = buildPublishMessage(changeId, config, publishMode, operator);
        return new PublishPrepareResult(task, message, publishMode);
    }

    private void completeAfterMessagePublished(String changeId, PublishDecision decision) {
        transactionTemplate.executeWithoutResult(status -> publishTaskRepository.findByChangeId(changeId).ifPresent(task -> {
            task.setStatus(decision.status().name());
            task.setUpdatedAt(LocalDateTime.now());
            publishTaskRepository.save(task);
        }));
    }

    private void validatePublishRequest(DdcPublishRequest request) {
        requireText(request.getAppCode(), "appCode");
        requireText(request.getEnv(), "env");
        requireText(request.getNamespace(), "namespace");
        requireText(request.getConfigKey(), "configKey");
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DdcAdminException(fieldName + " is required");
        }
    }

    private DdcPublishTaskEntity newPublishTask(String changeId, DdcConfigItemEntity config, PublishMode publishMode,
                                                int targetCount, Long timeoutMs, String operator) {
        LocalDateTime now = LocalDateTime.now();
        DdcPublishTaskEntity task = new DdcPublishTaskEntity();
        task.setId(UuidV7.simpleString());
        task.setChangeId(changeId);
        task.setConfigId(config.getId());
        task.setAppCode(config.getAppCode());
        task.setEnv(config.getEnv());
        task.setNamespace(config.getNamespace());
        task.setConfigKey(config.getConfigKey());
        task.setTargetVersion(config.getCurrentVersion());
        task.setPublishMode(publishMode.name());
        task.setStatus(PublishStatus.PUBLISHING.name());
        task.setTargetCount(targetCount);
        task.setAckCount(0);
        task.setFailedCount(0);
        task.setIgnoredCount(0);
        task.setTimeoutCount(0);
        task.setTimeoutMs(timeoutMs);
        task.setOperator(operator);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }

    private DdcPublishAckEntity newPublishAck(DdcPublishTaskEntity task, String instanceId) {
        DdcPublishAckEntity ack = new DdcPublishAckEntity();
        ack.setId(UuidV7.simpleString());
        ack.setChangeId(task.getChangeId());
        ack.setInstanceId(instanceId);
        ack.setAppCode(task.getAppCode());
        ack.setEnv(task.getEnv());
        ack.setNamespace(task.getNamespace());
        ack.setConfigKey(task.getConfigKey());
        ack.setTargetVersion(task.getTargetVersion());
        return ack;
    }

    private void saveVersion(DdcConfigItemEntity config, String oldValue, String newValue, String operator) {
        DdcConfigVersionEntity version = new DdcConfigVersionEntity();
        version.setId(UuidV7.simpleString());
        version.setConfigId(config.getId());
        version.setAppCode(config.getAppCode());
        version.setEnv(config.getEnv());
        version.setNamespace(config.getNamespace());
        version.setConfigKey(config.getConfigKey());
        version.setVersion(config.getCurrentVersion());
        version.setOldValue(oldValue);
        version.setNewValue(newValue);
        version.setValueType(config.getValueType());
        version.setChangeType(ChangeType.UPDATE.name());
        version.setChangeReason("publish config");
        version.setOperator(operator);
        version.setCreatedAt(LocalDateTime.now());
        versionRepository.save(version);
    }

    private void savePublishOperation(DdcConfigItemEntity config, String changeId, String operator) {
        DdcOperationLogEntity log = new DdcOperationLogEntity();
        log.setId(UuidV7.simpleString());
        log.setAppCode(config.getAppCode());
        log.setEnv(config.getEnv());
        log.setNamespace(config.getNamespace());
        log.setConfigKey(config.getConfigKey());
        log.setOperationType("PUBLISH");
        log.setOperator(operator);
        log.setOperationContent(changeId);
        log.setCreatedAt(LocalDateTime.now());
        operationLogRepository.save(log);
    }

    private DdcPublishMessage buildPublishMessage(String changeId, DdcConfigItemEntity config, PublishMode publishMode, String operator) {
        DdcPublishMessage message = new DdcPublishMessage();
        message.setChangeId(changeId);
        message.setAppCode(config.getAppCode());
        message.setEnv(config.getEnv());
        message.setNamespace(config.getNamespace());
        message.setConfigKey(config.getConfigKey());
        message.setConfigValue(config.getConfigValue());
        message.setValueType(config.getValueType());
        message.setTargetVersion(config.getCurrentVersion());
        message.setPublishMode(publishMode.name());
        message.setOperator(operator);
        message.setTimestamp(System.currentTimeMillis());
        message.setChecksum(DdcChecksum.sha256(message));
        return message;
    }

    private int count(List<DdcPublishAckEntity> acks, DdcAckStatus status) {
        return (int) acks.stream()
                .filter(ack -> status.name().equals(ack.getAckStatus()))
                .count();
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private PublishMode parsePublishMode(String publishMode) {
        if (publishMode == null) {
            return PublishMode.ASYNC;
        }
        return PublishMode.valueOf(publishMode);
    }

    private LocalDateTime toAckAt(Long ackTime) {
        if (ackTime == null) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ackTime), ZoneId.systemDefault());
    }

    private record PublishPrepareResult(DdcPublishTaskEntity task, DdcPublishMessage message, PublishMode publishMode) {
    }
}
