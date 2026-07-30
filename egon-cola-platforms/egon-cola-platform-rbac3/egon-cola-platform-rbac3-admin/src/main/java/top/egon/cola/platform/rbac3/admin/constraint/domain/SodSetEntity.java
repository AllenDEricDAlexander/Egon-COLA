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
@Table(name = "rbac3_sod_set")
public class SodSetEntity extends TenantScopedEntity {

    @Id
    private Long id;
    @Column(name = "application_id")
    private Long applicationId;
    @Column(name = "set_code", nullable = false, length = 128)
    private String setCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "constraint_type", nullable = false, length = 32)
    private ConstraintType constraintType;
    @Column(name = "max_active_roles", nullable = false)
    private int maximumActiveRoles;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    @Column(name = "valid_to")
    private Instant validTo;

    protected SodSetEntity() {
    }

    public SodSetEntity(
            Long id,
            Long tenantId,
            Long applicationId,
            String setCode,
            ConstraintType constraintType,
            int maximumActiveRoles,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (constraintType == ConstraintType.DSD && applicationId == null) {
            throw new IllegalArgumentException("DSD requires an application");
        }
        if (maximumActiveRoles < 1 || validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("invalid SOD set limits");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = applicationId;
        this.setCode = required(setCode, "setCode");
        this.constraintType = Objects.requireNonNull(constraintType, "constraintType");
        this.maximumActiveRoles = maximumActiveRoles;
        this.status = Status.ACTIVE;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        markCreated(actorId, now);
    }

    public void update(
            Long applicationId,
            ConstraintType constraintType,
            int maximumActiveRoles,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (constraintType == ConstraintType.DSD && applicationId == null) {
            throw new IllegalArgumentException("DSD requires an application");
        }
        if (maximumActiveRoles < 1 || validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("invalid SOD set limits");
        }
        this.applicationId = applicationId;
        this.constraintType = constraintType;
        this.maximumActiveRoles = maximumActiveRoles;
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

    public String getSetCode() {
        return setCode;
    }

    public ConstraintType getConstraintType() {
        return constraintType;
    }

    public int getMaximumActiveRoles() {
        return maximumActiveRoles;
    }

    public Status getStatus() {
        return status;
    }

    public enum ConstraintType {
        SSD,
        DSD
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        EXPIRED
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
