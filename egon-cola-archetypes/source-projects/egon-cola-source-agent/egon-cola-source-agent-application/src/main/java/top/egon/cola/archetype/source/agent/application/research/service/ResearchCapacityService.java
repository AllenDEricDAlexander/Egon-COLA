package top.egon.cola.archetype.source.agent.application.research.service;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.archetype.source.agent.application.research.exception.DeepResearchApplicationException;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/** Fair process-local Bulkhead that bounds concurrent paid research runs. */
@Slf4j
public class ResearchCapacityService {

    private final Semaphore semaphore;

    public ResearchCapacityService(int maxConcurrentRuns) {
        if (maxConcurrentRuns < 1 || maxConcurrentRuns > 32) {
            throw new IllegalArgumentException("maxConcurrentRuns must be between 1 and 32");
        }
        this.semaphore = new Semaphore(maxConcurrentRuns, true);
    }

    public Lease acquire(String traceId) {
        if (!semaphore.tryAcquire()) {
            throw new DeepResearchApplicationException(
                    ResearchErrorCodeEnum.RESEARCH_CAPACITY_EXHAUSTED, traceId);
        }
        return new Lease();
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }

    public final class Lease implements AutoCloseable {
        private final AtomicBoolean released = new AtomicBoolean();

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                semaphore.release();
            }
        }
    }
}
