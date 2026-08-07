package top.egon.cola.component.ddc.admin.service.publish;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublishStartupRecovery implements ApplicationRunner {

    private static final List<String> ACTIVE_STATUSES = List.of(
            PublishStatus.PENDING.name(),
            PublishStatus.PUBLISHING.name(),
            PublishStatus.UNKNOWN.name()
    );

    private final DdcPublishTaskRepository taskRepository;

    private final DdcPendingPublishDispatcher dispatcher;

    private final DdcAdminProperties properties;

    private final Clock clock;

    @Autowired
    public PublishStartupRecovery(
            DdcPublishTaskRepository taskRepository,
            DdcPendingPublishDispatcher dispatcher,
            DdcAdminProperties properties) {
        this(
                taskRepository,
                dispatcher,
                properties,
                Clock.systemUTC()
        );
    }

    PublishStartupRecovery(
            DdcPublishTaskRepository taskRepository,
            DdcPendingPublishDispatcher dispatcher,
            DdcAdminProperties properties,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.dispatcher = dispatcher;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        recoverStale();
    }

    @Scheduled(
            fixedDelayString = "${egon.cola.component.ddc.admin.publish.recovery-stale-ms:120000}",
            initialDelayString = "${egon.cola.component.ddc.admin.publish.recovery-stale-ms:120000}"
    )
    public int recoverStale() {
        long staleMs = properties.getPublish().getRecoveryStaleMs();
        if (staleMs <= 0) {
            throw new IllegalStateException(
                    "DDC publish recovery stale interval must be positive"
            );
        }
        LocalDateTime staleBefore = LocalDateTime.ofInstant(
                clock.instant().minusMillis(staleMs),
                ZoneId.systemDefault()
        );
        List<DdcPublishTaskEntity> tasks =
                taskRepository.findByStatusInAndUpdatedAtBefore(
                        ACTIVE_STATUSES,
                        staleBefore
                );
        tasks.forEach(task -> replay(task.getChangeId()));
        return tasks.size();
    }

    private void replay(String changeId) {
        try {
            dispatcher.dispatch(changeId);
        } catch (DdcAdminException ignored) {
            // The dispatcher persisted the deterministic rejection on the task.
        }
    }
}
