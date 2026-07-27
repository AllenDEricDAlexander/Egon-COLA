package top.egon.cola.component.ddc.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcPublishTimeoutScannerTest {

    @Test
    void springSelectsTheProductionConstructor() {
        new ApplicationContextRunner()
                .withBean(
                        DdcPublishTaskRepository.class,
                        () -> mock(DdcPublishTaskRepository.class)
                )
                .withBean(
                        DdcPublishStateTransitionService.class,
                        () -> mock(DdcPublishStateTransitionService.class)
                )
                .withBean(DdcAdminProperties.class)
                .withBean(PublishTimeoutScanner.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(PublishTimeoutScanner.class));
    }

    @Test
    void routesExpiredPendingAndPublishingTasksThroughSharedTransitions() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-24T10:00:00Z"),
                ZoneOffset.UTC
        );
        DdcPublishTaskRepository repository = mock(DdcPublishTaskRepository.class);
        DdcPublishStateTransitionService transitions =
                mock(DdcPublishStateTransitionService.class);
        DdcAdminProperties properties = new DdcAdminProperties();
        properties.getPublish().setDispatchTimeoutMs(5000);
        LocalDateTime now = LocalDateTime.ofInstant(
                clock.instant(),
                ZoneId.systemDefault()
        );
        DdcPublishTaskEntity pending = task(
                "pending",
                PublishStatus.PENDING,
                now.minusSeconds(6),
                null,
                30000L
        );
        DdcPublishTaskEntity publishing = task(
                "publishing",
                PublishStatus.PUBLISHING,
                now.minusMinutes(1),
                now.minusSeconds(31),
                30000L
        );
        DdcPublishTaskEntity live = task(
                "live",
                PublishStatus.PUBLISHING,
                now.minusMinutes(1),
                now.minusSeconds(10),
                30000L
        );
        when(repository.findByStatusIn(List.of(
                PublishStatus.PENDING.name(),
                PublishStatus.PUBLISHING.name()
        ))).thenReturn(List.of(pending, publishing, live));
        PublishTimeoutScanner scanner =
                new PublishTimeoutScanner(repository, transitions, properties, clock);

        assertThat(scanner.scanExpired()).isEqualTo(2);

        verify(transitions).fail(
                "pending",
                "DISPATCH_TIMEOUT",
                "publish dispatch timed out"
        );
        verify(transitions).timeout(
                "publishing",
                "publish acknowledgement timed out"
        );
    }

    private DdcPublishTaskEntity task(String changeId,
                                      PublishStatus status,
                                      LocalDateTime createdAt,
                                      LocalDateTime dispatchedAt,
                                      Long timeoutMs) {
        DdcPublishTaskEntity task = new DdcPublishTaskEntity();
        task.setChangeId(changeId);
        task.setStatus(status.name());
        task.setCreatedAt(createdAt);
        task.setDispatchedAt(dispatchedAt);
        task.setTimeoutMs(timeoutMs);
        return task;
    }
}
