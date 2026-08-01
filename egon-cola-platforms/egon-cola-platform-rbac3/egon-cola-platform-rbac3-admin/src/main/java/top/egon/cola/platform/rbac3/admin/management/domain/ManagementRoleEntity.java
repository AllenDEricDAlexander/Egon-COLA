package top.egon.cola.platform.rbac3.admin.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "rbac3_management_role")
@IdClass(ManagementRoleEntity.Key.class)
public class ManagementRoleEntity {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Id
    @Column(name = "policy_id", nullable = false)
    private Long policyId;
    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    protected ManagementRoleEntity() {
    }

    public ManagementRoleEntity(Long tenantId, Long policyId, Long roleId) {
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.roleId = roleId;
    }

    public record Key(Long tenantId, Long policyId, Long roleId)
            implements Serializable {
    }
}
