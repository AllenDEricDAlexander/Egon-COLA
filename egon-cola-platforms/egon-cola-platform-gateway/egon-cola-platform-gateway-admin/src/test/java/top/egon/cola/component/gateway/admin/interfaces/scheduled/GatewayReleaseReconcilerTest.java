package top.egon.cola.component.gateway.admin.release.controller.scheduled;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishStatus;
import top.egon.cola.component.gateway.admin.release.service.GatewayReleasePublicationCoordinator;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleasePublicationRepository;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleaseRepository;
import top.egon.cola.component.gateway.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.routing.domain.po.GatewayDraftPO;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftJpaRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.FAILED;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.SUCCESS;

class GatewayReleaseReconcilerTest {

    private static final Instant NOW =
            Instant.parse("2026-07-26T08:00:00Z");

    @Test
    void resumesAttemptByJournalAndAdvancesDraftAfterActivationSuccess() {
        GatewayReleaseRepository releases = mock(GatewayReleaseRepository.class);
        GatewayReleasePublicationCoordinator coordinator =
                mock(GatewayReleasePublicationCoordinator.class);
        GatewayDraftJpaRepository drafts = mock(GatewayDraftJpaRepository.class);
        GatewayDraftPO draft = new GatewayDraftPO(
                "group-1",
                "admin",
                NOW
        );
        top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO attempt =
                new top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO(
                        "release-1",
                        "group-1",
                        2
                );
        when(releases.recoverable()).thenReturn(List.of(attempt));
        when(coordinator.resume("release-1", 2))
                .thenReturn(outcome(SUCCESS));
        when(drafts.findById("group-1"))
                .thenReturn(Optional.of(draft));
        GatewayReleaseReconciler reconciler = new GatewayReleaseReconciler(
                releases,
                coordinator,
                drafts,
                transactions(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        reconciler.reconcile();

        verify(coordinator).resume("release-1", 2);
        verify(releases).completeAttempt(
                eq("release-1"),
                eq(2),
                eq(GatewayReleaseStatus.SUCCESS),
                eq(false),
                eq("018f22d8155d70008000000000000001"),
                isNull(),
                isNull(),
                eq(List.of()),
                eq(NOW)
        );
        assertThat(draft.getBasedOnReleaseId()).isEqualTo("release-1");
        verify(drafts).flush();
    }

    @Test
    void failedPhaseDoesNotAdvanceDraft() {
        GatewayReleaseRepository releases = mock(GatewayReleaseRepository.class);
        GatewayReleasePublicationCoordinator coordinator =
                mock(GatewayReleasePublicationCoordinator.class);
        GatewayDraftJpaRepository drafts = mock(GatewayDraftJpaRepository.class);
        when(releases.recoverable()).thenReturn(List.of(
                new top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO(
                        "release-1",
                        "group-1",
                        2
                )
        ));
        when(coordinator.resume("release-1", 2))
                .thenReturn(outcome(FAILED));
        GatewayReleaseReconciler reconciler = new GatewayReleaseReconciler(
                releases,
                coordinator,
                drafts,
                transactions(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        reconciler.reconcile();

        verify(releases).completeAttempt(
                eq("release-1"),
                eq(2),
                eq(GatewayReleaseStatus.FAILED),
                eq(false),
                eq("018f22d8155d70008000000000000001"),
                eq("DDC_PUBLISH_FAILED"),
                eq("failed"),
                eq(List.of()),
                eq(NOW)
        );
        verify(drafts, never()).flush();
    }

    private top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO outcome(
            top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum status) {
        return new top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO(
                status,
                "018f22d8155d70008000000000000001",
                new DdcManagementPublishResult(
                        "018f22d8155d70008000000000000001",
                        DdcManagementPublishStatus.valueOf(status.name()),
                        2L,
                        "checksum",
                        0,
                        List.of(),
                        status == SUCCESS ? null : "failed",
                        NOW,
                        NOW,
                        NOW
                ),
                false
        );
    }

    private TransactionTemplate transactions() {
        return new TransactionTemplate(new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(
                    TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        });
    }
}
