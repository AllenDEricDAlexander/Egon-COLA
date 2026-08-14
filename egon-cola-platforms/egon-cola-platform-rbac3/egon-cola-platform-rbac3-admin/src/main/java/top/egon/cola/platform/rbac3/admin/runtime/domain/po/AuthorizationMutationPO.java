package top.egon.cola.platform.rbac3.admin.runtime.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.AuthorizationMutationScopeTypeEnum;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.AuthorizationMutationStatusEnum;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;

/**
 * Stores the durable status of a user/tenant authorization publication.
 */
@Entity(name = "AuthorizationMutationEntity")
@Table(name = "rbac3_authorization_mutation")
public class AuthorizationMutationPO extends TenantScopedPO {

    @Id
    @Column(name = "mutation_id")
    private Long mutationId;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private AuthorizationMutationScopeTypeEnum scopeType;

    @Column(name = "command_id", nullable = false, length = 128)
    private String commandId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuthorizationMutationStatusEnum status;

    @Column(name = "old_auth_version")
    private Long oldAuthVersion;

    @Column(name = "new_auth_version")
    private Long newAuthVersion;

    @Column(name = "old_policy_version")
    private Long oldPolicyVersion;

    @Column(name = "new_policy_version")
    private Long newPolicyVersion;

    @Column(name = "guard_created_at")
    private Instant guardCreatedAt;

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

    protected AuthorizationMutationPO() {
    }

    public AuthorizationMutationPO(
            Long mutationId,
            Long tenantId,
            Long userId,
            AuthorizationMutationScopeTypeEnum scopeType,
            String commandId,
            Long oldAuthVersion,
            Long newAuthVersion,
            Long oldPolicyVersion,
            Long newPolicyVersion,
            String actorId,
            Instant now) {
        this.mutationId = mutationId;
        setTenantId(tenantId);
        this.userId = userId;
        this.scopeType = scopeType;
        this.commandId = commandId;
        this.status = AuthorizationMutationStatusEnum.PREPARING;
        this.oldAuthVersion = oldAuthVersion;
        this.newAuthVersion = newAuthVersion;
        this.oldPolicyVersion = oldPolicyVersion;
        this.newPolicyVersion = newPolicyVersion;
        markCreated(actorId, now);
    }

    public void committed(Instant now, String actorId) {
        status = AuthorizationMutationStatusEnum.COMMITTED;
        committedAt = now;
        markUpdated(actorId, now);
    }

    public void fenced(Instant now, String actorId) {
        guardCreatedAt = now;
        markUpdated(actorId, now);
    }

    public void projected(Instant now, String actorId) {
        status = AuthorizationMutationStatusEnum.PROJECTED;
        projectedAt = now;
        markUpdated(actorId, now);
    }

    public void completed(Instant now, String actorId) {
        status = AuthorizationMutationStatusEnum.COMPLETED;
        completedAt = now;
        markUpdated(actorId, now);
    }

    public void recoveryRequired(String errorCode, Instant now, String actorId) {
        status = AuthorizationMutationStatusEnum.RECOVERY_REQUIRED;
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

    public AuthorizationMutationScopeTypeEnum getScopeType() {
        return scopeType;
    }

    public String getCommandId() {
        return commandId;
    }

    public AuthorizationMutationStatusEnum getStatus() {
        return status;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public int getAttempt() {
        return attempt;
    }
}
