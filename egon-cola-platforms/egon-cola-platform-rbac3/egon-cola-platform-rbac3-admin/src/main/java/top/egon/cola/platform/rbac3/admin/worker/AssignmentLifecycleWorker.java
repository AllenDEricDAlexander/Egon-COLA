package top.egon.cola.platform.rbac3.admin.worker;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Applies due assignment state changes claimed by PostgreSQL SKIP LOCKED queries.
 */
public final class AssignmentLifecycleWorker {

    private final LifecycleStore store;
    private final ChangePublisher publisher;
    private final Clock clock;
    private final int batchSize;

    public AssignmentLifecycleWorker(
            LifecycleStore store,
            ChangePublisher publisher,
            Clock clock,
            int batchSize) {
        this.store = Objects.requireNonNull(store, "store");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalArgumentException("batchSize must be between 1 and 500");
        }
        this.batchSize = batchSize;
    }

    public int runOnce() {
        return store.processDue(clock.instant(), batchSize, publisher);
    }

    @FunctionalInterface
    public interface LifecycleStore {

        int processDue(Instant now, int batchSize, ChangePublisher publisher);
    }

    @FunctionalInterface
    public interface ChangePublisher {

        void publish(LifecycleChange change);
    }

    public record LifecycleChange(
            String tenantId,
            String assignmentId,
            String userId,
            String changeType,
            long authVersion) {
    }
}
