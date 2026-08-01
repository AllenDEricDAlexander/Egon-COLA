package top.egon.cola.platform.rbac3.admin.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "rbac3_user")
public class UserEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(nullable = false, length = 128)
    private String username;

    @Column(name = "normalized_username", nullable = false, length = 128)
    private String normalizedUsername;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    @Column(name = "primary_org_unit_id")
    private Long primaryOrgUnitId;

    @Column(name = "primary_position_id")
    private Long primaryPositionId;

    @Column(name = "directory_snapshot_version", nullable = false)
    private long directorySnapshotVersion;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected UserEntity() {
    }

    public UserEntity(
            Long id,
            Long tenantId,
            String username,
            String displayName,
            String actorId,
            Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.username = required(username, "username");
        this.normalizedUsername = normalize(username);
        this.displayName = required(displayName, "displayName");
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    public void changeStatus(Status nextStatus, String reason, String actorId, Instant now) {
        Objects.requireNonNull(nextStatus, "nextStatus");
        required(reason, "reason");
        if (status == Status.ARCHIVED) {
            throw new IllegalStateException("archived user is terminal");
        }
        status = nextStatus;
        authVersion = Math.incrementExact(authVersion);
        if (nextStatus == Status.ARCHIVED) {
            archivedAt = now;
        }
        markUpdated(actorId, now);
    }

    public Long getId() {
        return id;
    }

    public String getNormalizedUsername() {
        return normalizedUsername;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Status getStatus() {
        return status;
    }

    public long getAuthVersion() {
        return authVersion;
    }

    public long getDirectorySnapshotVersion() {
        return directorySnapshotVersion;
    }

    public long advanceAuthorizationVersion(
            long expectedVersion,
            String actorId,
            Instant now
    ) {
        if (authVersion != expectedVersion) {
            throw new IllegalStateException("user authorization version conflict");
        }
        authVersion = Math.incrementExact(authVersion);
        markUpdated(actorId, now);
        return authVersion;
    }

    public static String normalize(String value) {
        return Normalizer.normalize(required(value, "username"), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public enum Status {
        INVITED,
        ACTIVE,
        LOCKED,
        DISABLED,
        ARCHIVED
    }
}
