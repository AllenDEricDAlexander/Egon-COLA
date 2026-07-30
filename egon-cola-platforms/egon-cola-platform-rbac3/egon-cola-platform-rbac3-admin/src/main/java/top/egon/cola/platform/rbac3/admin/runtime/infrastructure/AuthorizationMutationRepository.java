package top.egon.cola.platform.rbac3.admin.runtime.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationMutationCoordinator;
import top.egon.cola.platform.rbac3.admin.runtime.domain.AuthorizationMutationEntity;

import java.time.Instant;

@Repository
public class AuthorizationMutationRepository implements
        AuthorizationMutationCoordinator.MutationStore {

    private final EntityManager entityManager;

    public AuthorizationMutationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void prepare(AuthorizationMutationCoordinator.MutationRecord record) {
        var scope = record.scope();
        var versions = record.versions();
        entityManager.persist(new AuthorizationMutationEntity(
                Long.valueOf(record.mutationId()),
                Long.valueOf(scope.tenantId()),
                "USER".equals(scope.scopeType())
                        ? Long.valueOf(scope.scopeId()) : null,
                "SESSION".equals(scope.scopeType())
                        ? Long.valueOf(scope.scopeId()) : null,
                AuthorizationMutationEntity.ScopeType.valueOf(scope.scopeType()),
                scope.commandId(), versions.oldSessionVersion(),
                versions.newSessionVersion(), versions.oldAuthVersion(),
                versions.newAuthVersion(), versions.oldPolicyVersion(),
                versions.newPolicyVersion(), scope.actorId(), record.createdAt()));
    }

    @Override
    @Transactional
    public void transition(
            String mutationId,
            AuthorizationMutationCoordinator.MutationStatus status,
            String errorCode,
            Instant now
    ) {
        AuthorizationMutationEntity mutation = entityManager.find(
                AuthorizationMutationEntity.class, Long.valueOf(mutationId),
                LockModeType.PESSIMISTIC_WRITE);
        if (mutation == null) {
            throw new IllegalStateException("authorization mutation is missing");
        }
        String actorId = mutation.getUpdatedBy();
        switch (status) {
            case COMMITTED -> mutation.committed(now, actorId);
            case FENCED -> mutation.fenced(now, actorId);
            case PROJECTED -> mutation.projected(now, actorId);
            case COMPLETED -> mutation.completed(now, actorId);
            case RECOVERY_REQUIRED -> mutation.recoveryRequired(
                    errorCode, now, actorId);
        }
    }
}
