package top.egon.cola.platform.rbac3.admin.runtime.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;

@Entity
@Table(name = "rbac3_authorization_mutation")
public class AuthorizationMutationEntity extends TenantScopedEntity {

    @Id
    @Column(name = "mutation_id")
    private Long mutationId;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "session_id")
    private Long sessionId;
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private ScopeType scopeType;
    @Column(name = "command_id", nullable = false, length = 128)
    private String commandId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    @Column(name = "old_session_version")
    private Long oldSessionVersion;
    @Column(name = "new_session_version")
    private Long newSessionVersion;
    @Column(name = "old_auth_version")
    private Long oldAuthVersion;
    @Column(name = "new_auth_version")
    private Long newAuthVersion;
    @Column(name = "old_policy_version")
    private Long oldPolicyVersion;
    @Column(name = "new_policy_version")
    private Long newPolicyVersion;
    @Column(name = "fence_created_at")
    private Instant fenceCreatedAt;
    @Column(name = "committed_at")
    private Instant committedAt;
    @Column(name = "projected_at")
    private Instant projectedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "last_error_code", length = 128)
    private String lastErrorCode;
    @Column(nullable = false)
    private int attempt;

    protected AuthorizationMutationEntity() {
    }

    public AuthorizationMutationEntity(
            Long mutationId,
            Long tenantId,
            Long userId,
            Long sessionId,
            ScopeType scopeType,
            String commandId,
            Long oldSessionVersion,
            Long newSessionVersion,
            Long oldAuthVersion,
            Long newAuthVersion,
            Long oldPolicyVersion,
            Long newPolicyVersion,
            String actorId,
            Instant now
    ) {
        this.mutationId = mutationId;
        setTenantId(tenantId);
        this.userId = userId;
        this.sessionId = sessionId;
        this.scopeType = scopeType;
        this.commandId = commandId;
        this.status = Status.PREPARING;
        this.oldSessionVersion = oldSessionVersion;
        this.newSessionVersion = newSessionVersion;
        this.oldAuthVersion = oldAuthVersion;
        this.newAuthVersion = newAuthVersion;
        this.oldPolicyVersion = oldPolicyVersion;
        this.newPolicyVersion = newPolicyVersion;
        markCreated(actorId, now);
    }

    public void committed(Instant now, String actorId) {
        status = Status.COMMITTED;
        committedAt = now;
        markUpdated(actorId, now);
    }

    public void fenced(Instant now, String actorId) {
        fenceCreatedAt = now;
        markUpdated(actorId, now);
    }

    public void projected(Instant now, String actorId) {
        status = Status.PROJECTED;
        projectedAt = now;
        markUpdated(actorId, now);
    }

    public void completed(Instant now, String actorId) {
        status = Status.COMPLETED;
        completedAt = now;
        markUpdated(actorId, now);
    }

    public void recoveryRequired(String errorCode, Instant now, String actorId) {
        status = Status.RECOVERY_REQUIRED;
        lastErrorCode = errorCode;
        attempt = Math.incrementExact(attempt);
        markUpdated(actorId, now);
    }

    public Long getMutationId() {
        return mutationId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public ScopeType getScopeType() {
        return scopeType;
    }

    public String getCommandId() {
        return commandId;
    }

    public Status getStatus() {
        return status;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public int getAttempt() {
        return attempt;
    }

    public enum ScopeType {
        SESSION,
        USER,
        TENANT
    }

    public enum Status {
        PREPARING,
        COMMITTED,
        PROJECTED,
        COMPLETED,
        ABORTED,
        RECOVERY_REQUIRED
    }
}
