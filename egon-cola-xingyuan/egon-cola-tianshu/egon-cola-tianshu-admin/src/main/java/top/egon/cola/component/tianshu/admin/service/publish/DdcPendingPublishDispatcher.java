package top.egon.cola.component.tianshu.admin.service.publish;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.config.DdcOutboxConfiguration.DdcConfigDispatchOutboxRouteDTO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigVersionBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishAckBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcConfigDispatchEventConverter;
import top.egon.cola.component.tianshu.admin.model.enums.PublishMode;
import top.egon.cola.component.tianshu.admin.model.enums.PublishStatus;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcRedisRepository;
import top.egon.cola.component.tianshu.format.DdcChecksum;
import top.egon.cola.component.tianshu.model.config.DdcPublishMessage;
import top.egon.cola.component.tianshu.model.config.DdcPublishTarget;
import top.egon.cola.component.tianshu.model.event.DdcConfigDispatchPreparedEvent;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service("ddcPendingPublishDispatcher")
@RequiredArgsConstructor
@Validated
public class DdcPendingPublishDispatcher {

    private static final Set<String> DISPATCHABLE_STATUSES = Set.of(
            PublishStatus.PENDING.name(),
            PublishStatus.PUBLISHING.name(),
            PublishStatus.UNKNOWN.name());

    @Qualifier("ddcConfigItemRepository")
    private final DdcConfigItemRepository configItemRepository;

    @Qualifier("ddcConfigVersionRepository")
    private final DdcConfigVersionRepository versionRepository;

    @Qualifier("ddcPublishTaskRepository")
    private final DdcPublishTaskRepository taskRepository;

    @Qualifier("ddcPublishAckRepository")
    private final DdcPublishAckRepository ackRepository;

    @Qualifier("ddcRedisRepository")
    private final DdcRedisRepository redisRepository;

    @Qualifier("ddcPublishStateTransitionService")
    private final DdcPublishStateTransitionService stateTransitions;

    @Qualifier("ddcConfigDispatchEventConverterImpl")
    private final DdcConfigDispatchEventConverter eventConverter;

    @Qualifier("transactionalOutbox")
    private final TransactionalOutbox transactionalOutbox;

    @Qualifier("ddcConfigDispatchOutboxRoute")
    private final DdcConfigDispatchOutboxRouteDTO outboxRoute;

    @Qualifier("ddcPublishTransactionTemplate")
    private final TransactionTemplate transactionTemplate;

    @Qualifier("ddcPublishClock")
    private final Clock clock;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Qualifier("egon.cola.component.tianshu.admin-top.egon.cola.component.tianshu.admin.config.DdcAdminProperties")
    private final DdcAdminProperties properties;

    public DdcPublishTaskBO dispatch(@NotBlank String changeId) {
        try {
            DdcPublishTaskBO prepared = transactionTemplate.execute(status -> prepareAndEnqueue(changeId));
            return prepared == null ? stateTransitions.fail(
                    changeId, "OUTBOX_PREPARE", "publish outbox preparation returned no task") : prepared;
        } catch (DdcAdminException failure) {
            stateTransitions.fail(changeId, "OUTBOX_PREPARE", failure.getMessage());
            throw failure;
        } catch (RuntimeException failure) {
            log.warn("Tianshu YAML publish could not enqueue its prepared event; changeId={}, failureType={}",
                    changeId, failure.getClass().getSimpleName());
            return stateTransitions.unknown(changeId, "OUTBOX_ENQUEUE", failure.getMessage());
        }
    }

    private DdcPublishTaskBO prepareAndEnqueue(String changeId) {
        DdcPublishTaskBO task = requiredTask(changeId);
        if (!DISPATCHABLE_STATUSES.contains(task.getStatus())) {
            return task;
        }
        boolean currentAttemptAlreadyPublishing = PublishStatus.PUBLISHING.name().equals(task.getStatus());
        if (!currentAttemptAlreadyPublishing) {
            task = stateTransitions.markPublishing(changeId);
        }
        if (!PublishStatus.PUBLISHING.name().equals(task.getStatus())) {
            return task;
        }
        if (currentAttemptAlreadyPublishing && publishedVersionAlreadyAdvanced(task)) {
            return task;
        }

        DdcAtomicPublishCommand command = command(changeId);
        DdcConfigDispatchPreparedEvent event = eventConverter.toTarget(command);
        validationUtils.validate(event);
        if (!eventConverter.isValid(event)) {
            throw new DdcAdminException("prepared config event checksum is invalid");
        }
        transactionalOutbox.enqueue(OutboxMessage.builder()
                .messageId(event.getEventId())
                .idempotencyKey(event.getEventId())
                .channel(outboxRoute.channel())
                .destination(outboxRoute.destination(event))
                .payload(event)
                .contentType("application/json")
                .schemaVersion(event.getSchemaVersion())
                .header("tenantId", Long.toString(event.getTenantId()))
                .header("eventType", outboxRoute.eventType())
                .build());
        return requiredTask(changeId);
    }

