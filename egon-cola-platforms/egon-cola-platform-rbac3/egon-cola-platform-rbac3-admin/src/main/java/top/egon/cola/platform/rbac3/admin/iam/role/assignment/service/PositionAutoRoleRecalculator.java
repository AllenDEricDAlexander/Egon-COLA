package top.egon.cola.platform.rbac3.admin.iam.role.assignment.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.domain.enums.AutoAssignmentRuleMatchTypeEnum;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.domain.enums.AutoAssignmentRuleStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.domain.enums.UserRoleAssignmentStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.domain.enums.UserRoleAssignmentTypeEnum;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.domain.po.AutoAssignmentRulePO;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.domain.po.UserRoleAssignmentPO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ExpectedVersionsVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationScopeVO;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationMutationCoordinator;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Reconciles POSITION automatic role assignments from current position memberships. */
@Service
public final class PositionAutoRoleRecalculator {

    private static final String SOURCE_TYPE = "POSITION_RULE";

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;
    private final AuthorizationMutationCoordinator mutationCoordinator;

    public PositionAutoRoleRecalculator(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock,
            AuthorizationMutationCoordinator mutationCoordinator) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.databaseClock = Objects.requireNonNull(databaseClock, "databaseClock");
        this.mutationCoordinator = Objects.requireNonNull(
                mutationCoordinator, "mutationCoordinator");
    }

    @Transactional
    public void recalculateForPosition(Long tenantId, Long positionId, String actorId) {
        Instant now = databaseClock.transactionNow();
        Set<Long> activeMembershipUsers = new LinkedHashSet<>(entityManager.createQuery("""
                        select distinct a.userId from UserPositionAssignmentEntity a
                         where a.tenantId = :tenantId and a.positionId = :positionId
                           and a.status = :status and a.validFrom <= :now
                           and (a.validTo is null or a.validTo > :now)
                        """, Long.class)
                .setParameter("tenantId", tenantId)
                .setParameter("positionId", positionId)
                .setParameter("status", top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums
                        .UserDirectoryAssignmentStatusEnum.ACTIVE)
                .setParameter("now", now)
                .getResultList());

        List<AutoAssignmentRulePO> allRules = entityManager.createQuery("""
                        select r from AutoAssignmentRuleEntity r
                         where r.tenantId = :tenantId
                           and r.matchType = :matchType
                           and r.matchReferenceId = :positionId
                        order by r.id
                        """, AutoAssignmentRulePO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("matchType", AutoAssignmentRuleMatchTypeEnum.POSITION)
                .setParameter("positionId", positionId)
                .getResultList();
        Set<String> allRuleIds = allRules.stream()
                .map(rule -> rule.getId().toString())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!allRuleIds.isEmpty()) {
            activeMembershipUsers.addAll(entityManager.createQuery("""
                            select distinct a.userId from UserRoleAssignmentEntity a
                             where a.tenantId = :tenantId and a.sourceType = :sourceType
                               and a.sourceId in :sourceIds
                               and a.status <> :revoked
                            """, Long.class)
                    .setParameter("tenantId", tenantId)
                    .setParameter("sourceType", SOURCE_TYPE)
                    .setParameter("sourceIds", allRuleIds)
                    .setParameter("revoked", UserRoleAssignmentStatusEnum.REVOKED)
                    .getResultList());
        }

        for (Long userId : activeMembershipUsers) {
            recalculateUser(tenantId, userId, allRules, now, actorId);
        }
    }

    private void recalculateUser(
            Long tenantId,
            Long userId,
            List<AutoAssignmentRulePO> allRules,
            Instant now,
            String actorId) {
        List<AutoAssignmentRulePO> effectiveRules = allRules.stream()
                .filter(rule -> isEffective(rule, now))
                .toList();
        Set<String> desiredSourceIds = effectiveRules.stream()
                .map(rule -> rule.getId().toString())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<UserRoleAssignmentPO> existing = entityManager.createQuery("""
                        select a from UserRoleAssignmentEntity a
                         where a.tenantId = :tenantId and a.userId = :userId
                           and a.sourceType = :sourceType
                           and a.status <> :revoked
                        order by a.id
                        """, UserRoleAssignmentPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("userId", userId)
                .setParameter("sourceType", SOURCE_TYPE)
                .setParameter("revoked", UserRoleAssignmentStatusEnum.REVOKED)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        UserPO user = entityManager.find(UserPO.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            return;
        }
        long oldAuthVersion = user.getAuthVersion();
        int[] changed = {0};
        mutationCoordinator.execute(
                new MutationScopeVO(
                        tenantId.toString(), "USER", userId.toString(),
                        "POSITION_ROLE_RECALCULATE:" + userId, actorId),
                userId.toString(),
                new ExpectedVersionsVO(oldAuthVersion, oldAuthVersion + 1, null, null),
                () -> {
                    Map<String, UserRoleAssignmentPO> existingBySource = new LinkedHashMap<>();
                    for (UserRoleAssignmentPO assignment : existing) {
                        existingBySource.put(assignment.getSourceId(), assignment);
                        if (!desiredSourceIds.contains(assignment.getSourceId())) {
                            assignment.revoke(actorId, now);
                            changed[0]++;
                        }
                    }
                    for (AutoAssignmentRulePO rule : effectiveRules) {
                        String sourceId = rule.getId().toString();
                        UserRoleAssignmentPO assignment = existingBySource.get(sourceId);
                        if (assignment == null || assignment.getRoleId() == null
                                || !assignment.getRoleId().equals(rule.getRoleId())) {
                            if (assignment != null
                                    && assignment.getStatus() != UserRoleAssignmentStatusEnum.REVOKED) {
                                assignment.revoke(actorId, now);
                            }
                            UserRoleAssignmentPO created = new UserRoleAssignmentPO(
                                    idGenerator.nextLongId(), tenantId, userId, rule.getRoleId(),
                                    UserRoleAssignmentTypeEnum.AUTO, rule.getValidFrom(),
                                    rule.getValidTo(), SOURCE_TYPE, sourceId,
                                    "POSITION_AUTO_ASSIGNMENT", rule.getRuleCode(), actorId, now);
                            entityManager.persist(created);
                            changed[0]++;
                        }
                    }
                    if (changed[0] > 0) {
                        user.applyAuthorizationChange(true, actorId, now);
                    }
                    return changed[0];
                });
    }

    private static boolean isEffective(AutoAssignmentRulePO rule, Instant now) {
        return rule.getStatus() == AutoAssignmentRuleStatusEnum.ACTIVE
                && !rule.getValidFrom().isAfter(now)
                && (rule.getValidTo() == null || rule.getValidTo().isAfter(now));
    }
}
