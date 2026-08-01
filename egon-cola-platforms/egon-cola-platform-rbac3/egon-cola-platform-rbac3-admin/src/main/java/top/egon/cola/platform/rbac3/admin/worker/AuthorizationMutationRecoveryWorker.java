package top.egon.cola.platform.rbac3.admin.worker;

import top.egon.cola.platform.rbac3.admin.runtime.application.RuntimeQueryService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Replays committed authorization mutations claimed with database row ownership.
 */
public final class AuthorizationMutationRecoveryWorker
        implements RuntimeQueryService.MutationRecoveryPort {

    private final RecoveryStore store;
    private final ProjectionExecutor projector;
    private final Clock clock;
    private final int batchSize;

    public AuthorizationMutationRecoveryWorker(
            RecoveryStore store,
            ProjectionExecutor projector,
            Clock clock,
            int batchSize) {
        this.store = Objects.requireNonNull(store, "store");
        this.projector = Objects.requireNonNull(projector, "projector");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (batchSize < 1 || batchSize > 200) {
            throw new IllegalArgumentException("batchSize must be between 1 and 200");
        }
        this.batchSize = batchSize;
    }

    @Override
    public RuntimeQueryService.RetryResult retry(
            String tenantId,
            String mutationId,
            String actorId) {
        MutationWork work = store.claimById(
                        required(tenantId, "tenantId"),
                        required(mutationId, "mutationId"))
                .orElseThrow(() -> new IllegalArgumentException(
                        "authorization mutation was not found"));
        if ("COMPLETED".equals(work.status())) {
            return new RuntimeQueryService.RetryResult(mutationId, "COMPLETED");
        }
        boolean completed = recover(work, required(actorId, "actorId"));
        return new RuntimeQueryService.RetryResult(
                mutationId, completed ? "COMPLETED" : "RECOVERY_REQUIRED");
    }

    public int runOnce() {
        int completed = 0;
        for (MutationWork work : store.claimRecoverable(batchSize)) {
            if (recover(work, "rbac3-recovery-worker")) {
                completed++;
            }
        }
        return completed;
    }

    private boolean recover(MutationWork work, String actorId) {
        Instant now = clock.instant();
        try {
            projector.project(work);
            store.completed(work.mutationId(), now, actorId);
            return true;
        } catch (RuntimeException unavailable) {
            store.failed(
                    work.mutationId(), "AUTH_PROPAGATION_PENDING", now, actorId);
            return false;
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public interface RecoveryStore {

        Optional<MutationWork> claimById(String tenantId, String mutationId);

        List<MutationWork> claimRecoverable(int batchSize);

        void completed(String mutationId, Instant now, String actorId);

        void failed(String mutationId, String reasonCode, Instant now, String actorId);
    }

    @FunctionalInterface
    public interface ProjectionExecutor {

        void project(MutationWork mutation);
    }

    public record MutationWork(
            String mutationId,
            String tenantId,
            String scopeType,
            String scopeId,
            String status) {
    }
}
