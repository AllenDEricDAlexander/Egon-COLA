package top.egon.cola.archetype.source.agent.application.knowledge.service;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fair process-local Bulkhead that bounds concurrent knowledge questions.
 *
 * <p>Separate from the research capacity: a question and a research run cost different things and
 * saturating one must not refuse the other. Acquisition never queues — a caller that arrives when
 * every permit is taken is refused at once, so a saturated instance answers {@code 429} instead of
 * holding sockets open.
 */
@Slf4j
public class KnowledgeQaCapacityService {

    private final Semaphore permits;

    public KnowledgeQaCapacityService(int qaMaxConcurrent) {
        if (qaMaxConcurrent < 1 || qaMaxConcurrent > 32) {
            throw new IllegalArgumentException("qaMaxConcurrent must be between 1 and 32");
        }
        this.permits = new Semaphore(qaMaxConcurrent, true);
    }

    /**
     * @return the permit to hold for the lifetime of one answer, or an empty result when the
     *         instance is saturated
     */
    public Optional<Lease> tryAcquire() {
        return permits.tryAcquire() ? Optional.of(new Lease()) : Optional.empty();
    }

    public int availablePermits() {
        return permits.availablePermits();
    }

    /**
     * The permit itself. Closing twice returns the permit once: an extra release would raise the
     * configured concurrency silently, which is the one failure a Bulkhead must not have.
     */
    public final class Lease implements AutoCloseable {

        private final AtomicBoolean released = new AtomicBoolean();

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                permits.release();
            }
        }
    }
}
