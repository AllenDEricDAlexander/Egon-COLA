package top.egon.cola.platform.rbac3.admin.role.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

@Entity
@Table(name = "rbac3_role")
public class RoleEntity extends TenantScopedEntity {

    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{2,63}$");

    @Id
    private Long id;
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    @Column(name = "role_code", nullable = false, length = 128, updatable = false)
    private String roleCode;
    @Column(name = "role_name", nullable = false, length = 200)
    private String roleName;
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 32)
    private RoleType roleType;
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;
    @Column(nullable = false)
    private boolean privileged;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    @Column(name = "landing_route_id")
    private Long landingRouteId;
    @Column(name = "landing_priority", nullable = false)
    private int landingPriority;
    @Column(name = "max_assignment_days")
    private Integer maximumAssignmentDays;

    protected RoleEntity() {
    }

    public RoleEntity(
            Long id,
            Long tenantId,
            Long applicationId,
            String roleCode,
            String roleName,
            RoleType roleType,
            RiskLevel riskLevel,
            boolean privileged,
            Long landingRouteId,
            int landingPriority,
            Integer maximumAssignmentDays,
            String actorId,
            Instant now) {
        if (!CODE.matcher(roleCode).matches()) {
            throw new IllegalArgumentException("roleCode is invalid");
        }
        if (landingPriority < 0 || maximumAssignmentDays != null && maximumAssignmentDays < 1) {
            throw new IllegalArgumentException("role limits are invalid");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.roleCode = roleCode;
        this.roleName = required(roleName, "roleName");
        this.roleType = Objects.requireNonNull(roleType, "roleType");
        this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
        this.privileged = privileged;
        this.status = Status.ACTIVE;
        this.landingRouteId = landingRouteId;
        this.landingPriority = landingPriority;
        this.maximumAssignmentDays = maximumAssignmentDays;
        markCreated(actorId, now);
    }

    public RoleNode toRoleNode() {
        return new RoleNode(
                id.toString(),
                applicationId.toString(),
                roleCode,
                status == Status.ACTIVE,
                RoleNode.RiskLevel.valueOf(riskLevel.name()),
                privileged,
                landingRouteId == null ? null : landingRouteId.toString(),
                landingPriority);
    }

    public void update(
            String roleName,
            Status status,
            Long landingRouteId,
            int landingPriority,
            Integer maximumAssignmentDays,
            String actorId,
            Instant now) {
        if (landingPriority < 0
                || maximumAssignmentDays != null && maximumAssignmentDays < 1) {
            throw new IllegalArgumentException("role limits are invalid");
        }
        this.roleName = required(roleName, "roleName");
        this.status = Objects.requireNonNull(status, "status");
        this.landingRouteId = landingRouteId;
        this.landingPriority = landingPriority;
        this.maximumAssignmentDays = maximumAssignmentDays;
        markUpdated(actorId, now);
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public boolean isPrivileged() {
        return privileged;
    }

    public Status getStatus() {
        return status;
    }

    public Integer getMaximumAssignmentDays() {
        return maximumAssignmentDays;
    }

    public enum RoleType {
        PUBLIC,
        POSITION,
        MANAGEMENT,
        TEMPORARY,
        EMERGENCY
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        ARCHIVED
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
