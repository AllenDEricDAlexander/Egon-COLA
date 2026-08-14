package top.egon.cola.platform.rbac3.starter.event;

import top.egon.cola.platform.rbac3.starter.cache.AuthorizationSnapshotCache;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Applies monotonic RBAC3 invalidations without session or Redis key scans.
 */
public final class Rbac3AuthorizationInvalidationConsumer {

    private final String systemCode;
    private final AuthorizationSnapshotCache cache;
    private final ConcurrentHashMap<String, Long> versions = new ConcurrentHashMap<>();

    public Rbac3AuthorizationInvalidationConsumer(
            String systemCode,
            AuthorizationSnapshotCache cache) {
        this.systemCode = required(systemCode, "systemCode");
        this.cache = Objects.requireNonNull(cache, "cache");
    }

    public void accept(Event event) {
        Objects.requireNonNull(event, "event");
        if (!systemCode.equals(event.systemCode())) {
            return;
        }
        String scope = scope(event);
        AtomicBoolean accepted = new AtomicBoolean();
        versions.compute(scope, (ignored, current) -> {
            if (current == null || event.version() > current) {
                accepted.set(true);
                return event.version();
            }
            return current;
        });
        if (!accepted.get()) {
            return;
        }
        switch (event.type()) {
            case "RBAC_AUTHORIZATION_CONTEXT_CHANGED" -> cache.invalidate(
                    new AuthorizationSnapshotCache.Key(
                            systemCode, event.tenantId(),
                            required(event.identitySub(), "identitySub")));
            case "RBAC_USER_AUTHORIZATION_CHANGED", "RBAC_IDENTITY_MAPPING_CHANGED" ->
                    cache.invalidateUser(systemCode, event.tenantId(),
                            required(event.identitySub(), "identitySub"));
            case "RBAC_TENANT_POLICY_CHANGED" -> cache.invalidateTenant(systemCode, event.tenantId());
            default -> throw new IllegalArgumentException(
                    "unsupported RBAC3 invalidation event: " + event.type());
        }
    }

    private String scope(Event event) {
        return switch (event.type()) {
            case "RBAC_AUTHORIZATION_CONTEXT_CHANGED" -> event.type() + ':'
                    + event.tenantId() + ':' + required(event.identitySub(), "identitySub");
            case "RBAC_USER_AUTHORIZATION_CHANGED", "RBAC_IDENTITY_MAPPING_CHANGED" ->
                    event.type() + ':' + event.tenantId() + ':'
                            + required(event.identitySub(), "identitySub");
            case "RBAC_TENANT_POLICY_CHANGED" -> event.type() + ':' + event.tenantId();
            default -> event.type() + ':' + event.eventId();
        };
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    /** Stable event envelope used by the RBAC3 invalidation stream. */
    public record Event(
            String eventId,
            String type,
            String systemCode,
            String tenantId,
            String identitySub,
            long version) {

        public Event {
            eventId = required(eventId, "eventId");
            type = required(type, "type");
            systemCode = required(systemCode, "systemCode");
            tenantId = required(tenantId, "tenantId");
            if (version < 0) {
                throw new IllegalArgumentException("version must not be negative");
            }
        }
    }
}
