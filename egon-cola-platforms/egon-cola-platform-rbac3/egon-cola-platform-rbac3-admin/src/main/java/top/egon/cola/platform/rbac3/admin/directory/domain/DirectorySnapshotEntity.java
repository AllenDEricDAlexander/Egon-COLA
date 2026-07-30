package top.egon.cola.platform.rbac3.admin.directory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "rbac3_directory_snapshot")
public class DirectorySnapshotEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "provider_code", nullable = false, length = 128)
    private String providerCode;

    @Column(name = "snapshot_version", nullable = false)
    private long snapshotVersion;

    @Column(nullable = false, length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> counts = new LinkedHashMap<>();

    protected DirectorySnapshotEntity() {
    }

    public DirectorySnapshotEntity(
            Long id,
            Long tenantId,
            String providerCode,
            long snapshotVersion,
            String checksum,
            Instant generatedAt,
            Map<String, Object> payload,
            String actorId,
            Instant now) {
        if (snapshotVersion < 0) {
            throw new IllegalArgumentException("snapshotVersion must not be negative");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.providerCode = required(providerCode, "providerCode");
        this.snapshotVersion = snapshotVersion;
        this.checksum = required(checksum, "checksum");
        this.status = Status.RECEIVED;
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
        this.receivedAt = Objects.requireNonNull(now, "now");
        this.payload = Map.copyOf(Objects.requireNonNull(payload, "payload"));
        markCreated(actorId, now);
    }

    public void validate(Map<String, Object> validationCounts, String actorId, Instant now) {
        if (status != Status.RECEIVED) {
            throw new IllegalStateException("only received snapshot can be validated");
        }
        counts = Map.copyOf(Objects.requireNonNull(validationCounts, "validationCounts"));
        status = Status.VALIDATED;
        markUpdated(actorId, now);
    }

    public void activate(String actorId, Instant now) {
        if (status != Status.VALIDATED) {
            throw new IllegalStateException("only validated snapshot can be activated");
        }
        status = Status.ACTIVE;
        activatedAt = now;
        markUpdated(actorId, now);
    }

    public Long getId() {
        return id;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public long getSnapshotVersion() {
        return snapshotVersion;
    }

    public String getChecksum() {
        return checksum;
    }

    public Status getStatus() {
        return status;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public enum Status {
        RECEIVED,
        VALIDATED,
        ACTIVE,
        REJECTED,
        ARCHIVED
    }
}
