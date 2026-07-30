package top.egon.cola.platform.rbac3.admin.assignment.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.assignment.application.AssignmentFacade;
import top.egon.cola.platform.rbac3.admin.assignment.domain.UserRoleAssignmentEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserEntity;
import top.egon.cola.platform.rbac3.admin.role.domain.RoleEntity;
import top.egon.cola.platform.rbac3.core.constraint.PrerequisiteRoleSpecification;
import top.egon.cola.platform.rbac3.core.constraint.SsdSpecification;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class AssignmentRepository implements
        AssignmentFacade.AssignmentFactSource,
        AssignmentFacade.AssignmentStore {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;

    public AssignmentRepository(
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
    public AssignmentFacade.AssignmentFacts load(
            AssignmentFacade.AssignRequest request
    ) {
        Long tenantId = Long.valueOf(request.tenantId());
        Long roleId = Long.valueOf(request.roleId());
        UserEntity user = entityManager.find(
                UserEntity.class, Long.valueOf(request.targetUserId()));
        if (user == null || !tenantId.equals(user.getTenantId())
                || user.getStatus() != UserEntity.Status.ACTIVE) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        RoleEntity role = entityManager.find(RoleEntity.class, roleId);
        if (role == null || !tenantId.equals(role.getTenantId())
                || role.getStatus() != RoleEntity.Status.ACTIVE) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        String rootId = uniqueRoot(tenantId, role.getApplicationId(), roleId);
        Set<String> currentRoles = activeRoles(
                tenantId, Long.valueOf(request.targetUserId()), request.databaseNow());
        List<SsdSpecification.SsdSet> ssdSets = ssdSets(tenantId, request.databaseNow());
        List<PrerequisiteRoleSpecification.PrerequisiteGroup> prerequisites =
                prerequisites(tenantId, roleId);
        AssignmentFacade.Cardinality cardinality = cardinality(
                tenantId, Long.valueOf(rootId),
                Long.valueOf(request.targetUserId()), request.databaseNow());
        return new AssignmentFacade.AssignmentFacts(
                rootId, role.getRiskLevel().name(), role.isPrivileged(),
                role.getRoleType().name(), role.getMaximumAssignmentDays(),
                currentRoles, ssdSets,
                prerequisites, cardinality);
    }

    @Override
    @Transactional
    public String assign(AssignmentFacade.AssignmentCommand command) {
        var request = command.request();
        Instant now = databaseClock.transactionNow();
        requireNoOverlap(request);
        Long id = idGenerator.nextLongId();
        UserRoleAssignmentEntity assignment = new UserRoleAssignmentEntity(
                id,
                Long.valueOf(request.tenantId()),
                Long.valueOf(request.targetUserId()),
                Long.valueOf(request.roleId()),
                UserRoleAssignmentEntity.AssignmentType.valueOf(request.assignmentType()),
                request.validFrom(), request.validTo(), "MANAGEMENT_POLICY",
                command.managementPolicyId(), request.reason(), request.ticketNo(),
                request.actorId(), now);
        entityManager.persist(assignment);
        if (assignment.getStatus() == UserRoleAssignmentEntity.Status.ACTIVE) {
            advanceUserAuthorizationVersion(
                    request.tenantId(), request.targetUserId(),
                    request.expectedUserAuthVersion(), request.actorId(), now);
        }
        return id.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentFacade.AssignmentChangeFacts loadChange(
            AssignmentFacade.ChangeRequest request
    ) {
        UserRoleAssignmentEntity assignment = requireAssignment(
                request.tenantId(), request.assignmentId(), null);
        RoleEntity role = entityManager.find(RoleEntity.class, assignment.getRoleId());
        if (role == null || !role.getTenantId().equals(assignment.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return new AssignmentFacade.AssignmentChangeFacts(
                uniqueRoot(role.getTenantId(), role.getApplicationId(), role.getId()),
                role.getRiskLevel().name(), role.isPrivileged());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentFacade.AssignmentView> assignments(
            String tenantId,
            String userId,
            Instant databaseNow
    ) {
        return entityManager.createQuery("""
                        select a from UserRoleAssignmentEntity a
                         where a.tenantId = :tenantId and a.userId = :userId
                         order by a.validFrom desc, a.id desc
                        """, UserRoleAssignmentEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("userId", Long.valueOf(userId))
                .getResultList().stream()
                .map(assignment -> new AssignmentFacade.AssignmentView(
                        assignment.getId().toString(), assignment.getRoleId().toString(),
                        assignment.getAssignmentType().name(),
                        effectiveStatus(assignment, databaseNow),
                        assignment.getValidFrom(), assignment.getValidTo(),
                        assignment.getSourceType(), assignment.getSourceId(),
                        assignment.getVersion()))
                .toList();
    }

    @Override
    @Transactional
    public String change(AssignmentFacade.ChangeRequest request) {
        UserRoleAssignmentEntity assignment = requireAssignment(
                request.tenantId(), request.assignmentId(),
                request.expectedAssignmentVersion());
        if (!assignment.getUserId().equals(Long.valueOf(request.targetUserId()))) {
            throw new Rbac3RuleViolation("MANAGED_USER_SCOPE_DENIED");
        }
        Instant now = databaseClock.transactionNow();
        try {
            switch (request.operation()) {
                case REVOKE -> assignment.revoke(request.actorId(), now);
                case SUSPEND -> assignment.suspend(request.actorId(), now);
                case RESUME -> assignment.resume(request.actorId(), now);
            }
        } catch (IllegalStateException invalidState) {
            throw new Rbac3RuleViolation("INVALID_STATE_TRANSITION");
        }
        advanceUserAuthorizationVersion(
                request.tenantId(), request.targetUserId(),
                request.expectedUserAuthVersion(), request.actorId(), now);
        return assignment.getId().toString();
    }

    private void requireNoOverlap(AssignmentFacade.AssignRequest request) {
        Number overlaps = (Number) entityManager.createNativeQuery("""
                        select count(*) from rbac3_user_role_assignment a
                         where a.tenant_id = :tenantId
                           and a.user_id = :userId and a.role_id = :roleId
                           and a.status in ('PENDING', 'ACTIVE', 'SUSPENDED')
                           and a.valid_from < coalesce(:validTo, 'infinity'::timestamptz)
                           and coalesce(a.valid_to, 'infinity'::timestamptz) > :validFrom
                        """)
                .setParameter("tenantId", Long.valueOf(request.tenantId()))
                .setParameter("userId", Long.valueOf(request.targetUserId()))
                .setParameter("roleId", Long.valueOf(request.roleId()))
                .setParameter("validFrom", request.validFrom())
                .setParameter("validTo", request.validTo())
                .getSingleResult();
        if (overlaps.longValue() > 0L) {
            throw new Rbac3RuleViolation("ASSIGNMENT_TIME_OVERLAP");
        }
    }

    private void advanceUserAuthorizationVersion(
            String tenantId,
            String userId,
            long expectedVersion,
            String actorId,
            Instant now
    ) {
        UserEntity user = entityManager.find(
                UserEntity.class, Long.valueOf(userId), LockModeType.PESSIMISTIC_WRITE);
        if (user == null || !Long.valueOf(tenantId).equals(user.getTenantId())
                || user.getStatus() != UserEntity.Status.ACTIVE) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        try {
            user.advanceAuthorizationVersion(expectedVersion, actorId, now);
        } catch (IllegalStateException conflict) {
            throw new Rbac3RuleViolation("AUTH_MUTATION_CONFLICT");
        }
    }

    @Transactional
    public void revoke(
            String tenantId,
            String assignmentId,
            long expectedVersion,
            String actorId
    ) {
        UserRoleAssignmentEntity assignment = entityManager.find(
                UserRoleAssignmentEntity.class, Long.valueOf(assignmentId),
                LockModeType.PESSIMISTIC_WRITE);
        if (assignment == null
                || !Long.valueOf(tenantId).equals(assignment.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        if (assignment.getVersion() != expectedVersion) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        assignment.revoke(actorId, databaseClock.transactionNow());
    }

    private UserRoleAssignmentEntity requireAssignment(
            String tenantId,
            String assignmentId,
            Long expectedVersion
    ) {
        UserRoleAssignmentEntity assignment = expectedVersion == null
                ? entityManager.find(
                        UserRoleAssignmentEntity.class, Long.valueOf(assignmentId))
                : entityManager.find(
                        UserRoleAssignmentEntity.class, Long.valueOf(assignmentId),
                        LockModeType.PESSIMISTIC_WRITE);
        if (assignment == null
                || !Long.valueOf(tenantId).equals(assignment.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        if (expectedVersion != null
                && assignment.getVersion() != expectedVersion) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        return assignment;
    }

    private String effectiveStatus(
            UserRoleAssignmentEntity assignment,
            Instant now
    ) {
        if (assignment.getStatus() == UserRoleAssignmentEntity.Status.ACTIVE
                && assignment.getValidTo() != null
                && !assignment.getValidTo().isAfter(now)) {
            return UserRoleAssignmentEntity.Status.EXPIRED.name();
        }
        return assignment.getStatus().name();
    }

    private String uniqueRoot(Long tenantId, Long applicationId, Long roleId) {
        List<?> roots = entityManager.createNativeQuery("""
                        select c.ancestor_role_id
                          from rbac3_role_closure c
                         where c.tenant_id = :tenantId
                           and c.application_id = :applicationId
                           and c.descendant_role_id = :roleId
                           and not exists (
                               select 1 from rbac3_role_inheritance i
                                where i.tenant_id = c.tenant_id
                                  and i.application_id = c.application_id
                                  and i.junior_role_id = c.ancestor_role_id)
                         order by c.ancestor_role_id
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationId", applicationId)
                .setParameter("roleId", roleId)
                .getResultList();
        if (roots.size() != 1) {
            throw new Rbac3RuleViolation(
                    roots.isEmpty()
                            ? "ROLE_ACTIVATION_REQUIRED"
                            : "ROLE_ACTIVATION_ROOT_AMBIGUOUS");
        }
        return roots.getFirst().toString();
    }

    private Set<String> activeRoles(Long tenantId, Long userId, Instant now) {
        return entityManager.createQuery("""
                        select a.roleId from UserRoleAssignmentEntity a
                         where a.tenantId = :tenantId and a.userId = :userId
                           and a.status = :status and a.validFrom <= :now
                           and (a.validTo is null or a.validTo > :now)
                         order by a.roleId
                        """, Long.class)
                .setParameter("tenantId", tenantId)
                .setParameter("userId", userId)
                .setParameter("status", UserRoleAssignmentEntity.Status.ACTIVE)
                .setParameter("now", now)
                .getResultList().stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private List<SsdSpecification.SsdSet> ssdSets(Long tenantId, Instant now) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        select s.id, s.max_active_roles, m.role_id
                          from rbac3_sod_set s
                          join rbac3_sod_member m
                            on m.tenant_id = s.tenant_id and m.sod_set_id = s.id
                         where s.tenant_id = :tenantId
                           and s.constraint_type = 'SSD' and s.status = 'ACTIVE'
                           and s.valid_from <= :now
                           and (s.valid_to is null or s.valid_to > :now)
                         order by s.id, m.role_id
                        """, Object[].class)
                .setParameter("tenantId", tenantId)
                .setParameter("now", now)
                .getResultList();
        Map<String, Set<String>> roles = new LinkedHashMap<>();
        Map<String, Integer> maximums = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String id = row[0].toString();
            maximums.put(id, ((Number) row[1]).intValue());
            roles.computeIfAbsent(id, ignored -> new LinkedHashSet<>())
                    .add(row[2].toString());
        }
        return roles.entrySet().stream()
                .map(entry -> new SsdSpecification.SsdSet(
                        entry.getKey(), maximums.get(entry.getKey()), entry.getValue()))
                .toList();
    }

    private List<PrerequisiteRoleSpecification.PrerequisiteGroup> prerequisites(
            Long tenantId,
            Long roleId
    ) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        select p.group_code, p.match_mode, p.prerequisite_role_id
                          from rbac3_role_prerequisite p
                         where p.tenant_id = :tenantId
                           and p.target_role_id = :roleId and p.status = 'ACTIVE'
                         order by p.group_code, p.prerequisite_role_id
                        """, Object[].class)
                .setParameter("tenantId", tenantId)
                .setParameter("roleId", roleId)
                .getResultList();
        Map<String, String> modes = new LinkedHashMap<>();
        Map<String, Set<String>> required = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String group = row[0].toString();
            modes.put(group, row[1].toString());
            required.computeIfAbsent(group, ignored -> new LinkedHashSet<>())
                    .add(row[2].toString());
        }
        return required.entrySet().stream()
                .map(entry -> new PrerequisiteRoleSpecification.PrerequisiteGroup(
                        entry.getKey(),
                        PrerequisiteRoleSpecification.MatchMode.valueOf(
                                modes.get(entry.getKey())),
                        entry.getValue()))
                .toList();
    }

    private AssignmentFacade.Cardinality cardinality(
            Long tenantId,
            Long rootRoleId,
            Long targetUserId,
            Instant now
    ) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        select c.scope_type, c.max_active
                          from rbac3_role_cardinality c
                         where c.tenant_id = :tenantId and c.role_id = :roleId
                           and c.status = 'ACTIVE' and c.valid_from <= :now
                           and (c.valid_to is null or c.valid_to > :now)
                         order by c.valid_from desc limit 1
                        """, Object[].class)
                .setParameter("tenantId", tenantId)
                .setParameter("roleId", rootRoleId)
                .setParameter("now", now)
                .getResultList();
        if (rows.isEmpty()) {
            return new AssignmentFacade.Cardinality(
                    "TENANT", tenantId.toString(), Long.MAX_VALUE, 0L);
        }
        String scopeType = rows.getFirst()[0].toString();
        long maximum = ((Number) rows.getFirst()[1]).longValue();
        String scopeId = resolveScopeId(tenantId, targetUserId, scopeType, now);
        long active = activeAssignments(
                tenantId, rootRoleId, scopeType, scopeId, now);
        return new AssignmentFacade.Cardinality(scopeType, scopeId, maximum, active);
    }

    private long activeAssignments(
            Long tenantId,
            Long rootRoleId,
            String scopeType,
            String scopeId,
            Instant now
    ) {
        String scopePredicate = switch (scopeType) {
            case "TENANT" -> "";
            case "DEPT" -> """
                    and exists (
                        select 1 from rbac3_user_position_snapshot ups
                         where ups.tenant_id = a.tenant_id
                           and ups.user_id = a.user_id
                           and ups.org_unit_id = :scopeId
                           and ups.status = 'ACTIVE'
                           and ups.valid_from <= :now
                           and (ups.valid_to is null or ups.valid_to > :now))
                    """;
            case "ORG" -> """
                    and exists (
                        with recursive scope_units(id) as (
                            select ou.id from rbac3_org_unit ou
                             where ou.tenant_id = :tenantId and ou.id = :scopeId
                            union all
                            select child.id from rbac3_org_unit child
                            join scope_units parent on child.parent_id = parent.id
                             where child.tenant_id = :tenantId
                        )
                        select 1 from rbac3_user_position_snapshot ups
                         where ups.tenant_id = a.tenant_id
                           and ups.user_id = a.user_id
                           and ups.org_unit_id in (select id from scope_units)
                           and ups.status = 'ACTIVE'
                           and ups.valid_from <= :now
                           and (ups.valid_to is null or ups.valid_to > :now))
                    """;
            default -> throw new Rbac3RuleViolation("REQUEST_INVALID");
        };
        var query = entityManager.createNativeQuery("""
                        select count(distinct a.id)
                          from rbac3_user_role_assignment a
                          join rbac3_role_closure c
                            on c.tenant_id = a.tenant_id
                           and c.descendant_role_id = a.role_id
                         where a.tenant_id = :tenantId
                           and c.ancestor_role_id = :rootRoleId
                           and a.status = 'ACTIVE' and a.valid_from <= :now
                           and (a.valid_to is null or a.valid_to > :now)
                        """ + scopePredicate)
                .setParameter("tenantId", tenantId)
                .setParameter("rootRoleId", rootRoleId)
                .setParameter("now", now);
        if (!"TENANT".equals(scopeType)) {
            query.setParameter("scopeId", Long.valueOf(scopeId));
        }
        return ((Number) query.getSingleResult()).longValue();
    }

    private String resolveScopeId(
            Long tenantId,
            Long userId,
            String scopeType,
            Instant now
    ) {
        if ("TENANT".equals(scopeType)) {
            return tenantId.toString();
        }
        if (!Set.of("ORG", "DEPT").contains(scopeType)) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        String typePredicate = "DEPT".equals(scopeType)
                ? "and ou.unit_type = 'DEPT'"
                : "and ou.unit_type = 'ORG'";
        String traversal = "DEPT".equals(scopeType)
                ? "select ou.id, ou.parent_id, ou.depth, ou.unit_type "
                    + "from rbac3_org_unit ou "
                    + "where ou.tenant_id = :tenantId and ou.id = primary_scope.org_unit_id"
                : """
                    with recursive ancestors(id, parent_id, depth, unit_type) as (
                        select ou.id, ou.parent_id, ou.depth, ou.unit_type
                          from rbac3_org_unit ou
                         where ou.tenant_id = :tenantId
                           and ou.id = primary_scope.org_unit_id
                        union all
                        select parent.id, parent.parent_id, parent.depth,
                               parent.unit_type
                          from rbac3_org_unit parent
                          join ancestors child on child.parent_id = parent.id
                         where parent.tenant_id = :tenantId
                    ) select * from ancestors
                    """;
        String sql = """
                        with primary_scope as (
                            select ups.org_unit_id
                              from rbac3_user_position_snapshot ups
                              join rbac3_directory_snapshot ds
                                on ds.tenant_id = ups.tenant_id
                               and ds.id = ups.snapshot_id
                             where ups.tenant_id = :tenantId
                               and ups.user_id = :userId
                               and ups.primary_flag = true
                               and ups.status = 'ACTIVE'
                               and ds.status = 'ACTIVE'
                               and ups.valid_from <= :now
                               and (ups.valid_to is null or ups.valid_to > :now)
                             order by ups.id limit 1
                        )
                        select ou.id from primary_scope,
                        lateral (
                        """ + traversal + """
                        ) ou
                         where 1 = 1
                        """ + typePredicate + """
                         order by ou.depth desc limit 1
                        """;
        List<?> ids = entityManager.createNativeQuery(sql)
                .setParameter("tenantId", tenantId)
                .setParameter("userId", userId)
                .setParameter("now", now)
                .getResultList();
        if (ids.size() != 1) {
            throw new Rbac3RuleViolation("MANAGED_USER_SCOPE_DENIED");
        }
        return ids.getFirst().toString();
    }
}
