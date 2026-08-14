package top.egon.cola.platform.rbac3.admin.runtime.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationPublicationGuardVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationPublicationGuardRepository;
import top.egon.cola.platform.rbac3.admin.runtime.repository.redis.RedisAuthorizationRuntimeRepository;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Expands authorization mutation guards to active IdP subjects.
 */
@Repository
public class JpaAuthorizationPublicationGuardRepository
        implements AuthorizationPublicationGuardRepository {

    private static final Duration GUARD_TTL = Duration.ofMinutes(15);
    private final EntityManager entityManager;
    private final RedisAuthorizationRuntimeRepository runtimeStore;

    public JpaAuthorizationPublicationGuardRepository(
            EntityManager entityManager,
            RedisAuthorizationRuntimeRepository runtimeStore) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.runtimeStore = Objects.requireNonNull(runtimeStore, "runtimeStore");
    }

    @Override
    @Transactional(readOnly = true)
    public void put(AuthorizationPublicationGuardVO guard) {
        subjects(guard.tenantId(), guard.scopeType(), guard.scopeId()).forEach(
                identitySub -> runtimeStore.createFence(
                        guard.tenantId(), identitySub, guard.mutationId(), GUARD_TTL));
    }

    @Override
    @Transactional(readOnly = true)
    public void remove(String tenantId, String scopeType, String scopeId) {
        subjects(tenantId, scopeType, scopeId).forEach(
                identitySub -> runtimeStore.removeFence(tenantId, identitySub));
    }

    private List<String> subjects(String tenantId, String scopeType, String scopeId) {
        String normalizedType = required(scopeType).toUpperCase(java.util.Locale.ROOT);
        String tenant = required(tenantId);
        if ("USER".equals(normalizedType)) {
            return entityManager.createQuery("""
                            select u.identitySub from UserEntity u
                             where u.tenantId = :tenantId and u.id = :userId
                               and u.status = :status
                            """, String.class)
                    .setParameter("tenantId", Long.valueOf(tenant))
                    .setParameter("userId", Long.valueOf(required(scopeId)))
                    .setParameter("status", UserStatusEnum.ACTIVE)
                    .getResultList();
        }
        if ("TENANT".equals(normalizedType)) {
            return entityManager.createQuery("""
                            select u.identitySub from UserEntity u
                             where u.tenantId = :tenantId and u.status = :status
                            """, String.class)
                    .setParameter("tenantId", Long.valueOf(tenant))
                    .setParameter("status", UserStatusEnum.ACTIVE)
                    .getResultList();
        }
        throw new IllegalArgumentException("unsupported authorization scope: " + scopeType);
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("authorization scope identifier is required");
        }
        return value.trim();
    }
}
