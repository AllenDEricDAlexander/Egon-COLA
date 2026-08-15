package top.egon.cola.platform.rbac3.admin.iam.organization.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.OrgUnitStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.UserDirectoryAssignmentStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.po.OrgUnitPO;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.po.UserOrganizationAssignmentPO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Manages explicit user-to-organization memberships owned by RBAC. */
@Service
public final class UserOrganizationAssignmentService {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;

    public UserOrganizationAssignmentService(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.databaseClock = Objects.requireNonNull(databaseClock, "databaseClock");
    }

    @Transactional(readOnly = true)
    public List<AssignmentView> list(Long tenantId, Long userId) {
        return entityManager.createQuery("""
                        select a from UserOrganizationAssignmentEntity a
                         where a.tenantId = :tenantId and a.userId = :userId
                         order by a.id
                        """, UserOrganizationAssignmentPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .map(UserOrganizationAssignmentService::view)
                .toList();
    }

    @Transactional
    public AssignmentView assign(
            Long tenantId,
            Long userId,
            AssignCommand command,
            String actorId) {
        Objects.requireNonNull(command, "command");
        Instant now = databaseClock.transactionNow();
        UserPO user = requireUser(tenantId, userId, true);
        OrgUnitPO organization = requireOrganization(tenantId, command.orgUnitId());
        Instant validFrom = command.validFrom() == null ? now : command.validFrom();
        Long assignmentId = idGenerator.nextLongId();
        UserOrganizationAssignmentPO assignment = new UserOrganizationAssignmentPO(
                assignmentId, tenantId, user.getId(), organization.getId(),
                validFrom, command.validTo(), "MANUAL", "MANUAL:" + assignmentId,
                command.reason(), command.ticketNo(), actorId, now);
        entityManager.persist(assignment);
        user.applyAuthorizationChange(true, actorId, now);
        return view(assignment);
    }

    @Transactional
    public void revoke(
            Long tenantId,
            Long userId,
            Long assignmentId,
            long expectedVersion,
            String actorId) {
        Instant now = databaseClock.transactionNow();
        UserOrganizationAssignmentPO assignment = entityManager.find(
                UserOrganizationAssignmentPO.class, assignmentId,
                LockModeType.PESSIMISTIC_WRITE);
        if (assignment == null
                || !tenantId.equals(assignment.getTenantId())
                || !userId.equals(assignment.getUserId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        if (!"MANUAL".equals(assignment.getSourceType())) {
            throw new IllegalStateException("directory snapshot assignment is read-only");
        }
        if (assignment.getVersion() != expectedVersion) {
            throw new Rbac3RuleViolation("DIRECTORY_VERSION_CONFLICT");
        }
        UserPO user = requireUser(tenantId, userId, false);
        if (assignment.getStatus() != UserDirectoryAssignmentStatusEnum.ACTIVE
                && assignment.getStatus() != UserDirectoryAssignmentStatusEnum.SUSPENDED) {
            throw new IllegalStateException("organization assignment is not revocable");
        }
        assignment.revoke(actorId, now);
        user.applyAuthorizationChange(true, actorId, now);
    }

    private UserPO requireUser(Long tenantId, Long userId, boolean active) {
        UserPO user = entityManager.find(UserPO.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new Rbac3RuleViolation("USER_NOT_FOUND");
        }
        if (active && user.getStatus() != UserStatusEnum.ACTIVE) {
            throw new Rbac3RuleViolation("USER_INACTIVE");
        }
        return user;
    }

    private OrgUnitPO requireOrganization(Long tenantId, Long orgUnitId) {
        OrgUnitPO organization = entityManager.find(OrgUnitPO.class, orgUnitId);
        if (organization == null || !tenantId.equals(organization.getTenantId())) {
            throw new Rbac3RuleViolation("DIRECTORY_ORG_NOT_FOUND");
        }
        if (organization.getStatus() != OrgUnitStatusEnum.ACTIVE) {
            throw new Rbac3RuleViolation("DIRECTORY_ORG_INACTIVE");
        }
        return organization;
    }

    private static AssignmentView view(UserOrganizationAssignmentPO assignment) {
        return new AssignmentView(
                assignment.getId().toString(), assignment.getUserId().toString(),
                assignment.getOrgUnitId().toString(), assignment.getStatus().name(),
                assignment.getValidFrom(), assignment.getValidTo(),
                assignment.getSourceType(), assignment.getSourceId(),
                assignment.getReason(), assignment.getTicketNo(), assignment.getVersion());
    }

    public record AssignCommand(
            Long orgUnitId,
            Instant validFrom,
            Instant validTo,
            String reason,
            String ticketNo) {
    }

    public record AssignmentView(
            String assignmentId,
            String userId,
            String orgUnitId,
            String status,
            Instant validFrom,
            Instant validTo,
            String sourceType,
            String sourceId,
            String reason,
            String ticketNo,
            long version) {
    }
}
