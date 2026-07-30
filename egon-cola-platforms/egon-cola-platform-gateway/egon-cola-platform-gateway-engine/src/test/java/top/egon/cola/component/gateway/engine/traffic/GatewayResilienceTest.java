package top.egon.cola.component.gateway.engine.traffic;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayResilienceTest {

    @Test
    void bulkheadRejectsWithoutWaitingAndReleasesExactlyOnce() {
        GatewayBulkheadRegistry registry = new GatewayBulkheadRegistry();
        GatewayBulkheadRegistry.Permit first = registry.tryAcquire(
                "bulkhead",
                1,
                1,
                "orders",
                1
        );
        GatewayBulkheadRegistry.Permit rejected = registry.tryAcquire(
                "bulkhead",
                1,
                1,
                "orders",
                1
        );

        assertTrue(first.acquired());
        assertFalse(rejected.acquired());
        first.close();
        first.close();
        assertTrue(registry.tryAcquire(
                "bulkhead",
                1,
                1,
                "orders",
                1
        ).acquired());
    }

    @Test
    void circuitCountsRetryableFailuresButNotBusinessFailures() {
        GatewayCircuitBreakerRegistry registry =
                new GatewayCircuitBreakerRegistry();
        GatewayCircuitBreakerRegistry.CircuitPolicy policy =
                new GatewayCircuitBreakerRegistry.CircuitPolicy(
                        50,
                        2,
                        2,
                        Duration.ofSeconds(30),
                        1
                );
        GatewayCircuitBreakerRegistry.CallPermission business =
                registry.tryAcquire("circuit", 1, 1, "a:lease", policy);
        business.complete(ProviderCallClassification.BUSINESS_FAILURE);
        GatewayCircuitBreakerRegistry.CallPermission retryable =
                registry.tryAcquire("circuit", 1, 1, "a:lease", policy);
        retryable.complete(ProviderCallClassification.RETRYABLE_FAILURE);

        assertFalse(registry.available(
                "circuit",
                1,
                1,
                "a:lease"
        ));
    }

    @Test
    void retryRequiresExplicitSafeOperationAndSharesAttemptBudget() {
        GatewayAttemptExecutor executor = new GatewayAttemptExecutor();
        GatewayRetryPolicy policy = new GatewayRetryPolicy(
                true,
                3,
                Duration.ZERO,
                Duration.ZERO,
                1,
                Duration.ofMillis(1),
                java.util.Set.of(503),
                java.util.Set.of("UNAVAILABLE")
        );
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.execute(
                policy,
                true,
                true,
                Duration.ofSeconds(1),
                () -> attempts.incrementAndGet() < 3
                        ? Mono.error(new IllegalStateException("retry"))
                        : Mono.just("ok"),
                ignored -> true
        ).block();

        assertEquals("ok", result);
        assertEquals(3, attempts.get());
        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        policy,
                        false,
                        true,
                        Duration.ofSeconds(1),
                        () -> Mono.just("unsafe"),
                        ignored -> true
                ).block()
        );
    }
}
