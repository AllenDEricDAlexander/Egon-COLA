package top.egon.cola.platform.rbac3.admin.management.domain;

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
@Table(name = "rbac3_management_policy")
public class ManagementPolicyEntity extends TenantScopedEntity {

    @Id
    private Long id;
    @Column(name = "policy_code", nullable = false, length = 128)
    private String policyCode;
    @Column(nullable = false, length = 200)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    @Column(name = "valid_to")
    private Instant validTo;
    @Column(name = "max_assignment_days")
    private Integer maximumAssignmentDays;
    @Enumerated(EnumType.STRING)
    @Column(name = "max_risk_level", nullable = false, length = 32)
    private RiskLevel maximumRiskLevel;
    @Enumerated(EnumType.STRING)
    @Column(name = "required_auth_strength", nullable = false, length = 32)
    private AuthenticationStrength requiredAuthenticationStrength;
    @Column(name = "require_reason", nullable = false)
    private boolean requireReason;
    @Column(name = "require_ticket", nullable = false)
    private boolean requireTicket;
    @Column(name = "include_inherited_subject_roles", nullable = false)
    private boolean includeInheritedSubjectRoles;
    @Column(name = "require_all_affiliations_in_scope", nullable = false)
    private boolean requireAllAffiliationsInScope;

    protected ManagementPolicyEntity() {
    }

    public ManagementPolicyEntity(
            Long id,
            Long tenantId,
            String policyCode,
            String name,
            Instant validFrom,
            Instant validTo,
            Integer maximumAssignmentDays,
            RiskLevel maximumRiskLevel,
            AuthenticationStrength requiredAuthenticationStrength,
            boolean requireReason,
            boolean requireTicket,
            boolean includeInheritedSubjectRoles,
            boolean requireAllAffiliationsInScope,
            String actorId,
            Instant now
    ) {
        if (validTo != null && !validTo.isAfter(validFrom)
                || maximumAssignmentDays != null && maximumAssignmentDays < 1) {
            throw new IllegalArgumentException("invalid management policy limits");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.policyCode = required(policyCode, "policyCode");
        this.name = required(name, "name");
        this.status = Status.ACTIVE;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        this.maximumAssignmentDays = maximumAssignmentDays;
        this.maximumRiskLevel = Objects.requireNonNull(maximumRiskLevel, "maximumRiskLevel");
        this.requiredAuthenticationStrength = Objects.requireNonNull(
                requiredAuthenticationStrength, "requiredAuthenticationStrength");
        this.requireReason = requireReason;
        this.requireTicket = requireTicket;
        this.includeInheritedSubjectRoles = includeInheritedSubjectRoles;
        this.requireAllAffiliationsInScope = requireAllAffiliationsInScope;
        markCreated(actorId, now);
    }

    public Long getId() {
        return id;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public Integer getMaximumAssignmentDays() {
        return maximumAssignmentDays;
    }

    public RiskLevel getMaximumRiskLevel() {
        return maximumRiskLevel;
    }

    public AuthenticationStrength getRequiredAuthenticationStrength() {
        return requiredAuthenticationStrength;
    }

    public boolean isRequireReason() {
        return requireReason;
    }

    public boolean isRequireTicket() {
        return requireTicket;
    }

    public boolean isIncludeInheritedSubjectRoles() {
        return includeInheritedSubjectRoles;
    }

    public boolean isRequireAllAffiliationsInScope() {
        return requireAllAffiliationsInScope;
    }

    public void update(
            String name,
            Instant validFrom,
            Instant validTo,
            Integer maximumAssignmentDays,
            RiskLevel maximumRiskLevel,
            AuthenticationStrength requiredAuthenticationStrength,
            boolean requireReason,
            boolean requireTicket,
            boolean includeInheritedSubjectRoles,
            boolean requireAllAffiliationsInScope,
            String actorId,
            Instant now
    ) {
        validate(validFrom, validTo, maximumAssignmentDays);
        this.name = required(name, "name");
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        this.maximumAssignmentDays = maximumAssignmentDays;
        this.maximumRiskLevel = Objects.requireNonNull(maximumRiskLevel, "maximumRiskLevel");
        this.requiredAuthenticationStrength = Objects.requireNonNull(
                requiredAuthenticationStrength, "requiredAuthenticationStrength");
        this.requireReason = requireReason;
        this.requireTicket = requireTicket;
        this.includeInheritedSubjectRoles = includeInheritedSubjectRoles;
        this.requireAllAffiliationsInScope = requireAllAffiliationsInScope;
        markUpdated(actorId, now);
    }

    public void disable(String actorId, Instant now) {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("only active management policy can be disabled");
        }
        status = Status.DISABLED;
        markUpdated(actorId, now);
    }

    private static void validate(
            Instant validFrom,
            Instant validTo,
            Integer maximumAssignmentDays
    ) {
        Objects.requireNonNull(validFrom, "validFrom");
        if (validTo != null && !validTo.isAfter(validFrom)
                || maximumAssignmentDays != null && maximumAssignmentDays < 1) {
            throw new IllegalArgumentException("invalid management policy limits");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        EXPIRED,
        ARCHIVED
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum AuthenticationStrength {
        PASSWORD,
        MFA,
        STRONG
    }
}
