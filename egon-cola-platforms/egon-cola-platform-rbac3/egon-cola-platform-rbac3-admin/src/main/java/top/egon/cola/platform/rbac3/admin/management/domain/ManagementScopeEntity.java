package top.egon.cola.platform.rbac3.admin.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "rbac3_management_scope")
@IdClass(ManagementScopeEntity.Key.class)
public class ManagementScopeEntity {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Id
    @Column(name = "policy_id", nullable = false)
    private Long policyId;
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private ScopeType scopeType;
    @Id
    @Column(name = "scope_ref_id")
    private Long scopeReferenceId;

    protected ManagementScopeEntity() {
    }

    public ManagementScopeEntity(
            Long tenantId,
            Long policyId,
            ScopeType scopeType,
            Long scopeReferenceId
    ) {
        if (scopeType == ScopeType.SELF_DEPT && scopeReferenceId != null
                || scopeType != ScopeType.SELF_DEPT && scopeReferenceId == null) {
            throw new IllegalArgumentException("invalid management scope");
        }
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.scopeType = scopeType;
        this.scopeReferenceId = scopeReferenceId;
    }

    public record Key(
            Long tenantId,
            Long policyId,
            ScopeType scopeType,
            Long scopeReferenceId
    ) implements Serializable {
    }

    public enum ScopeType {
        SELF_DEPT,
        DEPT,
        DEPT_TREE,
        ORG,
        ORG_TREE,
        CUSTOM_DEPT,
        CUSTOM_USER
    }
}
