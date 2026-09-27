package top.egon.cola.component.tianshu.admin.service.publish;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.tianshu.admin.model.vo.DdcConfigResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PublishResourceLockRegistryTest {

    @BeforeEach
    void bindTenant() {
        MDC.put("tenantId", "101");
    }

    @AfterEach
    void clearTenant() {
        MDC.clear();
    }

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

    @Test
    void sameResourceCanBeLockedIndependentlyByDifferentTenants() {
        PublishResourceLockRegistry registry = new PublishResourceLockRegistry();
        DdcConfigResourceKey key = key("switch");

        assertThat(registry.tryAcquire(key, "change-101")).isTrue();
        MDC.put("tenantId", "202");
        assertThat(registry.tryAcquire(key, "change-202")).isTrue();
        assertThat(registry.owner(key)).contains("change-202");

        registry.release(key, "change-202");
        MDC.put("tenantId", "101");
        assertThat(registry.owner(key)).contains("change-101");
    }

    private void acquire(PublishResourceLockRegistry registry,
                         DdcConfigResourceKey key,
                         String changeId,
                         CountDownLatch ready,
                         CountDownLatch start,
                         List<Boolean> results) {
        ready.countDown();
        try {
            MDC.put("tenantId", "101");
            start.await();
            results.add(registry.tryAcquire(key, changeId));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            MDC.clear();
        }
    }

    private DdcConfigResourceKey key(String resourceName) {
        return new DdcConfigResourceKey("demo", "dev", "default", resourceName);
    }
}
