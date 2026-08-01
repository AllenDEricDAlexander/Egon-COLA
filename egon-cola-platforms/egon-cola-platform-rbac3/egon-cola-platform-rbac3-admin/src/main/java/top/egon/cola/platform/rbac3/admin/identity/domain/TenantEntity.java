package top.egon.cola.platform.rbac3.admin.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "rbac3_tenant")
public class TenantEntity {

    @Id
    private Long id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "policy_version", nullable = false)
    private long policyVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> settings = new LinkedHashMap<>();

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 128)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    protected TenantEntity() {
    }

    public TenantEntity(Long id, String code, String name, String actorId, Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        this.code = required(code, "code");
        this.name = required(name, "name");
        this.status = Status.INITIALIZING;
        this.createdAt = Objects.requireNonNull(now, "now");
        this.createdBy = required(actorId, "actorId");
        this.updatedAt = now;
        this.updatedBy = this.createdBy;
    }

    public void configure(Map<String, Object> newSettings, String actorId, Instant now) {
        settings = new LinkedHashMap<>(Objects.requireNonNull(newSettings, "newSettings"));
        touch(actorId, now);
    }

    public void activate(String actorId, Instant now) {
        if (status == Status.CLOSED) {
            throw new IllegalStateException("closed tenant cannot be activated");
        }
        status = Status.ACTIVE;
        touch(actorId, now);
    }

    public boolean changeStatus(
            Status nextStatus,
            long expectedVersion,
            String reason,
            String actorId,
            Instant now) {
        Objects.requireNonNull(nextStatus, "nextStatus");
        required(reason, "reason");
        if (version != expectedVersion) {
            throw new IllegalStateException("tenant version conflict");
        }
        if (status == Status.CLOSED && nextStatus != Status.CLOSED) {
            throw new IllegalStateException("closed tenant is terminal");
        }
        if (status == nextStatus) {
            return false;
        }
        status = nextStatus;
        touch(actorId, now);
        return true;
    }

    public void incrementPolicyVersion(String actorId, Instant now) {
        policyVersion = Math.incrementExact(policyVersion);
        touch(actorId, now);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public long getPolicyVersion() {
        return policyVersion;
    }

    public Map<String, Object> getSettings() {
        return Map.copyOf(settings);
    }

    public long getVersion() {
        return version;
    }

    private void touch(String actorId, Instant now) {
        updatedBy = required(actorId, "actorId");
        updatedAt = Objects.requireNonNull(now, "now");
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public enum Status {
        INITIALIZING,
        ACTIVE,
        SUSPENDED,
        CLOSED
    }
}
