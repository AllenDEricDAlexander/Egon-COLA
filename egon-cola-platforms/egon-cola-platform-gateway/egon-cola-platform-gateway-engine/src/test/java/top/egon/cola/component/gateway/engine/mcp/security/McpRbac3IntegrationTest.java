package top.egon.cola.component.gateway.engine.mcp.security;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationPort;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationRequest;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.cache.AuthorizationSnapshotCache;
import top.egon.cola.platform.rbac3.starter.cache.SingleFlightSnapshotLoader;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpRbac3IntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-02T05:00:00Z");

    @Test
    void loadsDownstreamSnapshotAndEnforcesPermissionAndVersionFence() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        AtomicInteger fetches = new AtomicInteger();
        AuthorizationSnapshotCache cache = new AuthorizationSnapshotCache(
                new InMemoryStore(),
                clock,
                Duration.ofSeconds(5)
        );
        SingleFlightSnapshotLoader loader = new SingleFlightSnapshotLoader(
                cache,
                (systemCode, principal) -> {
                    fetches.incrementAndGet();
                    return snapshot(systemCode);
                },
                "gateway-mcp",
                Duration.ofMinutes(5),
                clock
        );
        Rbac3McpAuthorizationAdapter adapter =
                new Rbac3McpAuthorizationAdapter(loader);

        McpAuthorizationPort.Decision allowed = authorize(
                adapter,
                request(Set.of(
                        "mcp:billing:tool:pay_invoice:call",
                        "invoice:pay"
                ), 7L, 3L, 11L)
        );
        McpAuthorizationPort.Decision missingPermission = authorize(
                adapter,
                request(Set.of("invoice:refund"), 7L, 3L, 11L)
        );
        McpAuthorizationPort.Decision staleSnapshot = authorize(
                adapter,
                request(Set.of("invoice:pay"), 8L, 3L, 11L)
        );

        assertTrue(allowed.allowed());
        assertEquals(7L, allowed.authVersion());
        assertFalse(missingPermission.allowed());
        assertEquals("RBAC3_PERMISSION_DENIED", missingPermission.reasonCode());
        assertFalse(staleSnapshot.allowed());
        assertEquals("RBAC3_SNAPSHOT_FENCED", staleSnapshot.reasonCode());
        assertEquals(1, fetches.get());
    }

    private McpAuthorizationPort.Decision authorize(
            Rbac3McpAuthorizationAdapter adapter,
            McpAuthorizationRequest request) {
        return Mono.from(adapter.authorize(request)).block();
    }

    private McpAuthorizationRequest request(
            Set<String> permissions,
            long minimumAuthVersion,
            long minimumContextVersion,
            long minimumPolicyVersion) {
        return new McpAuthorizationRequest(
                "https://idp.internal",
                "alice-sub",
                "tenant-a",
                "session-1",
                "finance-web",
                "token-1",
                2L,
                "https://resource.egon.top/gateway-mcp",
                NOW.minusSeconds(30),
                NOW.plusSeconds(300),
                permissions,
                minimumAuthVersion,
                minimumContextVersion,
                minimumPolicyVersion
        );
    }

    private SystemAuthorizationSnapshot snapshot(String systemCode) {
        return new SystemAuthorizationSnapshot(
                "tenant-a",
                "alice-sub",
                "user-1",
                "session-1",
                systemCode,
                7L,
                3L,
                11L,
                java.util.List.of("billing-operator"),
                Set.of(
                        "mcp:billing:tool:pay_invoice:call",
                        "invoice:pay"
                ),
                Map.of(),
                Map.of(),
                "snapshot-checksum",
                NOW,
                NOW.plusSeconds(300)
        );
    }

    private static final class InMemoryStore
            implements AuthorizationSnapshotCache.SnapshotStore {

        private final Map<AuthorizationSnapshotCache.Key,
                SystemAuthorizationSnapshot> values = new ConcurrentHashMap<>();

        @Override
        public Optional<SystemAuthorizationSnapshot> get(
                AuthorizationSnapshotCache.Key key) {
            return Optional.ofNullable(values.get(key));
        }

        @Override
        public void put(
                AuthorizationSnapshotCache.Key key,
                SystemAuthorizationSnapshot snapshot,
                Duration ttl) {
            values.put(key, snapshot);
        }

        @Override
        public void invalidate(AuthorizationSnapshotCache.Key key) {
            values.remove(key);
        }

        @Override
        public void invalidateUser(
                String systemCode,
                String tenantId,
                String identitySub) {
            values.entrySet().removeIf(entry ->
                    entry.getKey().systemCode().equals(systemCode)
                            && entry.getKey().tenantId().equals(tenantId)
                            && entry.getValue().identitySub().equals(
                            identitySub
                    ));
        }

        @Override
        public void invalidateTenant(
                String systemCode,
                String tenantId) {
            values.keySet().removeIf(key ->
                    key.systemCode().equals(systemCode)
                            && key.tenantId().equals(tenantId)
            );
        }
    }
}
