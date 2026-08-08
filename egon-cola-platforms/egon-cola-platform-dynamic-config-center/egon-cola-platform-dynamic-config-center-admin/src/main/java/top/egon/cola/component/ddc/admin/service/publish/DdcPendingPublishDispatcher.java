package top.egon.cola.component.ddc.admin.service.publish;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.PublishMode;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.dto.DdcPublishTarget;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class DdcPendingPublishDispatcher {

    private static final Set<String> DISPATCHABLE_STATUSES = Set.of(
            PublishStatus.PENDING.name(),
            PublishStatus.PUBLISHING.name(),
            PublishStatus.UNKNOWN.name()
    );

    private final DdcConfigItemRepository configItemRepository;

    private final DdcConfigVersionRepository versionRepository;

    private final DdcPublishTaskRepository taskRepository;

    private final DdcPublishAckRepository ackRepository;

    private final DdcRedisRepository redisRepository;

    private final DdcPublishStateTransitionService stateTransitions;

    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    @Autowired
    public DdcPendingPublishDispatcher(
            DdcConfigItemRepository configItemRepository,
            DdcConfigVersionRepository versionRepository,
            DdcPublishTaskRepository taskRepository,
            DdcPublishAckRepository ackRepository,
            DdcRedisRepository redisRepository,
            DdcPublishStateTransitionService stateTransitions,
            PlatformTransactionManager transactionManager) {
        this(
                configItemRepository,
                versionRepository,
                taskRepository,
                ackRepository,
                redisRepository,
                stateTransitions,
                transactionManager,
                Clock.systemUTC()
        );
    }

    DdcPendingPublishDispatcher(
            DdcConfigItemRepository configItemRepository,
            DdcConfigVersionRepository versionRepository,
            DdcPublishTaskRepository taskRepository,
            DdcPublishAckRepository ackRepository,
            DdcRedisRepository redisRepository,
            DdcPublishStateTransitionService stateTransitions,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.configItemRepository = configItemRepository;
        this.versionRepository = versionRepository;
        this.taskRepository = taskRepository;
        this.ackRepository = ackRepository;
        this.redisRepository = redisRepository;
        this.stateTransitions = stateTransitions;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public DdcPublishTaskEntity dispatch(String changeId) {
        DdcPublishTaskEntity task = requiredTask(changeId);
        if (!DISPATCHABLE_STATUSES.contains(task.getStatus())) {
            return task;
        }
        stateTransitions.markPublishing(changeId);
        DdcAtomicPublishCommand command;
        try {
            command = transactionTemplate.execute(status -> command(changeId));
        } catch (DdcAdminException exception) {
            stateTransitions.fail(
                    changeId,
                    "DISPATCH_PREPARE",
                    exception.getMessage()
            );
            throw exception;
        } catch (RuntimeException exception) {
            return stateTransitions.unknown(
                    changeId,
                    "DISPATCH_PREPARE",
                    exception.getMessage()
            );
        }
        if (command == null) {
            return stateTransitions.fail(
                    changeId,
                    "DISPATCH_PREPARE",
                    "publish command preparation failed"
            );
        }
        try {
            redisRepository.dispatch(command);
        } catch (DdcAdminException exception) {
            stateTransitions.fail(
                    changeId,
                    "REDIS_DISPATCH",
                    exception.getMessage()
            );
            throw exception;
        } catch (RuntimeException exception) {
            return stateTransitions.unknown(
                    changeId,
                    "REDIS_DISPATCH",
                    exception.getMessage()
            );
        }
        try {
            Boolean advanced = transactionTemplate.execute(
                    status -> advancePublishedVersion(command)
            );
            if (!Boolean.TRUE.equals(advanced)) {
                return stateTransitions.unknown(
                        changeId,
                        "PUBLISHED_POINTER",
                        "published pointer update is uncertain"
                );
            }
        } catch (RuntimeException exception) {
            return stateTransitions.unknown(
                    changeId,
                    "PUBLISHED_POINTER",
                    exception.getMessage()
            );
        }
        return stateTransitions.refreshAfterAck(changeId);
    }

    private DdcAtomicPublishCommand command(String changeId) {
        DdcPublishTaskEntity task = requiredTask(changeId);
        DdcConfigItemEntity config = configItemRepository.findById(task.getConfigId())
                .orElseThrow(() -> new DdcAdminException("config item not found"));
        if (config.getPublishedVersion() != null
                && config.getPublishedVersion() > task.getTargetVersion()) {
            throw new DdcAdminException("published config version is newer than task");
        }
        DdcConfigVersionEntity version = versionRepository.findByConfigIdAndVersion(
                        task.getConfigId(),
                        task.getTargetVersion()
                )
                .orElseThrow(() -> new DdcAdminException(
                        "published config version not found"
                ));
        List<DdcPublishAckEntity> targets = ackRepository.findByChangeId(changeId)
                .stream()
                .sorted(Comparator
                        .comparing(DdcPublishAckEntity::getInstanceId)
                        .thenComparing(DdcPublishAckEntity::getLeaseId))
                .toList();
        if (targets.isEmpty()) {
            throw new DdcAdminException("publish targets not found");
        }
        if (!Objects.equals(
                task.getResourceChecksum(),
                DdcChecksum.resource(
                        task.getResourceName(),
                        version.getFormat(),
                        version.getNewContent()
                )
        )) {
            throw new DdcAdminException("publish resource checksum changed");
        }
        DdcPublishMessage message = message(task, version, targets);
        return new DdcAtomicPublishCommand(
                task.getConfigId(),
                task.getChangeId(),
                task.getBizCode(),
                task.getEnv(),
                task.getAppCode(),
                task.getResourceName(),
                config.getPublishedVersion(),
                task.getTargetVersion(),
                version.getNewContent(),
                message.getChecksum(),
                message
        );
    }

    private DdcPublishMessage message(
            DdcPublishTaskEntity task,
            DdcConfigVersionEntity version,
            List<DdcPublishAckEntity> targets) {
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
                .map(target -> new DdcPublishTarget(
                        target.getInstanceId(),
                        target.getLeaseId()
                ))
                .toList());
        message.setChecksum(DdcChecksum.sha256(message));
        return message;
    }

    private boolean advancePublishedVersion(DdcAtomicPublishCommand command) {
        int changed = configItemRepository.advancePublishedVersion(
                command.configId(),
                command.expectedPublishedVersion(),
                command.targetVersion(),
                now()
        );
        if (changed == 1) {
            return true;
        }
        return configItemRepository.findById(command.configId())
                .map(DdcConfigItemEntity::getPublishedVersion)
                .filter(version -> Objects.equals(version, command.targetVersion()))
                .isPresent();
    }

    private long timestamp(DdcPublishTaskEntity task) {
        LocalDateTime createdAt = task.getCreatedAt();
        if (createdAt == null) {
            throw new DdcAdminException("publish task creation time is required");
        }
        return createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private DdcPublishTaskEntity requiredTask(String changeId) {
        return taskRepository.findByChangeId(changeId)
                .orElseThrow(() -> new DdcAdminException("publish task not found"));
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
    }
}
