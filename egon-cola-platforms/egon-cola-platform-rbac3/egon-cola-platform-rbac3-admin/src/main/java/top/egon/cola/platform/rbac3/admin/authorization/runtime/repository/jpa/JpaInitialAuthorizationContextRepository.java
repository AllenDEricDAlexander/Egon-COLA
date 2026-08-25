package top.egon.cola.platform.rbac3.admin.authorization.runtime.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.state.domain.po.TenantAuthorizationStatePO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.repository.InitialAuthorizationContextRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** PostgreSQL lookup for the restricted pre-activation authorization context. */
@Repository
public class JpaInitialAuthorizationContextRepository
        implements InitialAuthorizationContextRepository {

    private final EntityManager entityManager;

    public JpaInitialAuthorizationContextRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InitialAuthorizationContext> find(
            String tenantId,
            String identitySub
    ) {
        Long normalizedTenantId = tenantId(tenantId);
        List<UserPO> users = entityManager.createQuery("""
                        select user from UserEntity user
                        where user.tenantId = :tenantId
                          and user.identitySub = :identitySub
                        """, UserPO.class)
                .setParameter("tenantId", normalizedTenantId)
                .setParameter("identitySub", required(identitySub, "identitySub"))
                .setMaxResults(2)
                .getResultList();
        if (users.size() > 1) {
            throw new IllegalStateException("duplicate RBAC user identity");
        }
        if (users.isEmpty() || users.getFirst().getStatus() != UserStatusEnum.ACTIVE) {
            return Optional.empty();
        }
        TenantAuthorizationStatePO state = entityManager.find(
                TenantAuthorizationStatePO.class,
                normalizedTenantId
        );
        if (state == null) {
            return Optional.empty();
        }
        UserPO user = users.getFirst();
        return Optional.of(new InitialAuthorizationContext(
                user.getId().toString(),
                user.getAuthVersion(),
                state.getPolicyVersion()
        ));
    }

    private static Long tenantId(String value) {
        String normalized = required(value, "tenantId");
        try {
            long parsed = Long.parseLong(normalized);
            if (parsed <= 0L) {
                throw new NumberFormatException("tenantId must be positive");
            }
            return parsed;
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("tenantId is invalid", invalid);
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
