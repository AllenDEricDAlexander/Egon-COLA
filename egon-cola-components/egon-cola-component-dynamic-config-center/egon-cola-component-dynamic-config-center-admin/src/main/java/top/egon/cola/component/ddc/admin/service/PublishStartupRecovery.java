package top.egon.cola.component.ddc.admin.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;

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

    public PublishStartupRecovery(
            DdcPublishTaskRepository taskRepository,
            DdcPublishStateTransitionService stateTransitions) {
        this.taskRepository = taskRepository;
        this.stateTransitions = stateTransitions;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        taskRepository.findByStatusIn(ACTIVE_STATUSES)
                .forEach(task -> stateTransitions.unknown(
                        task.getChangeId(),
                        "admin restarted before publish completed"
                ));
    }
}
