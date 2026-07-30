package top.egon.cola.component.gateway.engine.http.proxy;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.traffic.GatewayAttemptExecutor;
import top.egon.cola.component.gateway.engine.traffic.GatewayRetryPolicy;
import top.egon.cola.component.gateway.engine.transport.GatewayCommitGuard;
import top.egon.cola.component.gateway.engine.transport.GatewayRetryGate;
import top.egon.cola.component.gateway.engine.transport.GatewayTransportTimeouts;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Existing attempt orchestration constrained by transport commit facts.
 */
public final class GatewayHttpAttemptCoordinator {

    private final GatewayAttemptExecutor executor = new GatewayAttemptExecutor();

    private final GatewayRetryGate retryGate = new GatewayRetryGate();

    public <T> Mono<T> execute(
            EffectiveGatewayTransportPolicy transportPolicy,
            GatewayRetryPolicy retryPolicy,
            GatewayCommitGuard commitGuard,
            boolean idempotent,
            boolean replayable,
            Duration totalBudget,
            Supplier<Mono<T>> attempt,
            Predicate<Throwable> retryableTransportFailure,
            Predicate<Throwable> retryableLegacyStatus) {
        Objects.requireNonNull(transportPolicy, "transportPolicy");
        Objects.requireNonNull(retryPolicy, "retryPolicy");
        Objects.requireNonNull(commitGuard, "commitGuard");
        Objects.requireNonNull(attempt, "attempt");
        Objects.requireNonNull(
                retryableTransportFailure,
                "retryableTransportFailure"
        );
        Objects.requireNonNull(retryableLegacyStatus, "retryableLegacyStatus");
        boolean eligible = retryGate.canRetryTransportFailure(
                transportPolicy,
                commitGuard,
                retryPolicy.enabled(),
                idempotent,
                replayable,
                1,
                retryPolicy.maxAttempts()
        ) || retryGate.canRetryLegacyStatus(
                transportPolicy,
                commitGuard,
                retryPolicy.enabled(),
                idempotent,
                replayable,
                1,
                retryPolicy.maxAttempts()
        );
        AtomicInteger attempts = new AtomicInteger();
        if (!eligible) {
            return GatewayTransportTimeouts.total(
                    Mono.defer(() -> {
                        attempts.incrementAndGet();
                        return attempt.get();
                    }),
                    transportPolicy.totalTimeout()
            );
        }
        return executor.execute(
                retryPolicy,
                idempotent,
                replayable,
                totalBudget,
                () -> {
                    attempts.incrementAndGet();
                    return attempt.get();
                },
                failure -> retryableLegacyStatus.test(failure)
                        ? retryGate.canRetryLegacyStatus(
                                transportPolicy,
                                commitGuard,
                                retryPolicy.enabled(),
                                idempotent,
                                replayable,
                                attempts.get(),
                                retryPolicy.maxAttempts()
                        )
                        : retryableTransportFailure.test(failure)
                        && retryGate.canRetryTransportFailure(
                                transportPolicy,
                                commitGuard,
                                retryPolicy.enabled(),
                                idempotent,
                                replayable,
                                attempts.get(),
                                retryPolicy.maxAttempts()
                        )
        );
    }
}
