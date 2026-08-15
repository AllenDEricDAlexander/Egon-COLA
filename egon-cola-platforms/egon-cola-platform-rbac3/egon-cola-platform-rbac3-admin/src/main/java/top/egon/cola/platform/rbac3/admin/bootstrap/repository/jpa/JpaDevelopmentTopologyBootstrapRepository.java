package top.egon.cola.platform.rbac3.admin.bootstrap.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.domain.po.UserRoleAssignmentPO;
import top.egon.cola.platform.rbac3.admin.bootstrap.domain.Rbac3DevelopmentTopology;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.po.TenantPO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.po.ApplicationPO;
import top.egon.cola.platform.rbac3.admin.iam.permission.domain.po.PermissionPO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.po.RolePO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.po.RolePermissionPO;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.bootstrap.repository.DevelopmentBootstrapPort;
import top.egon.cola.platform.rbac3.admin.bootstrap.domain.vo.ApplicationDefinitionVO;
import top.egon.cola.platform.rbac3.admin.iam.permission.domain.enums.PermissionRiskLevelEnum;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.enums.RoleTypeEnum;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.enums.RoleRiskLevelEnum;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.enums.RolePermissionStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.domain.enums.UserRoleAssignmentTypeEnum;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.domain.enums.UserRoleAssignmentStatusEnum;

/**
 * 类型 `JpaDevelopmentTopologyBootstrapRepository` 位于当前包内，是类型，用于承载 `Postgresql Development Topology Bootstrap Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaDevelopmentTopologyBootstrapRepository` is a type in its package and carries the responsibility, state, or contract for `Postgresql Development Topology Bootstrap Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * PostgreSQL-backed, idempotent local topology bootstrap guarded by an advisory lock.
 */
