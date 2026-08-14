package top.egon.cola.platform.rbac3.starter.cache;

import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.client.Rbac3AuthorizationClient;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads one USER authorization projection per system/tenant/subject.
 */
public final class SingleFlightSnapshotLoader {
    private final AuthorizationSnapshotCache cache;
    private final Rbac3AuthorizationClient client;
    private final String systemCode;
    private final Duration cacheTtl;
    private final Clock clock;
    private final ConcurrentHashMap<AuthorizationSnapshotCache.Key,
            CompletableFuture<SystemAuthorizationSnapshot>> flights = new ConcurrentHashMap<>();

    public SingleFlightSnapshotLoader(AuthorizationSnapshotCache cache,
                                      Rbac3AuthorizationClient client,
                                      String systemCode,
                                      Duration cacheTtl,
                                      Clock clock) {
        this.cache = Objects.requireNonNull(cache, "cache");
        this.client = Objects.requireNonNull(client, "client");
        this.systemCode = required(systemCode, "systemCode");
        this.cacheTtl = Objects.requireNonNull(cacheTtl, "cacheTtl");
        if (cacheTtl.compareTo(Duration.ofSeconds(1)) < 0 || cacheTtl.compareTo(Duration.ofMinutes(10)) > 0) {
            throw new IllegalArgumentException("cacheTtl is outside the safe range");
        }
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SystemAuthorizationSnapshot load(IdentityPrincipal principal) {
        Objects.requireNonNull(principal, "principal");
        var key = new AuthorizationSnapshotCache.Key(systemCode, principal.tenantId(), principal.subject());
        return cached(key).filter(snapshot -> boundTo(snapshot, principal)).orElseGet(() -> join(key, principal));
    }

    private SystemAuthorizationSnapshot join(AuthorizationSnapshotCache.Key key, IdentityPrincipal principal) {
        CompletableFuture<SystemAuthorizationSnapshot> created = new CompletableFuture<>();
        CompletableFuture<SystemAuthorizationSnapshot> active = flights.putIfAbsent(key, created);
        if (active == null) {
            active = created;
            try {
                SystemAuthorizationSnapshot snapshot = cached(key)
                        .filter(value -> boundTo(value, principal))
                        .orElseGet(() -> fetch(principal));
                created.complete(snapshot);
            } catch (Throwable failure) {
                created.completeExceptionally(failure);
            } finally {
                flights.remove(key, created);
            }
        }
        try {
            SystemAuthorizationSnapshot snapshot = active.join();
            if (!boundTo(snapshot, principal)) {
                throw new Rbac3AuthorizationClient.AuthorizationDeniedException("RBAC3_AUTHORIZATION_BINDING_MISMATCH");
            }
            return snapshot;
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new Rbac3AuthorizationClient.AuthorizationUnavailableException("RBAC3_AUTHORIZATION_FETCH_FAILED", cause);
        }
    }

    private SystemAuthorizationSnapshot fetch(IdentityPrincipal principal) {
        try {
            SystemAuthorizationSnapshot snapshot = client.fetch(systemCode, principal);
            if (!boundTo(snapshot, principal)) {
                throw new Rbac3AuthorizationClient.AuthorizationDeniedException("RBAC3_AUTHORIZATION_BINDING_MISMATCH");
            }
            Duration remaining = Duration.between(clock.instant(), snapshot.expiresAt());
            cache.put(new AuthorizationSnapshotCache.Key(systemCode, principal.tenantId(), principal.subject()),
                    snapshot, remaining.compareTo(cacheTtl) < 0 ? remaining : cacheTtl);
            return snapshot;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new Rbac3AuthorizationClient.AuthorizationUnavailableException("RBAC3_AUTHORIZATION_FETCH_INTERRUPTED", exception);
        }
    }

    private Optional<SystemAuthorizationSnapshot> cached(AuthorizationSnapshotCache.Key key) {
        try {
            return cache.get(key);
        } catch (RuntimeException exception) {
            throw new Rbac3AuthorizationClient.AuthorizationUnavailableException("RBAC3_AUTHORIZATION_CACHE_UNAVAILABLE", exception);
        }
    }

    private boolean boundTo(SystemAuthorizationSnapshot snapshot, IdentityPrincipal principal) {
        return snapshot.systemCode().equals(systemCode)
                && snapshot.tenantId().equals(principal.tenantId())
                && snapshot.identitySub().equals(principal.subject())
                && snapshot.expiresAt().isAfter(clock.instant());
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
