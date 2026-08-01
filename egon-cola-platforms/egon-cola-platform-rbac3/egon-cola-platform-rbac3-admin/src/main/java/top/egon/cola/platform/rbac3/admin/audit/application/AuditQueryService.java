package top.egon.cola.platform.rbac3.admin.audit.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Tenant-scoped audit write/query facade with mandatory redaction and read audit.
 */
public final class AuditQueryService {

    private static final Duration MAX_QUERY_WINDOW = Duration.ofDays(31);
    private static final String REDACTED = "<redacted>";
    private static final List<String> SECRET_KEY_PARTS = List.of(
            "password", "passwd", "secret", "token", "authorization",
            "credential", "privatekey", "private_key", "refresh");

    private final AuditStore store;
    private final Clock clock;

    public AuditQueryService(AuditStore store, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AuditView record(AuditCommand command) {
        Objects.requireNonNull(command, "command");
        Map<String, Object> before = sanitize(command.beforeSnapshot());
        Map<String, Object> after = sanitize(command.afterSnapshot());
        String checksum = checksum(command, before, after);
        AuditView view = new AuditView(
                null, command.tenantId(), command.eventType(),
                command.outcome(), command.severity(), command.actorType(), command.actorId(),
                command.targetType(), command.targetId(), command.managementPolicyId(),
                command.reasonCode(), command.requestId(), command.traceId(),
                before, after, checksum, command.occurredAt());
        return store.append(view);
    }

    public Page query(
            Query query,
            String readerId,
            String requestId,
            String traceId) {
        validate(query);
        Page page = store.query(query);
        record(new AuditCommand(
                query.tenantId(), "AUDIT_LOGS_READ", "SUCCESS", "INFO", "USER",
                required(readerId, "readerId"), "AUDIT_QUERY", null, null, "ALLOW",
                required(requestId, "requestId"), required(traceId, "traceId"),
                Map.of(), Map.of(
                        "returned", page.items().size(),
                        "from", query.from().toString(),
                        "to", query.to().toString()), clock.instant()));
        return page;
    }

    private void validate(Query query) {
        Objects.requireNonNull(query, "query");
        if (query.to().isBefore(query.from())) {
            throw new IllegalArgumentException("audit query end must not precede start");
        }
        if (Duration.between(query.from(), query.to()).compareTo(MAX_QUERY_WINDOW) > 0) {
            throw new IllegalArgumentException("audit query window must not exceed 31 days");
        }
        if (query.pageSize() < 1 || query.pageSize() > 200) {
            throw new IllegalArgumentException("pageSize must be between 1 and 200");
        }
    }

    private Map<String, Object> sanitize(Map<String, ?> source) {
        var result = new TreeMap<String, Object>();
        Objects.requireNonNull(source, "snapshot").forEach((key, value) -> {
            String normalized = key.toLowerCase(Locale.ROOT).replace("-", "");
            boolean secret = SECRET_KEY_PARTS.stream().anyMatch(normalized::contains);
            result.put(key, secret ? REDACTED : sanitizeValue(value));
        });
        return Map.copyOf(result);
    }

    private Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            var normalized = new LinkedHashMap<String, Object>();
            map.forEach((key, nested) -> normalized.put(String.valueOf(key), nested));
            return sanitize(normalized);
        }
        if (value instanceof Iterable<?> values) {
            var sanitized = new ArrayList<>();
            values.forEach(valueItem -> sanitized.add(sanitizeValue(valueItem)));
            return List.copyOf(sanitized);
        }
        return value;
    }

    private String checksum(
            AuditCommand command,
            Map<String, Object> before,
            Map<String, Object> after) {
        String canonical = String.join("\u001f",
                command.tenantId(), command.eventType(), command.outcome(),
                command.actorType(), command.actorId(), command.requestId(),
                command.traceId(), before.toString(), after.toString(),
                command.occurredAt().toString());
        try {
            return "sha256:" + HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public interface AuditStore {
        AuditView append(AuditView record);

        Page query(Query query);
    }

    public record AuditCommand(
            String tenantId,
            String eventType,
            String outcome,
            String severity,
            String actorType,
            String actorId,
            String targetType,
            String targetId,
            String managementPolicyId,
            String reasonCode,
            String requestId,
            String traceId,
            Map<String, ?> beforeSnapshot,
            Map<String, ?> afterSnapshot,
            Instant occurredAt) {
        public AuditCommand {
            tenantId = required(tenantId, "tenantId");
            eventType = required(eventType, "eventType");
            outcome = required(outcome, "outcome");
            severity = required(severity, "severity");
            actorType = required(actorType, "actorType");
            actorId = required(actorId, "actorId");
            requestId = required(requestId, "requestId");
            traceId = required(traceId, "traceId");
            beforeSnapshot = Map.copyOf(Objects.requireNonNull(
                    beforeSnapshot, "beforeSnapshot"));
            afterSnapshot = Map.copyOf(Objects.requireNonNull(
                    afterSnapshot, "afterSnapshot"));
            occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        }
    }

    public record Query(
            String tenantId,
            Instant from,
            Instant to,
            String actorId,
            String targetId,
            String eventType,
            String outcome,
            String reasonCode,
            String requestId,
            String traceId,
            String targetType,
            int pageSize,
            String cursor) {
        public Query {
            tenantId = required(tenantId, "tenantId");
            from = Objects.requireNonNull(from, "from");
            to = Objects.requireNonNull(to, "to");
        }
    }

    public record AuditView(
            String id,
            String tenantId,
            String eventType,
            String outcome,
            String severity,
            String actorType,
            String actorId,
            String targetType,
            String targetId,
            String managementPolicyId,
            String reasonCode,
            String requestId,
            String traceId,
            Map<String, Object> beforeSnapshot,
            Map<String, Object> afterSnapshot,
            String payloadChecksum,
            Instant createdAt) {
    }

    public record Page(List<AuditView> items, String nextCursor) {
        public Page {
            items = List.copyOf(items);
        }
    }
}
