package top.egon.cola.component.gateway.engine.discovery;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class PassiveHealthTracker {

    private final PassiveHealthPolicy policy;

    private final Clock clock;

    private final Map<String, InstanceState> states = new ConcurrentHashMap<>();

    public PassiveHealthTracker(PassiveHealthPolicy policy, Clock clock) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public boolean eligible(String runtimeIdentity) {
        InstanceState state = states.get(required(runtimeIdentity));
        return state == null || state.eligible(clock.instant());
    }

    public void record(
            String runtimeIdentity,
            ProviderCallOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome");
        InstanceState state = states.computeIfAbsent(
                required(runtimeIdentity),
                ignored -> new InstanceState()
        );
        state.record(outcome, clock.instant(), policy);
    }

    public PassiveHealthSnapshot snapshot(String runtimeIdentity) {
        InstanceState state = states.get(required(runtimeIdentity));
        return state == null
                ? PassiveHealthSnapshot.healthy()
                : state.snapshot(clock.instant());
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("runtimeIdentity is required");
        }
        return value;
    }

    private static final class InstanceState {

        private final Deque<Sample> samples = new ArrayDeque<>();

        private int consecutiveFailures;

        private int ejectionLevel;

        private Instant ejectedUntil;

        private synchronized boolean eligible(Instant now) {
            return ejectedUntil == null || !now.isBefore(ejectedUntil);
        }

        private synchronized void record(
                ProviderCallOutcome outcome,
                Instant now,
                PassiveHealthPolicy policy) {
            evictOldSamples(now.minus(policy.window()));
            if (outcome == ProviderCallOutcome.BUSINESS_REJECTION
                    || outcome == ProviderCallOutcome.CANCELLED) {
                return;
            }
            if (outcome == ProviderCallOutcome.SUCCESS) {
                consecutiveFailures = 0;
                ejectionLevel = 0;
                ejectedUntil = null;
                samples.addLast(new Sample(now, false));
                return;
            }
            consecutiveFailures++;
            samples.addLast(new Sample(now, true));
            long failures = samples.stream().filter(Sample::failed).count();
            boolean consecutiveExceeded = consecutiveFailures
                    >= policy.consecutiveFailureThreshold();
            boolean rateExceeded = samples.size() >= policy.minimumSamples()
                    && (double) failures / samples.size()
                    >= policy.failureRateThreshold();
            if (consecutiveExceeded || rateExceeded) {
                eject(now, policy);
            }
        }

        private void evictOldSamples(Instant earliest) {
            while (!samples.isEmpty()
                    && samples.getFirst().occurredAt().isBefore(earliest)) {
                samples.removeFirst();
            }
        }

        private void eject(Instant now, PassiveHealthPolicy policy) {
            long multiplier = 1L << Math.min(ejectionLevel, 20);
            Duration requested;
            try {
                requested = policy.baseEjectionDuration()
                        .multipliedBy(multiplier);
            } catch (ArithmeticException overflow) {
                requested = policy.maximumEjectionDuration();
            }
            Duration actual = requested.compareTo(
                    policy.maximumEjectionDuration()
            ) > 0 ? policy.maximumEjectionDuration() : requested;
            ejectedUntil = now.plus(actual);
            ejectionLevel++;
            consecutiveFailures = 0;
        }

        private synchronized PassiveHealthSnapshot snapshot(Instant now) {
            return new PassiveHealthSnapshot(
                    eligible(now),
                    consecutiveFailures,
                    samples.size(),
                    ejectedUntil
            );
        }
    }

    private record Sample(Instant occurredAt, boolean failed) {
    }

    public record PassiveHealthSnapshot(
            boolean eligible,
            int consecutiveFailures,
            int sampleCount,
            Instant ejectedUntil
    ) {

        private static PassiveHealthSnapshot healthy() {
            return new PassiveHealthSnapshot(true, 0, 0, null);
        }
    }
}
