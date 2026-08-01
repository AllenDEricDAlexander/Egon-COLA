package top.egon.cola.platform.rbac3.admin.integration.runtime;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationFenceService;
import top.egon.cola.platform.rbac3.admin.snapshot.infrastructure.RedisAuthorizationRuntimeStore;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Expands coarse authorization mutation fences to the affected runtime sessions.
 */
@Repository
public class Rbac3AuthorizationFenceStore implements AuthorizationFenceService.FenceStore {

    private static final Duration FENCE_TTL = Duration.ofMinutes(15);

    private final EntityManager entityManager;
    private final RedisAuthorizationRuntimeStore runtimeStore;

    public Rbac3AuthorizationFenceStore(
            EntityManager entityManager,
            RedisAuthorizationRuntimeStore runtimeStore) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.runtimeStore = Objects.requireNonNull(runtimeStore, "runtimeStore");
    }

    @Override
    @Transactional(readOnly = true)
    public void put(AuthorizationFenceService.Fence fence) {
        sessions(fence.tenantId(), fence.scopeType(), fence.scopeId()).forEach(
                sessionId -> runtimeStore.createSessionFence(
                        fence.tenantId(), sessionId, fence.mutationId(), FENCE_TTL));
    }

    @Override
    @Transactional(readOnly = true)
    public void remove(String tenantId, String scopeType, String scopeId) {
        sessions(tenantId, scopeType, scopeId).forEach(
                sessionId -> runtimeStore.removeSessionFence(tenantId, sessionId));
    }

    private List<String> sessions(String tenantId, String scopeType, String scopeId) {
        return switch (required(scopeType).toUpperCase(Locale.ROOT)) {
            case "SESSION" -> List.of(required(scopeId));
            case "USER" -> activeSessions(tenantId, Long.valueOf(required(scopeId)));
            case "TENANT" -> {
                if (!required(tenantId).equals(required(scopeId))) {
                    throw new IllegalArgumentException("tenant fence scope must match tenantId");
                }
                yield activeSessions(tenantId, null);
            }
            default -> throw new IllegalArgumentException(
                    "unsupported authorization fence scope: " + scopeType);
        };
    }

    private List<String> activeSessions(String tenantId, Long userId) {
        String userPredicate = userId == null ? "" : " and s.userId = :userId";
        var query = entityManager.createQuery("""
                select s.sessionId from SessionEntity s
                 where s.tenantId = :tenantId
                   and s.status = :status
                """ + userPredicate, Long.class)
                .setParameter("tenantId", Long.valueOf(required(tenantId)))
                .setParameter("status",
                        top.egon.cola.platform.rbac3.admin.session.domain.SessionEntity.Status.ACTIVE);
        if (userId != null) {
            query.setParameter("userId", userId);
        }
        return query.getResultList().stream().map(String::valueOf).toList();
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("authorization fence identifier is required");
        }
        return value.trim();
    }
}