    /** Returns false when the attempt is stale, terminal without success, or past its ACK deadline. */
    public boolean deliverPrepared(@Valid DdcConfigDispatchPreparedEvent event) {
        if (event == null) {
            throw new DdcAdminException("prepared config event is required");
        }
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            validationUtils.validate(event);
            if (!eventConverter.isValid(event)) {
                throw new DdcAdminException("prepared config event checksum is invalid");
            }
            DdcPublishTaskBO task = taskRepository.findForUpdateByChangeId(event.getMessage().getChangeId())
                    .orElse(null);
            if (!isCurrentAttempt(task, event)) {
                log.info("Skipping stale Tianshu YAML dispatch event; tenantId={}, eventId={}",
                        event.getTenantId(), event.getEventId());
                return false;
            }
            if (PublishStatus.SUCCESS.name().equals(task.getStatus())) {
                return true;
            }
            if (!hasLiveDeadline(task)) {
                log.info("Skipping expired Tianshu YAML dispatch event; tenantId={}, eventId={}",
                        event.getTenantId(), event.getEventId());
                return false;
            }

            DdcAtomicPublishCommand command = eventConverter.toCommand(event);
            validationUtils.validate(command);
            if (!resourceMatchesTask(task, command)) {
                throw new DdcAdminException("prepared config event no longer matches its publish task");
            }
            redisRepository.dispatch(command, event);
            if (!advancePublishedVersion(command)) {
                throw new IllegalStateException("published pointer update is uncertain after Redis dispatch");
            }
            stateTransitions.refreshAfterAck(task.getChangeId());
            return true;
        }));
    }

    public void failPrepared(
            @Valid DdcConfigDispatchPreparedEvent event,
            @NotBlank String failureStage,
            @NotBlank String errorMessage) {
        if (event == null) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            validationUtils.validate(event);
            if (!eventConverter.isValid(event)) {
                return;
            }
            DdcPublishTaskBO task = taskRepository.findForUpdateByChangeId(event.getMessage().getChangeId())
                    .orElse(null);
            if (isCurrentAttempt(task, event) && PublishStatus.PUBLISHING.name().equals(task.getStatus())) {
                stateTransitions.fail(task.getChangeId(), failureStage, errorMessage);
            }
        });
    }

    private DdcAtomicPublishCommand command(String changeId) {
        DdcPublishTaskBO task = requiredTask(changeId);
        DdcConfigItemBO config = configItemRepository.findByPublicId(task.getConfigId())
                .orElseThrow(() -> new DdcAdminException("config item not found"));
        if (config.getPublishedVersion() != null
                && config.getPublishedVersion() > task.getTargetVersion()) {
            throw new DdcAdminException("published config version is newer than task");
        }
        DdcConfigVersionBO version = versionRepository.findByConfigIdAndVersion(
                        task.getConfigId(), task.getTargetVersion())
                .orElseThrow(() -> new DdcAdminException("published config version not found"));
        List<DdcPublishAckBO> targets = ackRepository.findByChangeId(changeId).stream()
                .sorted(Comparator.comparing(DdcPublishAckBO::getInstanceId)
                        .thenComparing(DdcPublishAckBO::getLeaseId))
                .toList();
        if (targets.isEmpty()) {
            throw new DdcAdminException("publish targets not found");
        }
        if (!Objects.equals(task.getResourceChecksum(), DdcChecksum.resource(
                task.getResourceName(), version.getFormat(), version.getNewContent()))) {
            throw new DdcAdminException("publish resource checksum changed");
        }

        DdcPublishMessage message = message(task, version, targets);
        return DdcAtomicPublishCommand.builder()
                .configId(task.getConfigId())
                .changeId(task.getChangeId())
                .bizCode(task.getBizCode())
                .env(task.getEnv())
                .appCode(task.getAppCode())
                .resourceName(task.getResourceName())
                .expectedPublishedVersion(config.getPublishedVersion())
                .targetVersion(task.getTargetVersion())
                .content(version.getNewContent())
                .eventChecksum(message.getChecksum())
                .message(message)
                .tenantId(task.getTenantId())
                .attempt(task.getAttemptCount() == null ? 0 : task.getAttemptCount())
                .build();
    }

    private DdcPublishMessage message(
            DdcPublishTaskBO task,
            DdcConfigVersionBO version,
            List<DdcPublishAckBO> targets) {
        DdcPublishMessage message = new DdcPublishMessage();
        message.setChangeId(task.getChangeId());
        message.setBizCode(task.getBizCode());
        message.setAppCode(task.getAppCode());
        message.setEnv(task.getEnv());
        message.setResourceName(task.getResourceName());
        message.setContent(version.getNewContent());
        message.setFormat(version.getFormat());
        message.setTargetVersion(task.getTargetVersion());
        message.setPublishMode(PublishMode.SYNC_ALL_ACK.name());
        message.setOperator(task.getOperator());
        message.setTimestamp(timestamp(task));
        message.setResourceChecksum(task.getResourceChecksum());
        message.setTargets(targets.stream()
                .map(target -> new DdcPublishTarget(target.getInstanceId(), target.getLeaseId()))
                .toList());
        message.setChecksum(DdcChecksum.sha256(message));
        return message;
    }

    private boolean isCurrentAttempt(DdcPublishTaskBO task, DdcConfigDispatchPreparedEvent event) {
        if (task == null
                || !Objects.equals(task.getTenantId(), event.getTenantId())
                || !Objects.equals(task.getConfigId(), event.getConfigId())
                || !Objects.equals(task.getAttemptCount() == null ? 0 : task.getAttemptCount(), event.getAttempt())
                || !Objects.equals(task.getChangeId(), event.getMessage().getChangeId())
                || !Objects.equals(task.getTargetVersion(), event.getMessage().getTargetVersion())
                || !Objects.equals(task.getResourceChecksum(), event.getMessage().getResourceChecksum())) {
            return false;
        }
        return PublishStatus.PUBLISHING.name().equals(task.getStatus())
                || PublishStatus.SUCCESS.name().equals(task.getStatus());
    }

    private boolean resourceMatchesTask(DdcPublishTaskBO task, DdcAtomicPublishCommand command) {
        if (!Objects.equals(task.getBizCode(), command.bizCode())
                || !Objects.equals(task.getEnv(), command.env())
                || !Objects.equals(task.getAppCode(), command.appCode())
                || !Objects.equals(task.getResourceName(), command.resourceName())
                || !Objects.equals(task.getTargetVersion(), command.targetVersion())
                || !Objects.equals(task.getTenantId(), command.tenantId())) {
            return false;
        }
        List<DdcPublishTarget> persistedTargets = ackRepository.findByChangeId(task.getChangeId()).stream()
                .sorted(Comparator.comparing(DdcPublishAckBO::getInstanceId)
                        .thenComparing(DdcPublishAckBO::getLeaseId))
                .map(target -> new DdcPublishTarget(target.getInstanceId(), target.getLeaseId()))
                .toList();
        return persistedTargets.equals(command.message().getTargets());
    }

    private boolean publishedVersionAlreadyAdvanced(DdcPublishTaskBO task) {
        return configItemRepository.findByPublicId(task.getConfigId())
                .map(DdcConfigItemBO::getPublishedVersion)
                .filter(version -> Objects.equals(version, task.getTargetVersion()))
                .isPresent();
    }

    private boolean hasLiveDeadline(DdcPublishTaskBO task) {
        LocalDateTime dispatchTime = task.getDispatchedAt() == null ? task.getUpdatedAt() : task.getDispatchedAt();
        if (dispatchTime == null) {
            return false;
        }
        long timeoutMs = task.getTimeoutMs() == null
                ? properties.getPublish().getDefaultTimeoutMs()
                : task.getTimeoutMs();
        Instant deadline = dispatchTime.atZone(ZoneId.systemDefault()).toInstant().plusMillis(timeoutMs);
        return deadline.isAfter(clock.instant());
    }

    private boolean advancePublishedVersion(DdcAtomicPublishCommand command) {
        int changed = configItemRepository.advancePublishedVersion(
                command.configId(), command.expectedPublishedVersion(), command.targetVersion(), now());
        if (changed == 1) {
            return true;
        }
        return configItemRepository.findByPublicId(command.configId())
                .map(DdcConfigItemBO::getPublishedVersion)
                .filter(version -> Objects.equals(version, command.targetVersion()))
                .isPresent();
    }

    private long timestamp(DdcPublishTaskBO task) {
        LocalDateTime dispatchedAt = task.getDispatchedAt();
        if (dispatchedAt == null) {
            throw new DdcAdminException("publish attempt dispatch time is required");
        }
        return dispatchedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private DdcPublishTaskBO requiredTask(String changeId) {
        return taskRepository.findByChangeId(changeId)
                .orElseThrow(() -> new DdcAdminException("publish task not found"));
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
    }
}
