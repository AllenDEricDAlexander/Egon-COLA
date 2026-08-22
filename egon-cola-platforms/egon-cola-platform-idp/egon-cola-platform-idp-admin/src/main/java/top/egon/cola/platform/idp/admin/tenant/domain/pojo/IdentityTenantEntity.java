package top.egon.cola.platform.idp.admin.tenant.domain.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** IdP-owned tenant catalog aggregate mapped to {@code identity_tenant}. */
@Entity
@Table(name = "identity_tenant")
public class IdentityTenantEntity {

    private static final Pattern TENANT_CODE =
            Pattern.compile("^[a-z][a-z0-9-]{2,63}$");

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "tenant_code", nullable = false, length = 64)
    private String tenantCode;

    @Column(name = "tenant_name", nullable = false, length = 200)
    private String tenantName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String settings;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false, length = 128,
            updatable = false)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    protected IdentityTenantEntity() {
    }

    /** Creates an INITIALIZING tenant with version zero. */
    public static IdentityTenantEntity create(
            String id,
            String tenantCode,
            String tenantName,
            String settings,
            String actor,
            Instant now
    ) {
        IdentityTenantEntity entity = new IdentityTenantEntity();
        entity.id = required(id, "id");
        entity.tenantCode = code(tenantCode);
        entity.tenantName = name(tenantName);
        entity.status = Status.INITIALIZING;
        entity.settings = settings(settings);
        entity.version = 0L;
        entity.createdAt = Objects.requireNonNull(now, "now");
        entity.updatedAt = now;
        entity.createdBy = actor(actor);
        entity.updatedBy = entity.createdBy;
        return entity;
    }

    /** Applies a version-checked catalog update and legal lifecycle transition. */
    public void update(
            String newTenantName,
            String newSettings,
            Status newStatus,
            long expectedVersion,
            String actor,
            Instant now
    ) {
        if (expectedVersion != version) {
            throw new IllegalStateException("tenant version conflict");
        }
        Objects.requireNonNull(newStatus, "status");
        if (status == Status.CLOSED && newStatus != Status.CLOSED) {
            throw new IllegalStateException("closed tenant cannot change status");
        }
        if (!legalTransition(status, newStatus)) {
            throw new IllegalStateException("invalid tenant status transition");
        }
        tenantName = newTenantName == null ? tenantName : name(newTenantName);
        settings = newSettings == null ? settings : settings(newSettings);
        status = newStatus;
        version = Math.addExact(version, 1L);
        updatedAt = Objects.requireNonNull(now, "now");
        updatedBy = actor(actor);
    }

    public String getId() {
        return id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public String getTenantName() {
        return tenantName;
    }

    public Status getStatus() {
        return status;
    }

    public String getSettings() {
        return settings;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    private static boolean legalTransition(Status current, Status next) {
        if (current == next) {
            return true;
        }
        return switch (current) {
            case INITIALIZING -> next == Status.ACTIVE;
            case ACTIVE -> next == Status.SUSPENDED || next == Status.CLOSED;
            case SUSPENDED -> next == Status.ACTIVE || next == Status.CLOSED;
            case CLOSED -> false;
        };
    }

    private static String code(String value) {
        String normalized = required(value, "tenantCode")
                .toLowerCase(Locale.ROOT);
        if (!TENANT_CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("tenantCode is invalid");
        }
        return normalized;
    }

    private static String name(String value) {
        String normalized = required(value, "tenantName");
        if (normalized.length() > 200) {
            throw new IllegalArgumentException("tenantName is invalid");
        }
        return normalized;
    }

    private static String settings(String value) {
        String normalized = value == null || value.isBlank()
                ? "{}"
                : value.trim();
        if (!normalized.startsWith("{") || !normalized.endsWith("}")
                || normalized.getBytes(StandardCharsets.UTF_8).length > 65_536) {
            throw new IllegalArgumentException("tenant settings are invalid");
        }
        return normalized;
    }

    private static String actor(String value) {
        return required(value, "actor");
    }

    private static String required(String value, String field) {
        Objects.requireNonNull(value, field);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    /** IdP tenant lifecycle state. */
    public enum Status {
        INITIALIZING,
        ACTIVE,
        SUSPENDED,
        CLOSED
    }
}
