package top.egon.cola.platform.rbac3.admin.iam.user.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.CreateUserCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.UpdateUserCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;

/**
 * Owns only the RBAC tenant membership and IdP subject binding.
 * It deliberately has no password, credential, or profile operations.
 */
@Service
public class UserCrudFacade {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;

    public UserCrudFacade(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
    }

    @Transactional
    public UserDirectoryVO create(
            Long tenantId,
            CreateUserCommandDTO command,
            String actorId) {
        String identitySub = required(command.identitySub(), "identitySub");
        boolean exists = !entityManager.createQuery("""
                        select u.id from UserEntity u
                         where u.tenantId = :tenantId and u.identitySub = :identitySub
                        """, Long.class)
                .setParameter("tenantId", tenantId)
                .setParameter("identitySub", identitySub)
                .setMaxResults(1)
                .getResultList().isEmpty();
        if (exists) {
            throw new Rbac3RuleViolation("USER_IDENTITY_SUB_CONFLICT");
        }
        Instant now = databaseClock.transactionNow();
        UserPO user = new UserPO(
                idGenerator.nextLongId(), tenantId, identitySub,
                command.status(), actorId, now);
        entityManager.persist(user);
        return view(user);
    }

    @Transactional
    public UserDirectoryVO update(
            Long tenantId,
            Long userId,
            UpdateUserCommandDTO command,
            String actorId) {
        UserPO user = require(tenantId, userId, true);
        String identitySub = required(command.identitySub(), "identitySub");
        boolean exists = !entityManager.createQuery("""
                        select u.id from UserEntity u
                         where u.tenantId = :tenantId and u.identitySub = :identitySub
                           and u.id <> :userId
                        """, Long.class)
                .setParameter("tenantId", tenantId)
                .setParameter("identitySub", identitySub)
                .setParameter("userId", userId)
                .setMaxResults(1)
                .getResultList().isEmpty();
        if (exists) {
            throw new Rbac3RuleViolation("USER_IDENTITY_SUB_CONFLICT");
        }
        Instant now = databaseClock.transactionNow();
        user.updateIdentitySub(identitySub, command.expectedAuthVersion(), actorId, now);
        return view(user);
    }

    @Transactional
    public void delete(Long tenantId, Long userId, long expectedAuthVersion, String actorId) {
        UserPO user = require(tenantId, userId, true);
        user.changeStatus(UserStatusEnum.ARCHIVED, "user archived", expectedAuthVersion,
                actorId, databaseClock.transactionNow());
    }

    private UserPO require(Long tenantId, Long userId, boolean lock) {
        UserPO user = lock
                ? entityManager.find(UserPO.class, userId, LockModeType.PESSIMISTIC_WRITE)
                : entityManager.find(UserPO.class, userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return user;
    }

    private static UserDirectoryVO view(UserPO user) {
        return new UserDirectoryVO(user.getId().toString(), user.getIdentitySub(),
                user.getStatus().name(), user.getAuthVersion());
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
