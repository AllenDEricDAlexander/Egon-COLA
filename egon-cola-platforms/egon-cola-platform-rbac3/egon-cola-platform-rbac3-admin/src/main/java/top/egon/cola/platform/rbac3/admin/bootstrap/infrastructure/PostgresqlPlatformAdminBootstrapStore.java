package top.egon.cola.platform.rbac3.admin.bootstrap.infrastructure;

import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.AuditPort;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;
import top.egon.cola.platform.rbac3.admin.assignment.domain.UserRoleAssignmentEntity;
import top.egon.cola.platform.rbac3.admin.bootstrap.cli.Rbac3PlatformAdminBootstrapCli;
import top.egon.cola.platform.rbac3.admin.identity.domain.TenantEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserCredentialEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserEntity;
import top.egon.cola.platform.rbac3.admin.resource.domain.ApplicationEntity;
import top.egon.cola.platform.rbac3.admin.resource.domain.PermissionEntity;
import top.egon.cola.platform.rbac3.admin.role.domain.RoleEntity;
import top.egon.cola.platform.rbac3.admin.role.domain.RolePermissionEntity;

import java.nio.CharBuffer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Creates the first platform security administrator under one PostgreSQL transaction lock.
 */
@Repository
public class PostgresqlPlatformAdminBootstrapStore
        implements Rbac3PlatformAdminBootstrapCli.BootstrapPort {

    private static final long BOOTSTRAP_LOCK_KEY = 0x5242414333424f4fL;
    private static final String ACTOR = "rbac3-platform-bootstrap";
    private static final String APPLICATION_CODE = "rbac3-system";
    private static final String ROLE_CODE = "ROLE_PLATFORM_ADMIN";
    private static final Pattern TENANT_CODE = Pattern.compile("^[a-z][a-z0-9-]{2,63}$");

    private static final List<String> PLATFORM_PERMISSIONS = List.of(
            "system:application:read",
            "system:audit:read",
            "system:authorization-constraint:manage",
            "system:authorization-constraint:read",
            "system:authorization-runtime:operate",
            "system:authorization-runtime:read",
            "system:authorization-simulation:execute",
            "system:data-rule:manage",
            "system:data-rule:read",
            "system:directory-snapshot:read",
            "system:directory:read",
            "system:directory:sync",
            "system:field-rule:manage",
            "system:field-rule:read",
            "system:management-policy:manage",
            "system:management-policy:read",
            "system:operation-sod:manage",
            "system:operation-sod:read",
            "system:resource-manifest:activate",
            "system:resource-manifest:read",
            "system:resource-manifest:submit",
            "system:resource:archive",
            "system:resource:read",
            "system:role-activation:read",
            "system:role-activation:use",
            "system:role-assignment:manage",
            "system:role-assignment:read",
            "system:role-inheritance:manage",
            "system:role-permission:manage",
            "system:role:create",
            "system:role:read",
            "system:role:update",
            "system:session:logout",
            "system:session:read",
            "system:session:revoke",
            "system:tenant:manage",
            "system:tenant:read",
            "system:tenant:target",
            "system:user-status:manage",
            "system:user:read");

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;
    private final AuditPort auditPort;
    private final AuthorizationEventPort eventPort;
    private final Clock clock;

    public PostgresqlPlatformAdminBootstrapStore(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            PasswordEncoder passwordEncoder,
            AuditPort auditPort,
            AuthorizationEventPort eventPort,
            Clock clock) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.eventPort = Objects.requireNonNull(eventPort, "eventPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public void bootstrap(String tenantCode, String username, char[] password) {
        String normalizedTenantCode = normalizeTenantCode(tenantCode);
        String normalizedUsername = UserEntity.normalize(username);
        requirePassword(password);
        acquireLock();
        rejectExistingAdministrator(normalizedTenantCode);
        rejectExistingTenant(normalizedTenantCode);

        Instant now = clock.instant();
        Long tenantId = idGenerator.nextLongId();
        Long applicationId = idGenerator.nextLongId();
        Long roleId = idGenerator.nextLongId();
        Long userId = idGenerator.nextLongId();

        TenantEntity tenant = new TenantEntity(
                tenantId, normalizedTenantCode, "Platform", ACTOR, now);
        tenant.configure(Map.of("builtInApplicationCode", APPLICATION_CODE), ACTOR, now);
        tenant.activate(ACTOR, now);
        entityManager.persist(tenant);
        entityManager.persist(new ApplicationEntity(
                applicationId, tenantId, APPLICATION_CODE,
                "RBAC3 System Administration", 0, ACTOR, now));
        RoleEntity administratorRole = new RoleEntity(
                roleId, tenantId, applicationId, ROLE_CODE,
                "Platform Security Administrator", RoleEntity.RoleType.MANAGEMENT,
                RoleEntity.RiskLevel.CRITICAL, true, null, 0, null, ACTOR, now);
        entityManager.persist(administratorRole);

        for (String permissionCode : PLATFORM_PERMISSIONS) {
            Long permissionId = idGenerator.nextLongId();
            entityManager.persist(new PermissionEntity(
                    permissionId, tenantId, applicationId, permissionCode,
                    permissionName(permissionCode), risk(permissionCode),
                    "Built-in RBAC3 platform administration capability", ACTOR, now));
            entityManager.persist(new RolePermissionEntity(
                    idGenerator.nextLongId(), tenantId, applicationId, roleId,
                    permissionId, now, null, ACTOR, now));
        }

        UserEntity administrator = new UserEntity(
                userId, tenantId, normalizedUsername, username.trim(), ACTOR, now);
        administrator.advanceAuthorizationVersion(0, ACTOR, now);
        entityManager.persist(administrator);
        entityManager.persist(new UserCredentialEntity(
                idGenerator.nextLongId(), tenantId, userId,
                passwordEncoder.encode(CharBuffer.wrap(password)), true, ACTOR, now));
        UserRoleAssignmentEntity assignment = new UserRoleAssignmentEntity(
                idGenerator.nextLongId(), tenantId, userId, roleId,
                UserRoleAssignmentEntity.AssignmentType.DIRECT, now, null,
                "BOOTSTRAP", normalizedTenantCode, "Initial platform administrator",
                null, ACTOR, now);
        entityManager.persist(assignment);
        tenant.incrementPolicyVersion(ACTOR, now);
        entityManager.flush();
        insertSelfClosure(tenantId, applicationId, roleId);

        String requestId = "bootstrap:" + tenantId;
        auditPort.append(new AuditPort.AuditEvent(
                tenantId.toString(), "PLATFORM_ADMIN_BOOTSTRAPPED", ACTOR,
                "USER", userId.toString(), requestId, requestId,
                Map.of("tenantCode", normalizedTenantCode,
                        "username", normalizedUsername,
                        "roleCode", ROLE_CODE), now));
        eventPort.enqueue(new AuthorizationEventPort.AuthorizationEvent(
                tenantId.toString(), "USER", userId.toString(),
                "ASSIGNMENT_CHANGED",
                Map.of("assignmentId", assignment.getId().toString(),
                        "userId", userId.toString(),
                        "changeType", "BOOTSTRAPPED",
                        "authVersion", Long.toString(administrator.getAuthVersion())),
                requestId));
    }

    private void acquireLock() {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", BOOTSTRAP_LOCK_KEY)
                .getSingleResult();
    }

    private void rejectExistingAdministrator(String tenantCode) {
        Number count = (Number) entityManager.createNativeQuery("""
                        select count(*)
                          from rbac3_user_role_assignment assignment
                          join rbac3_role role
                            on role.tenant_id = assignment.tenant_id
                           and role.id = assignment.role_id
                          join rbac3_tenant tenant
                            on tenant.id = assignment.tenant_id
                         where lower(tenant.code) = :tenantCode
                           and role.role_code = :roleCode
                           and assignment.status = 'ACTIVE'
                           and assignment.valid_from <= current_timestamp
                           and (assignment.valid_to is null
                                or assignment.valid_to > current_timestamp)
                        """)
                .setParameter("tenantCode", tenantCode)
                .setParameter("roleCode", ROLE_CODE)
                .getSingleResult();
        if (count.longValue() > 0) {
            throw new IllegalStateException("platform administrator already exists");
        }
    }

    private void rejectExistingTenant(String tenantCode) {
        Number count = (Number) entityManager.createNativeQuery(
                        "select count(*) from rbac3_tenant where lower(code) = :tenantCode")
                .setParameter("tenantCode", tenantCode)
                .getSingleResult();
        if (count.longValue() > 0) {
            throw new IllegalStateException(
                    "platform tenant already exists; use the explicit recovery runbook");
        }
    }

    private void insertSelfClosure(Long tenantId, Long applicationId, Long roleId) {
        entityManager.createNativeQuery("""
                        insert into rbac3_role_closure (
                            tenant_id, application_id, ancestor_role_id,
                            descendant_role_id, depth
                        ) values (:tenantId, :applicationId, :roleId, :roleId, 0)
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationId", applicationId)
                .setParameter("roleId", roleId)
                .executeUpdate();
    }

    private static String normalizeTenantCode(String value) {
        String normalized = Objects.requireNonNull(value, "tenantCode")
                .trim().toLowerCase(Locale.ROOT);
        if (!TENANT_CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("tenant code is invalid");
        }
        return normalized;
    }

    private static void requirePassword(char[] password) {
        Objects.requireNonNull(password, "password");
        if (password.length < 12 || password.length > 64) {
            throw new IllegalArgumentException("password must contain 12 to 64 characters");
        }
    }

    private static PermissionEntity.RiskLevel risk(String permissionCode) {
        return permissionCode.endsWith(":read")
                ? PermissionEntity.RiskLevel.MEDIUM
                : permissionCode.endsWith(":operate")
                || permissionCode.endsWith(":manage")
                || permissionCode.endsWith(":revoke")
                ? PermissionEntity.RiskLevel.CRITICAL
                : PermissionEntity.RiskLevel.HIGH;
    }

    private static String permissionName(String permissionCode) {
        return permissionCode.substring("system:".length())
                .replace(':', ' ')
                .replace('-', ' ');
    }
}
