package top.egon.cola.platform.rbac3.admin.runtime.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationMutationCoordinator;
import top.egon.cola.platform.rbac3.admin.runtime.application.RuntimeQueryService;
import top.egon.cola.platform.rbac3.admin.runtime.domain.AuthorizationMutationEntity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class AuthorizationMutationRepository implements
        AuthorizationMutationCoordinator.MutationStore,
        RuntimeQueryService.MutationQueryPort {

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
