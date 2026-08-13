package top.egon.cola.platform.rbac3.admin.management.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
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

/**
 * 类型 `ManagementPolicyRepository` 位于当前包内，是类型，用于承载 `Management Policy Repository` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManagementPolicyRepository` is a type in its package and carries the responsibility, state, or contract for `Management Policy Repository`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ManagementPolicyRepository` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ManagementPolicyRepository` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Repository
public class ManagementPolicyRepository implements
        ManagementPolicyFacade.PolicyFactSource,
        ManagementPolicyFacade.ControlStore {

    /**
     * 字段 `entityManager` 表示 `ManagementPolicyRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `ManagementPolicyRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `ManagementPolicyRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `ManagementPolicyRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `idGenerator` 表示 `ManagementPolicyRepository` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `ManagementPolicyRepository` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `ManagementPolicyRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `ManagementPolicyRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `databaseClock` 表示 `ManagementPolicyRepository` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `ManagementPolicyRepository` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `ManagementPolicyRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `ManagementPolicyRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `ManagementPolicyRepository` 用于创建并初始化 `ManagementPolicyRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementPolicyRepository` creates and initializes `ManagementPolicyRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementPolicyRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementPolicyRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ManagementPolicyRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock
    ) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `policies` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `policies` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `policies` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `policies` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `policies` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `policies`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param subjectId 输入参数 `subjectId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param targetUserId 输入参数 `targetUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `savePolicy` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `save Policy` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `savePolicy` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `save Policy` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `savePolicy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `savePolicy`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `policies` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `policies` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `policies` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `policies` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `policies` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `policies`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `policy` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `policy` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `policy` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `policy` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `policy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `policy`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public ManagementPolicyFacade.PolicyView policy(
            String tenantId,
            String policyId
    ) {
        return view(requirePolicy(
                Long.valueOf(tenantId), Long.valueOf(policyId), false));
    }

    /**
     * 方法 `save` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `save` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `save` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `save` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `save` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `save`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `disable` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `disable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `disable` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `disable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `disable` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `disable`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `capabilities` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `capabilities` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `capabilities` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `capabilities` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `capabilities` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `capabilities`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param subjectUserId 输入参数 `subjectUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `manageableUsers` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `manageable Users` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `manageableUsers` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `manageable Users` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `manageableUsers` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `manageableUsers`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param subjectUserId 输入参数 `subjectUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `manageableRoles` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `manageable Roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `manageableRoles` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `manageable Roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `manageableRoles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `manageableRoles`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param subjectUserId 输入参数 `subjectUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `subjectPolicyIds` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `subject Policy Ids` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `subjectPolicyIds` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `subject Policy Ids` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `subjectPolicyIds` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `subjectPolicyIds`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param subjectUserId 输入参数 `subjectUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `fact` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `fact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fact` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `fact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fact` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fact`, then continue the business flow using its result, exception, or side effect.
     *
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param subjectId 输入参数 `subjectId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param targetUserId 输入参数 `targetUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param targetAllowed 输入参数 `targetAllowed`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `targetInScope` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `target In Scope` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `targetInScope` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `target In Scope` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `targetInScope` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `targetInScope`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param subjectUserId 输入参数 `subjectUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param targetUserId 输入参数 `targetUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requireAllAffiliations 输入参数 `requireAllAffiliations`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `deleteChildren` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `delete Children` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `deleteChildren` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `delete Children` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `deleteChildren` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `deleteChildren`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `insertScope` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `insert Scope` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `insertScope` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `insert Scope` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `insertScope` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `insertScope`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scope 输入参数 `scope`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `requirePolicy` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `require Policy` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requirePolicy` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `require Policy` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requirePolicy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requirePolicy`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param forUpdate 输入参数 `forUpdate`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `view` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `view` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `view` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `view` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `view` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `view`, then continue the business flow using its result, exception, or side effect.
     *
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `stringSet` 按照 `ManagementPolicyRepository` 的职责处理输入，完成 `string Set` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `stringSet` processes its inputs according to `ManagementPolicyRepository`'s responsibility, performs the `string Set` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `stringSet` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `stringSet`, then continue the business flow using its result, exception, or side effect.
     *
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Set<String> stringSet(List<Long> values) {
        return values.stream().map(Object::toString)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 类型 `SavePolicyCommand` 位于 `ManagementPolicyRepository` 内，是记录类型，用于承载 `Save Policy Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SavePolicyCommand` is a record inside `ManagementPolicyRepository` and carries the responsibility, state, or contract for `Save Policy Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SavePolicyCommand` 作为 `ManagementPolicyRepository` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SavePolicyCommand` as the responsibility boundary of `ManagementPolicyRepository`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param policyId 记录组件 `policyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyId` carries constructor data whose meaning is defined by the record contract.
     * @param policyCode 记录组件 `policyCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyCode` carries constructor data whose meaning is defined by the record contract.
     * @param name 记录组件 `name` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `name` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param maximumAssignmentDays 记录组件 `maximumAssignmentDays` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumAssignmentDays` carries constructor data whose meaning is defined by the record contract.
     * @param maximumRiskLevel 记录组件 `maximumRiskLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumRiskLevel` carries constructor data whose meaning is defined by the record contract.
     * @param requiredAuthenticationStrength 记录组件 `requiredAuthenticationStrength` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requiredAuthenticationStrength` carries constructor data whose meaning is defined by the record contract.
     * @param requireReason 记录组件 `requireReason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requireReason` carries constructor data whose meaning is defined by the record contract.
     * @param requireTicket 记录组件 `requireTicket` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requireTicket` carries constructor data whose meaning is defined by the record contract.
     * @param includeInheritedSubjectRoles 记录组件 `includeInheritedSubjectRoles` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `includeInheritedSubjectRoles` carries constructor data whose meaning is defined by the record contract.
     * @param requireAllAffiliationsInScope 记录组件 `requireAllAffiliationsInScope` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requireAllAffiliationsInScope` carries constructor data whose meaning is defined by the record contract.
     * @param subjects 记录组件 `subjects` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `subjects` carries constructor data whose meaning is defined by the record contract.
     * @param scopes 记录组件 `scopes` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopes` carries constructor data whose meaning is defined by the record contract.
     * @param activationRootRoleIds 记录组件 `activationRootRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRootRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param operations 记录组件 `operations` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operations` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record SavePolicyCommand(
            /**
             * 字段 `tenantId` 表示 `SavePolicyCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `policyId` 表示 `SavePolicyCommand` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyId` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyId`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String policyId,
            /**
             * 字段 `policyCode` 表示 `SavePolicyCommand` 中与 `policy Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyCode` stores the `policy Code`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyCode` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyCode`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String policyCode,
            /**
             * 字段 `name` 表示 `SavePolicyCommand` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `name` stores the `name`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `name` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `name`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String name,
            /**
             * 字段 `validFrom` 表示 `SavePolicyCommand` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `SavePolicyCommand` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `maximumAssignmentDays` 表示 `SavePolicyCommand` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Integer maximumAssignmentDays,
            /**
             * 字段 `maximumRiskLevel` 表示 `SavePolicyCommand` 中与 `maximum Risk Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumRiskLevel` stores the `maximum Risk Level`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumRiskLevel` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumRiskLevel`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String maximumRiskLevel,
            /**
             * 字段 `requiredAuthenticationStrength` 表示 `SavePolicyCommand` 中与 `required Authentication Strength` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requiredAuthenticationStrength` stores the `required Authentication Strength`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requiredAuthenticationStrength` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requiredAuthenticationStrength`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requiredAuthenticationStrength,
            /**
             * 字段 `requireReason` 表示 `SavePolicyCommand` 中与 `require Reason` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requireReason` stores the `require Reason`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requireReason` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requireReason`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean requireReason,
            /**
             * 字段 `requireTicket` 表示 `SavePolicyCommand` 中与 `require Ticket` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requireTicket` stores the `require Ticket`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requireTicket` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requireTicket`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean requireTicket,
            /**
             * 字段 `includeInheritedSubjectRoles` 表示 `SavePolicyCommand` 中与 `include Inherited Subject Roles` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `includeInheritedSubjectRoles` stores the `include Inherited Subject Roles`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `includeInheritedSubjectRoles` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `includeInheritedSubjectRoles`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean includeInheritedSubjectRoles,
            /**
             * 字段 `requireAllAffiliationsInScope` 表示 `SavePolicyCommand` 中与 `require All Affiliations In Scope` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requireAllAffiliationsInScope` stores the `require All Affiliations In Scope`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requireAllAffiliationsInScope` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requireAllAffiliationsInScope`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean requireAllAffiliationsInScope,
            /**
             * 字段 `subjects` 表示 `SavePolicyCommand` 中与 `subjects` 相关的状态、依赖、配置或结果（声明类型 `List&lt;Subject&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subjects` stores the `subjects`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `List&lt;Subject&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subjects` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subjects`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<Subject> subjects,
            /**
             * 字段 `scopes` 表示 `SavePolicyCommand` 中与 `scopes` 相关的状态、依赖、配置或结果（声明类型 `List&lt;Scope&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopes` stores the `scopes`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `List&lt;Scope&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopes` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopes`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<Scope> scopes,
            /**
             * 字段 `activationRootRoleIds` 表示 `SavePolicyCommand` 中与 `activation Root Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRootRoleIds` stores the `activation Root Role Ids`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRootRoleIds` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRootRoleIds`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> activationRootRoleIds,
            /**
             * 字段 `operations` 表示 `SavePolicyCommand` 中与 `operations` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `operations` stores the `operations`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `operations` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `operations`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<String> operations,
            /**
             * 字段 `expectedVersion` 表示 `SavePolicyCommand` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedVersion,
            /**
             * 字段 `actorId` 表示 `SavePolicyCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `SavePolicyCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `SavePolicyCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `SavePolicyCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {
        /**
         * 构造器 `SavePolicyCommand` 用于创建并初始化 `SavePolicyCommand` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SavePolicyCommand` creates and initializes `SavePolicyCommand`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SavePolicyCommand` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SavePolicyCommand`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyCode 输入参数 `policyCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param maximumAssignmentDays 输入参数 `maximumAssignmentDays`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param maximumRiskLevel 输入参数 `maximumRiskLevel`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requiredAuthenticationStrength 输入参数 `requiredAuthenticationStrength`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requireReason 输入参数 `requireReason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requireTicket 输入参数 `requireTicket`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param includeInheritedSubjectRoles 输入参数 `includeInheritedSubjectRoles`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requireAllAffiliationsInScope 输入参数 `requireAllAffiliationsInScope`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param subjects 输入参数 `subjects`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopes 输入参数 `scopes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param activationRootRoleIds 输入参数 `activationRootRoleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param operations 输入参数 `operations`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SavePolicyCommand {
            subjects = List.copyOf(subjects);
            scopes = List.copyOf(scopes);
            activationRootRoleIds = List.copyOf(activationRootRoleIds);
            operations = Set.copyOf(operations);
        }
    }

    /**
     * 类型 `Subject` 位于 `ManagementPolicyRepository` 内，是记录类型，用于承载 `Subject` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Subject` is a record inside `ManagementPolicyRepository` and carries the responsibility, state, or contract for `Subject`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Subject` 作为 `ManagementPolicyRepository` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Subject` as the responsibility boundary of `ManagementPolicyRepository`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param type 记录组件 `type` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `type` carries constructor data whose meaning is defined by the record contract.
     * @param id 记录组件 `id` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `id` carries constructor data whose meaning is defined by the record contract.
     */
    public record Subject(/**
 * 字段 `type` 表示 `Subject` 中与 `type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `type` stores the `type`-related state, dependency, configuration, or result of `Subject` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `type` 时应保持 `Subject` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `type`, preserve `Subject`'s lifecycle, immutability, and thread-safety constraints.
 */ String type, /**
 * 字段 `id` 表示 `Subject` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `id` stores the `id`-related state, dependency, configuration, or result of `Subject` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `id` 时应保持 `Subject` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `id`, preserve `Subject`'s lifecycle, immutability, and thread-safety constraints.
 */ String id) {
    }

    /**
     * 类型 `Scope` 位于 `ManagementPolicyRepository` 内，是记录类型，用于承载 `Scope` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Scope` is a record inside `ManagementPolicyRepository` and carries the responsibility, state, or contract for `Scope`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Scope` 作为 `ManagementPolicyRepository` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Scope` as the responsibility boundary of `ManagementPolicyRepository`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param type 记录组件 `type` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `type` carries constructor data whose meaning is defined by the record contract.
     * @param referenceId 记录组件 `referenceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `referenceId` carries constructor data whose meaning is defined by the record contract.
     */
    public record Scope(/**
 * 字段 `type` 表示 `Scope` 中与 `type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `type` stores the `type`-related state, dependency, configuration, or result of `Scope` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `type` 时应保持 `Scope` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `type`, preserve `Scope`'s lifecycle, immutability, and thread-safety constraints.
 */ String type, /**
 * 字段 `referenceId` 表示 `Scope` 中与 `reference Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `referenceId` stores the `reference Id`-related state, dependency, configuration, or result of `Scope` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `referenceId` 时应保持 `Scope` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `referenceId`, preserve `Scope`'s lifecycle, immutability, and thread-safety constraints.
 */ String referenceId) {
    }
}
