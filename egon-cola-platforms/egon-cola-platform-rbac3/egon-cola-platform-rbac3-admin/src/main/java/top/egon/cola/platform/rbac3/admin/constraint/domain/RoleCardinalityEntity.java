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
@Table(name = "rbac3_role_cardinality")
public class RoleCardinalityEntity extends TenantScopedEntity {

    @Id
    private Long id;
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private ScopeType scopeType;
    @Column(name = "max_active", nullable = false)
    private int maximumActive;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    @Column(name = "valid_to")
    private Instant validTo;

    protected RoleCardinalityEntity() {
    }

    public RoleCardinalityEntity(
            Long id,
            Long tenantId,
            Long roleId,
            ScopeType scopeType,
            int maximumActive,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (maximumActive < 1 || validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("invalid role cardinality");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.scopeType = Objects.requireNonNull(scopeType, "scopeType");
        this.maximumActive = maximumActive;
        this.status = Status.ACTIVE;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        markCreated(actorId, now);
    }

    public void update(
            ScopeType scopeType,
            int maximumActive,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (maximumActive < 1 || validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("invalid role cardinality");
        }
        this.scopeType = scopeType;
        this.maximumActive = maximumActive;
        this.validFrom = validFrom;
        this.validTo = validTo;
        markUpdated(actorId, now);
    }

    public enum ScopeType {
        TENANT,
        ORG,
        DEPT
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        EXPIRED
    }
}
