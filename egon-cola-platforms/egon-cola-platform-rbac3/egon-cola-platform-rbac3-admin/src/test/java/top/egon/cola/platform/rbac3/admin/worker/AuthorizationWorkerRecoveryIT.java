package top.egon.cola.platform.rbac3.admin.worker;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.integration.outbox.Rbac3RuntimeProjectionDeliveryHandler;
import top.egon.cola.platform.rbac3.admin.runtime.application.RuntimeQueryService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationWorkerRecoveryIT {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void mutationRecoveryIsAddressedByIdAndIsIdempotent() {
        InMemoryRecoveryStore store = new InMemoryRecoveryStore();
        AtomicInteger projections = new AtomicInteger();
        var worker = new AuthorizationMutationRecoveryWorker(
                store, mutation -> projections.incrementAndGet(),
                Clock.fixed(NOW, ZoneOffset.UTC), 20);

        RuntimeQueryService.RetryResult first = worker.retry("7", "900", "operator");
        RuntimeQueryService.RetryResult duplicate = worker.retry("7", "900", "operator");

        assertThat(first.status()).isEqualTo("COMPLETED");
        assertThat(duplicate.status()).isEqualTo("COMPLETED");
        assertThat(projections).hasValue(1);
        assertThat(store.failureCodes).isEmpty();
    }

    @Test
    void scheduledRecoveryClaimsOnlyItsSkipLockedBatchAndRetriesFailures() {
        InMemoryRecoveryStore store = new InMemoryRecoveryStore();
        store.pending.add(new AuthorizationMutationRecoveryWorker.MutationWork(
                "901", "7", "SESSION", "99", "RECOVERY_REQUIRED"));
        AtomicInteger attempts = new AtomicInteger();
        var worker = new AuthorizationMutationRecoveryWorker(
                store, mutation -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("redis unavailable");
            }
        }, Clock.fixed(NOW, ZoneOffset.UTC), 1);

        assertThat(worker.runOnce()).isZero();
        assertThat(store.failureCodes).containsExactly("AUTH_PROPAGATION_PENDING");
        assertThat(worker.runOnce()).isEqualTo(1);
    }

    @Test
    void outboxProjectionUsesStableEventAndAggregateVersionCheckpoint() {
        InMemoryCheckpointStore checkpoints = new InMemoryCheckpointStore();
        AtomicInteger rebuilds = new AtomicInteger();
        var worker = new RuntimeSnapshotRebuildWorker(
                checkpoints, event -> rebuilds.incrementAndGet());
        var event = event("event-1", 4L);

        assertThat(worker.project(event)).isEqualTo(
                Rbac3RuntimeProjectionDeliveryHandler.ProjectionOutcome.APPLIED);
        assertThat(worker.project(event)).isEqualTo(
                Rbac3RuntimeProjectionDeliveryHandler.ProjectionOutcome.ALREADY_APPLIED);
        assertThat(rebuilds).hasValue(1);
    }

    @Test
    void assignmentWorkerPublishesOnlyCommittedLifecycleChanges() {
        List<AssignmentLifecycleWorker.LifecycleChange> published = new ArrayList<>();
        AssignmentLifecycleWorker worker = new AssignmentLifecycleWorker(
                (now, batchSize, publisher) -> {
                    publisher.publish(new AssignmentLifecycleWorker.LifecycleChange(
                            "7", "assignment-1", "user-1", "ACTIVATED", 5));
                    return 1;
                }, published::add, Clock.fixed(NOW, ZoneOffset.UTC), 10);

        assertThat(worker.runOnce()).isEqualTo(1);
        assertThat(published).singleElement()
                .extracting(AssignmentLifecycleWorker.LifecycleChange::changeType)
                .isEqualTo("ACTIVATED");
    }

    private Rbac3RuntimeProjectionDeliveryHandler.EventEnvelope event(
            String eventId,
            long version) {
        return new Rbac3RuntimeProjectionDeliveryHandler.EventEnvelope(
                eventId, "rbac3.role-activation.changed.v1", 1, NOW,
                "7", "SESSION", "99", version, "trace-1",
                Map.of("mutationId", "900"));
    }

    private static final class InMemoryRecoveryStore
            implements AuthorizationMutationRecoveryWorker.RecoveryStore {

        private final List<AuthorizationMutationRecoveryWorker.MutationWork> pending =
                new ArrayList<>(List.of(new AuthorizationMutationRecoveryWorker.MutationWork(
                        "900", "7", "SESSION", "99", "RECOVERY_REQUIRED")));
        private final List<String> completed = new ArrayList<>();
        private final List<String> failureCodes = new ArrayList<>();

        @Override
        public Optional<AuthorizationMutationRecoveryWorker.MutationWork> claimById(
                String tenantId, String mutationId) {
            if (completed.contains(mutationId)) {
                return Optional.of(new AuthorizationMutationRecoveryWorker.MutationWork(
                        mutationId, tenantId, "SESSION", "99", "COMPLETED"));
            }
            return pending.stream()
                    .filter(value -> value.tenantId().equals(tenantId)
                            && value.mutationId().equals(mutationId))
                    .findFirst();
        }

        @Override
        public List<AuthorizationMutationRecoveryWorker.MutationWork> claimRecoverable(
                int batchSize) {
            return pending.stream()
                    .filter(value -> !completed.contains(value.mutationId()))
                    .limit(batchSize)
                    .toList();
        }

        @Override
        public void completed(String mutationId, Instant now, String actorId) {
            if (!completed.contains(mutationId)) {
                completed.add(mutationId);
            }
        }

        @Override
        public void failed(String mutationId, String reasonCode, Instant now, String actorId) {
            failureCodes.add(reasonCode);
        }
    }

    private static final class InMemoryCheckpointStore
            implements RuntimeSnapshotRebuildWorker.ProjectionCheckpointStore {

        private final Map<String, Long> versions = new java.util.HashMap<>();

        @Override
        public RuntimeSnapshotRebuildWorker.Claim claim(
                String tenantId,
                String eventId,
                String aggregateType,
                String aggregateId,
                long aggregateVersion) {
            Long previous = versions.putIfAbsent(eventId, aggregateVersion);
            return previous == null
                    ? RuntimeSnapshotRebuildWorker.Claim.ACQUIRED
                    : RuntimeSnapshotRebuildWorker.Claim.ALREADY_APPLIED;
        }

        @Override
        public void complete(
                String tenantId,
                String eventId,
                String aggregateType,
                String aggregateId,
                long aggregateVersion) {
        }

        @Override
        public void release(
                String tenantId,
                String eventId,
                String aggregateType,
                String aggregateId,
                long aggregateVersion) {
            versions.remove(eventId, aggregateVersion);
        }
    }
}
