package top.egon.cola.platform.rbac3.admin.runtime.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationMutationCoordinator;
import top.egon.cola.platform.rbac3.admin.runtime.application.RuntimeQueryService;
import top.egon.cola.platform.rbac3.admin.runtime.domain.AuthorizationMutationEntity;
import top.egon.cola.platform.rbac3.admin.worker.AuthorizationMutationRecoveryWorker;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AuthorizationMutationRepository implements
        AuthorizationMutationCoordinator.MutationStore,
        RuntimeQueryService.MutationQueryPort,
        AuthorizationMutationRecoveryWorker.RecoveryStore {

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

    @Override
    @Transactional(readOnly = true)
    public RuntimeQueryService.MutationPage query(
            String tenantId,
            String status,
            String cursor,
            int pageSize) {
        StringBuilder hql = new StringBuilder("""
                select m from AuthorizationMutationEntity m
                 where m.tenantId = :tenantId
                """);
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("tenantId", Long.valueOf(tenantId));
        if (status != null && !status.isBlank()) {
            hql.append(" and m.status = :status");
            parameters.put("status", AuthorizationMutationEntity.Status.valueOf(
                    status.trim()));
        }
        if (cursor != null && !cursor.isBlank()) {
            hql.append(" and m.mutationId < :cursorId");
            parameters.put("cursorId", parseCursor(cursor));
        }
        hql.append(" order by m.mutationId desc");
        var query = entityManager.createQuery(hql.toString(), AuthorizationMutationEntity.class);
        parameters.forEach(query::setParameter);
        var rows = query.setMaxResults(pageSize + 1).getResultList();
        boolean more = rows.size() > pageSize;
        var pageRows = more ? rows.subList(0, pageSize) : rows;
        String nextCursor = more
                ? pageRows.getLast().getMutationId().toString()
                : null;
        return new RuntimeQueryService.MutationPage(
                pageRows.stream().map(this::toView).toList(), nextCursor);
    }

    @Override
    @Transactional
    public Optional<AuthorizationMutationRecoveryWorker.MutationWork> claimById(
            String tenantId,
            String mutationId) {
        List<AuthorizationMutationEntity> rows = entityManager.createQuery("""
                        select m from AuthorizationMutationEntity m
                         where m.tenantId = :tenantId
                           and m.mutationId = :mutationId
                        """, AuthorizationMutationEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("mutationId", Long.valueOf(mutationId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setMaxResults(1)
                .getResultList();
        return rows.stream().findFirst().map(this::toWork);
    }

    @Override
    @Transactional
    public List<AuthorizationMutationRecoveryWorker.MutationWork> claimRecoverable(
            int batchSize) {
        @SuppressWarnings("unchecked")
        List<Number> mutationIds = entityManager.createNativeQuery("""
                        select mutation_id
                          from rbac3_authorization_mutation
                         where status in ('COMMITTED', 'PROJECTED', 'RECOVERY_REQUIRED')
                         order by updated_at, mutation_id
                         for update skip locked
                         limit :batchSize
                        """)
                .setParameter("batchSize", batchSize)
                .getResultList();
        return mutationIds.stream()
                .map(Number::longValue)
                .map(id -> entityManager.find(AuthorizationMutationEntity.class, id))
                .filter(java.util.Objects::nonNull)
                .map(this::toWork)
                .toList();
    }

    @Override
    @Transactional
    public void completed(String mutationId, Instant now, String actorId) {
        AuthorizationMutationEntity mutation = locked(mutationId);
        if (mutation.getStatus() == AuthorizationMutationEntity.Status.COMPLETED) {
            return;
        }
        if (mutation.getStatus() != AuthorizationMutationEntity.Status.PROJECTED) {
            mutation.projected(now, actorId);
        }
        mutation.completed(now, actorId);
    }

    @Override
    @Transactional
    public void failed(
            String mutationId,
            String reasonCode,
            Instant now,
            String actorId) {
        AuthorizationMutationEntity mutation = locked(mutationId);
        if (mutation.getStatus() != AuthorizationMutationEntity.Status.COMPLETED) {
            mutation.recoveryRequired(reasonCode, now, actorId);
        }
    }

    private RuntimeQueryService.MutationView toView(
            AuthorizationMutationEntity mutation) {
        String scopeId = switch (mutation.getScopeType()) {
            case USER -> String.valueOf(mutation.getUserId());
            case SESSION -> String.valueOf(mutation.getSessionId());
            case TENANT -> String.valueOf(mutation.getTenantId());
        };
        return new RuntimeQueryService.MutationView(
                mutation.getMutationId().toString(),
                mutation.getScopeType().name(), scopeId,
                mutation.getCommandId(), mutation.getStatus().name(),
                mutation.getAttempt(), mutation.getLastErrorCode(),
                mutation.getUpdatedAt());
    }

    private AuthorizationMutationRecoveryWorker.MutationWork toWork(
            AuthorizationMutationEntity mutation) {
        String scopeId = switch (mutation.getScopeType()) {
            case USER -> String.valueOf(mutation.getUserId());
            case SESSION -> String.valueOf(mutation.getSessionId());
            case TENANT -> String.valueOf(mutation.getTenantId());
        };
        return new AuthorizationMutationRecoveryWorker.MutationWork(
                mutation.getMutationId().toString(),
                mutation.getTenantId().toString(),
                mutation.getScopeType().name(),
                scopeId,
                mutation.getStatus().name());
    }

    private AuthorizationMutationEntity locked(String mutationId) {
        AuthorizationMutationEntity mutation = entityManager.find(
                AuthorizationMutationEntity.class, Long.valueOf(mutationId),
                LockModeType.PESSIMISTIC_WRITE);
        if (mutation == null) {
            throw new IllegalStateException("authorization mutation is missing");
        }
        return mutation;
    }

    private static Long parseCursor(String cursor) {
        try {
            long value = Long.parseLong(cursor.trim());
            if (value <= 0) {
                throw new NumberFormatException("cursor must be positive");
            }
            return value;
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("mutation cursor is invalid", invalid);
        }
    }
}
