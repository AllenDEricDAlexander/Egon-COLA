package top.egon.cola.platform.rbac3.admin.management.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.management.application.ManagementPolicyFacade;
import top.egon.cola.platform.rbac3.admin.management.domain.ManagementOperationEntity;
import top.egon.cola.platform.rbac3.admin.management.domain.ManagementPolicyEntity;
import top.egon.cola.platform.rbac3.admin.management.domain.ManagementRoleEntity;
import top.egon.cola.platform.rbac3.admin.management.domain.ManagementScopeEntity;
import top.egon.cola.platform.rbac3.admin.management.domain.ManagementSubjectEntity;
import top.egon.cola.platform.rbac3.core.delegation.ManagementPolicyDecisionService;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Repository
public class ManagementPolicyRepository implements
        ManagementPolicyFacade.PolicyFactSource,
        ManagementPolicyFacade.ControlStore {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;

    public ManagementPolicyRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock
    ) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagementPolicyDecisionService.ManagementPolicyFact> policies(
            String tenantId,
            String subjectId,
            String targetUserId,
            Instant databaseNow
    ) {
        Long tenant = Long.valueOf(tenantId);
        List<ManagementPolicyEntity> candidates = entityManager.createQuery("""
                        select distinct p from ManagementPolicyEntity p,
                             ManagementSubjectEntity s
                         where p.tenantId = :tenantId and s.tenantId = p.tenantId
                           and s.policyId = p.id and p.status = :status
                           and p.validFrom <= :now
                           and (p.validTo is null or p.validTo > :now)
                           and ((s.subjectType = :userType and s.subjectId = :subjectId)
                             or (s.subjectType = :roleType and exists (
                                 select 1 from UserRoleAssignmentEntity a
                                  where a.tenantId = p.tenantId
                                    and a.userId = :subjectId
                                    and (a.roleId = s.subjectId
                                      or (p.includeInheritedSubjectRoles = true and exists (
                                          select 1 from RoleClosureEntity c
                                           where c.tenantId = a.tenantId
                                             and c.ancestorRoleId = s.subjectId
                                             and c.descendantRoleId = a.roleId)))
                                    and a.status = :assignmentStatus
                                    and a.validFrom <= :now
                                    and (a.validTo is null or a.validTo > :now)))
                             or (s.subjectType = :positionType and exists (
                                 select 1 from UserPositionSnapshotEntity ups
                                  where ups.tenantId = p.tenantId
                                    and ups.userId = :subjectId
                                    and ups.positionId = s.subjectId
                                    and ups.status = :positionStatus
                                    and ups.validFrom <= :now
                                    and (ups.validTo is null or ups.validTo > :now))))
                         order by p.id
                        """, ManagementPolicyEntity.class)
                .setParameter("tenantId", tenant)
                .setParameter("status", ManagementPolicyEntity.Status.ACTIVE)
                .setParameter("now", databaseNow)
                .setParameter("userType", ManagementSubjectEntity.SubjectType.USER)
                .setParameter("roleType", ManagementSubjectEntity.SubjectType.ROLE)
                .setParameter("positionType",
                        ManagementSubjectEntity.SubjectType.POSITION)
                .setParameter("assignmentStatus",
                        top.egon.cola.platform.rbac3.admin.assignment.domain
                                .UserRoleAssignmentEntity.Status.ACTIVE)
                .setParameter("positionStatus",
                        top.egon.cola.platform.rbac3.admin.directory.domain
                                .UserPositionSnapshotEntity.Status.ACTIVE)
                .setParameter("subjectId", Long.valueOf(subjectId))
                .getResultList();
        return candidates.stream()
                .map(policy -> fact(
                        policy, subjectId, targetUserId,
                        targetInScope(
                                tenant, policy.getId(), Long.valueOf(subjectId),
                                Long.valueOf(targetUserId), databaseNow,
                                policy.isRequireAllAffiliationsInScope())))
                .toList();
    }

    @Transactional
    private String savePolicy(SavePolicyCommand command) {
        Instant now = databaseClock.transactionNow();
        Long tenantId = Long.valueOf(command.tenantId());
        Long policyId;
        if (command.policyId() == null) {
            policyId = idGenerator.nextLongId();
            entityManager.persist(new ManagementPolicyEntity(
                    policyId, tenantId, command.policyCode(), command.name(),
                    command.validFrom(), command.validTo(), command.maximumAssignmentDays(),
                    ManagementPolicyEntity.RiskLevel.valueOf(command.maximumRiskLevel()),
                    ManagementPolicyEntity.AuthenticationStrength.valueOf(
                            command.requiredAuthenticationStrength()),
                    command.requireReason(), command.requireTicket(),
                    command.includeInheritedSubjectRoles(),
                    command.requireAllAffiliationsInScope(), command.actorId(), now));
        } else {
            policyId = Long.valueOf(command.policyId());
            ManagementPolicyEntity current = entityManager.find(
                    ManagementPolicyEntity.class, policyId, LockModeType.PESSIMISTIC_WRITE);
            if (current == null || !tenantId.equals(current.getTenantId())) {
                throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
            }
            if (current.getVersion() != command.expectedVersion()) {
                throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
            }
            if (!current.getPolicyCode().equals(command.policyCode())) {
                throw new Rbac3RuleViolation("REQUEST_INVALID");
            }
            current.update(
                    command.name(), command.validFrom(), command.validTo(),
                    command.maximumAssignmentDays(),
                    ManagementPolicyEntity.RiskLevel.valueOf(
                            command.maximumRiskLevel()),
                    ManagementPolicyEntity.AuthenticationStrength.valueOf(
                            command.requiredAuthenticationStrength()),
                    command.requireReason(), command.requireTicket(),
                    command.includeInheritedSubjectRoles(),
                    command.requireAllAffiliationsInScope(), command.actorId(), now);
            deleteChildren(tenantId, policyId);
        }
        command.subjects().forEach(subject -> entityManager.persist(
                new ManagementSubjectEntity(
                        tenantId, policyId,
                        ManagementSubjectEntity.SubjectType.valueOf(subject.type()),
                        Long.valueOf(subject.id()))));
        command.scopes().forEach(scope -> insertScope(tenantId, policyId, scope));
        command.activationRootRoleIds().forEach(roleId -> entityManager.persist(
                new ManagementRoleEntity(tenantId, policyId, Long.valueOf(roleId))));
        command.operations().forEach(operation -> entityManager.persist(
                new ManagementOperationEntity(
                        tenantId, policyId,
                        ManagementOperationEntity.Operation.valueOf(operation))));
        return policyId.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagementPolicyFacade.PolicyView> policies(String tenantId) {
        return entityManager.createQuery("""
                        select p from ManagementPolicyEntity p
                         where p.tenantId = :tenantId
                         order by p.policyCode
                        """, ManagementPolicyEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .getResultList().stream()
                .map(this::view)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ManagementPolicyFacade.PolicyView policy(
            String tenantId,
            String policyId
    ) {
        return view(requirePolicy(
                Long.valueOf(tenantId), Long.valueOf(policyId), false));
    }

    @Override
    @Transactional
    public ManagementPolicyFacade.PolicyView save(
            ManagementPolicyFacade.SaveCommand command
    ) {
        var restrictions = command.restrictions();
        String policyId = savePolicy(new SavePolicyCommand(
                command.tenantId(), command.policyId(), command.policyCode(),
                command.name(), command.validFrom(), command.validTo(),
                restrictions.maximumAssignmentDays(),
                restrictions.maximumRiskLevel(),
                restrictions.requiredAuthenticationStrength(),
                restrictions.requireReason(), restrictions.requireTicket(),
                restrictions.includeInheritedSubjectRoles(),
                restrictions.requireAllAffiliationsInScope(),
                command.subjects().stream()
                        .map(subject -> new Subject(subject.type(), subject.id()))
                        .toList(),
                command.scopes().stream()
                        .map(scope -> new Scope(scope.type(), scope.referenceId()))
                        .toList(),
                command.activationRootRoleIds(), command.operations(),
                command.expectedVersion(), command.actorId()));
        entityManager.flush();
        return view(requirePolicy(
                Long.valueOf(command.tenantId()), Long.valueOf(policyId), false));
    }

    @Override
    @Transactional
    public ManagementPolicyFacade.PolicyView disable(
            String tenantId,
            String policyId,
            long expectedVersion,
            String actorId
    ) {
        ManagementPolicyEntity policy = requirePolicy(
                Long.valueOf(tenantId), Long.valueOf(policyId), true);
        if (policy.getVersion() != expectedVersion) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        try {
            policy.disable(actorId, databaseClock.transactionNow());
        } catch (IllegalStateException invalidState) {
            throw new Rbac3RuleViolation("INVALID_STATE_TRANSITION");
        }
        entityManager.flush();
        return view(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public ManagementPolicyFacade.CapabilityView capabilities(
            String tenantId,
            String subjectUserId,
            Instant databaseNow
    ) {
        List<Long> policyIds = subjectPolicyIds(
                Long.valueOf(tenantId), Long.valueOf(subjectUserId), databaseNow);
        if (policyIds.isEmpty()) {
            return new ManagementPolicyFacade.CapabilityView(
                    List.of(), Set.of(), List.of());
        }
        List<?> operationRows = entityManager.createNativeQuery("""
                        select distinct operation_code
                          from rbac3_management_operation
                         where tenant_id = :tenantId and policy_id in (:policyIds)
                         order by operation_code
                        """)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("policyIds", policyIds)
                .getResultList();
        List<?> roleRows = entityManager.createNativeQuery("""
                        select distinct role_id
                          from rbac3_management_role
                         where tenant_id = :tenantId and policy_id in (:policyIds)
                         order by role_id
                        """)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("policyIds", policyIds)
                .getResultList();
        return new ManagementPolicyFacade.CapabilityView(
                policyIds.stream().map(Object::toString).toList(),
                operationRows.stream().map(Object::toString)
                        .collect(java.util.stream.Collectors.toCollection(
                                LinkedHashSet::new)),
                roleRows.stream().map(Object::toString).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagementPolicyFacade.ManagedUserView> manageableUsers(
            String tenantId,
            String subjectUserId,
            String query,
            Instant databaseNow
    ) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        select id, username, display_name
                          from rbac3_user
                         where tenant_id = :tenantId and status = 'ACTIVE'
                           and (:query = '' or normalized_username like :pattern
                             or lower(display_name) like :pattern)
                         order by normalized_username, id
                         limit 200
                        """)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("query", normalized)
                .setParameter("pattern", '%' + normalized + '%')
                .getResultList();
        return rows.stream()
                .filter(row -> policies(
                        tenantId, subjectUserId, row[0].toString(), databaseNow).stream()
                        .anyMatch(policy -> policy.targetUserIds()
                                .contains(row[0].toString())))
                .limit(50)
                .map(row -> new ManagementPolicyFacade.ManagedUserView(
                        row[0].toString(), row[1].toString(), row[2].toString()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagementPolicyFacade.ManagedRoleView> manageableRoles(
            String tenantId,
            String subjectUserId,
            String query,
            Instant databaseNow
    ) {
        List<Long> policyIds = subjectPolicyIds(
                Long.valueOf(tenantId), Long.valueOf(subjectUserId), databaseNow);
        if (policyIds.isEmpty()) {
            return List.of();
        }
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        select distinct r.id, r.role_code, r.role_name,
                               r.risk_level, r.privileged
                          from rbac3_management_role mr
                          join rbac3_role r
                            on r.tenant_id = mr.tenant_id and r.id = mr.role_id
                         where mr.tenant_id = :tenantId
                           and mr.policy_id in (:policyIds)
                           and r.status = 'ACTIVE'
                           and (:query = '' or lower(r.role_code) like :pattern
                             or lower(r.role_name) like :pattern)
                         order by r.role_code, r.id
                         limit 50
                        """)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("policyIds", policyIds)
                .setParameter("query", normalized)
                .setParameter("pattern", '%' + normalized + '%')
                .getResultList();
        return rows.stream()
                .map(row -> new ManagementPolicyFacade.ManagedRoleView(
                        row[0].toString(), row[1].toString(), row[2].toString(),
                        row[3].toString(), (Boolean) row[4]))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Long> subjectPolicyIds(
            Long tenantId,
            Long subjectUserId,
            Instant databaseNow
    ) {
        return entityManager.createNativeQuery("""
                        select distinct p.id
                          from rbac3_management_policy p
                          join rbac3_management_subject s
                            on s.tenant_id = p.tenant_id and s.policy_id = p.id
                         where p.tenant_id = :tenantId and p.status = 'ACTIVE'
                           and p.valid_from <= :now
                           and (p.valid_to is null or p.valid_to > :now)
                           and (
                               (s.subject_type = 'USER'
                                   and s.subject_id = :subjectUserId)
                               or (s.subject_type = 'POSITION' and exists (
                                   select 1 from rbac3_user_position_snapshot ups
                                    join rbac3_directory_snapshot ds
                                      on ds.tenant_id = ups.tenant_id
                                     and ds.id = ups.snapshot_id
                                   where ups.tenant_id = p.tenant_id
                                     and ups.user_id = :subjectUserId
                                     and ups.position_id = s.subject_id
                                     and ups.status = 'ACTIVE'
                                     and ds.status = 'ACTIVE'
                                     and ups.valid_from <= :now
                                     and (ups.valid_to is null or ups.valid_to > :now)))
                               or (s.subject_type = 'ROLE' and exists (
                                   select 1 from rbac3_user_role_assignment a
                                   where a.tenant_id = p.tenant_id
                                     and a.user_id = :subjectUserId
                                     and a.status = 'ACTIVE'
                                     and a.valid_from <= :now
                                     and (a.valid_to is null or a.valid_to > :now)
                                     and (a.role_id = s.subject_id
                                       or (p.include_inherited_subject_roles and exists (
                                           select 1 from rbac3_role_closure c
                                            where c.tenant_id = a.tenant_id
                                              and c.ancestor_role_id = s.subject_id
                                              and c.descendant_role_id = a.role_id)))))
                           )
                         order by p.id
                        """, Long.class)
                .setParameter("tenantId", tenantId)
                .setParameter("subjectUserId", subjectUserId)
                .setParameter("now", databaseNow)
                .getResultList();
    }

    private ManagementPolicyDecisionService.ManagementPolicyFact fact(
            ManagementPolicyEntity policy,
            String subjectId,
            String targetUserId,
            boolean targetAllowed
    ) {
        Set<String> roles = stringSet(entityManager.createQuery("""
                        select r.roleId from ManagementRoleEntity r
                         where r.tenantId = :tenantId and r.policyId = :policyId
                         order by r.roleId
                        """, Long.class)
                .setParameter("tenantId", policy.getTenantId())
                .setParameter("policyId", policy.getId())
                .getResultList());
        Set<String> operations = entityManager.createQuery("""
                        select o.operation from ManagementOperationEntity o
                         where o.tenantId = :tenantId and o.policyId = :policyId
                         order by o.operation
                        """, ManagementOperationEntity.Operation.class)
                .setParameter("tenantId", policy.getTenantId())
                .setParameter("policyId", policy.getId())
                .getResultList().stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new ManagementPolicyDecisionService.ManagementPolicyFact(
                policy.getId().toString(), Set.of(subjectId),
                targetAllowed ? Set.of(targetUserId) : Set.of(),
                roles, operations,
                new ManagementPolicyDecisionService.Restrictions(
                        policy.getMaximumAssignmentDays() == null
                                ? Integer.MAX_VALUE
                                : policy.getMaximumAssignmentDays(),
                        policy.getMaximumRiskLevel().name(),
                        policy.getRequiredAuthenticationStrength().name(),
                        policy.isRequireReason(), policy.isRequireTicket()),
                policy.getValidFrom(), policy.getValidTo(),
                policy.getStatus() == ManagementPolicyEntity.Status.ACTIVE);
    }

    private boolean targetInScope(
            Long tenantId,
            Long policyId,
            Long subjectUserId,
            Long targetUserId,
            Instant now,
            boolean requireAllAffiliations
    ) {
        Number count = (Number) entityManager.createNativeQuery("""
                        with recursive scope_tree(policy_id, scope_type, root_id, id) as (
                            select s.policy_id, s.scope_type, s.scope_ref_id, ou.id
                              from rbac3_management_scope s
                              join rbac3_org_unit ou
                                on ou.tenant_id = s.tenant_id
                               and ou.id = s.scope_ref_id
                             where s.tenant_id = :tenantId
                               and s.policy_id = :policyId
                               and s.scope_type in ('DEPT_TREE', 'ORG', 'ORG_TREE')
                            union all
                            select tree.policy_id, tree.scope_type, tree.root_id, child.id
                              from scope_tree tree
                              join rbac3_org_unit child
                                on child.tenant_id = :tenantId
                               and child.parent_id = tree.id
                        ), target_affiliations as (
                            select distinct ups.org_unit_id
                              from rbac3_user_position_snapshot ups
                              join rbac3_directory_snapshot ds
                                on ds.tenant_id = ups.tenant_id
                               and ds.id = ups.snapshot_id
                             where ups.tenant_id = :tenantId
                               and ups.user_id = :targetUserId
                               and ups.status = 'ACTIVE'
                               and ds.status = 'ACTIVE'
                               and ups.valid_from <= :now
                               and (ups.valid_to is null or ups.valid_to > :now)
                        ), subject_primary as (
                            select ups.org_unit_id
                              from rbac3_user_position_snapshot ups
                              join rbac3_org_unit ou
                                on ou.tenant_id = ups.tenant_id
                               and ou.id = ups.org_unit_id
                             where ups.tenant_id = :tenantId
                               and ups.user_id = :subjectUserId
                               and ups.status = 'ACTIVE'
                               and ups.primary_flag = true
                               and ou.unit_type = 'DEPT'
                               and ups.valid_from <= :now
                               and (ups.valid_to is null or ups.valid_to > :now)
                             order by ups.id limit 1
                        ), matches as (
                            select ta.org_unit_id,
                                   bool_or(
                                       (s.scope_type = 'CUSTOM_USER'
                                           and s.scope_ref_id = :targetUserId)
                                       or (s.scope_type = 'SELF_DEPT'
                                           and ta.org_unit_id in
                                               (select org_unit_id from subject_primary))
                                       or (s.scope_type in ('DEPT', 'CUSTOM_DEPT')
                                           and s.scope_ref_id = ta.org_unit_id)
                                       or (s.scope_type in
                                               ('DEPT_TREE', 'ORG', 'ORG_TREE')
                                           and ta.org_unit_id in (
                                               select tree.id from scope_tree tree
                                                where tree.policy_id = s.policy_id
                                                  and tree.scope_type = s.scope_type
                                                  and tree.root_id = s.scope_ref_id))) matched
                              from target_affiliations ta
                              cross join rbac3_management_scope s
                             where s.tenant_id = :tenantId
                               and s.policy_id = :policyId
                             group by ta.org_unit_id
                        )
                        select case
                                 when exists (
                                     select 1 from rbac3_management_scope s
                                      where s.tenant_id = :tenantId
                                        and s.policy_id = :policyId
                                        and s.scope_type = 'CUSTOM_USER'
                                        and s.scope_ref_id = :targetUserId)
                                   then 1
                                 when :requireAllAffiliations
                                   then case when exists (select 1 from matches)
                                              and not exists (
                                                  select 1 from matches
                                                   where matched = false)
                                             then 1 else 0 end
                                 else case when exists (
                                     select 1 from matches where matched = true)
                                     then 1 else 0 end
                               end
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("policyId", policyId)
                .setParameter("subjectUserId", subjectUserId)
                .setParameter("targetUserId", targetUserId)
                .setParameter("now", now)
                .setParameter("requireAllAffiliations", requireAllAffiliations)
                .getSingleResult();
        return count.longValue() > 0L;
    }

    private void deleteChildren(Long tenantId, Long policyId) {
        entityManager.createQuery("""
                        delete from ManagementSubjectEntity s
                         where s.tenantId = :tenantId and s.policyId = :policyId
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("policyId", policyId)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        delete from rbac3_management_scope
                         where tenant_id = :tenantId and policy_id = :policyId
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("policyId", policyId)
                .executeUpdate();
        entityManager.createQuery("""
                        delete from ManagementRoleEntity r
                         where r.tenantId = :tenantId and r.policyId = :policyId
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("policyId", policyId)
                .executeUpdate();
        entityManager.createQuery("""
                        delete from ManagementOperationEntity o
                         where o.tenantId = :tenantId and o.policyId = :policyId
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("policyId", policyId)
                .executeUpdate();
    }

    private void insertScope(Long tenantId, Long policyId, Scope scope) {
        entityManager.createNativeQuery("""
                        insert into rbac3_management_scope (
                            tenant_id, policy_id, scope_type, scope_ref_id)
                        values (:tenantId, :policyId, :scopeType, :scopeReferenceId)
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("policyId", policyId)
                .setParameter("scopeType", scope.type())
                .setParameter("scopeReferenceId", scope.referenceId() == null
                        ? null : Long.valueOf(scope.referenceId()))
                .executeUpdate();
    }

    private ManagementPolicyEntity requirePolicy(
            Long tenantId,
            Long policyId,
            boolean forUpdate
    ) {
        ManagementPolicyEntity policy = forUpdate
                ? entityManager.find(
                        ManagementPolicyEntity.class, policyId,
                        LockModeType.PESSIMISTIC_WRITE)
                : entityManager.find(ManagementPolicyEntity.class, policyId);
        if (policy == null || !tenantId.equals(policy.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return policy;
    }

    private ManagementPolicyFacade.PolicyView view(ManagementPolicyEntity policy) {
        @SuppressWarnings("unchecked")
        List<Object[]> subjectRows = entityManager
                .createNativeQuery("""
                        select subject_type, subject_id
                          from rbac3_management_subject
                         where tenant_id = :tenantId and policy_id = :policyId
                         order by subject_type, subject_id
                        """)
                .setParameter("tenantId", policy.getTenantId())
                .setParameter("policyId", policy.getId())
                .getResultList();
        List<ManagementPolicyFacade.Subject> subjects = subjectRows.stream()
                .map(row -> new ManagementPolicyFacade.Subject(
                        row[0].toString(), row[1].toString()))
                .toList();
        @SuppressWarnings("unchecked")
        List<Object[]> scopeRows = entityManager
                .createNativeQuery("""
                        select scope_type, scope_ref_id
                          from rbac3_management_scope
                         where tenant_id = :tenantId and policy_id = :policyId
                         order by scope_type, scope_ref_id nulls first
                        """)
                .setParameter("tenantId", policy.getTenantId())
                .setParameter("policyId", policy.getId())
                .getResultList();
        List<ManagementPolicyFacade.Scope> scopes = scopeRows.stream()
                .map(row -> new ManagementPolicyFacade.Scope(
                        row[0].toString(), row[1] == null ? null : row[1].toString()))
                .toList();
        List<?> roleRows = entityManager.createNativeQuery("""
                        select role_id from rbac3_management_role
                         where tenant_id = :tenantId and policy_id = :policyId
                         order by role_id
                        """)
                .setParameter("tenantId", policy.getTenantId())
                .setParameter("policyId", policy.getId())
                .getResultList();
        List<String> roles = roleRows.stream().map(Object::toString).toList();
        List<?> operationRows = entityManager.createNativeQuery("""
                        select operation_code from rbac3_management_operation
                         where tenant_id = :tenantId and policy_id = :policyId
                         order by operation_code
                        """)
                .setParameter("tenantId", policy.getTenantId())
                .setParameter("policyId", policy.getId())
                .getResultList();
        Set<String> operations = operationRows.stream().map(Object::toString)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new ManagementPolicyFacade.PolicyView(
                policy.getId().toString(), policy.getPolicyCode(), policy.getName(),
                policy.getStatus().name(), policy.getValidFrom(), policy.getValidTo(),
                new ManagementPolicyFacade.Restrictions(
                        policy.getMaximumAssignmentDays(),
                        policy.getMaximumRiskLevel().name(),
                        policy.getRequiredAuthenticationStrength().name(),
                        policy.isRequireReason(), policy.isRequireTicket(),
                        policy.isIncludeInheritedSubjectRoles(),
                        policy.isRequireAllAffiliationsInScope()),
                subjects, scopes, roles, operations, policy.getVersion());
    }

    private Set<String> stringSet(List<Long> values) {
        return values.stream().map(Object::toString)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public record SavePolicyCommand(
            String tenantId,
            String policyId,
            String policyCode,
            String name,
            Instant validFrom,
            Instant validTo,
            Integer maximumAssignmentDays,
            String maximumRiskLevel,
            String requiredAuthenticationStrength,
            boolean requireReason,
            boolean requireTicket,
            boolean includeInheritedSubjectRoles,
            boolean requireAllAffiliationsInScope,
            List<Subject> subjects,
            List<Scope> scopes,
            List<String> activationRootRoleIds,
            Set<String> operations,
            long expectedVersion,
            String actorId
    ) {
        public SavePolicyCommand {
            subjects = List.copyOf(subjects);
            scopes = List.copyOf(scopes);
            activationRootRoleIds = List.copyOf(activationRootRoleIds);
            operations = Set.copyOf(operations);
        }
    }

    public record Subject(String type, String id) {
    }

    public record Scope(String type, String referenceId) {
    }
}
