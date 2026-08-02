package top.egon.cola.platform.rbac3.admin.bootstrap.infrastructure;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.assignment.domain.UserRoleAssignmentEntity;
import top.egon.cola.platform.rbac3.admin.bootstrap.application.Rbac3DevelopmentBootstrap;
import top.egon.cola.platform.rbac3.admin.bootstrap.application.Rbac3DevelopmentTopology;
import top.egon.cola.platform.rbac3.admin.identity.domain.ExternalIdentityEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.TenantEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserEntity;
import top.egon.cola.platform.rbac3.admin.resource.domain.ApplicationEntity;
import top.egon.cola.platform.rbac3.admin.resource.domain.PermissionEntity;
import top.egon.cola.platform.rbac3.admin.role.domain.RoleEntity;
import top.egon.cola.platform.rbac3.admin.role.domain.RolePermissionEntity;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** PostgreSQL-backed, idempotent local topology bootstrap guarded by an advisory lock. */
@Repository
public class PostgresqlDevelopmentTopologyBootstrapStore
        implements Rbac3DevelopmentBootstrap.BootstrapPort {

    private static final long BOOTSTRAP_LOCK_KEY = 0x5242414333494450L;
    private static final String ACTOR = "rbac3-development-bootstrap";

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final Clock clock;

    public PostgresqlDevelopmentTopologyBootstrapStore(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            Clock clock) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public void bootstrap(String tenantCode, String username, String identitySub) {
        acquireLock();
        Instant now = clock.instant();
        String normalizedTenantCode = normalize(tenantCode);
        TenantEntity tenant = findTenant(normalizedTenantCode);
        boolean changed = false;
        if (tenant == null) {
            tenant = new TenantEntity(
                    idGenerator.nextLongId(),
                    normalizedTenantCode,
                    "Development " + normalizedTenantCode,
                    ACTOR,
                    now
            );
            tenant.configure(
                    Map.of("builtInApplicationCode", "rbac3-admin"),
                    ACTOR,
                    now
            );
            tenant.activate(ACTOR, now);
            entityManager.persist(tenant);
            changed = true;
        }
        String normalizedUsername = UserEntity.normalize(username);
        UserEntity user = findUser(tenant.getId(), normalizedUsername);
        if (user == null) {
            user = new UserEntity(
                    idGenerator.nextLongId(),
                    tenant.getId(),
                    username.trim(),
                    username.trim(),
                    ACTOR,
                    now
            );
            entityManager.persist(user);
            changed = true;
        }
        changed |= ensureIdentityMapping(
                tenant.getId(), user.getId(), identitySub.trim(), now);

        for (var definition : Rbac3DevelopmentTopology.applications()) {
            changed |= ensureApplication(tenant.getId(), user.getId(), definition, now);
        }
        if (changed) {
            tenant.incrementPolicyVersion(ACTOR, now);
            user.advanceAuthorizationVersion(user.getAuthVersion(), ACTOR, now);
        }
        entityManager.flush();
    }

    private void acquireLock() {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", BOOTSTRAP_LOCK_KEY)
                .getSingleResult();
    }

    private TenantEntity findTenant(String tenantCode) {
        return singleOrNull(entityManager.createQuery("""
                        select tenant from TenantEntity tenant
                         where lower(tenant.code) = :tenantCode
                        """, TenantEntity.class)
                .setParameter("tenantCode", tenantCode)
                .getResultList(), "tenant");
    }

    private UserEntity findUser(Long tenantId, String normalizedUsername) {
        return singleOrNull(entityManager.createQuery("""
                        select user from UserEntity user
                         where user.tenantId = :tenantId
                           and user.normalizedUsername = :username
                        """, UserEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("username", normalizedUsername)
                .getResultList(), "user");
    }

    private boolean ensureIdentityMapping(
            Long tenantId,
            Long userId,
            String identitySub,
            Instant now) {
        List<ExternalIdentityEntity> mappings = entityManager.createQuery("""
                        select identity from ExternalIdentityEntity identity
                         where identity.tenantId = :tenantId
                           and identity.providerCode = 'IDP'
                           and identity.externalSubjectId = :identitySub
                        """, ExternalIdentityEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("identitySub", identitySub)
                .getResultList();
        if (!mappings.isEmpty()) {
            if (mappings.size() != 1 || !mappings.getFirst().getUserId().equals(userId)) {
                throw new IllegalStateException(
                        "IdP subject is already bound to another RBAC3 user");
            }
            return false;
        }
        entityManager.persist(ExternalIdentityEntity.idpMapping(
                idGenerator.nextLongId(), tenantId, identitySub, userId, ACTOR, now));
        return true;
    }

    private boolean ensureApplication(
            Long tenantId,
            Long userId,
            Rbac3DevelopmentTopology.ApplicationDefinition definition,
            Instant now) {
        ApplicationEntity application = findApplication(
                tenantId, definition.applicationCode());
        boolean changed = false;
        if (application == null) {
            application = new ApplicationEntity(
                    idGenerator.nextLongId(), tenantId,
                    definition.applicationCode(), definition.applicationName(),
                    definition.displayPriority(), ACTOR, now);
            entityManager.persist(application);
            changed = true;
        }
        RoleEntity role = findRole(
                tenantId, application.getId(), definition.roleCode());
        if (role == null) {
            role = new RoleEntity(
                    idGenerator.nextLongId(), tenantId, application.getId(),
                    definition.roleCode(), definition.applicationName() + " Administrator",
                    RoleEntity.RoleType.MANAGEMENT, RoleEntity.RiskLevel.MEDIUM,
                    false, null, 0, null, ACTOR, now);
            entityManager.persist(role);
            entityManager.flush();
            insertSelfClosure(tenantId, application.getId(), role.getId());
            changed = true;
        }
        for (String permissionCode : definition.permissions()) {
            changed |= ensurePermission(
                    tenantId, application.getId(), role.getId(), permissionCode, now);
        }
        if (!hasAssignment(tenantId, userId, role.getId())) {
            entityManager.persist(new UserRoleAssignmentEntity(
                    idGenerator.nextLongId(), tenantId, userId, role.getId(),
                    UserRoleAssignmentEntity.AssignmentType.DIRECT, now, null,
                    "DEVELOPMENT", definition.applicationCode(),
                    "Unified identity local administrator", null, ACTOR, now));
            changed = true;
        }
        return changed;
    }

    private ApplicationEntity findApplication(Long tenantId, String applicationCode) {
        return singleOrNull(entityManager.createQuery("""
                        select application from ApplicationEntity application
                         where application.tenantId = :tenantId
                           and application.applicationCode = :applicationCode
                        """, ApplicationEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationCode", applicationCode)
                .getResultList(), "application");
    }

    private RoleEntity findRole(Long tenantId, Long applicationId, String roleCode) {
        return singleOrNull(entityManager.createQuery("""
                        select role from RoleEntity role
                         where role.tenantId = :tenantId
                           and role.applicationId = :applicationId
                           and role.roleCode = :roleCode
                        """, RoleEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationId", applicationId)
                .setParameter("roleCode", roleCode)
                .getResultList(), "role");
    }

    private boolean ensurePermission(
            Long tenantId,
            Long applicationId,
            Long roleId,
            String permissionCode,
            Instant now) {
        PermissionEntity permission = singleOrNull(entityManager.createQuery("""
                        select permission from PermissionEntity permission
                         where permission.tenantId = :tenantId
                           and permission.permissionCode = :permissionCode
                        """, PermissionEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("permissionCode", permissionCode)
                .getResultList(), "permission");
        boolean changed = false;
        if (permission == null) {
            permission = new PermissionEntity(
                    idGenerator.nextLongId(), tenantId, applicationId,
                    permissionCode, permissionCode, risk(permissionCode),
                    "Unified identity local administrative capability", ACTOR, now);
            entityManager.persist(permission);
            changed = true;
        } else if (!permission.getApplicationId().equals(applicationId)) {
            throw new IllegalStateException(
                    "permission code is already owned by another application: "
                            + permissionCode);
        }
        if (!hasRolePermission(tenantId, roleId, permission.getId())) {
            entityManager.persist(new RolePermissionEntity(
                    idGenerator.nextLongId(), tenantId, applicationId,
                    roleId, permission.getId(), now, null, ACTOR, now));
            changed = true;
        }
        return changed;
    }

    private boolean hasRolePermission(Long tenantId, Long roleId, Long permissionId) {
        Number count = (Number) entityManager.createQuery("""
                        select count(mapping) from RolePermissionEntity mapping
                         where mapping.tenantId = :tenantId
                           and mapping.roleId = :roleId
                           and mapping.permissionId = :permissionId
                           and mapping.status = :status
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("roleId", roleId)
                .setParameter("permissionId", permissionId)
                .setParameter("status", RolePermissionEntity.Status.ACTIVE)
                .getSingleResult();
        return count.longValue() > 0;
    }

    private boolean hasAssignment(Long tenantId, Long userId, Long roleId) {
        Number count = (Number) entityManager.createQuery("""
                        select count(assignment) from UserRoleAssignmentEntity assignment
                         where assignment.tenantId = :tenantId
                           and assignment.userId = :userId
                           and assignment.roleId = :roleId
                           and assignment.status in :statuses
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("userId", userId)
                .setParameter("roleId", roleId)
                .setParameter("statuses", List.of(
                        UserRoleAssignmentEntity.Status.ACTIVE,
                        UserRoleAssignmentEntity.Status.PENDING))
                .getSingleResult();
        return count.longValue() > 0;
    }

    private void insertSelfClosure(Long tenantId, Long applicationId, Long roleId) {
        entityManager.createNativeQuery("""
                        insert into rbac3_role_closure (
                            tenant_id, application_id, ancestor_role_id,
                            descendant_role_id, depth
                        ) values (:tenantId, :applicationId, :roleId, :roleId, 0)
                        on conflict do nothing
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationId", applicationId)
                .setParameter("roleId", roleId)
                .executeUpdate();
    }

    private static PermissionEntity.RiskLevel risk(String permissionCode) {
        if (permissionCode.endsWith(":read") || permissionCode.equals("DDC_READ")) {
            return PermissionEntity.RiskLevel.MEDIUM;
        }
        return permissionCode.endsWith(":admin")
                || permissionCode.endsWith(":manage")
                || permissionCode.endsWith(":activate")
                || permissionCode.endsWith(":revoke")
                ? PermissionEntity.RiskLevel.CRITICAL
                : PermissionEntity.RiskLevel.HIGH;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("tenantCode is required");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static <T> T singleOrNull(List<T> values, String name) {
        if (values.size() > 1) {
            throw new IllegalStateException("duplicate development " + name);
        }
        return values.isEmpty() ? null : values.getFirst();
    }
}
