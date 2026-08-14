package top.egon.cola.platform.rbac3.admin.bootstrap.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.audit.repository.AuditPort;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationEventPublisher;
import top.egon.cola.platform.rbac3.admin.assignment.domain.po.UserRoleAssignmentPO;
import top.egon.cola.platform.rbac3.admin.bootstrap.repository.PlatformAdminBootstrapRepository;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.ApplicationPO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.PermissionPO;
import top.egon.cola.platform.rbac3.admin.role.domain.po.RolePO;
import top.egon.cola.platform.rbac3.admin.role.domain.po.RolePermissionPO;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.PermissionRiskLevelEnum;
import top.egon.cola.platform.rbac3.admin.role.domain.enums.RoleTypeEnum;
import top.egon.cola.platform.rbac3.admin.role.domain.enums.RoleRiskLevelEnum;
import top.egon.cola.platform.rbac3.admin.assignment.domain.enums.UserRoleAssignmentTypeEnum;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditEventVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationEventVO;

/**
 * 类型 `JpaPlatformAdminBootstrapRepository` 位于当前包内，是类型，用于承载 `Postgresql Platform Admin Bootstrap Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaPlatformAdminBootstrapRepository` is a type in its package and carries the responsibility, state, or contract for `Postgresql Platform Admin Bootstrap Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Creates the first platform security administrator under one PostgreSQL transaction lock.
 */
