package top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.security;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.core.mcp.security.McpAuthorizationPort;
import top.egon.cola.component.yuheng.core.mcp.security.McpAuthorizationRequest;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.starter.cache.AuthorizationSnapshotCache;
import top.egon.cola.platform.tianquan.jianshen.starter.cache.SingleFlightSnapshotLoader;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpRequestObservationComponent;
import top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpRequestObservationComponentTest;
import top.egon.cola.component.yuheng.runtime.security.domain.GatewaySecurityException;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.domain.GatewayVerifiedIdentityBO;
import top.egon.cola.platform.tianquan.jianshen.starter.client.Rbac3AuthorizationClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class McpRbac3IntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-02T05:00:00Z");

    @Test
    void strictFineDenialsShareOneOriginalProofHandoffAndAllowNeverRecordsDeny() {
        var observer = mock(IdpObservedVerificationComponent.class);
        when(observer.authorizationDenied(any(), any())).thenAnswer(call -> {
            assertTrue(Thread.currentThread().getName().startsWith("boundedElastic"));
            return true;
        });
        try (var validation = Validation.buildDefaultValidatorFactory(); var scope = new McpRequestObservationComponent()) {
            var denied = bearer(request(Set.of("invoice:refund"), 7, 3, 3));
            var context = McpRequestObservationComponentTest.context(denied);
            scope.bind(context);
            var adapter = configured(observer, validation.getValidator(), systemCode -> snapshot(systemCode));
            assertFalse(strict(adapter, denied, scope).allowed());
            assertFalse(strict(adapter, denied, scope).allowed());
            var allowed = bearer(request(Set.of("invoice:pay"), 7, 3, 3));
            assertTrue(strict(adapter, allowed, scope).allowed());
            verify(observer, times(1)).authorizationDenied(
                    same(((GatewayVerifiedIdentityBO) context.principal().trustedAuthenticationEvidence()).proof()), eq(context.traceId()));
            scope.requireAvailable();
        }
    }

    @Test
    void failedOrThrowingStrictHandoffsRemainUnavailableWithoutRetry() {
        for (boolean throwsFailure : new boolean[]{false, true}) {
            var observer = mock(IdpObservedVerificationComponent.class);
            if (throwsFailure) { when(observer.authorizationDenied(any(), any())).thenThrow(new IllegalStateException("controlled-audit")); }
            else { when(observer.authorizationDenied(any(), any())).thenReturn(false); }
            try (var validation = Validation.buildDefaultValidatorFactory(); var scope = new McpRequestObservationComponent()) {
                var request = bearer(request(Set.of("invoice:refund"), 7, 3, 3));
                scope.bind(McpRequestObservationComponentTest.context(request));
                var adapter = configured(observer, validation.getValidator(), this::snapshot);
                assertEquals(503, assertThrows(GatewaySecurityException.class, () -> strict(adapter, request, scope)).httpStatus());
                assertEquals(503, assertThrows(GatewaySecurityException.class, () -> strict(adapter, request, scope)).httpStatus());
                verify(observer, times(1)).authorizationDenied(any(), any());
                assertThrows(GatewaySecurityException.class, scope::requireAvailable);
            }
        }
    }

    @Test
    void unavailableSnapshotAndVersionFenceAre503WithoutInventingADeny() {
        for (boolean unavailable : new boolean[]{false, true}) {
            var observer = mock(IdpObservedVerificationComponent.class);
            var fetches = new AtomicInteger();
            try (var validation = Validation.buildDefaultValidatorFactory(); var scope = new McpRequestObservationComponent()) {
                var request = bearer(request(Set.of("invoice:pay"), 8, 3, 3));
                scope.bind(McpRequestObservationComponentTest.context(request));
                var adapter = configured(observer, validation.getValidator(), systemCode -> {
                    fetches.incrementAndGet();
                    if (unavailable) { throw new Rbac3AuthorizationClient.AuthorizationUnavailableException("controlled-snapshot"); }
                    return snapshot(systemCode);
                });
                assertEquals(503, assertThrows(GatewaySecurityException.class, () -> strict(adapter, request, scope)).httpStatus());
                assertEquals(1, fetches.get());
                verifyNoInteractions(observer);
            }
        }
    }

    @Test
    void missingForeignOrClosedPhysicalMarkerRejectsEvenAnAllowingSnapshot() {
        var observer = mock(IdpObservedVerificationComponent.class);
        var fetches = new AtomicInteger();
        try (var validation = Validation.buildDefaultValidatorFactory(); var scope = new McpRequestObservationComponent();
             var foreign = new McpRequestObservationComponent()) {
            var request = bearer(request(Set.of("invoice:pay"), 7, 3, 3));
            scope.bind(McpRequestObservationComponentTest.context(request));
            var adapter = configured(observer, validation.getValidator(), systemCode -> { fetches.incrementAndGet(); return snapshot(systemCode); });
            assertEquals(503, assertThrows(GatewaySecurityException.class,
                    () -> Mono.from(adapter.authorize(request)).contextWrite(c -> c.put(McpRequestObservationComponent.class, scope)).block()).httpStatus());
            assertEquals(503, assertThrows(GatewaySecurityException.class,
                    () -> Mono.from(adapter.authorize(request)).contextWrite(c -> c.put(McpRequestObservationComponent.class, scope)
                            .put(GatewayAuthContext.class, foreign.authorizationScope())).block()).httpStatus());
            scope.close();
            assertEquals(503, assertThrows(GatewaySecurityException.class, () -> strict(adapter, request, scope)).httpStatus());
            assertEquals(0, fetches.get());
            verifyNoInteractions(observer);
        }
    }

    @Test
    void foreignRequestMetadataCannotReuseTheVerifiedRequest() {
        var observer = mock(IdpObservedVerificationComponent.class);
        var fetches = new AtomicInteger();
        var original = bearer(request(Set.of("invoice:pay"), 7, 3, 3));
        try (var validation = Validation.buildDefaultValidatorFactory()) {
            for (int changed = 0; changed < 8; changed++) {
                var candidate = new McpAuthorizationRequest(changed == 0 ? "https://foreign.example" : original.issuer(),
                        changed == 1 ? "foreign-subject" : original.subjectId(), changed == 2 ? "foreign-tenant" : original.tenantId(),
                        changed == 3 ? "foreign-client" : original.clientId(), changed == 4 ? "foreign-token" : original.tokenId(),
                        changed == 5 ? "https://foreign.example/resource" : original.resourceUri(),
                        changed == 6 ? original.issuedAt().minusSeconds(1) : original.issuedAt(),
                        changed == 7 ? original.expiresAt().plusSeconds(1) : original.expiresAt(), original.requiredPermissions(),
                        7, 3, 3, original.userAccessToken());
                try (var scope = new McpRequestObservationComponent()) {
                    scope.bind(McpRequestObservationComponentTest.context(original));
                    var adapter = configured(observer, validation.getValidator(), systemCode -> { fetches.incrementAndGet(); return snapshot(systemCode); });
                    assertEquals(503, assertThrows(GatewaySecurityException.class, () -> strict(adapter, candidate, scope)).httpStatus());
                }
            }
        }
        assertEquals(0, fetches.get());
        verifyNoInteractions(observer);
    }

    @Test
    void missingProofOrUnverifiedScopesFailBeforeLoadingAuthorization() {
        var observer = mock(IdpObservedVerificationComponent.class);
        var fetches = new AtomicInteger();
        var request = bearer(request(Set.of("invoice:pay"), 7, 3, 3));
        var context = McpRequestObservationComponentTest.context(request);
        var principal = context.principal();
        var valid = (GatewayVerifiedIdentityBO) principal.trustedAuthenticationEvidence();
        try (var validation = Validation.buildDefaultValidatorFactory()) {
            for (Object evidence : new Object[]{null, new GatewayVerifiedIdentityBO(null, Set.of("yuheng:read")),
                    new GatewayVerifiedIdentityBO(valid.proof(), Set.of("openid"))}) {
                try (var scope = new McpRequestObservationComponent()) {
                    scope.bind(context.withPrincipal(new top.egon.cola.component.yuheng.core.context.GatewayPrincipal(
                            principal.principalId(), principal.principalType(), principal.tenantId(), principal.displayName(), true, principal.attributes(), evidence)));
                    var adapter = configured(observer, validation.getValidator(), systemCode -> { fetches.incrementAndGet(); return snapshot(systemCode); });
                    assertEquals(503, assertThrows(GatewaySecurityException.class, () -> strict(adapter, request, scope)).httpStatus());
                }
            }
        }
        assertEquals(0, fetches.get()); verifyNoInteractions(observer);
    }

    @Test
    void verifiedServiceIdentityIsADenialAndNeverLoadsAUserSnapshot() {
        var observer = mock(IdpObservedVerificationComponent.class);
        when(observer.authorizationDenied(any(), any())).thenReturn(true);
        var fetches = new AtomicInteger(); var request = bearer(request(Set.of("invoice:pay"), 7, 3, 3));
        var context = McpRequestObservationComponentTest.context(request); var principal = context.principal();
        var attributes = new java.util.LinkedHashMap<>(principal.attributes());
        attributes.remove("tianquan-shoubing.audience"); attributes.put("tianquan-shoubing.client-id", request.clientId());
        try (var validation = Validation.buildDefaultValidatorFactory(); var scope = new McpRequestObservationComponent()) {
            scope.bind(context.withPrincipal(new top.egon.cola.component.yuheng.core.context.GatewayPrincipal(principal.principalId(),
                    "SERVICE", principal.tenantId(), null, true, attributes, principal.trustedAuthenticationEvidence())));
            var adapter = configured(observer, validation.getValidator(), systemCode -> { fetches.incrementAndGet(); return snapshot(systemCode); });
            var result = strict(adapter, request, scope);
            assertFalse(result.allowed()); assertEquals("TIANQUAN_JIANSHEN_USER_PRINCIPAL_REQUIRED", result.reasonCode());
            assertEquals(0, fetches.get()); verify(observer, times(1)).authorizationDenied(any(), eq(context.traceId()));
        }
    }

    private Rbac3McpAuthorizationAdapter configured(IdpObservedVerificationComponent observer, jakarta.validation.Validator validator,
            java.util.function.Function<String, SystemAuthorizationSnapshot> fetcher) {
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        var cache = new AuthorizationSnapshotCache(new InMemoryStore(), clock, Duration.ofSeconds(5));
        var loader = new SingleFlightSnapshotLoader(cache, (code, principal) -> fetcher.apply(code), "yuheng-mcp", Duration.ofMinutes(5), clock);
        return Rbac3McpAuthorizationAdapter.createConfigured(loader, observer, IdpSecurityMetricsRecorder.NOOP, validator);
    }

    private McpAuthorizationPort.Decision strict(Rbac3McpAuthorizationAdapter adapter, McpAuthorizationRequest request,
            McpRequestObservationComponent scope) {
        return Mono.from(adapter.authorize(request)).contextWrite(c -> c.put(McpRequestObservationComponent.class, scope)
                .put(GatewayAuthContext.class, scope.authorizationScope())).block(Duration.ofSeconds(5));
    }

    private McpAuthorizationRequest bearer(McpAuthorizationRequest source) {
        return new McpAuthorizationRequest(source.issuer(), source.subjectId(), source.tenantId(), source.clientId(), source.tokenId(),
                source.resourceUri(), source.issuedAt(), source.expiresAt(), source.requiredPermissions(), source.minimumAuthVersion(),
                source.minimumContextVersion(), source.minimumPolicyVersion(), "controlled-original-bearer");
    }

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
                "yuheng-mcp",
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
                ), 7L, 3L, 3L)
        );
        McpAuthorizationPort.Decision missingPermission = authorize(
                adapter,
                request(Set.of("invoice:refund"), 7L, 3L, 3L)
        );
        McpAuthorizationPort.Decision staleSnapshot = authorize(
                adapter,
                request(Set.of("invoice:pay"), 8L, 3L, 3L)
        );

        assertTrue(allowed.allowed());
        assertEquals(7L, allowed.authVersion());
        assertFalse(missingPermission.allowed());
        assertEquals("TIANQUAN_JIANSHEN_PERMISSION_DENIED", missingPermission.reasonCode());
        assertFalse(staleSnapshot.allowed());
        assertEquals("TIANQUAN_JIANSHEN_SNAPSHOT_FENCED", staleSnapshot.reasonCode());
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
                "https://tianquan-shoubing.internal",
                "alice-sub",
                "tenant-a",
                "finance-web",
                "token-1",
                "https://resource.egon.top/yuheng-mcp",
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
                systemCode,
                7L,
                3L,
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
