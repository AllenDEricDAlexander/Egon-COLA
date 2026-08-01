package top.egon.cola.component.ddc.admin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigResourceKey;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Service
public class DdcPublishStateTransitionService {

    private static final List<String> ACTIVE_STATUSES = List.of(
            PublishStatus.PENDING.name(),
            PublishStatus.PUBLISHING.name(),
            PublishStatus.UNKNOWN.name()
    );

    private static final List<String> DISPATCHABLE_STATUSES = List.of(
            PublishStatus.PENDING.name(),
            PublishStatus.PUBLISHING.name(),
            PublishStatus.UNKNOWN.name()
    );

    private static final Set<String> TERMINAL_STATUSES = Set.of(
            PublishStatus.SUCCESS.name(),
            PublishStatus.FAILED.name(),
            PublishStatus.TIMEOUT.name(),
            PublishStatus.UNKNOWN.name()
    );

    private final DdcPublishTaskRepository taskRepository;

    private final DdcConfigItemRepository configItemRepository;

    private final DdcPublishAckRepository ackRepository;

    private final PublishResourceLockRegistry resourceRegistry;

    private final PublishCompletionWaiterRegistry waiterRegistry;

    private final Clock clock;

    @Autowired
    public DdcPublishStateTransitionService(
            DdcPublishTaskRepository taskRepository,
            DdcConfigItemRepository configItemRepository,
            DdcPublishAckRepository ackRepository,
            PublishResourceLockRegistry resourceRegistry,
            PublishCompletionWaiterRegistry waiterRegistry) {
        this(
                taskRepository,
                configItemRepository,
                ackRepository,
                resourceRegistry,
                waiterRegistry,
                Clock.systemUTC()
        );
    }

    DdcPublishStateTransitionService(
            DdcPublishTaskRepository taskRepository,
            DdcConfigItemRepository configItemRepository,
            DdcPublishAckRepository ackRepository,
            PublishResourceLockRegistry resourceRegistry,
            PublishCompletionWaiterRegistry waiterRegistry,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.configItemRepository = configItemRepository;
        this.ackRepository = ackRepository;
        this.resourceRegistry = resourceRegistry;
        this.waiterRegistry = waiterRegistry;
        this.clock = clock;
    }

    @Transactional
    public DdcPublishTaskEntity markPublishing(String changeId) {
        LocalDateTime now = now();
        taskRepository.transitionToPublishing(
                changeId,
                DISPATCHABLE_STATUSES,
                PublishStatus.PENDING.name(),
                PublishStatus.PUBLISHING.name(),
                now
        );
        return requiredTask(changeId);
    }

    @Transactional
    public DdcPublishTaskEntity refreshAfterAck(String changeId) {
        DdcPublishTaskEntity task = taskRepository.findForUpdateByChangeId(changeId)
                .orElseThrow(() -> new IllegalStateException(
                        "publish task not found: " + changeId
                ));
        if (isTerminal(task)) {
            return task;
        }
        List<DdcPublishAckEntity> targets = ackRepository.findByChangeId(changeId);
        int success = count(targets, DdcAckStatus.SUCCESS.name());
        int failed = count(targets, DdcAckStatus.FAILED.name());
        int ignored = count(targets, DdcAckStatus.IGNORED.name());
        int timeout = count(targets, DdcAckStatus.TIMEOUT.name());
        taskRepository.updateCounters(changeId, success, failed, ignored, timeout, now());
        if (failed > 0) {
            transitionToTerminal(
                    task,
                    PublishStatus.FAILED,
                    "ACK",
                    "one or more publish targets failed"
            );
        } else if (task.getTargetCount() != null
                && success == targets.size()
                && success == task.getTargetCount()
                && isPublished(task)) {
            transitionToTerminal(task, PublishStatus.SUCCESS, null, null);
        } else {
            signalAfterCommit(changeId);
        }
        return requiredTask(changeId);
    }

    @Transactional
    public DdcPublishTaskEntity fail(String changeId,
                                     String failureStage,
                                     String errorMessage) {
        DdcPublishTaskEntity task = requiredTask(changeId);
        transitionToTerminal(task, PublishStatus.FAILED, failureStage, errorMessage);
        return requiredTask(changeId);
    }

