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
@Table(name = "rbac3_operation_sod_rule")
public class OperationSodRuleEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "application_code", nullable = false, length = 128)
    private String applicationCode;

    @Column(name = "business_resource", nullable = false, length = 128)
    private String businessResource;

    @Column(name = "prior_action_code", nullable = false, length = 128)
    private String priorActionCode;

    @Column(name = "forbidden_later_action_code", nullable = false, length = 128)
    private String forbiddenLaterActionCode;

    @Column(name = "lookback_from")
    private Instant lookbackFrom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    protected OperationSodRuleEntity() {
    }

    public OperationSodRuleEntity(
            Long id,
            Long tenantId,
            String applicationCode,
            String businessResource,
            String priorActionCode,
            String forbiddenLaterActionCode,
            Instant lookbackFrom,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        validate(priorActionCode, forbiddenLaterActionCode, validFrom, validTo);
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationCode = required(applicationCode, "applicationCode");
        this.businessResource = required(businessResource, "businessResource");
        this.priorActionCode = priorActionCode.trim();
        this.forbiddenLaterActionCode = forbiddenLaterActionCode.trim();
        this.lookbackFrom = lookbackFrom;
        this.status = Status.ACTIVE;
        this.validFrom = validFrom;
        this.validTo = validTo;
        markCreated(actorId, now);
    }

    public void update(
            String businessResource,
            String priorActionCode,
            String forbiddenLaterActionCode,
            Instant lookbackFrom,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        validate(priorActionCode, forbiddenLaterActionCode, validFrom, validTo);
        this.businessResource = required(businessResource, "businessResource");
        this.priorActionCode = priorActionCode.trim();
        this.forbiddenLaterActionCode = forbiddenLaterActionCode.trim();
        this.lookbackFrom = lookbackFrom;
        this.validFrom = validFrom;
        this.validTo = validTo;
        markUpdated(actorId, now);
    }

    public Long getId() {
        return id;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public String getBusinessResource() {
        return businessResource;
    }

    public String getPriorActionCode() {
        return priorActionCode;
    }

    public String getForbiddenLaterActionCode() {
        return forbiddenLaterActionCode;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        EXPIRED
    }

    private static void validate(
            String priorActionCode,
            String forbiddenLaterActionCode,
            Instant validFrom,
            Instant validTo) {
        String prior = required(priorActionCode, "priorActionCode");
        String later = required(forbiddenLaterActionCode, "forbiddenLaterActionCode");
        if (prior.equals(later)) {
            throw new IllegalArgumentException("SOD actions must be different");
        }
        Objects.requireNonNull(validFrom, "validFrom");
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
