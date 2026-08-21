package top.egon.cola.component.rpc.consumer.generic;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcGenericTargetCacheTest {

    private static final String SERVICE = "sample.Service";

    private static final String METHOD = SERVICE + "/Echo";

    @Test
    void sharesOneStrategyForConcurrentNormalizedTarget() throws Exception {
        AtomicInteger creations = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        RpcGenericTargetCache cache = new RpcGenericTargetCache(
                ignored -> {
                    creations.incrementAndGet();
                    return strategy(closes);
                },
                8,
                Duration.ofSeconds(1)
        );
        RpcGenericInvocation invocation = invocation("one");
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<RpcGenericTargetCache.Entry>> futures =
                    java.util.stream.IntStream.range(0, 8)
                            .mapToObj(ignored -> executor.submit(() -> {
                                ready.countDown();
                                start.await();
                                return cache.resolve(invocation);
                            }))
                            .toList();
            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS))
                    .isTrue();
            start.countDown();
            for (Future<RpcGenericTargetCache.Entry> future : futures) {
                future.get().release();
            }
            assertThat(creations).hasValue(1);
            assertThat(cache.size()).isOne();
        } finally {
            executor.shutdownNow();
            cache.close();
        }
        assertThat(closes).hasValue(1);
    }

    @Test
    void evictsLeastRecentlyUsedIdleEntryAtBound() {
        AtomicInteger closes = new AtomicInteger();
        RpcGenericTargetCache cache = new RpcGenericTargetCache(
                ignored -> strategy(closes),
                2,
                Duration.ofSeconds(1)
        );
        RpcGenericTargetCache.Entry first = cache.resolve(invocation("one"));
        first.release();
        RpcGenericTargetCache.Entry second = cache.resolve(invocation("two"));
        second.release();
        cache.resolve(invocation("three")).release();

        assertThat(cache.size()).isEqualTo(2);
        assertThat(closes).hasValue(1);
        cache.close();
        assertThat(closes).hasValue(3);
    }

    @Test
    void evictsIdleEntriesAndClosesEachStrategyOnce() {
        AtomicLong now = new AtomicLong(1000);
        AtomicInteger closes = new AtomicInteger();
        RpcGenericTargetCache cache = new RpcGenericTargetCache(
                ignored -> strategy(closes),
                4,
                Duration.ofSeconds(1),
                now::get
        );
        cache.resolve(invocation("idle")).release();
        now.set(2100);
        cache.evictIdle();
        cache.evictIdle();
        cache.close();

        assertThat(closes).hasValue(1);
    }

    @Test
    void doesNotCacheFailedStrategyCreation() {
        AtomicInteger attempts = new AtomicInteger();
        RpcGenericTargetCache cache = new RpcGenericTargetCache(
                ignored -> {
                    if (attempts.getAndIncrement() == 0) {
                        throw new IllegalStateException("creation failed");
                    }
                    return strategy(new AtomicInteger());
                },
                4,
                Duration.ofSeconds(1)
        );
        assertThatThrownBy(() -> cache.resolve(invocation("failed")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(cache.resolve(invocation("failed"))).isNotNull().satisfies(
                RpcGenericTargetCache.Entry::release
        );
        assertThat(attempts).hasValue(2);
        cache.close();
    }

    private RpcGenericInvocation invocation(String suffix) {
        String service = SERVICE + suffix;
        return RpcGenericInvocation.gateway(
                service,
                "test",
                "1.0.0",
                service + "/Echo",
                new byte[]{1, 2, 3},
                1000,
                0,
                LoadBalance.ROUND_ROBIN,
                FailStrategy.FAIL_CLOSED,
                null
        );
    }

    private RpcReferenceStrategy strategy(AtomicInteger closes) {
        return new RpcReferenceStrategy() {
            @Override
            public RpcReferenceMode mode() {
                return RpcReferenceMode.GATEWAY;
            }

            @Override
            public String queryIdentity() {
                return SERVICE;
            }

            @Override
            public long revision() {
                return 1;
            }

            @Override
            public List<? extends RpcEndpoint> candidates() {
                return List.of();
            }

            @Override
            public void close() {
                closes.incrementAndGet();
            }
        };
    }
}
