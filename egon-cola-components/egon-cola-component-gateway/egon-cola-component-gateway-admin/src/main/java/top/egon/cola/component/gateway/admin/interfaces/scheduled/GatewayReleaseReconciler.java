package top.egon.cola.component.gateway.admin.interfaces.scheduled;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTarget;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleasePublicationCoordinator;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleasePublicationStore;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleaseStore;
import top.egon.cola.component.gateway.admin.domain.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;

import java.time.Clock;
import java.util.List;

@Component
public class GatewayReleaseReconciler {

    private final GatewayReleaseStore releases;

    private final GatewayReleasePublicationCoordinator coordinator;

    private final GatewayDraftRepository drafts;

    private final TransactionTemplate transactions;

    private final Clock clock;

    @Autowired
    public GatewayReleaseReconciler(
            GatewayReleaseStore releases,
            ObjectProvider<GatewayReleasePublicationCoordinator>
                    coordinator,
            GatewayDraftRepository drafts,
            TransactionTemplate transactions) {
        this(
                releases,
                coordinator.getIfAvailable(),
                drafts,
                transactions,
                Clock.systemUTC()
        );
    }

    GatewayReleaseReconciler(
            GatewayReleaseStore releases,
            GatewayReleasePublicationCoordinator coordinator,
            GatewayDraftRepository drafts,
            TransactionTemplate transactions,
            Clock clock) {
        this.releases = releases;
        this.coordinator = coordinator;
        this.drafts = drafts;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString =
                    "${gateway.admin.release-reconcile-delay:30000}"
    )
    public void reconcile() {
        if (coordinator == null) {
            return;
        }
        releases.recoverable().forEach(this::reconcile);
    }

    private void reconcile(GatewayReleaseStore.RecoverableAttempt attempt) {
        GatewayReleasePublicationCoordinator.PublicationOutcome outcome;
        try {
            outcome = coordinator.resume(
                    attempt.releaseId(),
                    attempt.attemptNo()
            );
        } catch (RuntimeException unavailable) {
            return;
        }
        List<GatewayReleaseStore.TargetRecord> targets = outcome.result()
                .targets()
                .stream()
                .map(this::target)
                .toList();
        transactions.executeWithoutResult(transaction -> {
            releases.completeAttempt(
                    attempt.releaseId(),
                    attempt.attemptNo(),
                    releaseStatus(outcome.status()),
                    outcome.partialApplied(),
                    outcome.changeId(),
                    outcome.successful()
                            ? null
                            : "DDC_PUBLISH_" + outcome.status(),
                    outcome.result().errorMessage(),
                    targets,
                    clock.instant()
            );
            if (outcome.successful()) {
                advanceDraft(attempt);
            }
        });
    }

    private void advanceDraft(
            GatewayReleaseStore.RecoverableAttempt attempt) {
        GatewayDraftEntity draft = drafts.findById(
                attempt.gatewayGroupId()
        ).orElse(null);
        if (draft != null
                && !attempt.releaseId().equals(
                draft.getBasedOnReleaseId())) {
            draft.baseOn(
                    attempt.releaseId(),
                    "gateway_release_reconciler",
                    clock.instant()
            );
            drafts.flush();
        }
    }

    private GatewayReleaseStatus releaseStatus(
            GatewayReleasePublicationStore.PublicationStatus status) {
        return switch (status) {
            case SUCCESS -> GatewayReleaseStatus.SUCCESS;
            case FAILED, PARTIAL_SUCCESS -> GatewayReleaseStatus.FAILED;
            case TIMEOUT -> GatewayReleaseStatus.TIMEOUT;
            case PLANNED, RESOLVED, SUBMITTED, UNKNOWN ->
                    GatewayReleaseStatus.UNKNOWN;
        };
    }

    private GatewayReleaseStore.TargetRecord target(
            DdcManagementPublishTarget target) {
        return new GatewayReleaseStore.TargetRecord(
                target.instanceId(),
                target.leaseId(),
                target.status(),
                target.currentVersion(),
                null,
                target.errorMessage() == null
                        ? null
                        : "DDC_TARGET_ERROR",
                target.ackAt() == null
                        ? clock.instant()
                        : target.ackAt()
        );
    }
}
