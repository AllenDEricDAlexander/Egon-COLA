package top.egon.cola.platform.rbac3.admin.role.domain;

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
@Table(name = "rbac3_role_permission")
public class RolePermissionEntity extends TenantScopedEntity {

    @Id
    private Long id;
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    @Column(name = "valid_to")
    private Instant validTo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    protected RolePermissionEntity() {
    }

    public RolePermissionEntity(
            Long id,
            Long tenantId,
            Long applicationId,
            Long roleId,
            Long permissionId,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.permissionId = Objects.requireNonNull(permissionId, "permissionId");
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    public void disable(String actorId, Instant now) {
        if (status == Status.ACTIVE) {
            status = Status.DISABLED;
            markUpdated(actorId, now);
        }
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        EXPIRED
    }
}
