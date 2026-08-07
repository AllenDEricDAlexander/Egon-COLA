package top.egon.cola.component.ddc.admin.service.publish;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class PublishTimeoutScanner {

    private static final List<String> ACTIVE_STATUSES = List.of(
            PublishStatus.PENDING.name(),
            PublishStatus.PUBLISHING.name()
    );

    private final DdcPublishTaskRepository taskRepository;

    private final DdcPublishStateTransitionService stateTransitions;

    private final DdcAdminProperties properties;

    private final Clock clock;

    @Autowired
    public PublishTimeoutScanner(
            DdcPublishTaskRepository taskRepository,
            DdcPublishStateTransitionService stateTransitions,
            DdcAdminProperties properties) {
        this(taskRepository, stateTransitions, properties, Clock.systemUTC());
    }

    PublishTimeoutScanner(
            DdcPublishTaskRepository taskRepository,
            DdcPublishStateTransitionService stateTransitions,
            DdcAdminProperties properties,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.stateTransitions = stateTransitions;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${egon.cola.component.ddc.admin.publish.scan-interval-ms:1000}",
            initialDelayString = "${egon.cola.component.ddc.admin.publish.scan-interval-ms:1000}"
    )
    public int scanExpired() {
        Instant now = clock.instant();
        return taskRepository.findByStatusIn(ACTIVE_STATUSES).stream()
                .mapToInt(task -> expire(task, now))
                .sum();
    }

    private int expire(DdcPublishTaskEntity task, Instant now) {
        if (PublishStatus.PENDING.name().equals(task.getStatus())) {
            Instant deadline = instant(task.getCreatedAt())
                    .plusMillis(properties.getPublish().getDispatchTimeoutMs());
            if (!deadline.isAfter(now)) {
                stateTransitions.fail(
                        task.getChangeId(),
                        "DISPATCH_TIMEOUT",
                        "publish dispatch timed out"
                );
                return 1;
            }
            return 0;
        }
        LocalDateTime dispatchedAt =
                task.getDispatchedAt() == null ? task.getUpdatedAt() : task.getDispatchedAt();
        long timeoutMs = task.getTimeoutMs() == null
                ? properties.getPublish().getDefaultTimeoutMs()
                : task.getTimeoutMs();
        if (!instant(dispatchedAt).plusMillis(timeoutMs).isAfter(now)) {
            stateTransitions.timeout(
                    task.getChangeId(),
                    "publish acknowledgement timed out"
            );
            return 1;
        }
        return 0;
    }

    private Instant instant(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
