package top.egon.cola.component.gateway.admin.application.release;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishStatus;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.application.routing.GatewayDraftService;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupRepository;
import top.egon.cola.component.gateway.admin.rule.CompiledGatewayRelease;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.FAILED;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.SUCCESS;

class GatewayReleaseServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-07-26T08:00:00Z");

    @Test
    void activationSuccessCompletesReleaseAndAdvancesDraft() {
        Fixture fixture = fixture(outcome(SUCCESS));

        fixture.service.retry("release-1", actor(), request());

        verify(fixture.releases).completeAttempt(
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
        assertThat(fixture.draft.getBasedOnReleaseId())
                .isEqualTo("release-1");
        verify(fixture.drafts).flush();
    }

    @Test
    void failedPublicationDoesNotAdvanceDraft() {
        Fixture fixture = fixture(outcome(FAILED));

        fixture.service.retry("release-1", actor(), request());

        verify(fixture.releases).completeAttempt(
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
        assertThat(fixture.draft.getBasedOnReleaseId()).isNull();
        verify(fixture.drafts, never()).flush();
    }

    private Fixture fixture(
            GatewayReleasePublicationCoordinator.PublicationOutcome outcome) {
        GatewayGroupRepository groups = mock(GatewayGroupRepository.class);
        GatewayDraftRepository drafts = mock(GatewayDraftRepository.class);
        GatewayDraftService draftService = mock(GatewayDraftService.class);
        GatewayCatalogStore catalog = mock(GatewayCatalogStore.class);
        GatewayReleaseStore releases = mock(GatewayReleaseStore.class);
        GatewayAuditLogRepository audits =
                mock(GatewayAuditLogRepository.class);
        GatewayReleasePublicationCoordinator publications =
                mock(GatewayReleasePublicationCoordinator.class);
        CompiledGatewayRelease compiled = mock(CompiledGatewayRelease.class);
        GatewayDraftEntity draft = new GatewayDraftEntity(
                "group-1",
                "admin",
                NOW
        );
        GatewayReleaseStore.ReleaseRecord release = release();
        when(releases.find("release-1"))
                .thenReturn(Optional.of(release));
        when(releases.nextAttempt("release-1", NOW)).thenReturn(2);
        when(releases.loadCompiled("release-1")).thenReturn(compiled);
        when(releases.attempts("release-1")).thenReturn(List.of());
        when(drafts.findById("group-1")).thenReturn(Optional.of(draft));
        when(publications.execute(
                "release-1",
                2,
                compiled,
                "admin"
        )).thenReturn(outcome);
        GatewayReleaseService service = new GatewayReleaseService(
                groups,
                drafts,
                draftService,
                catalog,
                releases,
                audits,
                transactions(),
                publications,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        return new Fixture(service, releases, drafts, draft);
    }

    private GatewayReleasePublicationCoordinator.PublicationOutcome outcome(
            GatewayReleasePublicationStore.PublicationStatus status) {
        return new GatewayReleasePublicationCoordinator.PublicationOutcome(
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

    private GatewayReleaseStore.ReleaseRecord release() {
        return new GatewayReleaseStore.ReleaseRecord(
                "release-1",
                "group-1",
                1L,
                null,
                null,
                GatewayReleaseStatus.UNKNOWN,
                false,
                null,
                Map.of(),
                Map.of(),
                "retry",
                NOW,
                "admin",
                NOW
        );
    }

    private AdminActor actor() {
        return new AdminActor(
                "admin",
                AdminActor.ActorType.USER,
                Set.of(),
                Set.of()
        );
    }

    private RequestAuditContext request() {
        return new RequestAuditContext("request-1", "trace-1");
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

    private record Fixture(
            GatewayReleaseService service,
            GatewayReleaseStore releases,
            GatewayDraftRepository drafts,
            GatewayDraftEntity draft
    ) {
    }
}
