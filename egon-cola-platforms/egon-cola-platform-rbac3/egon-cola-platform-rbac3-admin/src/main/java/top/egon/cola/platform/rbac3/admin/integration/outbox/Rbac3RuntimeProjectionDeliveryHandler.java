package top.egon.cola.platform.rbac3.admin.integration.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Dispatches the fixed RBAC3 logical event catalog to the runtime projector.
 */
public final class Rbac3RuntimeProjectionDeliveryHandler implements DeliveryHandler {

    private static final Set<String> DESTINATIONS = Set.of(
            "rbac3.directory.snapshot-activated.v1",
            "rbac3.user.status-changed.v1",
            "rbac3.assignment.changed.v1",
            "rbac3.role.policy-changed.v1",
            "rbac3.management-policy.changed.v1",
            "rbac3.role-activation.changed.v1",
            "rbac3.manifest.activated.v1",
            "rbac3.session.revoked.v1",
            "rbac3.authorization.mutation-committed.v1",
            "rbac3.participation.recorded.v1");

    private final ProjectionSink sink;
    private final ObjectMapper objectMapper;

    public Rbac3RuntimeProjectionDeliveryHandler(ProjectionSink sink) {
        this(sink, new ObjectMapper().findAndRegisterModules());
    }

    public Rbac3RuntimeProjectionDeliveryHandler(
            ProjectionSink sink,
            ObjectMapper objectMapper) {
        this.sink = Objects.requireNonNull(sink, "sink");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public String channel() {
        return TransactionalOutboxAuthorizationEventAdapter.CHANNEL;
    }

    @Override
    public void validateDestination(String destination) {
        if (!DESTINATIONS.contains(destination)) {
            throw new IllegalArgumentException(
                    "unsupported RBAC3 runtime destination: " + destination);
        }
    }

    @Override
    public DeliveryResult deliver(DeliveryContext context) {
        validateDestination(context.destination());
        try {
            EventEnvelope envelope = parse(context.payload());
            if (!context.destination().equals(envelope.eventType())) {
                return DeliveryResult.permanentFailure(
                        "RBAC3_EVENT_DESTINATION_MISMATCH",
                        "event type does not match the outbox destination");
            }
            ProjectionOutcome outcome = sink.project(envelope);
            return switch (outcome) {
                case APPLIED, ALREADY_APPLIED -> DeliveryResult.success();
                case RETRYABLE_FAILURE -> DeliveryResult.retryableFailure(
                        "RBAC3_RUNTIME_PROJECTION_RETRYABLE",
                        "runtime projection has not converged");
                case PERMANENT_FAILURE -> DeliveryResult.permanentFailure(
                        "RBAC3_RUNTIME_PROJECTION_REJECTED",
                        "runtime projection rejected the event");
            };
        } catch (IllegalArgumentException invalid) {
            return DeliveryResult.permanentFailure(
                    "RBAC3_EVENT_INVALID", safeMessage(invalid));
        } catch (RuntimeException unavailable) {
            return DeliveryResult.retryableFailure(
                    "RBAC3_RUNTIME_UNAVAILABLE", "runtime projection is unavailable");
        }
    }

    private EventEnvelope parse(String payload) {
        try {
            JsonNode value = objectMapper.readTree(payload);
            String eventId = required(value, "eventId");
            String eventType = required(value, "eventType");
            int schemaVersion = value.path("schemaVersion").asInt(-1);
            long aggregateVersion = value.path("aggregateVersion").asLong(-1L);
            if (schemaVersion != 1 || aggregateVersion < 0L) {
                throw new IllegalArgumentException("unsupported RBAC3 event version");
            }
            @SuppressWarnings("unchecked")
            Map<String, String> safePayload = objectMapper.convertValue(
                    value.path("payload"), Map.class);
            return new EventEnvelope(
                    eventId, eventType, schemaVersion,
                    Instant.parse(required(value, "occurredAt")),
                    required(value, "tenantId"),
                    required(value, "aggregateType"),
                    required(value, "aggregateId"),
                    aggregateVersion,
                    optional(value, "traceId"),
                    safePayload);
        } catch (IllegalArgumentException invalid) {
            throw invalid;
        } catch (Exception invalid) {
            throw new IllegalArgumentException("invalid RBAC3 event envelope", invalid);
        }
    }

    private String required(JsonNode source, String field) {
        String value = optional(source, field);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String optional(JsonNode source, String field) {
        JsonNode value = source.get(field);
        return value == null || value.isNull() || value.asText().isBlank()
                ? null : value.asText().trim();
    }

    private String safeMessage(IllegalArgumentException invalid) {
        String message = invalid.getMessage();
        return message == null || message.isBlank()
                ? "invalid RBAC3 event envelope"
                : message.substring(0, Math.min(256, message.length()));
    }

    @FunctionalInterface
    public interface ProjectionSink {

        ProjectionOutcome project(EventEnvelope envelope);
    }

    public enum ProjectionOutcome {
        APPLIED,
        ALREADY_APPLIED,
        RETRYABLE_FAILURE,
        PERMANENT_FAILURE
    }

    public record EventEnvelope(
            String eventId,
            String eventType,
            int schemaVersion,
            Instant occurredAt,
            String tenantId,
            String aggregateType,
            String aggregateId,
            long aggregateVersion,
            String traceId,
            Map<String, String> payload) {

        public EventEnvelope {
            payload = Map.copyOf(payload);
        }
    }
}
