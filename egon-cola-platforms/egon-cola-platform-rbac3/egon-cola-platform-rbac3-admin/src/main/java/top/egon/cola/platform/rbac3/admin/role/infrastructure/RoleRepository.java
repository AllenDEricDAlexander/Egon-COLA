package top.egon.cola.platform.rbac3.admin.role.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;
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

/**
 * 类型 `RoleRepository` 位于当前包内，是类型，用于承载 `Role Repository` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RoleRepository` is a type in its package and carries the responsibility, state, or contract for `Role Repository`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `RoleRepository` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `RoleRepository` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Repository
public class RoleRepository implements RoleFacade.HierarchyStore, RoleFacade.RoleControlStore {

    /**
     * 字段 `entityManager` 表示 `RoleRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `RoleRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `RoleRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `RoleRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `closureStore` 表示 `RoleRepository` 中与 `closure Store` 相关的状态、依赖、配置或结果（声明类型 `PostgresqlRoleClosureStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `closureStore` stores the `closure Store`-related state, dependency, configuration, or result of `RoleRepository` (declared type `PostgresqlRoleClosureStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `closureStore` 时应保持 `RoleRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `closureStore`, preserve `RoleRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final PostgresqlRoleClosureStore closureStore;
    /**
     * 字段 `idGenerator` 表示 `RoleRepository` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `RoleRepository` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `RoleRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `RoleRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `databaseClock` 表示 `RoleRepository` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `RoleRepository` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `RoleRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `RoleRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;
    /**
     * 字段 `eventPort` 表示 `RoleRepository` 中与 `event Port` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationEventPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `eventPort` stores the `event Port`-related state, dependency, configuration, or result of `RoleRepository` (declared type `AuthorizationEventPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `eventPort` 时应保持 `RoleRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `eventPort`, preserve `RoleRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationEventPort eventPort;

    /**
     * 构造器 `RoleRepository` 用于创建并初始化 `RoleRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleRepository` creates and initializes `RoleRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param closureStore 输入参数 `closureStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param eventPort 输入参数 `eventPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `withGraphLock` 按照 `RoleRepository` 的职责处理输入，完成 `with Graph Lock` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `withGraphLock` processes its inputs according to `RoleRepository`'s responsibility, performs the `with Graph Lock` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `withGraphLock` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `withGraphLock`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param action 输入参数 `action`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `addEdge` 按照 `RoleRepository` 的职责处理输入，完成 `add Edge` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `addEdge` processes its inputs according to `RoleRepository`'s responsibility, performs the `add Edge` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `addEdge` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `addEdge`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param edge 输入参数 `edge`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `removeEdge` 按照 `RoleRepository` 的职责处理输入，完成 `remove Edge` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `removeEdge` processes its inputs according to `RoleRepository`'s responsibility, performs the `remove Edge` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `removeEdge` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `removeEdge`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param edge 输入参数 `edge`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `rebuildClosure` 按照 `RoleRepository` 的职责处理输入，完成 `rebuild Closure` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rebuildClosure` processes its inputs according to `RoleRepository`'s responsibility, performs the `rebuild Closure` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rebuildClosure` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rebuildClosure`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    public void rebuildClosure(String tenantId, String applicationId) {
        closureStore.rebuild(Long.parseLong(tenantId), Long.parseLong(applicationId));
    }

    /**
     * 方法 `assertRoleVersion` 按照 `RoleRepository` 的职责处理输入，完成 `assert Role Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assertRoleVersion` processes its inputs according to `RoleRepository`'s responsibility, performs the `assert Role Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assertRoleVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assertRoleVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedRoleVersion 输入参数 `expectedRoleVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `recordGraphMutation` 按照 `RoleRepository` 的职责处理输入，完成 `record Graph Mutation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `recordGraphMutation` processes its inputs according to `RoleRepository`'s responsibility, performs the `record Graph Mutation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `recordGraphMutation` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `recordGraphMutation`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param edge 输入参数 `edge`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param added 输入参数 `added`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `create` 按照 `RoleRepository` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `create` processes its inputs according to `RoleRepository`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ignoredNow 输入参数 `ignoredNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `assignPermission` 按照 `RoleRepository` 的职责处理输入，完成 `assign Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assignPermission` processes its inputs according to `RoleRepository`'s responsibility, performs the `assign Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assignPermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assignPermission`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ignoredNow 输入参数 `ignoredNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `assignPermissions` 按照 `RoleRepository` 的职责处理输入，完成 `assign Permissions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assignPermissions` processes its inputs according to `RoleRepository`'s responsibility, performs the `assign Permissions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assignPermissions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assignPermissions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ignoredNow 输入参数 `ignoredNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `removePermission` 按照 `RoleRepository` 的职责处理输入，完成 `remove Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `removePermission` processes its inputs according to `RoleRepository`'s responsibility, performs the `remove Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `removePermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `removePermission`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ignoredNow 输入参数 `ignoredNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `update` 按照 `RoleRepository` 的职责处理输入，完成 `update` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `update` processes its inputs according to `RoleRepository`'s responsibility, performs the `update` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `update` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `update`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ignoredNow 输入参数 `ignoredNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `roles` 按照 `RoleRepository` 的职责处理输入，完成 `roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `roles` processes its inputs according to `RoleRepository`'s responsibility, performs the `roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `roles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `roles`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `impact` 按照 `RoleRepository` 的职责处理输入，完成 `impact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `impact` processes its inputs according to `RoleRepository`'s responsibility, performs the `impact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `impact` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `impact`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `requireRole` 按照 `RoleRepository` 的职责处理输入，完成 `require Role` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireRole` processes its inputs according to `RoleRepository`'s responsibility, performs the `require Role` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireRole` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireRole`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `toView` 按照 `RoleRepository` 的职责处理输入，完成 `to View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toView` processes its inputs according to `RoleRepository`'s responsibility, performs the `to View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toView` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toView`, then continue the business flow using its result, exception, or side effect.
     *
     * @param role 输入参数 `role`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `policyMutation` 按照 `RoleRepository` 的职责处理输入，完成 `policy Mutation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `policyMutation` processes its inputs according to `RoleRepository`'s responsibility, performs the `policy Mutation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `policyMutation` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `policyMutation`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param aggregateType 输入参数 `aggregateType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param aggregateId 输入参数 `aggregateId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param eventType 输入参数 `eventType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RoleFacade.RoleMutationResult policyMutation(
            String tenantId,
            String aggregateType,
            String aggregateId,
            String eventType,
            String actorId,
            Instant now) {
        TenantPO tenant = entityManager.find(
                TenantPO.class, Long.valueOf(tenantId), LockModeType.PESSIMISTIC_WRITE);
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