@Repository
public class JpaPlatformAdminBootstrapRepository
        implements PlatformAdminBootstrapRepository {

    /**
     * 字段 `BOOTSTRAP_LOCK_KEY` 表示 `JpaPlatformAdminBootstrapRepository` 中与 `BOOTSTRAP LOCK KEY` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `BOOTSTRAP_LOCK_KEY` stores the `BOOTSTRAP LOCK KEY`-related state, dependency, configuration, or result of `JpaPlatformAdminBootstrapRepository` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `BOOTSTRAP_LOCK_KEY` 时应保持 `JpaPlatformAdminBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `BOOTSTRAP_LOCK_KEY`, preserve `JpaPlatformAdminBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final long BOOTSTRAP_LOCK_KEY = 0x5242414333424f4fL;
    /**
     * 字段 `ACTOR` 表示 `JpaPlatformAdminBootstrapRepository` 中与 `ACTOR` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `ACTOR` stores the `ACTOR`-related state, dependency, configuration, or result of `JpaPlatformAdminBootstrapRepository` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `ACTOR` 时应保持 `JpaPlatformAdminBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ACTOR`, preserve `JpaPlatformAdminBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String ACTOR = "rbac3-platform-bootstrap";
    /**
     * 字段 `APPLICATION_CODE` 表示 `JpaPlatformAdminBootstrapRepository` 中与 `APPLICATION CODE` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `APPLICATION_CODE` stores the `APPLICATION CODE`-related state, dependency, configuration, or result of `JpaPlatformAdminBootstrapRepository` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `APPLICATION_CODE` 时应保持 `JpaPlatformAdminBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `APPLICATION_CODE`, preserve `JpaPlatformAdminBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String APPLICATION_CODE = "rbac3-system";
    /**
     * 字段 `ROLE_CODE` 表示 `JpaPlatformAdminBootstrapRepository` 中与 `ROLE CODE` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `ROLE_CODE` stores the `ROLE CODE`-related state, dependency, configuration, or result of `JpaPlatformAdminBootstrapRepository` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `ROLE_CODE` 时应保持 `JpaPlatformAdminBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ROLE_CODE`, preserve `JpaPlatformAdminBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String ROLE_CODE = "ROLE_PLATFORM_ADMIN";
    /**
     * 字段 `TENANT_CODE` 表示 `JpaPlatformAdminBootstrapRepository` 中与 `TENANT CODE` 相关的状态、依赖、配置或结果（声明类型 `Pattern`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `TENANT_CODE` stores the `TENANT CODE`-related state, dependency, configuration, or result of `JpaPlatformAdminBootstrapRepository` (declared type `Pattern`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `TENANT_CODE` 时应保持 `JpaPlatformAdminBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `TENANT_CODE`, preserve `JpaPlatformAdminBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Pattern TENANT_CODE = Pattern.compile("^[a-z][a-z0-9-]{2,63}$");

    /**
     * 字段 `PLATFORM_PERMISSIONS` 表示 `JpaPlatformAdminBootstrapRepository` 中与 `PLATFORM PERMISSIONS` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `PLATFORM_PERMISSIONS` stores the `PLATFORM PERMISSIONS`-related state, dependency, configuration, or result of `JpaPlatformAdminBootstrapRepository` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `PLATFORM_PERMISSIONS` 时应保持 `JpaPlatformAdminBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `PLATFORM_PERMISSIONS`, preserve `JpaPlatformAdminBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
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
            "system:tenant:manage",
            "system:tenant:read",
            "system:tenant:target",
            "system:user-status:manage",
            "system:user:read");

    /**
     * 字段 `entityManager` 表示 `JpaPlatformAdminBootstrapRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaPlatformAdminBootstrapRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaPlatformAdminBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaPlatformAdminBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `idGenerator` 表示 `JpaPlatformAdminBootstrapRepository` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `JpaPlatformAdminBootstrapRepository` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `JpaPlatformAdminBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `JpaPlatformAdminBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `auditPort` 表示 `JpaPlatformAdminBootstrapRepository` 中与 `audit Port` 相关的状态、依赖、配置或结果（声明类型 `AuditPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `auditPort` stores the `audit Port`-related state, dependency, configuration, or result of `JpaPlatformAdminBootstrapRepository` (declared type `AuditPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `auditPort` 时应保持 `JpaPlatformAdminBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `auditPort`, preserve `JpaPlatformAdminBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuditPort auditPort;
    /**
     * 字段 `eventPort` 表示 `JpaPlatformAdminBootstrapRepository` 中与 `event Port` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationEventPublisher`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `eventPort` stores the `event Port`-related state, dependency, configuration, or result of `JpaPlatformAdminBootstrapRepository` (declared type `AuthorizationEventPublisher`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `eventPort` 时应保持 `JpaPlatformAdminBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `eventPort`, preserve `JpaPlatformAdminBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationEventPublisher eventPort;
    /**
     * 字段 `clock` 表示 `JpaPlatformAdminBootstrapRepository` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `JpaPlatformAdminBootstrapRepository` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `JpaPlatformAdminBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `JpaPlatformAdminBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 构造器 `JpaPlatformAdminBootstrapRepository` 用于创建并初始化 `JpaPlatformAdminBootstrapRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaPlatformAdminBootstrapRepository` creates and initializes `JpaPlatformAdminBootstrapRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaPlatformAdminBootstrapRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaPlatformAdminBootstrapRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param auditPort 输入参数 `auditPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param eventPort 输入参数 `eventPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaPlatformAdminBootstrapRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            AuditPort auditPort,
            AuthorizationEventPublisher eventPort,
            Clock clock) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.eventPort = Objects.requireNonNull(eventPort, "eventPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 方法 `bootstrap` 按照 `JpaPlatformAdminBootstrapRepository` 的职责处理输入，完成 `bootstrap` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bootstrap` processes its inputs according to `JpaPlatformAdminBootstrapRepository`'s responsibility, performs the `bootstrap` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bootstrap` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bootstrap`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void bootstrap(String tenantCode, String identitySub) {
        String normalizedTenantCode = normalizeTenantCode(tenantCode);
        String normalizedIdentitySub = required(identitySub, "identitySub");
        acquireLock();
        rejectExistingAdministrator(normalizedTenantCode);
        rejectExistingTenant(normalizedTenantCode);

        Instant now = clock.instant();
        Long tenantId = idGenerator.nextLongId();
        Long applicationId = idGenerator.nextLongId();
        Long roleId = idGenerator.nextLongId();
        Long userId = idGenerator.nextLongId();

        TenantPO tenant = new TenantPO(
                tenantId, normalizedTenantCode, "Platform", ACTOR, now);
        tenant.configure(Map.of("builtInApplicationCode", APPLICATION_CODE), ACTOR, now);
        tenant.activate(ACTOR, now);
        entityManager.persist(tenant);
        entityManager.persist(new ApplicationPO(
                applicationId, tenantId, APPLICATION_CODE,
                "RBAC3 System Administration", 0, ACTOR, now));
        RolePO administratorRole = new RolePO(
                roleId, tenantId, applicationId, ROLE_CODE,
                "Platform Security Administrator", RoleTypeEnum.MANAGEMENT,
                RoleRiskLevelEnum.CRITICAL, true, null, 0, null, ACTOR, now);
        entityManager.persist(administratorRole);

        for (String permissionCode : PLATFORM_PERMISSIONS) {
            Long permissionId = idGenerator.nextLongId();
            entityManager.persist(new PermissionPO(
                    permissionId, tenantId, applicationId, permissionCode,
                    permissionName(permissionCode), risk(permissionCode),
                    "Built-in RBAC3 platform administration capability", ACTOR, now));
            entityManager.persist(new RolePermissionPO(
                    idGenerator.nextLongId(), tenantId, applicationId, roleId,
                    permissionId, now, null, ACTOR, now));
        }

        UserPO administrator = new UserPO(
                userId, tenantId, normalizedIdentitySub,
                top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserStatusEnum.ACTIVE,
                ACTOR, now);
        administrator.advanceAuthorizationVersion(0, ACTOR, now);
        entityManager.persist(administrator);
        UserRoleAssignmentPO assignment = new UserRoleAssignmentPO(
                idGenerator.nextLongId(), tenantId, userId, roleId,
                UserRoleAssignmentTypeEnum.DIRECT, now, null,
                "BOOTSTRAP", normalizedTenantCode, "Initial platform administrator",
                null, ACTOR, now);
        entityManager.persist(assignment);
        tenant.incrementPolicyVersion(ACTOR, now);
        entityManager.flush();
        insertSelfClosure(tenantId, applicationId, roleId);

        String requestId = "bootstrap:" + tenantId;
        auditPort.append(new AuditEventVO(
                tenantId.toString(), "PLATFORM_ADMIN_BOOTSTRAPPED", ACTOR,
                "USER", userId.toString(), requestId, requestId,
                Map.of("tenantCode", normalizedTenantCode,
                        "identitySub", normalizedIdentitySub,
                        "roleCode", ROLE_CODE), now));
        eventPort.enqueue(new AuthorizationEventVO(
                tenantId.toString(), "USER", userId.toString(),
                "ASSIGNMENT_CHANGED",
                Map.of("assignmentId", assignment.getId().toString(),
                        "userId", userId.toString(),
                        "changeType", "BOOTSTRAPPED",
                        "authVersion", Long.toString(administrator.getAuthVersion())),
                requestId));
    }

    /**
     * 方法 `acquireLock` 按照 `JpaPlatformAdminBootstrapRepository` 的职责处理输入，完成 `acquire Lock` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `acquireLock` processes its inputs according to `JpaPlatformAdminBootstrapRepository`'s responsibility, performs the `acquire Lock` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `acquireLock` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `acquireLock`, then continue the business flow using its result, exception, or side effect.
     */
    private void acquireLock() {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", BOOTSTRAP_LOCK_KEY)
                .getSingleResult();
    }

    /**
     * 方法 `rejectExistingAdministrator` 按照 `JpaPlatformAdminBootstrapRepository` 的职责处理输入，完成 `reject Existing Administrator` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rejectExistingAdministrator` processes its inputs according to `JpaPlatformAdminBootstrapRepository`'s responsibility, performs the `reject Existing Administrator` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rejectExistingAdministrator` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rejectExistingAdministrator`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `rejectExistingTenant` 按照 `JpaPlatformAdminBootstrapRepository` 的职责处理输入，完成 `reject Existing Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rejectExistingTenant` processes its inputs according to `JpaPlatformAdminBootstrapRepository`'s responsibility, performs the `reject Existing Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rejectExistingTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rejectExistingTenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `insertSelfClosure` 按照 `JpaPlatformAdminBootstrapRepository` 的职责处理输入，完成 `insert Self Closure` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `insertSelfClosure` processes its inputs according to `JpaPlatformAdminBootstrapRepository`'s responsibility, performs the `insert Self Closure` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `insertSelfClosure` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `insertSelfClosure`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `normalizeTenantCode` 按照 `JpaPlatformAdminBootstrapRepository` 的职责处理输入，完成 `normalize Tenant Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `normalizeTenantCode` processes its inputs according to `JpaPlatformAdminBootstrapRepository`'s responsibility, performs the `normalize Tenant Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `normalizeTenantCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `normalizeTenantCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String normalizeTenantCode(String value) {
        String normalized = Objects.requireNonNull(value, "tenantCode")
                .trim().toLowerCase(Locale.ROOT);
        if (!TENANT_CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("tenant code is invalid");
        }
        return normalized;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    /**
     * 方法 `risk` 按照 `JpaPlatformAdminBootstrapRepository` 的职责处理输入，完成 `risk` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `risk` processes its inputs according to `JpaPlatformAdminBootstrapRepository`'s responsibility, performs the `risk` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `risk` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `risk`, then continue the business flow using its result, exception, or side effect.
     *
     * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static PermissionRiskLevelEnum risk(String permissionCode) {
        return permissionCode.endsWith(":read")
                ? PermissionRiskLevelEnum.MEDIUM
                : permissionCode.endsWith(":operate")
                || permissionCode.endsWith(":manage")
                || permissionCode.endsWith(":revoke")
                ? PermissionRiskLevelEnum.CRITICAL
                : PermissionRiskLevelEnum.HIGH;
    }

    /**
     * 方法 `permissionName` 按照 `JpaPlatformAdminBootstrapRepository` 的职责处理输入，完成 `permission Name` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `permissionName` processes its inputs according to `JpaPlatformAdminBootstrapRepository`'s responsibility, performs the `permission Name` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `permissionName` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `permissionName`, then continue the business flow using its result, exception, or side effect.
     *
     * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String permissionName(String permissionCode) {
        return permissionCode.substring("system:".length())
                .replace(':', ' ')
                .replace('-', ' ');
    }
}
