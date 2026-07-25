package top.egon.cola.component.gateway.engine.traffic;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GatewayBulkheadRegistry {

    private final Map<String, Bulkhead> bulkheads = new ConcurrentHashMap<>();

    public Permit tryAcquire(
            String policyId,
            long stateEpoch,
            long policyVersion,
            String dimension,
            int maxConcurrent) {
        if (maxConcurrent < 1) {
            throw new IllegalArgumentException(
                    "maxConcurrent must be positive"
            );
        }
        String key = String.join(
                ":",
                policyId,
                Long.toString(stateEpoch),
                Long.toString(policyVersion),
                dimension
        );
        Bulkhead bulkhead = bulkheads.computeIfAbsent(
                key,
                ignored -> Bulkhead.of(
                        key,
                        BulkheadConfig.custom()
                                .maxConcurrentCalls(maxConcurrent)
                                .maxWaitDuration(Duration.ZERO)
                                .build()
                )
        );
        if (!bulkhead.tryAcquirePermission()) {
            return Permit.rejected();
        }
        return new Permit(true, bulkhead::onComplete);
    }

    public static final class Permit implements AutoCloseable {

        private final boolean acquired;

        private final Runnable release;

        private final AtomicBoolean closed = new AtomicBoolean();

        private Permit(boolean acquired, Runnable release) {
            this.acquired = acquired;
            this.release = release;
        }

        static Permit rejected() {
            return new Permit(false, () -> {
            });
        }

        public boolean acquired() {
            return acquired;
        }

        @Override
        public void close() {
            if (acquired && closed.compareAndSet(false, true)) {
                release.run();
            }
        }
    }
}
