package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.gateway.core.provider.ProviderHealthState;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ActiveHealthTracker {

    private final int failureThreshold;

    private final int successThreshold;

    private final Map<String, State> states = new ConcurrentHashMap<>();

    public ActiveHealthTracker(
            int failureThreshold,
            int successThreshold) {
        if (failureThreshold < 1 || successThreshold < 1) {
            throw new IllegalArgumentException(
                    "active health thresholds must be positive"
            );
        }
        this.failureThreshold = failureThreshold;
        this.successThreshold = successThreshold;
    }

    public void record(String runtimeIdentity, boolean successful) {
        states.computeIfAbsent(
                required(runtimeIdentity),
                ignored -> new State()
        ).record(successful, failureThreshold, successThreshold);
    }

    public boolean eligible(String runtimeIdentity) {
        return snapshot(runtimeIdentity).state()
                != ProviderHealthState.UNHEALTHY;
    }

    public Snapshot snapshot(String runtimeIdentity) {
        State state = states.get(required(runtimeIdentity));
        return state == null ? Snapshot.unknown() : state.snapshot();
    }

    private String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "runtimeIdentity is required"
            );
        }
        return value;
    }

    private static final class State {

        private ProviderHealthState state = ProviderHealthState.UNKNOWN;

        private int consecutiveFailures;

        private int consecutiveSuccesses;

        private Instant lastProbeAt;

        private synchronized void record(
                boolean successful,
                int failureThreshold,
                int successThreshold) {
            lastProbeAt = Instant.now();
            if (successful) {
                consecutiveFailures = 0;
                consecutiveSuccesses++;
                if (consecutiveSuccesses >= successThreshold) {
                    state = ProviderHealthState.HEALTHY;
                }
            } else {
                consecutiveSuccesses = 0;
                consecutiveFailures++;
                if (consecutiveFailures >= failureThreshold) {
                    state = ProviderHealthState.UNHEALTHY;
                }
            }
        }

        private synchronized Snapshot snapshot() {
            return new Snapshot(
                    state,
                    consecutiveFailures,
                    consecutiveSuccesses,
                    lastProbeAt
            );
        }
    }

    public record Snapshot(
            ProviderHealthState state,
            int consecutiveFailures,
            int consecutiveSuccesses,
            Instant lastProbeAt
    ) {

        private static Snapshot unknown() {
            return new Snapshot(
                    ProviderHealthState.UNKNOWN,
                    0,
                    0,
                    null
            );
        }
    }
}
