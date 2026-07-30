package top.egon.cola.component.gateway.engine.traffic;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class GatewayAttemptExecutor {

    public <T> Mono<T> execute(
            GatewayRetryPolicy policy,
            boolean idempotent,
            boolean replayableBody,
            Duration totalBudget,
            Supplier<Mono<T>> attempt,
            Predicate<Throwable> retryable) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(totalBudget, "totalBudget");
        Objects.requireNonNull(attempt, "attempt");
        Objects.requireNonNull(retryable, "retryable");
        if (totalBudget.isZero() || totalBudget.isNegative()) {
            return Mono.error(new TimeoutException("GATEWAY_TIMEOUT"));
        }
        if (policy.enabled() && (!idempotent || !replayableBody)) {
            return Mono.error(new IllegalArgumentException(
                    "retry requires an idempotent operation and replayable body"
            ));
        }
        long deadline = System.nanoTime() + totalBudget.toNanos();
        return executeAttempt(
                policy,
                attempt,
                retryable,
                deadline,
                1
        ).timeout(totalBudget);
    }

    private <T> Mono<T> executeAttempt(
            GatewayRetryPolicy policy,
            Supplier<Mono<T>> attempt,
            Predicate<Throwable> retryable,
            long deadline,
            int attemptNumber) {
        return Mono.defer(attempt).onErrorResume(failure -> {
            if (!policy.enabled()
                    || attemptNumber >= policy.maxAttempts()
                    || !retryable.test(failure)) {
                return Mono.error(failure);
            }
            Duration backoff = policy.backoff(attemptNumber);
            long remaining = deadline - System.nanoTime();
            long required = backoff.toNanos()
                    + policy.minimumAttemptBudget().toNanos();
            if (remaining < required) {
                return Mono.error(new TimeoutException("GATEWAY_TIMEOUT"));
            }
            return Mono.delay(backoff).then(executeAttempt(
                    policy,
                    attempt,
                    retryable,
                    deadline,
                    attemptNumber + 1
            ));
        });
    }
}
