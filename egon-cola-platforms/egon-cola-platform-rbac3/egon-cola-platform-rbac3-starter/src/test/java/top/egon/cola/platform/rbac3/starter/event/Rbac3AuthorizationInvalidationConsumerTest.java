package top.egon.cola.platform.rbac3.starter.event;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.starter.cache.AuthorizationSnapshotCache;
import top.egon.cola.platform.rbac3.starter.cache.AuthorizationSnapshotCacheTest;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3AuthorizationInvalidationConsumerTest {

    @Test
    void contextEventDeletesOnlyTheExactSidAndIgnoresOlderDuplicates() {
        var clock = new AuthorizationSnapshotCacheTest.MutableClock(
                Instant.parse("2026-08-02T05:00:00Z"));
        var cache = new AuthorizationSnapshotCache(
                new AuthorizationSnapshotCacheTest.InMemoryStore(clock),
                clock, Duration.ofSeconds(5));
        var first = new AuthorizationSnapshotCache.Key("finance", "tenant-a", "sid-1");
        var second = new AuthorizationSnapshotCache.Key("finance", "tenant-a", "sid-2");
        cache.put(first, AuthorizationSnapshotCacheTest.snapshot("sid-1"),
                Duration.ofMinutes(5));
        cache.put(second, AuthorizationSnapshotCacheTest.snapshot("sid-2"),
                Duration.ofMinutes(5));
        Rbac3AuthorizationInvalidationConsumer consumer =
                new Rbac3AuthorizationInvalidationConsumer("finance", cache);

        consumer.accept(new Rbac3AuthorizationInvalidationConsumer.Event(
                "event-2", "RBAC_AUTHORIZATION_CONTEXT_CHANGED", "finance",
                "tenant-a", "alice-sub", "sid-1", 4));
        consumer.accept(new Rbac3AuthorizationInvalidationConsumer.Event(
                "event-1", "RBAC_AUTHORIZATION_CONTEXT_CHANGED", "finance",
                "tenant-a", "alice-sub", "sid-1", 3));

        assertThat(cache.get(first)).isEmpty();
        assertThat(cache.get(second)).isPresent();
    }
}
