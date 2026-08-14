package top.egon.cola.platform.rbac3.starter.cache;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.client.Rbac3AuthorizationClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SingleFlightSnapshotLoaderTest {

    private static final Instant NOW = Instant.parse("2026-08-02T05:00:00Z");

    @Test
    void concurrentMissesPerformOneRbac3Fetch() throws Exception {
        var clock = new AuthorizationSnapshotCacheTest.MutableClock(NOW);
        var cache = new AuthorizationSnapshotCache(
                new AuthorizationSnapshotCacheTest.InMemoryStore(clock),
                clock, Duration.ofSeconds(5));
        AtomicInteger fetches = new AtomicInteger();
        CountDownLatch release = new CountDownLatch(1);
        Rbac3AuthorizationClient client = (systemCode, principal) -> {
            fetches.incrementAndGet();
            release.await();
            return AuthorizationSnapshotCacheTest.snapshot(principal.subject());
        };
        SingleFlightSnapshotLoader loader = new SingleFlightSnapshotLoader(
                cache, client, "finance", Duration.ofMinutes(5), clock);

        try (var executor = Executors.newFixedThreadPool(20)) {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();
            for (int index = 0; index < 20; index++) {
                futures.add(executor.submit(() -> loader.load(principal())));
            }
            while (fetches.get() == 0) {
                Thread.onSpinWait();
            }
            release.countDown();
            for (var future : futures) {
                future.get();
            }
        }

        assertThat(fetches).hasValue(1);
    }

    @Test
    void expiredCacheAndUnavailableRbac3FailsClosedWithoutExtendingTtl() {
        var clock = new AuthorizationSnapshotCacheTest.MutableClock(NOW);
        var store = new AuthorizationSnapshotCacheTest.InMemoryStore(clock);
        var cache = new AuthorizationSnapshotCache(store, clock, Duration.ofSeconds(5));
        var key = new AuthorizationSnapshotCache.Key("finance", "tenant-a", "alice-sub");
        cache.put(key, AuthorizationSnapshotCacheTest.snapshot("alice-sub"),
                Duration.ofSeconds(1));
        clock.advance(Duration.ofSeconds(2));
        SingleFlightSnapshotLoader loader = new SingleFlightSnapshotLoader(
                cache,
                (systemCode, principal) -> {
                    throw new Rbac3AuthorizationClient.AuthorizationUnavailableException(
                            "RBAC3_UNAVAILABLE");
                },
                "finance", Duration.ofMinutes(5), clock);

        assertThatThrownBy(() -> loader.load(principal()))
                .isInstanceOf(Rbac3AuthorizationClient.AuthorizationUnavailableException.class)
                .hasMessageContaining("RBAC3_UNAVAILABLE");
        assertThat(cache.get(key)).isEmpty();
    }

    @Test
    void differentSubjectsUseDifferentFlightsAndRemainBound() throws Exception {
        var clock = new AuthorizationSnapshotCacheTest.MutableClock(NOW);
        var cache = new AuthorizationSnapshotCache(
                new AuthorizationSnapshotCacheTest.InMemoryStore(clock),
                clock, Duration.ofSeconds(5));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Rbac3AuthorizationClient client = (systemCode, principal) -> {
            entered.countDown();
            release.await();
            return AuthorizationSnapshotCacheTest.snapshot(principal.subject());
        };
        SingleFlightSnapshotLoader loader = new SingleFlightSnapshotLoader(
                cache, client, "finance", Duration.ofMinutes(5), clock);
        IdentityPrincipal anotherSubject = new IdentityPrincipal(
                "bob-sub", "tenant-a", "token-2", Set.of("finance"),
                NOW.minusSeconds(30), NOW.plusSeconds(300),
                AuthenticationContext.password());

        try (var executor = Executors.newFixedThreadPool(2)) {
            var owner = executor.submit(() -> loader.load(principal()));
            entered.await();
            var waiter = executor.submit(() -> loader.load(anotherSubject));
            release.countDown();

            assertThat(owner.get().identitySub()).isEqualTo("alice-sub");
            assertThat(waiter.get().identitySub()).isEqualTo("bob-sub");
        }
    }

    @Test
    void cacheBackendFailureIsClassifiedAsAuthorizationUnavailable() {
        AuthorizationSnapshotCache.SnapshotStore failingStore =
                new AuthorizationSnapshotCache.SnapshotStore() {
                    @Override
                    public Optional<SystemAuthorizationSnapshot> get(
                            AuthorizationSnapshotCache.Key key) {
                        throw new IllegalStateException("redis unavailable");
                    }

                    @Override
                    public void put(AuthorizationSnapshotCache.Key key,
                            SystemAuthorizationSnapshot snapshot,
                            Duration ttl) {
                    }

                    @Override
                    public void invalidate(AuthorizationSnapshotCache.Key key) {
                    }

                    @Override
                    public void invalidateUser(
                            String systemCode, String tenantId, String identitySub) {
                    }

                    @Override
                    public void invalidateTenant(String systemCode, String tenantId) {
                    }
                };
        var cache = new AuthorizationSnapshotCache(
                failingStore, ClockHolder.clock(), Duration.ofSeconds(5));
        var loader = new SingleFlightSnapshotLoader(
                cache,
                (systemCode, principal) -> AuthorizationSnapshotCacheTest.snapshot("alice-sub"),
                "finance", Duration.ofMinutes(5), ClockHolder.clock());

        assertThatThrownBy(() -> loader.load(principal()))
                .isInstanceOf(
                        Rbac3AuthorizationClient.AuthorizationUnavailableException.class)
                .hasMessage("RBAC3_AUTHORIZATION_CACHE_UNAVAILABLE")
                .hasRootCauseMessage("redis unavailable");
    }

    private IdentityPrincipal principal() {
        return new IdentityPrincipal(
                "alice-sub", "tenant-a", "token-1", Set.of("finance"),
                NOW.minusSeconds(30), NOW.plusSeconds(300),
                AuthenticationContext.password());
    }

    private static final class ClockHolder {

        private static java.time.Clock clock() {
            return java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC);
        }
    }
}
