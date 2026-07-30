package top.egon.cola.component.gateway.engine.traffic;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GatewayCircuitBreakerRegistry {

    private final Map<String, CircuitBreaker> breakers =
            new ConcurrentHashMap<>();

    public CallPermission tryAcquire(
            String policyId,
            long stateEpoch,
            long policyVersion,
            String providerRuntimeIdentity,
            CircuitPolicy policy) {
        String key = String.join(
                ":",
                policyId,
                Long.toString(stateEpoch),
                Long.toString(policyVersion),
                providerRuntimeIdentity
        );
        CircuitBreaker breaker = breakers.computeIfAbsent(
                key,
                ignored -> CircuitBreaker.of(
                        key,
                        CircuitBreakerConfig.custom()
                                .failureRateThreshold(
                                        policy.failureRateThreshold()
                                )
                                .slidingWindowSize(policy.slidingWindowSize())
                                .minimumNumberOfCalls(
                                        policy.minimumNumberOfCalls()
                                )
                                .waitDurationInOpenState(
                                        policy.openDuration()
                                )
                                .permittedNumberOfCallsInHalfOpenState(
                                        policy.halfOpenPermits()
                                )
                                .build()
                )
        );
        if (!breaker.tryAcquirePermission()) {
            return CallPermission.rejected();
        }
        return new CallPermission(breaker);
    }

    public boolean available(
            String policyId,
            long stateEpoch,
            long policyVersion,
            String providerRuntimeIdentity) {
        CircuitBreaker breaker = breakers.get(String.join(
                ":",
                policyId,
                Long.toString(stateEpoch),
                Long.toString(policyVersion),
                providerRuntimeIdentity
        ));
        return breaker == null
                || breaker.getState() != CircuitBreaker.State.OPEN;
    }

    public record CircuitPolicy(
            float failureRateThreshold,
            int slidingWindowSize,
            int minimumNumberOfCalls,
            Duration openDuration,
            int halfOpenPermits
    ) {

        public CircuitPolicy {
            if (failureRateThreshold <= 0
                    || failureRateThreshold > 100
                    || slidingWindowSize < 1
                    || minimumNumberOfCalls < 1
                    || minimumNumberOfCalls > slidingWindowSize
                    || halfOpenPermits < 1
                    || openDuration == null
                    || openDuration.isZero()
                    || openDuration.isNegative()) {
                throw new IllegalArgumentException("invalid circuit policy");
            }
        }
    }

    public static final class CallPermission implements AutoCloseable {

        private final CircuitBreaker breaker;

        private final long startedNanos;

        private final boolean acquired;

        private final AtomicBoolean completed = new AtomicBoolean();

        private CallPermission(CircuitBreaker breaker) {
            this.breaker = breaker;
            startedNanos = System.nanoTime();
            acquired = true;
        }

        private CallPermission() {
            breaker = null;
            startedNanos = 0;
            acquired = false;
        }

        static CallPermission rejected() {
            return new CallPermission();
        }

        public boolean acquired() {
            return acquired;
        }

        public void complete(ProviderCallClassification classification) {
            if (!acquired || !completed.compareAndSet(false, true)) {
                return;
            }
            long duration = Math.max(0, System.nanoTime() - startedNanos);
            switch (classification) {
                case RETRYABLE_FAILURE -> breaker.onError(
                        duration,
                        TimeUnit.NANOSECONDS,
                        new IllegalStateException("retryable provider failure")
                );
                case SUCCESS, BUSINESS_FAILURE -> breaker.onSuccess(
                        duration,
                        TimeUnit.NANOSECONDS
                );
                case CANCELLED -> breaker.releasePermission();
            }
        }

        @Override
        public void close() {
            complete(ProviderCallClassification.CANCELLED);
        }
    }
}
