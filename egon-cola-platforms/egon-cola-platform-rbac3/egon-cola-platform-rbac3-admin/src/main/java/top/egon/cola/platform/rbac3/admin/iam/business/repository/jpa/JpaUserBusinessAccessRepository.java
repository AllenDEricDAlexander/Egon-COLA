package top.egon.cola.platform.rbac3.admin.iam.business.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.po.ApplicationPO;
import top.egon.cola.platform.rbac3.admin.iam.business.domain.command.ReplaceUserBusinessAccessesCommand;
import top.egon.cola.platform.rbac3.admin.iam.business.domain.enums.UserBusinessAccessStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.business.domain.po.UserBusinessAccessPO;
import top.egon.cola.platform.rbac3.admin.iam.business.domain.vo.UserApplicationAccessVO;
import top.egon.cola.platform.rbac3.admin.iam.business.domain.vo.UserBusinessAccessVO;
import top.egon.cola.platform.rbac3.admin.iam.business.repository.UserBusinessAccessRepository;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JPA adapter for the tenant-scoped User Business grant table. */
@Repository
public class JpaUserBusinessAccessRepository
        implements UserBusinessAccessRepository {

    private static final String MANUAL = "MANUAL";

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;

    public JpaUserBusinessAccessRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBusinessAccessVO> accesses(Long tenantId, Long userId) {
        return entries(tenantId, userId).stream().map(this::toView).toList();
    }

    @Override
    @Transactional
    public List<UserBusinessAccessVO> replaceManualAccesses(
            Long tenantId,
            Long userId,
            List<ReplaceUserBusinessAccessesCommand.Item> desired,
            String actorId,
            Instant now) {
        UserPO user = requireUser(tenantId, userId);
        List<UserBusinessAccessPO> existing = entries(tenantId, userId);
        Map<String, UserBusinessAccessPO> manualByBusiness = new HashMap<>();
        for (UserBusinessAccessPO access : existing) {
            if (MANUAL.equals(access.getSourceType())) {
                if (manualByBusiness.put(access.getDdcBusinessId(), access) != null) {
                    throw new IllegalStateException("duplicate MANUAL Business access");
                }
            }
        }
        Set<String> requestedBusinesses = new HashSet<>();
        for (ReplaceUserBusinessAccessesCommand.Item item : desired) {
            if (!requestedBusinesses.add(item.ddcBusinessId())) {
                throw new IllegalArgumentException(
                        "duplicate ddcBusinessId: " + item.ddcBusinessId());
            }
            UserBusinessAccessPO access = manualByBusiness.remove(item.ddcBusinessId());
            if (access == null) {
                if (item.expectedVersion() != 0L) {
                    throw new Rbac3RuleViolation("AUTH_MUTATION_CONFLICT");
                }
                access = new UserBusinessAccessPO(
                        idGenerator.nextLongId(), tenantId, userId,
                        item.ddcBusinessId(), item.validFrom(), item.validTo(),
                        MANUAL, item.ddcBusinessId(), item.reason(), item.ticketNo(),
                        actorId, now);
                if (item.status() != UserBusinessAccessStatusEnum.ACTIVE) {
                    access.replace(item.status(), item.validFrom(), item.validTo(),
                            item.reason(), item.ticketNo(), 0L, actorId, now);
                }
                entityManager.persist(access);
            } else {
                try {
                    access.replace(
                            item.status(), item.validFrom(), item.validTo(),
                            item.reason(), item.ticketNo(), item.expectedVersion(),
                            actorId, now);
                } catch (IllegalStateException conflict) {
                    throw new Rbac3RuleViolation("AUTH_MUTATION_CONFLICT");
                }
            }
        }
        for (UserBusinessAccessPO removed : manualByBusiness.values()) {
            removed.revoke(actorId, now);
        }
        // Touching the User is intentional: authorization snapshots use auth_version
        // as the publication fence for both grants and role assignments.
        user.advanceAuthorizationVersion(user.getAuthVersion(), actorId,
                now == null ? databaseClock.transactionNow() : now);
        return entries(tenantId, userId).stream().map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> effectiveBusinessIds(
            Long tenantId,
            Long userId,
            Instant at) {
        return new HashSet<>(entityManager.createQuery("""
                        select a.ddcBusinessId from UserBusinessAccessEntity a
                         where a.tenantId = :tenantId and a.userId = :userId
                           and a.status = :status
                           and a.validFrom <= :at
                           and (a.validTo is null or a.validTo > :at)
                        """, String.class)
                .setParameter("tenantId", tenantId)
                .setParameter("userId", userId)
                .setParameter("status", UserBusinessAccessStatusEnum.ACTIVE)
                .setParameter("at", at)
                .getResultList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserApplicationAccessVO> applicationAccesses(
            Long tenantId,
            Long userId,
            Instant at) {
        return entityManager.createQuery("""
                        select distinct a from ApplicationEntity a, RoleEntity r,
                            UserRoleAssignmentEntity assignment,
                            UserBusinessAccessEntity businessAccess
                         where a.tenantId = :tenantId and a.status = :appStatus
                           and r.tenantId = a.tenantId and r.applicationId = a.id
                           and r.status = :roleStatus
                           and assignment.tenantId = r.tenantId
                           and assignment.roleId = r.id
                           and assignment.userId = :userId
                           and assignment.status = :assignmentStatus
                           and assignment.validFrom <= :at
                           and (assignment.validTo is null or assignment.validTo > :at)
                           and businessAccess.tenantId = a.tenantId
                           and businessAccess.userId = :userId
                           and businessAccess.ddcBusinessId = a.ddcBusinessId
                           and businessAccess.status = :businessStatus
                           and businessAccess.validFrom <= :at
                           and (businessAccess.validTo is null or businessAccess.validTo > :at)
                         order by a.displayPriority, a.applicationCode
                        """, ApplicationPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("userId", userId)
                .setParameter("at", at)
                .setParameter("appStatus", top.egon.cola.platform.rbac3.admin.iam.application.domain.enums.ApplicationStatusEnum.ACTIVE)
                .setParameter("roleStatus", top.egon.cola.platform.rbac3.admin.iam.role.domain.enums.RoleStatusEnum.ACTIVE)
                .setParameter("assignmentStatus", top.egon.cola.platform.rbac3.admin.iam.role.assignment.domain.enums.UserRoleAssignmentStatusEnum.ACTIVE)
                .setParameter("businessStatus", UserBusinessAccessStatusEnum.ACTIVE)
                .getResultList().stream()
                .map(application -> new UserApplicationAccessVO(
                        application.getId().toString(),
                        application.getDdcBusinessId(),
                        application.getDdcApplicationId(),
                        null,
                        application.getApplicationCode(),
                        application.getApplicationName(),
                        application.getStatus().name(),
                        at))
                .toList();
    }

    private List<UserBusinessAccessPO> entries(Long tenantId, Long userId) {
        return entityManager.createQuery("""
                        select a from UserBusinessAccessEntity a
                         where a.tenantId = :tenantId and a.userId = :userId
                         order by a.ddcBusinessId, a.id
                        """, UserBusinessAccessPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("userId", userId)
                .getResultList();
    }

    private UserPO requireUser(Long tenantId, Long userId) {
        UserPO user = entityManager.find(
                UserPO.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null || !tenantId.equals(user.getTenantId())
                || user.getStatus() != UserStatusEnum.ACTIVE) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return user;
    }

    private UserBusinessAccessVO toView(UserBusinessAccessPO access) {
        return new UserBusinessAccessVO(
                access.getId().toString(),
                access.getUserId().toString(),
                access.getDdcBusinessId(),
                null,
                null,
                access.getStatus().name(),
                access.getValidFrom(),
                access.getValidTo(),
                access.getSourceType(),
                access.getSourceId(),
                access.getReason(),
                access.getTicketNo(),
                access.getVersion());
    }
}
