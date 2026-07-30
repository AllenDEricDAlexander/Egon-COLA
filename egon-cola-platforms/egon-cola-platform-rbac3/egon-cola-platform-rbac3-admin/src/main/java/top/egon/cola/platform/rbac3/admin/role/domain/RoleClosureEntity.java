package top.egon.cola.platform.rbac3.admin.role.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@IdClass(RoleClosureEntity.Key.class)
@Table(name = "rbac3_role_closure")
public class RoleClosureEntity {

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;
    @Id
    @Column(name = "application_id")
    private Long applicationId;
    @Id
    @Column(name = "ancestor_role_id")
    private Long ancestorRoleId;
    @Id
    @Column(name = "descendant_role_id")
    private Long descendantRoleId;
    @Column(nullable = false)
    private int depth;

    protected RoleClosureEntity() {
    }

    public record Key(
            Long tenantId,
            Long applicationId,
            Long ancestorRoleId,
            Long descendantRoleId
    ) implements Serializable {
    }
}
