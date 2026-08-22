package top.egon.cola.platform.rbac3.admin.iam.authorizationstate.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.iam.authorizationstate.domain.po.TenantAuthorizationStatePO;
import top.egon.cola.platform.rbac3.admin.iam.authorizationstate.repository.TenantAuthorizationStateRepository;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;

import java.util.Objects;

/** JPA adapter for the RBAC tenant authorization-state table. */
@Repository
public class JpaTenantAuthorizationStateRepository
        implements TenantAuthorizationStateRepository {

    private final EntityManager entityManager;
    private final DatabaseClock databaseClock;

    public JpaTenantAuthorizationStateRepository(
            EntityManager entityManager,
            DatabaseClock databaseClock
    ) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.databaseClock = Objects.requireNonNull(databaseClock, "databaseClock");
    }

    @Override
    @Transactional
    public TenantAuthorizationStatePO requireForUpdate(Long tenantId) {
        TenantAuthorizationStatePO state = entityManager.find(
                TenantAuthorizationStatePO.class,
                requireTenant(tenantId),
                LockModeType.PESSIMISTIC_WRITE
        );
        if (state == null) {
            throw new IllegalStateException(
                    "tenant authorization state not found"
            );
        }
        return state;
    }

    @Override
    @Transactional
    public TenantAuthorizationStatePO ensureVerifiedTenant(
            VerifiedTenant tenant,
            String actorId
    ) {
        Objects.requireNonNull(tenant, "tenant");
        TenantAuthorizationStatePO existing = entityManager.find(
                TenantAuthorizationStatePO.class,
                tenant.tenantId()
        );
        if (existing != null) {
            return existing;
        }
        TenantAuthorizationStatePO created = new TenantAuthorizationStatePO(
                tenant.tenantId(),
                actorId,
                databaseClock.transactionNow()
        );
        entityManager.persist(created);
        return created;
    }

    @Override
    @Transactional
    public long increment(Long tenantId, String actorId) {
        TenantAuthorizationStatePO state = requireForUpdate(tenantId);
        state.incrementPolicyVersion(
                actorId,
                databaseClock.transactionNow()
        );
        return state.getPolicyVersion();
    }

    private static Long requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0L) {
            throw new IllegalArgumentException("tenantId is invalid");
        }
        return tenantId;
    }
}
