package top.egon.cola.platform.rbac3.admin.role.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.identity.domain.TenantEntity;
import top.egon.cola.platform.rbac3.admin.resource.domain.PermissionEntity;
import top.egon.cola.platform.rbac3.admin.role.application.RoleFacade;
import top.egon.cola.platform.rbac3.admin.role.domain.RoleEntity;
import top.egon.cola.platform.rbac3.admin.role.domain.RoleInheritanceEntity;
import top.egon.cola.platform.rbac3.admin.role.domain.RolePermissionEntity;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Repository
public class RoleRepository implements RoleFacade.HierarchyStore, RoleFacade.RoleControlStore {

    private final EntityManager entityManager;
    private final PostgresqlRoleClosureStore closureStore;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;
    private final AuthorizationEventPort eventPort;

    public RoleRepository(
            EntityManager entityManager,
            PostgresqlRoleClosureStore closureStore,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock,
            AuthorizationEventPort eventPort) {
        this.entityManager = entityManager;
        this.closureStore = closureStore;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
        this.eventPort = eventPort;
    }

    @Override
    @Transactional
    public <T> T withGraphLock(
            String tenantId,
            String applicationId,
            Function<RoleHierarchy, T> action) {
        long tenant = Long.parseLong(tenantId);
        long application = Long.parseLong(applicationId);
        closureStore.lockGraph(tenant, application);
        List<RoleEntity> roles = entityManager.createQuery("""
                        select r from RoleEntity r
                         where r.tenantId = :tenantId and r.applicationId = :applicationId
                        """, RoleEntity.class)
                .setParameter("tenantId", tenant)
                .setParameter("applicationId", application)
                .getResultList();
        List<RoleEdge> edges = entityManager.createQuery("""
                        select i from RoleInheritanceEntity i
                         where i.tenantId = :tenantId and i.applicationId = :applicationId
                        """, RoleInheritanceEntity.class)
                .setParameter("tenantId", tenant)
                .setParameter("applicationId", application)
                .getResultList()
                .stream()
                .map(edge -> new RoleEdge(
                        edge.getSeniorRoleId().toString(),
                        edge.getJuniorRoleId().toString()))
                .toList();
        return action.apply(new RoleHierarchy(
                roles.stream().map(RoleEntity::toRoleNode).toList(), edges));
    }

    @Override
    public void addEdge(String tenantId, String applicationId, RoleEdge edge) {
        entityManager.persist(new RoleInheritanceEntity(
                idGenerator.nextLongId(),
                Long.valueOf(tenantId),
                Long.valueOf(applicationId),
                Long.valueOf(edge.seniorRoleId()),
                Long.valueOf(edge.juniorRoleId()),
                "role-control-plane",
                databaseClock.transactionNow()));
    }

    @Override
    public void removeEdge(String tenantId, String applicationId, RoleEdge edge) {
        entityManager.createQuery("""
                        delete from RoleInheritanceEntity i
                         where i.tenantId = :tenantId
                           and i.applicationId = :applicationId
                           and i.seniorRoleId = :seniorRoleId
                           and i.juniorRoleId = :juniorRoleId
                        """)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", Long.valueOf(applicationId))
                .setParameter("seniorRoleId", Long.valueOf(edge.seniorRoleId()))
                .setParameter("juniorRoleId", Long.valueOf(edge.juniorRoleId()))
                .executeUpdate();
    }

    @Override
    public void rebuildClosure(String tenantId, String applicationId) {
        closureStore.rebuild(Long.parseLong(tenantId), Long.parseLong(applicationId));
    }

