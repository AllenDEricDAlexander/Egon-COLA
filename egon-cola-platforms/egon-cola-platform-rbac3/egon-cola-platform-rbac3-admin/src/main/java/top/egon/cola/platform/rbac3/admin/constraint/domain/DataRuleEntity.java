package top.egon.cola.platform.rbac3.admin.constraint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "rbac3_data_rule")
public class DataRuleEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private ScopeType scopeType;

    @Column(name = "directory_snapshot_version")
    private Long directorySnapshotVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    protected DataRuleEntity() {
    }

    public DataRuleEntity(
            Long id,
            Long tenantId,
            Long applicationId,
            Long roleId,
            Long permissionId,
            ScopeType scopeType,
            Long directorySnapshotVersion,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        validateWindow(validFrom, validTo);
        if (directorySnapshotVersion != null && directorySnapshotVersion < 0L) {
            throw new IllegalArgumentException("directorySnapshotVersion must not be negative");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.permissionId = Objects.requireNonNull(permissionId, "permissionId");
        this.scopeType = Objects.requireNonNull(scopeType, "scopeType");
        this.directorySnapshotVersion = directorySnapshotVersion;
        this.status = Status.ACTIVE;
        this.validFrom = validFrom;
        this.validTo = validTo;
        markCreated(actorId, now);
    }

    public void update(
            ScopeType scopeType,
            Long directorySnapshotVersion,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        validateWindow(validFrom, validTo);
        this.scopeType = Objects.requireNonNull(scopeType, "scopeType");
        this.directorySnapshotVersion = directorySnapshotVersion;
        this.validFrom = validFrom;
        this.validTo = validTo;
        markUpdated(actorId, now);
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public ScopeType getScopeType() {
        return scopeType;
    }

    public Status getStatus() {
        return status;
    }

    public enum ScopeType {
        ALL,
        SELF,
        DEPT,
        DEPT_TREE,
        ORG,
        ORG_TREE,
        CUSTOM
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        EXPIRED
    }

    private static void validateWindow(Instant validFrom, Instant validTo) {
        Objects.requireNonNull(validFrom, "validFrom");
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
    }
}
