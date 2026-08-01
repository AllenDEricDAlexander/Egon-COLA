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
@Table(name = "rbac3_management_operation")
@IdClass(ManagementOperationEntity.Key.class)
public class ManagementOperationEntity {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Id
    @Column(name = "policy_id", nullable = false)
    private Long policyId;
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_code", nullable = false, length = 32)
    private Operation operation;

    protected ManagementOperationEntity() {
    }

    public ManagementOperationEntity(Long tenantId, Long policyId, Operation operation) {
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.operation = operation;
    }

    public record Key(Long tenantId, Long policyId, Operation operation)
            implements Serializable {
    }

    public enum Operation {
        VIEW_ASSIGNMENT,
        ASSIGN_ROLE,
        REVOKE_ROLE,
        SUSPEND_ROLE,
        RESUME_ROLE,
        TEMPORARY_ASSIGN,
        VIEW_AUDIT,
        VIEW_IMPACT,
        SELF_REVOKE_LOW_RISK
    }
}
