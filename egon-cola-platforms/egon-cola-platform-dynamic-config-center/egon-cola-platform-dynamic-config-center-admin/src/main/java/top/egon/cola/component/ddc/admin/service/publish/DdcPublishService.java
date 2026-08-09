package top.egon.cola.component.ddc.admin.service.publish;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.model.dto.DdcPublishRequest;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcOperationLogEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.ChangeType;
import top.egon.cola.component.ddc.admin.model.enums.PublishMode;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigResourceKey;
import top.egon.cola.component.ddc.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.ddc.admin.repository.DdcOperationLogRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.config.DdcYamlConfigValidator;
import top.egon.cola.component.ddc.admin.service.lease.DdcConfigLeaseService;
import top.egon.cola.component.ddc.format.DdcChecksum;
import top.egon.cola.component.ddc.error.DdcErrorStatus;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcPublishTarget;
import top.egon.cola.component.ddc.model.config.DdcAckStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DdcPublishService {

    private static final List<String> RETRYABLE_STATUSES = List.of(
            PublishStatus.FAILED.name(),
            PublishStatus.TIMEOUT.name(),
            PublishStatus.UNKNOWN.name()
    );

    private static final List<String> ACTIVE_STATUSES = List.of(
            PublishStatus.PENDING.name(),
            PublishStatus.PUBLISHING.name(),
            PublishStatus.UNKNOWN.name()
    );

    private final DdcConfigItemRepository configItemRepository;

    private final DdcConfigVersionRepository versionRepository;

    private final DdcPublishTaskRepository publishTaskRepository;

    private final DdcPublishAckRepository publishAckRepository;

    private final DdcOperationLogRepository operationLogRepository;

    private final DdcPendingPublishDispatcher dispatcher;

    private final DdcConfigLeaseService leaseService;

    private final PublishResourceLockRegistry resourceRegistry;

    private final PublishCompletionWaiterRegistry waiterRegistry;

    private final DdcPublishStateTransitionService stateTransitions;

    private final PublishFailureRecorder failureRecorder;

    private final DdcAdminProperties properties;

    private final DdcYamlConfigValidator yamlValidator;

    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    @Autowired
    public DdcPublishService(
            DdcConfigItemRepository configItemRepository,
            DdcConfigVersionRepository versionRepository,
            DdcPublishTaskRepository publishTaskRepository,
            DdcPublishAckRepository publishAckRepository,
            DdcOperationLogRepository operationLogRepository,
            DdcPendingPublishDispatcher dispatcher,
            DdcConfigLeaseService leaseService,
            PublishResourceLockRegistry resourceRegistry,
            PublishCompletionWaiterRegistry waiterRegistry,
            DdcPublishStateTransitionService stateTransitions,
            PublishFailureRecorder failureRecorder,
            DdcAdminProperties properties,
            PlatformTransactionManager transactionManager) {
        this(
                configItemRepository,
                versionRepository,
                publishTaskRepository,
                publishAckRepository,
                operationLogRepository,
                dispatcher,
                leaseService,
                resourceRegistry,
                waiterRegistry,
                stateTransitions,
                failureRecorder,
                properties,
                transactionManager,
                Clock.systemUTC()
        );
    }

    DdcPublishService(
            DdcConfigItemRepository configItemRepository,
            DdcConfigVersionRepository versionRepository,
            DdcPublishTaskRepository publishTaskRepository,
            DdcPublishAckRepository publishAckRepository,
            DdcOperationLogRepository operationLogRepository,
            DdcPendingPublishDispatcher dispatcher,
            DdcConfigLeaseService leaseService,
            PublishResourceLockRegistry resourceRegistry,
            PublishCompletionWaiterRegistry waiterRegistry,
            DdcPublishStateTransitionService stateTransitions,
            PublishFailureRecorder failureRecorder,
            DdcAdminProperties properties,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.configItemRepository = configItemRepository;
        this.versionRepository = versionRepository;
        this.publishTaskRepository = publishTaskRepository;
        this.publishAckRepository = publishAckRepository;
        this.operationLogRepository = operationLogRepository;
        this.dispatcher = dispatcher;
        this.leaseService = leaseService;
        this.resourceRegistry = resourceRegistry;
        this.waiterRegistry = waiterRegistry;
        this.stateTransitions = stateTransitions;
        this.failureRecorder = failureRecorder;
        this.properties = properties;
        this.yamlValidator = new DdcYamlConfigValidator(
                properties.getMaxConfigBytes()
        );
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public DdcPublishResultVO publish(DdcPublishRequest request, String operator) {
        validatePublishRequest(request);
        long timeoutMs = timeout(request.getTimeoutMs());
        DdcPublishTaskEntity existing =
                publishTaskRepository.findByChangeId(request.getChangeId()).orElse(null);
        if (existing != null) {
            validateIdempotentRequest(existing, request, timeoutMs);
            return awaitIfActive(existing);
        }

        DdcConfigResourceKey resourceKey = resourceKey(request);
        if (!resourceRegistry.tryAcquire(resourceKey, request.getChangeId())) {
            throw new DdcAdminException(DdcErrorStatus.PUBLISH_IN_PROGRESS);
        }

        PublishPrepareResult prepared;
        try {
            prepared = transactionTemplate.execute(status ->
                    prepare(request, timeoutMs, operator));
            if (prepared == null) {
                throw new DdcAdminException("publish prepare failed");
            }
        } catch (RuntimeException exception) {
            recordPreparationFailure(request, exception);
            resourceRegistry.release(resourceKey, request.getChangeId());
            throw exception;
        }

        PublishCompletionWaiterRegistry.PublishWaiter waiter =
                waiterRegistry.register(request.getChangeId());
        if (prepared.dispatchRequired()) {
            DdcPublishTaskEntity dispatched;
            try {
                dispatched = dispatcher.dispatch(request.getChangeId());
            } catch (RuntimeException exception) {
                waiterRegistry.remove(request.getChangeId(), waiter);
                throw exception;
            }
            if (stateTransitions.isTerminal(dispatched)) {
                waiterRegistry.remove(request.getChangeId(), waiter);
                return DdcPublishResultVO.from(dispatched);
            }
        }
        return await(request.getChangeId(), waiter);
    }

    public DdcPublishResultVO retry(String changeId) {
        return retry(changeId, null);
    }

    public DdcPublishResultVO retry(String changeId, String operator) {
        requireText(changeId, "changeId");
        DdcPublishTaskEntity task = requiredTask(changeId);
        requireRetryable(task);
        DdcConfigResourceKey resourceKey = resourceKey(task);
        if (!resourceRegistry.tryAcquire(resourceKey, changeId)) {
            throw new DdcAdminException(DdcErrorStatus.PUBLISH_IN_PROGRESS);
        }

        RetryPrepareResult prepared;
        try {
            prepared = transactionTemplate.execute(status ->
                    prepareRetry(changeId, operator));
            if (prepared == null) {
                throw new DdcAdminException("publish retry prepare failed");
            }
        } catch (RuntimeException exception) {
            resourceRegistry.release(resourceKey, changeId);
            throw exception;
        }
        if (prepared.targetLeaseExpired()) {
            resourceRegistry.release(resourceKey, changeId);
            throw new DdcAdminException(DdcErrorStatus.TARGET_LEASE_EXPIRED);
        }

        PublishCompletionWaiterRegistry.PublishWaiter waiter =
                waiterRegistry.register(changeId);
        DdcPublishTaskEntity dispatched;
        try {
            dispatched = dispatcher.dispatch(changeId);
        } catch (RuntimeException exception) {
            waiterRegistry.remove(changeId, waiter);
            throw exception;
        }
        if (stateTransitions.isTerminal(dispatched)) {
            waiterRegistry.remove(changeId, waiter);
            return DdcPublishResultVO.from(dispatched);
        }
        return await(changeId, waiter);
    }

    @Transactional
    public DdcPublishResultVO ack(DdcAckRequest request) {
        validateAckRequest(request);
        DdcPublishTaskEntity task = publishTaskRepository.findByChangeId(request.getChangeId())
                .orElseThrow(() -> new DdcAdminException("publish task not found"));
        if (stateTransitions.isTerminal(task)) {
            return DdcPublishResultVO.from(task);
        }
        DdcPublishAckEntity target =
                publishAckRepository.findByChangeIdAndInstanceIdAndLeaseId(
                                request.getChangeId(),
                                request.getInstanceId(),
                                request.getLeaseId()
                        )
                        .orElseThrow(() ->
                                new DdcAdminException(DdcErrorStatus.LEASE_MISMATCH));
        if (!Objects.equals(task.getTargetVersion(), request.getTargetVersion())
                || !Objects.equals(task.getResourceChecksum(), request.getResourceChecksum())
                || !Objects.equals(target.getTargetVersion(), request.getTargetVersion())
                || !Objects.equals(target.getResourceChecksum(), request.getResourceChecksum())) {
            throw new DdcAdminException("ACK version or resource checksum does not match target");
        }
        if (target.getAckStatus() != null) {
            return DdcPublishResultVO.from(
                    stateTransitions.refreshAfterAck(request.getChangeId())
            );
        }
        target.setCurrentVersion(request.getCurrentVersion());
        target.setAckStatus(request.getStatus().name());
        target.setErrorMessage(request.getErrorMessage());
        target.setAckAt(toAckAt(request.getAckTime()));
        publishAckRepository.saveAndFlush(target);
        return DdcPublishResultVO.from(
                stateTransitions.refreshAfterAck(request.getChangeId())
        );
    }

    private DdcPublishResultVO awaitIfActive(DdcPublishTaskEntity task) {
        if (stateTransitions.isTerminal(task)) {
            return DdcPublishResultVO.from(task);
        }
        DdcConfigResourceKey key = resourceKey(task);
        String owner = resourceRegistry.owner(key).orElse(null);
        if (owner == null) {
            if (!resourceRegistry.tryAcquire(key, task.getChangeId())) {
                throw new DdcAdminException(DdcErrorStatus.PUBLISH_IN_PROGRESS);
            }
        } else if (!owner.equals(task.getChangeId())) {
            throw new DdcAdminException(DdcErrorStatus.PUBLISH_IN_PROGRESS);
        }
        return await(
                task.getChangeId(),
                waiterRegistry.register(task.getChangeId())
        );
    }

    private DdcPublishResultVO await(
            String changeId,
            PublishCompletionWaiterRegistry.PublishWaiter waiter) {
        try {
            while (true) {
                DdcPublishTaskEntity task = requiredTask(changeId);
                if (stateTransitions.isTerminal(task)) {
                    waiterRegistry.remove(changeId, waiter);
                    return DdcPublishResultVO.from(task);
                }
                Instant deadline = deadline(task);
                Instant now = clock.instant();
                if (!deadline.isAfter(now)) {
                    if (PublishStatus.PENDING.name().equals(task.getStatus())) {
                        stateTransitions.fail(
                                changeId,
                                "DISPATCH_TIMEOUT",
                                "publish dispatch timed out"
                        );
                    } else {
                        stateTransitions.timeout(
                                changeId,
                                "publish acknowledgement timed out"
                        );
                    }
                    continue;
                }
                long signalVersion = waiter.signalVersion();
                task = requiredTask(changeId);
                if (stateTransitions.isTerminal(task)) {
                    continue;
                }
                waiter.awaitAfter(
                        signalVersion,
                        minimum(
                                Duration.between(clock.instant(), deadline),
                                Duration.ofMillis(
                                        properties.getPublish()
                                                .getCompletionPollIntervalMs()
                                )
                        )
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DdcAdminException("publish wait interrupted", exception);
        }
    }

    private PublishPrepareResult prepare(DdcPublishRequest request,
                                         long timeoutMs,
                                         String operator) {
        DdcPublishTaskEntity concurrent =
                publishTaskRepository.findByChangeId(request.getChangeId()).orElse(null);
        if (concurrent != null) {
            validateIdempotentRequest(concurrent, request, timeoutMs);
            return new PublishPrepareResult(
                    concurrent,
                    false
            );
        }

        DdcConfigItemEntity config =
                configItemRepository
                        .findForPublishByBizCodeAndEnvAndAppCodeAndResourceName(
                                request.getBizCode(),
                                request.getEnv(),
                                request.getAppCode(),
                                request.getResourceName()
                        )
                        .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                        .orElseThrow(() -> new DdcAdminException("config item not found"));
        if (publishTaskRepository
                .findFirstByBizCodeAndEnvAndAppCodeAndResourceNameAndStatusIn(
                        request.getBizCode(),
                        request.getEnv(),
                        request.getAppCode(),
                        request.getResourceName(),
                        ACTIVE_STATUSES
                )
                .isPresent()) {
            throw new DdcAdminException(DdcErrorStatus.PUBLISH_IN_PROGRESS);
        }
        if (!Objects.equals(request.getExpectedVersion(), config.getCurrentVersion())) {
            throw new DdcAdminException("config version changed");
        }

        String oldContent = config.getContent();
        config.setContent(request.getContent());
        config.setCurrentVersion(config.getCurrentVersion() + 1);
        config.setUpdatedAt(now());
        configItemRepository.save(config);
        saveVersion(config, oldContent, request.getContent(), operator);

        List<DdcPublishTarget> targets = leaseService.activeTargets(
                request.getBizCode(),
                request.getEnv(),
                request.getAppCode()
        ).stream()
                .sorted(Comparator
                        .comparing(DdcPublishTarget::instanceId)
                        .thenComparing(DdcPublishTarget::leaseId))
                .toList();
        if (targets.isEmpty()) {
            throw new DdcAdminException(DdcErrorStatus.NO_LIVE_INSTANCE);
        }

        String resourceChecksum = DdcChecksum.resource(
                request.getResourceName(),
                request.getFormat(),
                request.getContent()
        );
        DdcPublishTaskEntity task = newTask(
                request,
                config,
                resourceChecksum,
                targets.size(),
                timeoutMs,
                operator
        );
        publishTaskRepository.save(task);
        List<DdcPublishAckEntity> targetRows = targets.stream()
                .map(target -> newTarget(task, target))
                .toList();
        publishAckRepository.saveAll(targetRows);
        savePublishOperation(config, request.getChangeId(), operator);
        return new PublishPrepareResult(task, true);
    }

    private RetryPrepareResult prepareRetry(
            String changeId,
            String operator) {
        DdcPublishTaskEntity task = requiredTask(changeId);
        requireRetryable(task);
        List<DdcPublishAckEntity> targetRows =
                publishAckRepository.findByChangeId(changeId).stream()
                        .sorted(Comparator
                                .comparing(DdcPublishAckEntity::getInstanceId)
                                .thenComparing(DdcPublishAckEntity::getLeaseId))
                        .toList();
        if (targetRows.isEmpty()) {
            throw new DdcAdminException("publish targets not found");
        }
        List<DdcPublishTarget> targets = targetRows.stream()
                .map(target -> new DdcPublishTarget(
                        target.getInstanceId(),
                        target.getLeaseId()
                ))
                .toList();
        if (!leaseService.areActiveTargets(
                task.getBizCode(),
                task.getEnv(),
                task.getAppCode(),
                targets
        )) {
            stateTransitions.markTargetLeaseExpired(changeId, RETRYABLE_STATUSES);
            return RetryPrepareResult.expired();
        }
        int reset = publishTaskRepository.resetForRetry(
                changeId,
                PublishStatus.PENDING.name(),
                operator,
                now(),
                RETRYABLE_STATUSES
        );
        if (reset != 1) {
            throw new DdcAdminException("publish task is not retryable");
        }
        publishAckRepository.resetTargets(changeId);
        return RetryPrepareResult.ready();
    }

    private DdcPublishTaskEntity newTask(DdcPublishRequest request,
                                         DdcConfigItemEntity config,
                                         String resourceChecksum,
                                         int targetCount,
                                         long timeoutMs,
                                         String operator) {
        LocalDateTime now = now();
        DdcPublishTaskEntity task = new DdcPublishTaskEntity();
        task.setId(UuidV7.simpleString());
        task.setChangeId(request.getChangeId());
        task.setConfigId(config.getId());
        task.setBizCode(config.getBizCode());
        task.setAppCode(config.getAppCode());
        task.setEnv(config.getEnv());
        task.setNamespace(null);
        task.setResourceName(config.getResourceName());
        task.setTargetVersion(config.getCurrentVersion());
        task.setPublishMode(PublishMode.SYNC_ALL_ACK.name());
        task.setResourceChecksum(resourceChecksum);
        task.setAttemptCount(0);
        task.setStatus(PublishStatus.PENDING.name());
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

    private DdcPublishAckEntity newTarget(DdcPublishTaskEntity task,
                                          DdcPublishTarget target) {
        DdcPublishAckEntity ack = new DdcPublishAckEntity();
        ack.setId(UuidV7.simpleString());
        ack.setChangeId(task.getChangeId());
        ack.setInstanceId(target.instanceId());
        ack.setLeaseId(target.leaseId());
        ack.setResourceChecksum(task.getResourceChecksum());
        ack.setBizCode(task.getBizCode());
        ack.setAppCode(task.getAppCode());
        ack.setEnv(task.getEnv());
        ack.setNamespace(null);
        ack.setResourceName(task.getResourceName());
        ack.setTargetVersion(task.getTargetVersion());
        return ack;
    }

    private void saveVersion(DdcConfigItemEntity config,
                             String oldContent,
                             String newContent,
                             String operator) {
        DdcConfigVersionEntity version = new DdcConfigVersionEntity();
        version.setId(UuidV7.simpleString());
        version.setConfigId(config.getId());
        version.setBizCode(config.getBizCode());
        version.setAppCode(config.getAppCode());
        version.setEnv(config.getEnv());
        version.setNamespace(null);
        version.setResourceName(config.getResourceName());
        version.setVersion(config.getCurrentVersion());
        version.setOldContent(oldContent);
        version.setNewContent(newContent);
        version.setFormat(config.getFormat());
        version.setChangeType(ChangeType.UPDATE.name());
        version.setChangeReason("publish config");
        version.setOperator(operator);
        version.setCreatedAt(now());
        versionRepository.save(version);
    }

    private void savePublishOperation(DdcConfigItemEntity config,
                                      String changeId,
                                      String operator) {
        DdcOperationLogEntity log = new DdcOperationLogEntity();
        log.setId(UuidV7.simpleString());
        log.setBizCode(config.getBizCode());
        log.setAppCode(config.getAppCode());
        log.setEnv(config.getEnv());
        log.setNamespace(null);
        log.setResourceName(config.getResourceName());
        log.setOperationType("PUBLISH");
        log.setOperator(operator);
        log.setOperationContent(changeId);
        log.setCreatedAt(now());
        operationLogRepository.save(log);
    }

    private void validatePublishRequest(DdcPublishRequest request) {
        if (request == null) {
            throw new DdcAdminException("publish request is required");
        }
        requireUuidV7(request.getChangeId());
        requireText(request.getBizCode(), "bizCode");
        requireText(request.getEnv(), "env");
        requireText(request.getAppCode(), "appCode");
        requireText(request.getResourceName(), "resourceName");
        requireText(request.getFormat(), "format");
        yamlValidator.validate(
                request.getResourceName(),
                request.getFormat(),
                request.getContent()
        );
        if (request.getExpectedVersion() == null || request.getExpectedVersion() < 0) {
            throw new DdcAdminException("expectedVersion is required");
        }
    }

    private void validateAckRequest(DdcAckRequest request) {
        if (request == null
                || request.getStatus() == null
                || request.getStatus() == DdcAckStatus.TIMEOUT) {
            throw new DdcAdminException("valid ACK status is required");
        }
        requireText(request.getChangeId(), "changeId");
        requireText(request.getInstanceId(), "instanceId");
        requireText(request.getLeaseId(), "leaseId");
        requireText(request.getResourceChecksum(), "resourceChecksum");
        if (request.getTargetVersion() == null) {
            throw new DdcAdminException("targetVersion is required");
        }
    }

    private void validateIdempotentRequest(DdcPublishTaskEntity task,
                                           DdcPublishRequest request,
                                           long timeoutMs) {
        boolean matches = Objects.equals(task.getBizCode(), request.getBizCode())
                && Objects.equals(task.getAppCode(), request.getAppCode())
                && Objects.equals(task.getEnv(), request.getEnv())
                && Objects.equals(
                        task.getResourceName(),
                        request.getResourceName()
                )
                && Objects.equals(task.getResourceChecksum(),
                DdcChecksum.resource(
                        request.getResourceName(),
                        request.getFormat(),
                        request.getContent()
                ))
                && Objects.equals(task.getTimeoutMs(), timeoutMs)
                && task.getTargetVersion() != null
                && task.getTargetVersion() == request.getExpectedVersion() + 1;
        if (!matches) {
            throw new DdcAdminException(DdcErrorStatus.CHANGE_ID_CONFLICT);
        }
    }

    private void requireRetryable(DdcPublishTaskEntity task) {
        if (task == null || !RETRYABLE_STATUSES.contains(task.getStatus())) {
            throw new DdcAdminException("publish task is not retryable");
        }
    }

    private long timeout(Long requestedTimeoutMs) {
        long timeoutMs = requestedTimeoutMs == null
                ? properties.getPublish().getDefaultTimeoutMs()
                : requestedTimeoutMs;
        if (timeoutMs <= 0 || timeoutMs > properties.getPublish().getMaxTimeoutMs()) {
            throw new DdcAdminException(
                    "timeoutMs must be between 1 and "
                            + properties.getPublish().getMaxTimeoutMs()
            );
        }
        return timeoutMs;
    }

    private Instant deadline(DdcPublishTaskEntity task) {
        if (PublishStatus.PENDING.name().equals(task.getStatus())) {
            LocalDateTime createdAt =
                    task.getCreatedAt() == null ? now() : task.getCreatedAt();
            return instant(createdAt).plusMillis(
                    properties.getPublish().getDispatchTimeoutMs()
            );
        }
        LocalDateTime dispatchedAt =
                task.getDispatchedAt() == null ? task.getUpdatedAt() : task.getDispatchedAt();
        if (dispatchedAt == null) {
            dispatchedAt = now();
        }
        long timeoutMs = task.getTimeoutMs() == null
                ? properties.getPublish().getDefaultTimeoutMs()
                : task.getTimeoutMs();
        return instant(dispatchedAt).plusMillis(timeoutMs);
    }

    private void recordPreparationFailure(DdcPublishRequest request,
                                          RuntimeException exception) {
        try {
            failureRecorder.recordFailure(
                    request.getChangeId(),
                    request.getBizCode(),
                    request.getEnv(),
                    request.getAppCode(),
                    request.getResourceName(),
                    exception.getMessage()
            );
        } catch (RuntimeException ignored) {
            // Preserve the original preparation failure.
        }
    }

    private DdcPublishTaskEntity requiredTask(String changeId) {
        return publishTaskRepository.findByChangeId(changeId)
                .orElseThrow(() -> new DdcAdminException("publish task not found"));
    }

    private DdcConfigResourceKey resourceKey(DdcPublishRequest request) {
        return new DdcConfigResourceKey(
                request.getBizCode(),
                request.getEnv(),
                request.getAppCode(),
                request.getResourceName()
        );
    }

    private DdcConfigResourceKey resourceKey(DdcPublishTaskEntity task) {
        return new DdcConfigResourceKey(
                task.getBizCode(),
                task.getEnv(),
                task.getAppCode(),
                task.getResourceName()
        );
    }

    private void requireUuidV7(String value) {
        requireText(value, "changeId");
        try {
            String canonical = value.length() == 32
                    ? value.substring(0, 8) + "-"
                    + value.substring(8, 12) + "-"
                    + value.substring(12, 16) + "-"
                    + value.substring(16, 20) + "-"
                    + value.substring(20)
                    : value;
            if (UUID.fromString(canonical).version() != 7) {
                throw new IllegalArgumentException("not UUIDv7");
            }
        } catch (IllegalArgumentException exception) {
            throw new DdcAdminException("changeId must be UUIDv7");
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DdcAdminException(fieldName + " is required");
        }
    }

    private Duration minimum(Duration left, Duration pollInterval) {
        if (pollInterval.isZero() || pollInterval.isNegative()) {
            throw new DdcAdminException(
                    "publish completion poll interval must be positive"
            );
        }
        return left.compareTo(pollInterval) < 0 ? left : pollInterval;
    }

    private LocalDateTime toAckAt(Long ackTime) {
        Instant instant = ackTime == null
                ? clock.instant()
                : Instant.ofEpochMilli(ackTime);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
    }

    private Instant instant(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private record PublishPrepareResult(
            DdcPublishTaskEntity task,
            boolean dispatchRequired
    ) {
    }

    private record RetryPrepareResult(
            boolean targetLeaseExpired
    ) {

        private static RetryPrepareResult ready() {
            return new RetryPrepareResult(false);
        }

        private static RetryPrepareResult expired() {
            return new RetryPrepareResult(true);
        }
    }
}
