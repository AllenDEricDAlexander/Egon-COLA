package top.egon.cola.platform.idp.admin.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "identity_outbox_event")
public class IdentityOutboxEventEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "aggregate_type", nullable = false, length = 128)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 128)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected IdentityOutboxEventEntity() {
    }

    public static IdentityOutboxEventEntity pending(
            String id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            Instant now
    ) {
        IdentityOutboxEventEntity entity = new IdentityOutboxEventEntity();
        entity.id = required(id, "id");
        entity.aggregateType = required(aggregateType, "aggregateType");
        entity.aggregateId = required(aggregateId, "aggregateId");
        entity.eventType = required(eventType, "eventType");
        entity.payload = required(payload, "payload");
        entity.status = Status.PENDING;
        entity.attemptCount = 0;
        entity.nextAttemptAt = Objects.requireNonNull(now, "now");
        entity.createdAt = now;
        return entity;
    }

    public Status getStatus() {
        return status;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    public enum Status {
        PENDING,
        PUBLISHED,
        FAILED
    }
}