    @Override
    public void assertRoleVersion(String tenantId, String roleId, long expectedRoleVersion) {
        if (expectedRoleVersion < 0L) {
            return;
        }
        RoleEntity role = entityManager.find(
                RoleEntity.class, Long.valueOf(roleId), LockModeType.PESSIMISTIC_WRITE);
        if (role == null || !role.getTenantId().equals(Long.valueOf(tenantId))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        if (role.getVersion() != expectedRoleVersion) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
    }

    @Override
    public void recordGraphMutation(
            String tenantId,
            String applicationId,
            RoleEdge edge,
            boolean added,
            String actorId) {
        policyMutation(
                tenantId,
                "ROLE_INHERITANCE",
                edge.seniorRoleId() + '-' + edge.juniorRoleId(),
                added ? "ROLE_INHERITANCE_ADDED" : "ROLE_INHERITANCE_REMOVED",
                actorId,
                databaseClock.transactionNow());
    }

    @Override
    @Transactional
    public RoleFacade.RoleMutationResult create(
            RoleFacade.CreateRoleCommand command,
            Instant ignoredNow) {
        Instant now = databaseClock.transactionNow();
        long roleId = idGenerator.nextLongId();
        closureStore.lockGraph(
                Long.parseLong(command.tenantId()), Long.parseLong(command.applicationId()));
        RoleEntity role = new RoleEntity(
                roleId,
                Long.valueOf(command.tenantId()),
                Long.valueOf(command.applicationId()),
                command.roleCode(),
                command.roleName(),
                RoleEntity.RoleType.valueOf(command.roleType()),
                RoleEntity.RiskLevel.valueOf(command.riskLevel()),
                command.privileged(),
                command.landingRouteId() == null
                        ? null : Long.valueOf(command.landingRouteId()),
                command.landingPriority(),
                command.maximumAssignmentDays(),
                command.actorId(),
                now);
        entityManager.persist(role);
        entityManager.flush();
        closureStore.rebuild(
                Long.parseLong(command.tenantId()), Long.parseLong(command.applicationId()));
        return policyMutation(
                command.tenantId(),
                "ROLE",
                Long.toString(roleId),
                "ROLE_CREATED",
                command.actorId(),
                now);
    }

    @Override
    @Transactional
    public RoleFacade.RoleMutationResult assignPermission(
            RoleFacade.AssignPermissionCommand command,
            Instant ignoredNow) {
        Instant now = databaseClock.transactionNow();
        RoleEntity role = entityManager.find(
                RoleEntity.class, Long.valueOf(command.roleId()), LockModeType.PESSIMISTIC_WRITE);
        PermissionEntity permission = entityManager.find(
                PermissionEntity.class,
                Long.valueOf(command.permissionId()),
                LockModeType.PESSIMISTIC_WRITE);
        Long applicationId = Long.valueOf(command.applicationId());
        Long tenantId = Long.valueOf(command.tenantId());
        if (role == null || permission == null
                || !role.getTenantId().equals(tenantId)
                || !permission.getTenantId().equals(tenantId)
                || !role.getApplicationId().equals(applicationId)
                || !permission.getApplicationId().equals(applicationId)) {
            throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
        }
        long assignmentId = idGenerator.nextLongId();
        entityManager.persist(new RolePermissionEntity(
                assignmentId,
                tenantId,
                applicationId,
                role.getId(),
                permission.getId(),
                command.validFrom(),
                command.validTo(),
                command.actorId(),
                now));
        return policyMutation(
                command.tenantId(),
                "ROLE_PERMISSION",
                Long.toString(assignmentId),
                "ROLE_PERMISSION_ASSIGNED",
                command.actorId(),
                now);
    }

    @Override
    @Transactional
    public RoleFacade.RoleMutationResult assignPermissions(
            RoleFacade.AssignPermissionsCommand command,
            Instant ignoredNow) {
        Instant now = databaseClock.transactionNow();
        Long tenantId = Long.valueOf(command.tenantId());
        Long applicationId = Long.valueOf(command.applicationId());
        RoleEntity role = entityManager.find(
                RoleEntity.class, Long.valueOf(command.roleId()), LockModeType.PESSIMISTIC_WRITE);
        if (role == null
                || !role.getTenantId().equals(tenantId)
                || !role.getApplicationId().equals(applicationId)) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        if (role.getVersion() != command.expectedRoleVersion()) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        List<Long> permissionIds = command.permissionIds().stream().map(Long::valueOf).toList();
        List<PermissionEntity> permissions = entityManager.createQuery("""
                        select p from PermissionEntity p
                         where p.tenantId = :tenantId
                           and p.applicationId = :applicationId
                           and p.id in :permissionIds
                        """, PermissionEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationId", applicationId)
                .setParameter("permissionIds", permissionIds)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (permissions.size() != permissionIds.size()
                || permissions.stream().anyMatch(permission ->
                permission.getStatus() != PermissionEntity.Status.ACTIVE)) {
            throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
        }
        Set<Long> existing = new LinkedHashSet<>(entityManager.createQuery("""
                        select rp.permissionId from RolePermissionEntity rp
                         where rp.tenantId = :tenantId
                           and rp.roleId = :roleId
                           and rp.permissionId in :permissionIds
                           and rp.status = :status
                        """, Long.class)
                .setParameter("tenantId", tenantId)
                .setParameter("roleId", role.getId())
                .setParameter("permissionIds", permissionIds)
                .setParameter("status", RolePermissionEntity.Status.ACTIVE)
                .getResultList());
        if (!existing.isEmpty()) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        for (Long permissionId : permissionIds) {
            entityManager.persist(new RolePermissionEntity(
                    idGenerator.nextLongId(),
                    tenantId,
                    applicationId,
                    role.getId(),
                    permissionId,
                    command.validFrom(),
                    command.validTo(),
                    command.actorId(),
                    now));
        }
        role.markUpdated(command.actorId(), now);
        return policyMutation(
                command.tenantId(),
                "ROLE_PERMISSION",
                command.roleId(),
                "ROLE_PERMISSIONS_ASSIGNED",
                command.actorId(),
                now);
    }

    @Override
    @Transactional
    public RoleFacade.RoleMutationResult removePermission(
            RoleFacade.RemovePermissionCommand command,
            Instant ignoredNow) {
        Instant now = databaseClock.transactionNow();
        RoleEntity role = requireRole(
                command.tenantId(), command.applicationId(), command.roleId());
        if (role.getVersion() != command.expectedRoleVersion()) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        List<RolePermissionEntity> bindings = entityManager.createQuery("""
                        select rp from RolePermissionEntity rp
                         where rp.tenantId = :tenantId
                           and rp.roleId = :roleId
                           and rp.permissionId = :permissionId
                           and rp.status = :status
                        """, RolePermissionEntity.class)
                .setParameter("tenantId", Long.valueOf(command.tenantId()))
                .setParameter("roleId", role.getId())
                .setParameter("permissionId", Long.valueOf(command.permissionId()))
                .setParameter("status", RolePermissionEntity.Status.ACTIVE)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (bindings.isEmpty()) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        bindings.forEach(binding -> binding.disable(command.actorId(), now));
        role.markUpdated(command.actorId(), now);
        return policyMutation(
                command.tenantId(),
                "ROLE_PERMISSION",
                command.roleId(),
                "ROLE_PERMISSION_REMOVED",
                command.actorId(),
                now);
    }

    @Override
    @Transactional
    public RoleFacade.RoleMutationResult update(
            RoleFacade.UpdateRoleCommand command,
            Instant ignoredNow) {
        Instant now = databaseClock.transactionNow();
        RoleEntity role = entityManager.find(
                RoleEntity.class, Long.valueOf(command.roleId()), LockModeType.PESSIMISTIC_WRITE);
        if (role == null || !role.getTenantId().equals(Long.valueOf(command.tenantId()))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        if (role.getVersion() != command.expectedRoleVersion()) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        role.update(
                command.roleName(),
                RoleEntity.Status.valueOf(command.status()),
                command.landingRouteId() == null ? null : Long.valueOf(command.landingRouteId()),
                command.landingPriority(),
                command.maximumAssignmentDays(),
                command.actorId(),
                now);
        return policyMutation(
                command.tenantId(),
                "ROLE",
                command.roleId(),
                "ROLE_UPDATED",
                command.actorId(),
                now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleFacade.RoleView> roles(String tenantId, String applicationId) {
        String hql = applicationId == null
                ? "select r from RoleEntity r where r.tenantId = :tenantId order by r.roleCode"
                : """
                    select r from RoleEntity r
                     where r.tenantId = :tenantId and r.applicationId = :applicationId
                     order by r.roleCode
                    """;
        var query = entityManager.createQuery(hql, RoleEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId));
        if (applicationId != null) {
            query.setParameter("applicationId", Long.valueOf(applicationId));
        }
        return query.getResultList().stream().map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleFacade.RoleImpactView impact(String tenantId, String roleId) {
        RoleEntity role = entityManager.find(RoleEntity.class, Long.valueOf(roleId));
        if (role == null || !role.getTenantId().equals(Long.valueOf(tenantId))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        List<String> roots = entityManager.createQuery("""
                        select c.ancestorRoleId from RoleClosureEntity c
                         where c.tenantId = :tenantId
                           and c.applicationId = :applicationId
                           and c.descendantRoleId = :roleId
                           and not exists (
                               select 1 from RoleInheritanceEntity i
                                where i.tenantId = c.tenantId
                                  and i.applicationId = c.applicationId
                                  and i.juniorRoleId = c.ancestorRoleId)
                        """, Long.class)
                .setParameter("tenantId", role.getTenantId())
                .setParameter("applicationId", role.getApplicationId())
                .setParameter("roleId", role.getId())
                .getResultList().stream().map(String::valueOf).toList();
        List<RoleEntity> family = entityManager.createQuery("""
                        select r from RoleEntity r
                         where r.tenantId = :tenantId
                           and r.applicationId = :applicationId
                           and r.id in (
                               select c.descendantRoleId from RoleClosureEntity c
                                where c.tenantId = :tenantId
                                  and c.applicationId = :applicationId
                                  and c.ancestorRoleId = :roleId)
                        """, RoleEntity.class)
                .setParameter("tenantId", role.getTenantId())
                .setParameter("applicationId", role.getApplicationId())
                .setParameter("roleId", roots.isEmpty() ? role.getId() : Long.valueOf(roots.getFirst()))
                .getResultList();
        String risk = family.stream()
                .map(RoleEntity::getRiskLevel)
                .max(java.util.Comparator.naturalOrder())
                .orElse(role.getRiskLevel())
                .name();
        long permissions = entityManager.createQuery("""
                        select count(distinct rp.permissionId) from RolePermissionEntity rp
                         where rp.tenantId = :tenantId
                           and rp.roleId in :roleIds
                           and rp.status = :status
                        """, Long.class)
                .setParameter("tenantId", role.getTenantId())
                .setParameter("roleIds", family.stream().map(RoleEntity::getId).toList())
                .setParameter("status", RolePermissionEntity.Status.ACTIVE)
                .getSingleResult();
        return new RoleFacade.RoleImpactView(
                roleId,
                roots,
                family.stream().map(value -> value.getId().toString()).sorted().toList(),
                risk,
                permissions,
                roots.size() == 1 ? List.of() : List.of("ROLE_ACTIVATION_ROOT_AMBIGUOUS"));
    }

    private RoleEntity requireRole(String tenantId, String applicationId, String roleId) {
        RoleEntity role = entityManager.find(
                RoleEntity.class, Long.valueOf(roleId), LockModeType.PESSIMISTIC_WRITE);
        if (role == null
                || !role.getTenantId().equals(Long.valueOf(tenantId))
                || !role.getApplicationId().equals(Long.valueOf(applicationId))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return role;
    }

    private RoleFacade.RoleView toView(RoleEntity role) {
        return new RoleFacade.RoleView(
                role.getId().toString(),
                role.getApplicationId().toString(),
                role.getRoleCode(),
                role.getRoleName(),
                role.getRoleType().name(),
                role.getRiskLevel().name(),
                role.isPrivileged(),
                role.getStatus().name(),
                role.getVersion());
    }

    private RoleFacade.RoleMutationResult policyMutation(
            String tenantId,
            String aggregateType,
            String aggregateId,
            String eventType,
            String actorId,
            Instant now) {
        TenantEntity tenant = entityManager.find(
                TenantEntity.class, Long.valueOf(tenantId), LockModeType.PESSIMISTIC_WRITE);
        if (tenant == null) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        tenant.incrementPolicyVersion(actorId, now);
        String propagationId = eventPort.enqueue(new AuthorizationEventPort.AuthorizationEvent(
                tenantId,
                aggregateType,
                aggregateId,
                eventType,
                Map.of("policyVersion", Long.toString(tenant.getPolicyVersion())),
                eventType.toLowerCase(java.util.Locale.ROOT) + '-' + aggregateId));
        return new RoleFacade.RoleMutationResult(
                aggregateId,
                tenant.getPolicyVersion(),
                propagationId,
                true);
    }
}
