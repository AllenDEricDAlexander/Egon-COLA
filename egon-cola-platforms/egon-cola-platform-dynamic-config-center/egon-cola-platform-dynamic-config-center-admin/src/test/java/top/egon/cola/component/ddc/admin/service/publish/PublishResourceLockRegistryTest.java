package top.egon.cola.component.ddc.admin.service.publish;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PublishResourceLockRegistryTest {

    @Test
    void onlyOneChangeCanOwnTheSameResource() throws Exception {
        PublishResourceLockRegistry registry = new PublishResourceLockRegistry();
        DdcConfigResourceKey key = key("switch");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Boolean> results = java.util.Collections.synchronizedList(new ArrayList<>());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> acquire(registry, key, "change-1", ready, start, results));
            executor.submit(() -> acquire(registry, key, "change-2", ready, start, results));
            assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
            start.countDown();
        }

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(registry.owner(key)).isPresent();
    }

    @Test
    void differentResourcesDoNotBlockAndOnlyOwnerCanRelease() {
        PublishResourceLockRegistry registry = new PublishResourceLockRegistry();
        DdcConfigResourceKey first = key("switch");
        DdcConfigResourceKey second = key("limit");

        assertThat(registry.tryAcquire(first, "change-1")).isTrue();
        assertThat(registry.tryAcquire(second, "change-2")).isTrue();

        registry.release(first, "change-2");
        assertThat(registry.owner(first)).contains("change-1");

        registry.release(first, "change-1");
        assertThat(registry.owner(first)).isEmpty();
    }

    private void acquire(PublishResourceLockRegistry registry,
                         DdcConfigResourceKey key,
                         String changeId,
                         CountDownLatch ready,
                         CountDownLatch start,
                         List<Boolean> results) {
        ready.countDown();
        try {
            start.await();
            results.add(registry.tryAcquire(key, changeId));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private DdcConfigResourceKey key(String configKey) {
        return new DdcConfigResourceKey("demo", "dev", "default", configKey);
    }
}
