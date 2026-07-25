package top.egon.cola.component.ddc.admin.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublishStartupRecoveryTest {

    @Test
    void onlyClaimsStalePersistedTasksWithoutDisruptingAnotherLiveAdmin()
            throws Exception {
        DdcPublishTaskRepository repository = mock(DdcPublishTaskRepository.class);
        DdcPublishStateTransitionService transitions =
                mock(DdcPublishStateTransitionService.class);
        DdcPublishTaskEntity pending = task("pending", PublishStatus.PENDING);
        DdcPublishTaskEntity publishing = task("publishing", PublishStatus.PUBLISHING);
        Instant now = Instant.parse("2026-07-25T00:00:00Z");
        DdcAdminProperties properties = new DdcAdminProperties();
        properties.getPublish().setRecoveryStaleMs(120000);
        LocalDateTime staleBefore = LocalDateTime.ofInstant(
                now.minusMillis(120000),
                ZoneId.systemDefault()
        );
        when(repository.findByStatusInAndUpdatedAtBefore(List.of(
                PublishStatus.PENDING.name(),
                PublishStatus.PUBLISHING.name()
        ), staleBefore)).thenReturn(List.of(pending, publishing));
        PublishStartupRecovery recovery =
                new PublishStartupRecovery(
                        repository,
                        transitions,
                        properties,
                        Clock.fixed(now, ZoneOffset.UTC)
                );

        recovery.run(null);

        verify(transitions).unknown(
                "pending",
                "publish owner did not complete before HA stale timeout"
        );
        verify(transitions).unknown(
                "publishing",
                "publish owner did not complete before HA stale timeout"
        );
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
