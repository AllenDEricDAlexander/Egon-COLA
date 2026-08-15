package top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.UserActiveRoleKey;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Durable active role selection keyed by tenant, user, application and root role.
 */
@Entity(name = "UserActiveRoleEntity")
@IdClass(UserActiveRoleKey.class)
@Table(name = "rbac3_user_active_role")
public class UserActiveRolePO {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Id
    @Column(name = "root_role_id", nullable = false)
    private Long rootRoleId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligible_assignment_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> eligibleAssignmentIds;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    protected UserActiveRolePO() {
    }

    public UserActiveRolePO(
            Long tenantId,
            Long userId,
            Long applicationId,
            Long rootRoleId,
            List<String> eligibleAssignmentIds,
            Instant activatedAt) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.rootRoleId = Objects.requireNonNull(rootRoleId, "rootRoleId");
        this.eligibleAssignmentIds = List.copyOf(
                Objects.requireNonNull(eligibleAssignmentIds, "eligibleAssignmentIds"));
        this.activatedAt = Objects.requireNonNull(activatedAt, "activatedAt");
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public Long getRootRoleId() {
        return rootRoleId;
    }

    public List<String> getEligibleAssignmentIds() {
        return List.copyOf(eligibleAssignmentIds);
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }
}
