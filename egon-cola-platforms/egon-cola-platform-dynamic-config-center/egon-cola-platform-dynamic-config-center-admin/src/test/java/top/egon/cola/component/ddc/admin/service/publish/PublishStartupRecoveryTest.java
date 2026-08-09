package top.egon.cola.component.ddc.admin.service.publish;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublishStartupRecoveryTest {

    @Test
    void onlyClaimsStalePersistedTasksWithoutDisruptingAnotherLiveAdmin()
            throws Exception {
        DdcPublishTaskRepository repository = mock(DdcPublishTaskRepository.class);
        DdcPendingPublishDispatcher dispatcher =
                mock(DdcPendingPublishDispatcher.class);
        DdcPublishTaskEntity pending = task("pending", PublishStatus.PENDING);
        DdcPublishTaskEntity publishing = task("publishing", PublishStatus.PUBLISHING);
        DdcPublishTaskEntity unknown = task("unknown", PublishStatus.UNKNOWN);
        Instant now = Instant.parse("2026-07-25T00:00:00Z");
        DdcAdminProperties properties = new DdcAdminProperties();
        properties.getPublish().setRecoveryStaleMs(120000);
        LocalDateTime staleBefore = LocalDateTime.ofInstant(
                now.minusMillis(120000),
                ZoneId.systemDefault()
        );
        when(repository.findByStatusInAndUpdatedAtBefore(List.of(
                PublishStatus.PENDING.name(),
                PublishStatus.PUBLISHING.name(),
                PublishStatus.UNKNOWN.name()
        ), staleBefore)).thenReturn(List.of(pending, publishing, unknown));
        LocalDateTime claimedAt = LocalDateTime.ofInstant(
                now,
                ZoneId.systemDefault()
        );
        when(repository.claimStaleForRecovery(
                "pending",
                List.of(
                        PublishStatus.PENDING.name(),
                        PublishStatus.PUBLISHING.name(),
                        PublishStatus.UNKNOWN.name()
                ),
                staleBefore,
                claimedAt
        )).thenReturn(1);
        when(repository.claimStaleForRecovery(
                "publishing",
                List.of(
                        PublishStatus.PENDING.name(),
                        PublishStatus.PUBLISHING.name(),
                        PublishStatus.UNKNOWN.name()
                ),
                staleBefore,
                claimedAt
        )).thenReturn(0);
        when(repository.claimStaleForRecovery(
                "unknown",
                List.of(
                        PublishStatus.PENDING.name(),
                        PublishStatus.PUBLISHING.name(),
                        PublishStatus.UNKNOWN.name()
                ),
                staleBefore,
                claimedAt
        )).thenReturn(1);
        PublishStartupRecovery recovery =
                new PublishStartupRecovery(
                        repository,
                        dispatcher,
                        properties,
                        Clock.fixed(now, ZoneOffset.UTC)
                );

        assertThat(recovery.recoverStale()).isEqualTo(2);

        verify(dispatcher).dispatch("pending");
        verify(dispatcher, never()).dispatch("publishing");
        verify(dispatcher).dispatch("unknown");
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
