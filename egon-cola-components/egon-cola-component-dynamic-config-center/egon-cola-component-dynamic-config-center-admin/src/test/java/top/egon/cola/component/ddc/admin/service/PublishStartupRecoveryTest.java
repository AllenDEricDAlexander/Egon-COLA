package top.egon.cola.component.ddc.admin.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublishStartupRecoveryTest {

    @Test
    void marksEveryPersistedActiveTaskUnknownWithoutInferringPartialAckSuccess()
            throws Exception {
        DdcPublishTaskRepository repository = mock(DdcPublishTaskRepository.class);
        DdcPublishStateTransitionService transitions =
                mock(DdcPublishStateTransitionService.class);
        DdcPublishTaskEntity pending = task("pending", PublishStatus.PENDING);
        DdcPublishTaskEntity publishing = task("publishing", PublishStatus.PUBLISHING);
        when(repository.findByStatusIn(List.of(
                PublishStatus.PENDING.name(),
                PublishStatus.PUBLISHING.name()
        ))).thenReturn(List.of(pending, publishing));
        PublishStartupRecovery recovery =
                new PublishStartupRecovery(repository, transitions);

        recovery.run(null);

        verify(transitions).unknown("pending", "admin restarted before publish completed");
        verify(transitions).unknown("publishing", "admin restarted before publish completed");
    }

    private DdcPublishTaskEntity task(String changeId, PublishStatus status) {
        DdcPublishTaskEntity task = new DdcPublishTaskEntity();
        task.setChangeId(changeId);
        task.setStatus(status.name());
        task.setAckCount(1);
        task.setTargetCount(2);
        return task;
    }
}
