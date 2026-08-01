package top.egon.cola.platform.rbac3.admin.integration.outbox;

import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Adapts RBAC3 logical authorization events to the public transactional-outbox API.
 */
public final class TransactionalOutboxAuthorizationEventAdapter
        implements AuthorizationEventPort {

    public static final String CHANNEL = "rbac3-runtime";

    private static final Map<String, String> DESTINATIONS = Map.ofEntries(
            Map.entry("RBAC3_SESSION_ACTIVE_ROLES_REPLACED",
                    "rbac3.role-activation.changed.v1"),
            Map.entry("ASSIGNMENT_CHANGED", "rbac3.assignment.changed.v1"),
            Map.entry("RESOURCE_MANIFEST_ACTIVATED", "rbac3.manifest.activated.v1"),
            Map.entry("RESOURCE_ARCHIVED", "rbac3.role.policy-changed.v1"),
            Map.entry("ROLE_CREATED", "rbac3.role.policy-changed.v1"),
            Map.entry("ROLE_UPDATED", "rbac3.role.policy-changed.v1"),
            Map.entry("ROLE_INHERITANCE_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("ROLE_PERMISSION_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("SOD_SET_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("ROLE_PREREQUISITE_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("ROLE_CARDINALITY_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("DATA_RULE_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("FIELD_RULE_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("OPERATION_SOD_RULE_CHANGED", "rbac3.role.policy-changed.v1"));

    private final TransactionalOutbox outbox;
    private final Clock clock;

    public TransactionalOutboxAuthorizationEventAdapter(
            TransactionalOutbox outbox,
            Clock clock) {
        this.outbox = Objects.requireNonNull(outbox, "outbox");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String enqueue(AuthorizationEvent event) {
        Objects.requireNonNull(event, "event");
        String destination = destination(event.eventType());
        long aggregateVersion = aggregateVersion(event.safePayload());
        String idempotencyKey = event.tenantId() + ':' + destination + ':'
                + event.aggregateId() + ':' + aggregateVersion;
        String eventId = sha256(idempotencyKey);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", destination);
        envelope.put("schemaVersion", 1);
        envelope.put("occurredAt", clock.instant());
        envelope.put("tenantId", event.tenantId());
        envelope.put("aggregateType", event.aggregateType());
        envelope.put("aggregateId", event.aggregateId());
        envelope.put("aggregateVersion", aggregateVersion);
        envelope.put("traceId", event.traceId());
        envelope.put("payload", event.safePayload());
        return outbox.enqueue(OutboxMessage.builder()
                        .messageId(eventId)
                        .idempotencyKey(idempotencyKey)
                        .channel(CHANNEL)
                        .destination(destination)
                        .payload(envelope)
                        .schemaVersion("1")
                        .traceId(event.traceId())
                        .build())
                .messageId();
    }

    static String destination(String internalEventType) {
        String normalized = required(internalEventType, "eventType")
                .toUpperCase(Locale.ROOT);
        String destination = DESTINATIONS.get(normalized);
        if (destination == null && normalized.endsWith("_CHANGED")) {
            destination = "rbac3.role.policy-changed.v1";
        }
        if (destination == null) {
            throw new IllegalArgumentException(
                    "unsupported RBAC3 authorization event type: " + normalized);
        }
        return destination;
    }

    private long aggregateVersion(Map<String, String> payload) {
        for (String key : new String[]{
                "aggregateVersion", "sessionVersion", "authVersion",
                "policyVersion", "manifestVersion"}) {
            String value = payload.get(key);
            if (value != null) {
                try {
                    long parsed = Long.parseLong(value);
                    if (parsed >= 0L) {
                        return parsed;
                    }
                } catch (NumberFormatException ignored) {
                    // Continue to the stable validation error below.
                }
            }
        }
        throw new IllegalArgumentException(
                "RBAC3 authorization event requires a non-negative aggregate version");
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is unavailable", unavailable);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
