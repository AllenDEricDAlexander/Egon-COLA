package top.egon.cola.platform.rbac3.admin.worker;

import top.egon.cola.platform.rbac3.admin.integration.outbox.Rbac3RuntimeProjectionDeliveryHandler;

import java.util.Objects;

/**
 * Idempotently rebuilds runtime projections by stable event and aggregate version.
 */
public final class RuntimeSnapshotRebuildWorker
        implements Rbac3RuntimeProjectionDeliveryHandler.ProjectionSink {

    private final ProjectionCheckpointStore checkpoints;
    private final RebuildPort rebuildPort;

    public RuntimeSnapshotRebuildWorker(
            ProjectionCheckpointStore checkpoints,
            RebuildPort rebuildPort) {
        this.checkpoints = Objects.requireNonNull(checkpoints, "checkpoints");
        this.rebuildPort = Objects.requireNonNull(rebuildPort, "rebuildPort");
    }

    @Override
    public Rbac3RuntimeProjectionDeliveryHandler.ProjectionOutcome project(
            Rbac3RuntimeProjectionDeliveryHandler.EventEnvelope event) {
        Claim claim = checkpoints.claim(
                event.tenantId(), event.eventId(), event.aggregateType(),
                event.aggregateId(), event.aggregateVersion());
        if (claim == Claim.ALREADY_APPLIED) {
            return Rbac3RuntimeProjectionDeliveryHandler
                    .ProjectionOutcome.ALREADY_APPLIED;
        }
        if (claim == Claim.STALE) {
            return Rbac3RuntimeProjectionDeliveryHandler
                    .ProjectionOutcome.PERMANENT_FAILURE;
        }
        if (claim == Claim.BUSY) {
            return Rbac3RuntimeProjectionDeliveryHandler
                    .ProjectionOutcome.RETRYABLE_FAILURE;
        }
        try {
            rebuildPort.rebuild(event);
            checkpoints.complete(
                    event.tenantId(), event.eventId(), event.aggregateType(),
                    event.aggregateId(), event.aggregateVersion());
            return Rbac3RuntimeProjectionDeliveryHandler.ProjectionOutcome.APPLIED;
        } catch (RuntimeException unavailable) {
            checkpoints.release(
                    event.tenantId(), event.eventId(), event.aggregateType(),
                    event.aggregateId(), event.aggregateVersion());
            return Rbac3RuntimeProjectionDeliveryHandler
                    .ProjectionOutcome.RETRYABLE_FAILURE;
        }
    }

    public enum Claim {
        ACQUIRED,
        ALREADY_APPLIED,
        STALE,
        BUSY
    }

    public interface ProjectionCheckpointStore {

        Claim claim(
                String tenantId,
                String eventId,
                String aggregateType,
                String aggregateId,
                long aggregateVersion);

        void complete(
                String tenantId,
                String eventId,
                String aggregateType,
                String aggregateId,
                long aggregateVersion);

        void release(
                String tenantId,
                String eventId,
                String aggregateType,
                String aggregateId,
                long aggregateVersion);
    }

    @FunctionalInterface
    public interface RebuildPort {

        void rebuild(Rbac3RuntimeProjectionDeliveryHandler.EventEnvelope event);
    }
}
