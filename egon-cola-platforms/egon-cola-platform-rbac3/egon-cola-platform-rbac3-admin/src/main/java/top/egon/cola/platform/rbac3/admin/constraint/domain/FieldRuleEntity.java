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
@Table(name = "rbac3_field_rule")
public class FieldRuleEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(name = "field_definition_id", nullable = false)
    private Long fieldDefinitionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 32)
    private AccessLevel accessLevel;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    protected FieldRuleEntity() {
    }

    public FieldRuleEntity(
            Long id,
            Long tenantId,
            Long applicationId,
            Long roleId,
            Long permissionId,
            Long fieldDefinitionId,
            AccessLevel accessLevel,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        validateWindow(validFrom, validTo);
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.permissionId = Objects.requireNonNull(permissionId, "permissionId");
        this.fieldDefinitionId = Objects.requireNonNull(
                fieldDefinitionId, "fieldDefinitionId");
        this.accessLevel = Objects.requireNonNull(accessLevel, "accessLevel");
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    public void update(
            AccessLevel accessLevel,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        validateWindow(validFrom, validTo);
        this.accessLevel = Objects.requireNonNull(accessLevel, "accessLevel");
        this.validFrom = validFrom;
        this.validTo = validTo;
        markUpdated(actorId, now);
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

    public Long getPermissionId() {
        return permissionId;
    }

    public Long getFieldDefinitionId() {
        return fieldDefinitionId;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public Status getStatus() {
        return status;
    }

    public enum AccessLevel {
        NONE,
        MASKED_READ,
        READ,
        WRITE
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        EXPIRED
    }

    private static void validateWindow(Instant validFrom, Instant validTo) {
        Objects.requireNonNull(validFrom, "validFrom");
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
    }
}
