package top.egon.cola.platform.rbac3.admin.role.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "rbac3_role_inheritance")
public class RoleInheritanceEntity extends TenantScopedEntity {

    @Id
    private Long id;
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    @Column(name = "senior_role_id", nullable = false)
    private Long seniorRoleId;
    @Column(name = "junior_role_id", nullable = false)
    private Long juniorRoleId;

    protected RoleInheritanceEntity() {
    }

    public RoleInheritanceEntity(
            Long id,
            Long tenantId,
            Long applicationId,
            Long seniorRoleId,
            Long juniorRoleId,
            String actorId,
            Instant now) {
        if (seniorRoleId.equals(juniorRoleId)) {
            throw new IllegalArgumentException("role inheritance must be distinct");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.seniorRoleId = Objects.requireNonNull(seniorRoleId, "seniorRoleId");
        this.juniorRoleId = Objects.requireNonNull(juniorRoleId, "juniorRoleId");
        markCreated(actorId, now);
    }

    public Long getSeniorRoleId() {
        return seniorRoleId;
    }

    public Long getJuniorRoleId() {
        return juniorRoleId;
    }
}