    @Transactional
    public DdcPublishTaskEntity timeout(String changeId,
                                        String errorMessage) {
        DdcPublishTaskEntity task = requiredTask(changeId);
        if (transitionToTerminal(task, PublishStatus.TIMEOUT, "ACK_TIMEOUT", errorMessage)) {
            LocalDateTime now = now();
            ackRepository.markIncompleteTimeout(
                    changeId,
                    DdcAckStatus.TIMEOUT.name(),
                    now
            );
            updateCounters(changeId, now);
        }
        return requiredTask(changeId);
    }

    @Transactional
    public DdcPublishTaskEntity unknown(String changeId,
                                        String errorMessage) {
        return unknown(changeId, "ADMIN_RESTART", errorMessage);
    }

    @Transactional
    public DdcPublishTaskEntity unknown(String changeId,
                                        String failureStage,
                                        String errorMessage) {
        DdcPublishTaskEntity task = requiredTask(changeId);
        transitionToTerminal(task, PublishStatus.UNKNOWN, failureStage, errorMessage);
        return requiredTask(changeId);
    }

    @Transactional
    public DdcPublishTaskEntity markTargetLeaseExpired(
            String changeId,
            List<String> retryableStatuses) {
        DdcPublishTaskEntity task = requiredTask(changeId);
        int changed = taskRepository.markRetryLeaseExpired(
                changeId,
                PublishStatus.FAILED.name(),
                now(),
                "TARGET_VALIDATION",
                "DDC_TARGET_LEASE_EXPIRED",
                retryableStatuses
        );
        if (changed == 1) {
            notifyTerminalAfterCommit(task);
        }
        return requiredTask(changeId);
    }

    public boolean isTerminal(DdcPublishTaskEntity task) {
        return task != null && TERMINAL_STATUSES.contains(task.getStatus());
    }

    public void releaseIfTerminal(DdcPublishTaskEntity task) {
        if (isTerminal(task)) {
            notifyTerminal(task);
        }
    }

    private boolean transitionToTerminal(DdcPublishTaskEntity task,
                                         PublishStatus status,
                                         String failureStage,
                                         String errorMessage) {
        LocalDateTime now = now();
        int changed = taskRepository.transitionToTerminal(
                task.getChangeId(),
                status.name(),
                now,
                failureStage,
                errorMessage,
                ACTIVE_STATUSES
        );
        if (changed == 1) {
            notifyTerminalAfterCommit(task);
            return true;
        }
        return false;
    }

    private void updateCounters(String changeId, LocalDateTime now) {
        List<DdcPublishAckEntity> targets = ackRepository.findByChangeId(changeId);
        taskRepository.updateCounters(
                changeId,
                count(targets, DdcAckStatus.SUCCESS.name()),
                count(targets, DdcAckStatus.FAILED.name()),
                count(targets, DdcAckStatus.IGNORED.name()),
                count(targets, DdcAckStatus.TIMEOUT.name()),
                now
        );
    }

    private int count(List<DdcPublishAckEntity> targets, String status) {
        return (int) targets.stream()
                .filter(target -> status.equals(target.getAckStatus()))
                .count();
    }

    private boolean isPublished(DdcPublishTaskEntity task) {
        return configItemRepository.findById(task.getConfigId())
                .map(config -> config.getPublishedVersion() != null
                        && config.getPublishedVersion().equals(task.getTargetVersion()))
                .orElse(false);
    }

    private DdcPublishTaskEntity requiredTask(String changeId) {
        return taskRepository.findByChangeId(changeId)
                .orElseThrow(() -> new IllegalStateException(
                        "publish task not found: " + changeId
                ));
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
    }

    private void signalAfterCommit(String changeId) {
        afterCommit(() -> waiterRegistry.signal(changeId));
    }

    private void notifyTerminalAfterCommit(DdcPublishTaskEntity task) {
        afterCommit(() -> notifyTerminal(task));
    }

    private void notifyTerminal(DdcPublishTaskEntity task) {
        waiterRegistry.signal(task.getChangeId());
        resourceRegistry.release(resourceKey(task), task.getChangeId());
    }

    private DdcConfigResourceKey resourceKey(DdcPublishTaskEntity task) {
        return new DdcConfigResourceKey(
                task.getBizCode(),
                task.getEnv(),
                task.getAppCode(),
                task.getConfigKey()
        );
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                }
        );
    }
}
