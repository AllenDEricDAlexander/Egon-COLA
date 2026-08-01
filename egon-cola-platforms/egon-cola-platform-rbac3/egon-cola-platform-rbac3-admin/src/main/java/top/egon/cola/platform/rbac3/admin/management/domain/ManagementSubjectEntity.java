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
@Table(name = "rbac3_management_subject")
@IdClass(ManagementSubjectEntity.Key.class)
public class ManagementSubjectEntity {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Id
    @Column(name = "policy_id", nullable = false)
    private Long policyId;
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 32)
    private SubjectType subjectType;
    @Id
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    protected ManagementSubjectEntity() {
    }

    public ManagementSubjectEntity(
            Long tenantId,
            Long policyId,
            SubjectType subjectType,
            Long subjectId
    ) {
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
    }

    public record Key(
            Long tenantId,
            Long policyId,
            SubjectType subjectType,
            Long subjectId
    ) implements Serializable {
    }

    public enum SubjectType {
        USER,
        ROLE,
        POSITION
    }
}
