package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.RuntimeProjectionTargetRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.domain.enums.UserStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.domain.po.UserPO;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Keyset-paged projection targets, including inactive memberships that need invalidation. */
@Repository
public class JpaRuntimeProjectionTargetRepository implements RuntimeProjectionTargetRepository {

    private final EntityManager entityManager;

    public JpaRuntimeProjectionTargetRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectionTarget> page(String tenantId, long afterUserId, int limit) {
        if (afterUserId < 0L || limit < 1 || limit > 200) {
            throw new IllegalArgumentException("invalid runtime projection page");
        }
        return entityManager.createQuery("""
                        select u from UserEntity u
                         where u.tenantId = :tenantId and u.id > :afterUserId
                         order by u.id
                        """, UserPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("afterUserId", afterUserId)
                .setMaxResults(limit)
                .getResultList().stream().map(this::target).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectionTarget> find(String tenantId, String userId) {
        return entityManager.createQuery("""
                        select u from UserEntity u
                         where u.tenantId = :tenantId and u.id = :userId
                        """, UserPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("userId", Long.valueOf(userId))
                .getResultStream().findFirst().map(this::target);
    }

    private ProjectionTarget target(UserPO user) {
        return new ProjectionTarget(user.getId().toString(), user.getIdentitySub(),
                user.getAuthVersion(), user.getStatus() == UserStatusEnum.ACTIVE);
    }
}
