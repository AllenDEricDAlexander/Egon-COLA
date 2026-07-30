package top.egon.cola.platform.rbac3.admin.constraint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@IdClass(DataRuleReferenceEntity.Key.class)
@Table(name = "rbac3_data_rule_ref")
public class DataRuleReferenceEntity {

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    @Id
    @Column(name = "data_rule_id")
    private Long dataRuleId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", length = 32)
    private ReferenceType referenceType;

    @Id
    @Column(name = "ref_id")
    private Long referenceId;

    protected DataRuleReferenceEntity() {
    }

    public DataRuleReferenceEntity(
            Long tenantId,
            Long dataRuleId,
            ReferenceType referenceType,
            Long referenceId) {
        this.tenantId = tenantId;
        this.dataRuleId = dataRuleId;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public enum ReferenceType {
        USER,
        DEPT,
        ORG,
        POSITION
    }

    public record Key(
            Long tenantId,
            Long dataRuleId,
            ReferenceType referenceType,
            Long referenceId) implements Serializable {
    }
}
