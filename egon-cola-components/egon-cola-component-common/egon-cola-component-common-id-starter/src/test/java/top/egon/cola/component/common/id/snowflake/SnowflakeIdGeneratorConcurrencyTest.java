package top.egon.cola.component.common.id.snowflake;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(value = 60, unit = TimeUnit.SECONDS)
class SnowflakeIdGeneratorConcurrencyTest {

    private static final Duration START_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration FUTURE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration TERMINATION_TIMEOUT = Duration.ofSeconds(5);

    @Test
    void platformThreadsGenerateOnlyPositiveUniqueIds() throws Exception {
        int threadCount = 32;
        int idsPerThread = 8_192;

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            assertConcurrentGeneration(executor, threadCount, idsPerThread);
        }
    }

    @Test
    void manyVirtualThreadsGenerateOnlyPositiveUniqueIds() throws Exception {
        int virtualThreadCount = 20_000;
        int idsPerThread = 16;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            assertConcurrentGeneration(executor, virtualThreadCount, idsPerThread);
        }
    }

    private void assertConcurrentGeneration(ExecutorService executor, int taskCount,
                                            int idsPerTask) throws Exception {
        int expectedCount = Math.multiplyExact(taskCount, idsPerTask);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(37);
        Set<Long> ids = ConcurrentHashMap.newKeySet(expectedCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>(taskCount);

        try {
            for (int task = 0; task < taskCount; task++) {
                futures.add(executor.submit(() -> {
                    if (!start.await(START_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                        throw new AssertionError("concurrent generation did not start before timeout");
                    }
                    for (int i = 0; i < idsPerTask; i++) {
                        long id = generator.nextLongId();
                        if (id <= 0L) {
                            throw new AssertionError("generated ID must be positive: " + id);
                        }
                        if (!ids.add(id)) {
                            throw new AssertionError("duplicate ID generated: " + id);
                        }
                    }
                    return null;
                }));
            }

            start.countDown();
            for (Future<?> future : futures) {
                future.get(FUTURE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(
                    TERMINATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS),
                    "executor did not terminate before timeout");
        }

        assertEquals(expectedCount, ids.size());
        assertTrue(ids.stream().allMatch(id -> id > 0L));
    }
}
