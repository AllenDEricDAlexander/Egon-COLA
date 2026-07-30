package top.egon.cola.platform.rbac3.admin.constraint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@IdClass(SodMemberEntity.Key.class)
@Table(name = "rbac3_sod_member")
public class SodMemberEntity {

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;
    @Id
    @Column(name = "sod_set_id")
    private Long sodSetId;
    @Id
    @Column(name = "role_id")
    private Long roleId;

    protected SodMemberEntity() {
    }

    public SodMemberEntity(Long tenantId, Long sodSetId, Long roleId) {
        this.tenantId = tenantId;
        this.sodSetId = sodSetId;
        this.roleId = roleId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public record Key(Long tenantId, Long sodSetId, Long roleId) implements Serializable {
    }
}
