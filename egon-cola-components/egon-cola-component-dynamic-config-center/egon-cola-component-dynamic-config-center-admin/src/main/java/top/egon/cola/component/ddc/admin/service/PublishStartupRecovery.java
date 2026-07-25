package top.egon.cola.component.ddc.admin.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublishStartupRecovery implements ApplicationRunner {

    private static final List<String> ACTIVE_STATUSES = List.of(
            PublishStatus.PENDING.name(),
            PublishStatus.PUBLISHING.name()
    );

    private final DdcPublishTaskRepository taskRepository;

    private final DdcPublishStateTransitionService stateTransitions;

    private final DdcAdminProperties properties;

    private final Clock clock;

    public PublishStartupRecovery(
            DdcPublishTaskRepository taskRepository,
            DdcPublishStateTransitionService stateTransitions,
            DdcAdminProperties properties) {
        this(
                taskRepository,
                stateTransitions,
                properties,
                Clock.systemUTC()
        );
    }

    PublishStartupRecovery(
            DdcPublishTaskRepository taskRepository,
            DdcPublishStateTransitionService stateTransitions,
            DdcAdminProperties properties,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.stateTransitions = stateTransitions;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long staleMs = properties.getPublish().getRecoveryStaleMs();
        if (staleMs <= 0) {
            throw new IllegalStateException(
                    "DDC publish recovery stale interval must be positive"
            );
        }
        LocalDateTime staleBefore = LocalDateTime.ofInstant(
                clock.instant().minusMillis(staleMs),
                clock.getZone()
        );
        taskRepository.findByStatusInAndUpdatedAtBefore(
                        ACTIVE_STATUSES,
                        staleBefore
                )
                .forEach(task -> stateTransitions.unknown(
                        task.getChangeId(),
                        "publish owner did not complete before HA stale timeout"
                ));
    }
}
