package top.egon.cola.platform.rbac3.admin.activation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Entity
@IdClass(SessionActiveRoleEntity.Key.class)
@Table(name = "rbac3_session_active_role")
public class SessionActiveRoleEntity {

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;
    @Id
    @Column(name = "session_id")
    private Long sessionId;
    @Id
    @Column(name = "root_role_id")
    private Long rootRoleId;
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    @Column(name = "session_version", nullable = false)
    private long sessionVersion;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligible_assignment_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> eligibleAssignmentIds;
    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    protected SessionActiveRoleEntity() {
    }

    public SessionActiveRoleEntity(
            Long tenantId,
            Long sessionId,
            Long applicationId,
            Long rootRoleId,
            long sessionVersion,
            List<String> eligibleAssignmentIds,
            Instant activatedAt
    ) {
        if (sessionVersion < 0) {
            throw new IllegalArgumentException("sessionVersion must not be negative");
        }
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.rootRoleId = Objects.requireNonNull(rootRoleId, "rootRoleId");
        this.sessionVersion = sessionVersion;
        this.eligibleAssignmentIds = List.copyOf(eligibleAssignmentIds);
        this.activatedAt = Objects.requireNonNull(activatedAt, "activatedAt");
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public Long getRootRoleId() {
        return rootRoleId;
    }

    public long getSessionVersion() {
        return sessionVersion;
    }

    public List<String> getEligibleAssignmentIds() {
        return List.copyOf(eligibleAssignmentIds);
    }

    public record Key(Long tenantId, Long sessionId, Long rootRoleId)
            implements Serializable {
    }
}
