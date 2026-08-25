package top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.enums.RoleResourceGrantStatusEnum;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.Objects;

/** Authoritative tenant-scoped direct role-resource grant row. */
@Entity(name = "RoleResourceGrantEntity")
@Table(
        name = "rbac3_role_resource_grant",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_rbac3_role_resource_grant_fact",
                columnNames = {"tenant_id", "role_id", "resource_id", "valid_from"}))
public class RoleResourceGrantPO extends TenantScopedPO {

    @Id
    private Long id;

    @Column(name = "application_id", nullable = false, updatable = false)
    private Long applicationId;

    @Column(name = "role_id", nullable = false, updatable = false)
    private Long roleId;

    @Column(name = "resource_id", nullable = false, updatable = false)
    private Long resourceId;

    @Column(name = "valid_from", nullable = false, updatable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RoleResourceGrantStatusEnum status;

    protected RoleResourceGrantPO() {
    }

    public RoleResourceGrantPO(
            Long id,
            Long tenantId,
            Long applicationId,
            Long roleId,
            Long resourceId,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        this.id = positive(id, "id");
        setTenantId(positive(tenantId, "tenantId"));
        this.applicationId = positive(applicationId, "applicationId");
        this.roleId = positive(roleId, "roleId");
        this.resourceId = positive(resourceId, "resourceId");
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validEnd(validFrom, validTo);
        this.status = RoleResourceGrantStatusEnum.ACTIVE;
        markCreated(actorId, Objects.requireNonNull(now, "now"));
    }

    public void disable(String actorId, Instant now) {
        Instant disabledAt = Objects.requireNonNull(now, "now");
        if (!disabledAt.isAfter(validFrom)) {
            throw new IllegalArgumentException("disable time must be after validFrom");
        }
        status = RoleResourceGrantStatusEnum.DISABLED;
        if (validTo == null || disabledAt.isBefore(validTo)) {
            validTo = disabledAt;
        }
        markUpdated(actorId, disabledAt);
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

    public Long getResourceId() {
        return resourceId;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public RoleResourceGrantStatusEnum getStatus() {
        return status;
    }

    private static Long positive(Long value, String fieldName) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static Instant validEnd(Instant start, Instant end) {
        if (end != null && !end.isAfter(start)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        return end;
    }
}
