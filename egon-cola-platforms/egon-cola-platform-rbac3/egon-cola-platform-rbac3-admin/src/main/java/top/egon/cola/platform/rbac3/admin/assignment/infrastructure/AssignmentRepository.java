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

/**
 * 类型 `AssignmentRepository` 位于当前包内，是类型，用于承载 `Assignment Repository` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AssignmentRepository` is a type in its package and carries the responsibility, state, or contract for `Assignment Repository`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `AssignmentRepository` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `AssignmentRepository` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Repository
public class AssignmentRepository implements
        AssignmentFacade.AssignmentFactSource,
        AssignmentFacade.AssignmentStore {

    /**
     * 字段 `entityManager` 表示 `AssignmentRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `AssignmentRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `AssignmentRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `AssignmentRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `idGenerator` 表示 `AssignmentRepository` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `AssignmentRepository` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `AssignmentRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `AssignmentRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `databaseClock` 表示 `AssignmentRepository` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `AssignmentRepository` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `AssignmentRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `AssignmentRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `AssignmentRepository` 用于创建并初始化 `AssignmentRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AssignmentRepository` creates and initializes `AssignmentRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AssignmentRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AssignmentRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AssignmentRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock
    ) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `load` 按照 `AssignmentRepository` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `load` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `assign` 按照 `AssignmentRepository` 的职责处理输入，完成 `assign` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assign` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `assign` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assign` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assign`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `loadChange` 按照 `AssignmentRepository` 的职责处理输入，完成 `load Change` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `loadChange` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `load Change` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `loadChange` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `loadChange`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `assignments` 按照 `AssignmentRepository` 的职责处理输入，完成 `assignments` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assignments` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `assignments` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assignments` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assignments`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `change` 按照 `AssignmentRepository` 的职责处理输入，完成 `change` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `change` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `change` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `change` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `change`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `requireNoOverlap` 按照 `AssignmentRepository` 的职责处理输入，完成 `require No Overlap` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireNoOverlap` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `require No Overlap` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireNoOverlap` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireNoOverlap`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `advanceUserAuthorizationVersion` 按照 `AssignmentRepository` 的职责处理输入，完成 `advance User Authorization Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `advanceUserAuthorizationVersion` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `advance User Authorization Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `advanceUserAuthorizationVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `advanceUserAuthorizationVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `revoke` 按照 `AssignmentRepository` 的职责处理输入，完成 `revoke` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `revoke` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `revoke` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `revoke` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `revoke`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param assignmentId 输入参数 `assignmentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `requireAssignment` 按照 `AssignmentRepository` 的职责处理输入，完成 `require Assignment` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireAssignment` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `require Assignment` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireAssignment` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireAssignment`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param assignmentId 输入参数 `assignmentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `effectiveStatus` 按照 `AssignmentRepository` 的职责处理输入，完成 `effective Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `effectiveStatus` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `effective Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `effectiveStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `effectiveStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param assignment 输入参数 `assignment`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `uniqueRoot` 按照 `AssignmentRepository` 的职责处理输入，完成 `unique Root` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `uniqueRoot` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `unique Root` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `uniqueRoot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `uniqueRoot`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `activeRoles` 按照 `AssignmentRepository` 的职责处理输入，完成 `active Roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activeRoles` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `active Roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activeRoles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activeRoles`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `ssdSets` 按照 `AssignmentRepository` 的职责处理输入，完成 `ssd Sets` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `ssdSets` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `ssd Sets` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `ssdSets` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `ssdSets`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `prerequisites` 按照 `AssignmentRepository` 的职责处理输入，完成 `prerequisites` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `prerequisites` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `prerequisites` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `prerequisites` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `prerequisites`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `cardinality` 按照 `AssignmentRepository` 的职责处理输入，完成 `cardinality` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `cardinality` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `cardinality` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `cardinality` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `cardinality`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rootRoleId 输入参数 `rootRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param targetUserId 输入参数 `targetUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `activeAssignments` 按照 `AssignmentRepository` 的职责处理输入，完成 `active Assignments` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activeAssignments` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `active Assignments` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activeAssignments` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activeAssignments`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rootRoleId 输入参数 `rootRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeId 输入参数 `scopeId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `resolveScopeId` 按照 `AssignmentRepository` 的职责处理输入，完成 `resolve Scope Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resolveScopeId` processes its inputs according to `AssignmentRepository`'s responsibility, performs the `resolve Scope Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `resolveScopeId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resolveScopeId`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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