@Repository
public class JpaDevelopmentTopologyBootstrapRepository
        implements DevelopmentBootstrapPort {

    /**
     * 字段 `BOOTSTRAP_LOCK_KEY` 表示 `JpaDevelopmentTopologyBootstrapRepository` 中与 `BOOTSTRAP LOCK KEY` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `BOOTSTRAP_LOCK_KEY` stores the `BOOTSTRAP LOCK KEY`-related state, dependency, configuration, or result of `JpaDevelopmentTopologyBootstrapRepository` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `BOOTSTRAP_LOCK_KEY` 时应保持 `JpaDevelopmentTopologyBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `BOOTSTRAP_LOCK_KEY`, preserve `JpaDevelopmentTopologyBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final long BOOTSTRAP_LOCK_KEY = 0x5242414333494450L;
    /**
     * 字段 `ACTOR` 表示 `JpaDevelopmentTopologyBootstrapRepository` 中与 `ACTOR` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `ACTOR` stores the `ACTOR`-related state, dependency, configuration, or result of `JpaDevelopmentTopologyBootstrapRepository` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `ACTOR` 时应保持 `JpaDevelopmentTopologyBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ACTOR`, preserve `JpaDevelopmentTopologyBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String ACTOR = "rbac3-development-bootstrap";

    /**
     * 字段 `entityManager` 表示 `JpaDevelopmentTopologyBootstrapRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaDevelopmentTopologyBootstrapRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaDevelopmentTopologyBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaDevelopmentTopologyBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `idGenerator` 表示 `JpaDevelopmentTopologyBootstrapRepository` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `JpaDevelopmentTopologyBootstrapRepository` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `JpaDevelopmentTopologyBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `JpaDevelopmentTopologyBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `clock` 表示 `JpaDevelopmentTopologyBootstrapRepository` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `JpaDevelopmentTopologyBootstrapRepository` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `JpaDevelopmentTopologyBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `JpaDevelopmentTopologyBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 构造器 `JpaDevelopmentTopologyBootstrapRepository` 用于创建并初始化 `JpaDevelopmentTopologyBootstrapRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaDevelopmentTopologyBootstrapRepository` creates and initializes `JpaDevelopmentTopologyBootstrapRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaDevelopmentTopologyBootstrapRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaDevelopmentTopologyBootstrapRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaDevelopmentTopologyBootstrapRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            Clock clock) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 方法 `bootstrap` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `bootstrap` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bootstrap` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `bootstrap` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bootstrap` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bootstrap`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param username 输入参数 `username`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void bootstrap(String tenantCode, String identitySub) {
        acquireLock();
        Instant now = clock.instant();
        String normalizedTenantCode = normalize(tenantCode);
        TenantPO tenant = findTenant(normalizedTenantCode);
        boolean changed = false;
        if (tenant == null) {
            tenant = new TenantPO(
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
        String normalizedIdentitySub = required(identitySub, "identitySub");
        UserPO user = findUser(tenant.getId(), normalizedIdentitySub);
        if (user == null) {
            user = new UserPO(
                    idGenerator.nextLongId(),
                    tenant.getId(),
                    normalizedIdentitySub,
                    top.egon.cola.platform.rbac3.admin.iam.user.domain.enums.UserStatusEnum.ACTIVE,
                    ACTOR,
                    now
            );
            entityManager.persist(user);
            changed = true;
        }

        for (var definition : Rbac3DevelopmentTopology.applications()) {
            changed |= ensureApplication(tenant.getId(), user.getId(), definition, now);
        }
        if (changed) {
            tenant.incrementPolicyVersion(ACTOR, now);
            user.advanceAuthorizationVersion(user.getAuthVersion(), ACTOR, now);
        }
        entityManager.flush();
    }

    /**
     * 方法 `acquireLock` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `acquire Lock` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `acquireLock` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `acquire Lock` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `findTenant` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `find Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findTenant` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `find Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findTenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private TenantPO findTenant(String tenantCode) {
        return singleOrNull(entityManager.createQuery("""
                        select tenant from TenantEntity tenant
                         where lower(tenant.code) = :tenantCode
                        """, TenantPO.class)
                .setParameter("tenantCode", tenantCode)
                .getResultList(), "tenant");
    }

    /**
     * 方法 `findUser` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `find User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findUser` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `find User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findUser`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param normalizedUsername 输入参数 `normalizedUsername`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private UserPO findUser(Long tenantId, String identitySub) {
        return singleOrNull(entityManager.createQuery("""
                        select user from UserEntity user
                         where user.tenantId = :tenantId
                           and user.identitySub = :identitySub
                        """, UserPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("identitySub", identitySub)
                .getResultList(), "user");
    }

    /**
     * 方法 `ensureApplication` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `ensure Application` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `ensureApplication` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `ensure Application` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `ensureApplication` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `ensureApplication`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param definition 输入参数 `definition`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean ensureApplication(
            Long tenantId,
            Long userId,
            ApplicationDefinitionVO definition,
            Instant now) {
        ApplicationPO application = findApplication(
                tenantId, definition.applicationCode());
        boolean changed = false;
        if (application == null) {
            application = new ApplicationPO(
                    idGenerator.nextLongId(), tenantId,
                    definition.applicationCode(), definition.applicationName(),
                    definition.displayPriority(), ACTOR, now);
            entityManager.persist(application);
            changed = true;
        }
        RolePO role = findRole(
                tenantId, application.getId(), definition.roleCode());
        if (role == null) {
            role = new RolePO(
                    idGenerator.nextLongId(), tenantId, application.getId(),
                    definition.roleCode(), definition.applicationName() + " Administrator",
                    RoleTypeEnum.MANAGEMENT, RoleRiskLevelEnum.MEDIUM,
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
            entityManager.persist(new UserRoleAssignmentPO(
                    idGenerator.nextLongId(), tenantId, userId, role.getId(),
                    UserRoleAssignmentTypeEnum.DIRECT, now, null,
                    "DEVELOPMENT", definition.applicationCode(),
                    "Unified identity local administrator", null, ACTOR, now));
            changed = true;
        }
        return changed;
    }

    /**
     * 方法 `findApplication` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `find Application` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findApplication` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `find Application` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findApplication` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findApplication`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ApplicationPO findApplication(Long tenantId, String applicationCode) {
        return singleOrNull(entityManager.createQuery("""
                        select application from ApplicationEntity application
                         where application.tenantId = :tenantId
                           and application.applicationCode = :applicationCode
                        """, ApplicationPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationCode", applicationCode)
                .getResultList(), "application");
    }

    /**
     * 方法 `findRole` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `find Role` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findRole` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `find Role` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findRole` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findRole`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleCode 输入参数 `roleCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RolePO findRole(Long tenantId, Long applicationId, String roleCode) {
        return singleOrNull(entityManager.createQuery("""
                        select role from RoleEntity role
                         where role.tenantId = :tenantId
                           and role.applicationId = :applicationId
                           and role.roleCode = :roleCode
                        """, RolePO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationId", applicationId)
                .setParameter("roleCode", roleCode)
                .getResultList(), "role");
    }

    /**
     * 方法 `ensurePermission` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `ensure Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `ensurePermission` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `ensure Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `ensurePermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `ensurePermission`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean ensurePermission(
            Long tenantId,
            Long applicationId,
            Long roleId,
            String permissionCode,
            Instant now) {
        PermissionPO permission = singleOrNull(entityManager.createQuery("""
                        select permission from PermissionEntity permission
                         where permission.tenantId = :tenantId
                           and permission.permissionCode = :permissionCode
                        """, PermissionPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("permissionCode", permissionCode)
                .getResultList(), "permission");
        boolean changed = false;
        if (permission == null) {
            permission = new PermissionPO(
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
            entityManager.persist(new RolePermissionPO(
                    idGenerator.nextLongId(), tenantId, applicationId,
                    roleId, permission.getId(), now, null, ACTOR, now));
            changed = true;
        }
        return changed;
    }

    /**
     * 方法 `hasRolePermission` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `has Role Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `hasRolePermission` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `has Role Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `hasRolePermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `hasRolePermission`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissionId 输入参数 `permissionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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
                .setParameter("status", RolePermissionStatusEnum.ACTIVE)
                .getSingleResult();
        return count.longValue() > 0;
    }

    /**
     * 方法 `hasAssignment` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `has Assignment` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `hasAssignment` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `has Assignment` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `hasAssignment` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `hasAssignment`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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
                        UserRoleAssignmentStatusEnum.ACTIVE,
                        UserRoleAssignmentStatusEnum.PENDING))
                .getSingleResult();
        return count.longValue() > 0;
    }

    /**
     * 方法 `insertSelfClosure` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `insert Self Closure` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `insertSelfClosure` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `insert Self Closure` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
                        on conflict do nothing
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationId", applicationId)
                .setParameter("roleId", roleId)
                .executeUpdate();
    }

    /**
     * 方法 `risk` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `risk` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `risk` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `risk` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `risk` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `risk`, then continue the business flow using its result, exception, or side effect.
     *
     * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static PermissionRiskLevelEnum risk(String permissionCode) {
        if (permissionCode.endsWith(":read") || permissionCode.equals("DDC_READ")) {
            return PermissionRiskLevelEnum.MEDIUM;
        }
        return permissionCode.endsWith(":admin")
                || permissionCode.endsWith(":manage")
                || permissionCode.endsWith(":activate")
                || permissionCode.endsWith(":revoke")
                ? PermissionRiskLevelEnum.CRITICAL
                : PermissionRiskLevelEnum.HIGH;
    }

    /**
     * 方法 `normalize` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `normalize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `normalize` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `normalize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `normalize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `normalize`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("tenantCode is required");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    /**
     * 方法 `singleOrNull` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `single Or Null` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `singleOrNull` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `single Or Null` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `singleOrNull` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `singleOrNull`, then continue the business flow using its result, exception, or side effect.
     *
     * @param <T> 类型参数表示查询结果的具体类型；type parameter representing the query result type.
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static <T> T singleOrNull(List<T> values, String name) {
        if (values.size() > 1) {
            throw new IllegalStateException("duplicate development " + name);
        }
        return values.isEmpty() ? null : values.getFirst();
    }
}
