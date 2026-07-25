package top.egon.cola.component.gateway.admin.interfaces.scheduled;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTarget;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleaseStore;
import top.egon.cola.component.gateway.admin.domain.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;

import java.time.Clock;
import java.util.List;

@Component
public class GatewayReleaseReconciler {

    private final GatewayReleaseStore releases;

    private final DdcManagementClient client;

    private final GatewayDraftRepository drafts;

    private final TransactionTemplate transactions;

    private final Clock clock;

    public GatewayReleaseReconciler(
            GatewayReleaseStore releases,
            ObjectProvider<DdcManagementClient> client,
            GatewayDraftRepository drafts,
            TransactionTemplate transactions) {
        this(
                releases,
                client.getIfAvailable(),
                drafts,
                transactions,
                Clock.systemUTC()
        );
    }

    GatewayReleaseReconciler(
            GatewayReleaseStore releases,
            DdcManagementClient client,
            GatewayDraftRepository drafts,
            TransactionTemplate transactions,
            Clock clock) {
        this.releases = releases;
        this.client = client;
        this.drafts = drafts;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString =
                    "${gateway.admin.release-reconcile-delay:30000}"
    )
    public void reconcile() {
        if (client == null) {
            return;
        }
        releases.recoverable().forEach(this::reconcile);
    }

    private void reconcile(GatewayReleaseStore.ReleaseRecord release) {
        DdcManagementPublishTask task;
        try {
            task = client.getPublishTask(release.changeId());
        } catch (RuntimeException unavailable) {
            return;
        }
        GatewayReleaseStatus status = terminal(task.status());
        if (status == null) {
            return;
        }
        int attempt = releases.latestAttempt(release.id());
        List<GatewayReleaseStore.TargetRecord> targets = task.targets()
                .stream()
                .map(this::target)
                .toList();
        transactions.executeWithoutResult(transaction -> {
            releases.completeAttempt(
                        release.id(),
                        attempt,
                        status,
                        task.status()
                                == DdcManagementPublishStatus.PARTIAL_SUCCESS,
                        task.changeId(),
                        task.errorMessage() == null
                                ? null
                                : "DDC_PUBLISH_" + task.status(),
                        task.errorMessage(),
                        targets,
                        clock.instant()
                );
            if (status == GatewayReleaseStatus.SUCCESS) {
                GatewayDraftEntity draft = drafts.findById(
                        release.gatewayGroupId()
                ).orElse(null);
                if (draft != null
                        && !release.id().equals(
                        draft.getBasedOnReleaseId())) {
                    draft.baseOn(
                            release.id(),
                            "gateway-release-reconciler",
                            clock.instant()
                    );
                    drafts.flush();
                }
            }
        });
    }

    private GatewayReleaseStatus terminal(
            DdcManagementPublishStatus status) {
        return switch (status) {
            case SUCCESS -> GatewayReleaseStatus.SUCCESS;
            case PARTIAL_SUCCESS, FAILED -> GatewayReleaseStatus.FAILED;
            case TIMEOUT -> GatewayReleaseStatus.TIMEOUT;
            case UNKNOWN -> GatewayReleaseStatus.UNKNOWN;
            case PENDING, PUBLISHING -> null;
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
