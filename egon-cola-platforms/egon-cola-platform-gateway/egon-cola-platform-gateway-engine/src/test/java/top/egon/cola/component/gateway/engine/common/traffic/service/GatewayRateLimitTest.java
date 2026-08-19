package top.egon.cola.component.gateway.engine.common.traffic.service;

import top.egon.cola.component.gateway.engine.rule.service.GatewayPolicyKeyCompiler;

import top.egon.cola.component.gateway.engine.common.traffic.service.GatewayTrafficContext;
import top.egon.cola.component.gateway.engine.common.traffic.service.LocalTokenBucketPolicy;
import top.egon.cola.component.gateway.engine.common.traffic.domain.RateLimitDecision;
import top.egon.cola.component.gateway.engine.common.traffic.domain.RateLimitFailureMode;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayRateLimitTest {

    @Test
    void keyCompilerOnlyUsesApprovedFieldsAndReturnsHash() {
        GatewayPolicyKeyCompiler.CompiledTrafficKey key =
                new GatewayPolicyKeyCompiler().compile(
                        "op:${operationId}:caller:${callerId}"
                );
        GatewayTrafficContext context = new GatewayTrafficContext(
                "orders",
                "route-1",
                "shop",
                "sensitive-caller",
                "127.0.0.1",
                null,
                null,
                Map.of(),
                Map.of(),
                Map.of()
        );

        String hash = key.hash(context);

        assertEquals(64, hash.length());
        assertNotEquals("sensitive-caller", hash);
    }

    @Test
    void localBucketRefillsAndKeepsStateBounded() {
        AtomicLong nanos = new AtomicLong();
        LocalTokenBucketRateLimiter limiter =
                new LocalTokenBucketRateLimiter(
                        nanos::get,
                        () -> 1_000L
                );
        LocalTokenBucketPolicy policy = policy(2);

        assertTrue(limiter.acquire(policy, hash("a"), 1).allowed());
        assertTrue(limiter.acquire(policy, hash("a"), 1).allowed());
        assertFalse(limiter.acquire(policy, hash("a"), 1).allowed());
        nanos.addAndGet(Duration.ofSeconds(1).toNanos());
        assertTrue(limiter.acquire(policy, hash("a"), 1).allowed());
        limiter.acquire(policy, hash("b"), 1);
        limiter.acquire(policy, hash("c"), 1);
        assertEquals(2, limiter.stateCount());
    }

    @Test
    void distributedFailureNeverAllowsAllAndCanUseLocalFallback() {
        LocalTokenBucketRateLimiter local =
                new LocalTokenBucketRateLimiter(
                        System::nanoTime,
                        System::currentTimeMillis
                );
        DistributedTokenBucketRateLimiter limiter =
                new DistributedTokenBucketRateLimiter(
                        (script, keys, arguments) -> {
                            throw new IllegalStateException("redis down");
                        },
                        local
                );

        RateLimitDecision denied = limiter.acquire(
                "prod",
                "default",
                policy(10),
                hash("caller"),
                1,
                RateLimitFailureMode.DENY
        );
        assertFalse(denied.allowed());
        assertTrue(denied.backendUnavailable());
        RateLimitDecision fallback = limiter.acquire(
                "prod",
                "default",
                policy(10),
                hash("caller"),
                1,
                RateLimitFailureMode.LOCAL_FALLBACK
        );
        assertTrue(fallback.allowed());
        assertTrue(fallback.localFallback());
        assertTrue(fallback.backendUnavailable());
    }

    @Test
    void redisUsesOneLuaCallAndHashedKeyOnly() {
        AtomicReference<List<String>> keys = new AtomicReference<>();
        DistributedTokenBucketRateLimiter limiter =
                new DistributedTokenBucketRateLimiter(
                        (script, actualKeys, arguments) -> {
                            assertTrue(script.contains("redis.call('TIME')"));
                            assertEquals("2", arguments.get(1));
                            keys.set(actualKeys);
                            return List.of(1L, 9L, 0L, 1_000L);
                        },
                        new LocalTokenBucketRateLimiter(
                                System::nanoTime,
                                System::currentTimeMillis
                        )
                );

        RateLimitDecision decision = limiter.acquire(
                "prod",
                "default",
                policy(10),
                hash("raw-sensitive-caller"),
                1,
                RateLimitFailureMode.DENY
        );

        assertTrue(decision.allowed());
        assertFalse(keys.get().getFirst().contains("raw-sensitive-caller"));
    }

    private LocalTokenBucketPolicy policy(int maxKeys) {
        return new LocalTokenBucketPolicy(
                "quota",
                1,
                2,
                1,
                Duration.ofSeconds(1),
                2,
                maxKeys,
                Duration.ofMinutes(1)
        );
    }

    private String hash(String value) {
        return GatewayPolicyKeyCompilerTestSupport.hash(value);
    }
}
