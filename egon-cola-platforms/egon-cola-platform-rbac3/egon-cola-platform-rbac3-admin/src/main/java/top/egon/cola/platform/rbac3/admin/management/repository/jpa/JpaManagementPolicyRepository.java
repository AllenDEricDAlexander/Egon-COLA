package top.egon.cola.platform.rbac3.admin.management.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementOperationPO;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementPolicyPO;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementRolePO;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementSubjectPO;
import top.egon.cola.platform.rbac3.core.delegation.ManagementPolicyDecisionService;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.UserPositionSnapshotStatusEnum;
import top.egon.cola.platform.rbac3.admin.management.domain.dto.SavePolicyCommandDTO;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementPolicySubjectPO;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementPolicyScopePO;
import top.egon.cola.platform.rbac3.admin.assignment.domain.enums.UserRoleAssignmentStatusEnum;
import top.egon.cola.platform.rbac3.admin.management.repository.ManagementPolicyFactRepository;
import top.egon.cola.platform.rbac3.admin.management.repository.ManagementPolicyControlRepository;
import top.egon.cola.platform.rbac3.admin.management.domain.dto.SaveCommandDTO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.PolicyVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicyRestrictionsVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.CapabilityVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagedUserVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagedRoleVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicyScopeVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicySubjectVO;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementOperationOperationEnum;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementPolicyStatusEnum;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementPolicyRiskLevelEnum;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementPolicyAuthenticationStrengthEnum;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementSubjectSubjectTypeEnum;

/**
 * 类型 `JpaManagementPolicyRepository` 位于当前包内，是类型，用于承载 `Management Policy Repository` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaManagementPolicyRepository` is a type in its package and carries the responsibility, state, or contract for `Management Policy Repository`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `JpaManagementPolicyRepository` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `JpaManagementPolicyRepository` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Repository
public class JpaManagementPolicyRepository implements
        ManagementPolicyFactRepository,
        ManagementPolicyControlRepository {

    /**
     * 字段 `entityManager` 表示 `JpaManagementPolicyRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaManagementPolicyRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaManagementPolicyRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaManagementPolicyRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `idGenerator` 表示 `JpaManagementPolicyRepository` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `JpaManagementPolicyRepository` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `JpaManagementPolicyRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `JpaManagementPolicyRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `databaseClock` 表示 `JpaManagementPolicyRepository` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `JpaManagementPolicyRepository` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `JpaManagementPolicyRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `JpaManagementPolicyRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `JpaManagementPolicyRepository` 用于创建并初始化 `JpaManagementPolicyRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaManagementPolicyRepository` creates and initializes `JpaManagementPolicyRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaManagementPolicyRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaManagementPolicyRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaManagementPolicyRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock
    ) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `policies` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `policies` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `policies` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `policies` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        List<ManagementPolicyPO> candidates = entityManager.createQuery("""
                        select distinct p from ManagementPolicyEntity p,
                             ManagementSubjectPO s
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
                        """, ManagementPolicyPO.class)
                .setParameter("tenantId", tenant)
                .setParameter("status", ManagementPolicyStatusEnum.ACTIVE)
                .setParameter("now", databaseNow)
                .setParameter("userType", ManagementSubjectSubjectTypeEnum.USER)
                .setParameter("roleType", ManagementSubjectSubjectTypeEnum.ROLE)
                .setParameter("positionType",
                        ManagementSubjectSubjectTypeEnum.POSITION)
                .setParameter("assignmentStatus", UserRoleAssignmentStatusEnum.ACTIVE)
                .setParameter("positionStatus",
                        top.egon.cola.platform.rbac3.admin.directory.domain.enums
                                .UserPositionSnapshotStatusEnum.ACTIVE)
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
     * 方法 `savePolicy` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `save Policy` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `savePolicy` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `save Policy` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `savePolicy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `savePolicy`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Transactional
    private String savePolicy(SavePolicyCommandDTO command) {
        Instant now = databaseClock.transactionNow();
        Long tenantId = Long.valueOf(command.tenantId());
        Long policyId;
        if (command.policyId() == null) {
            policyId = idGenerator.nextLongId();
            entityManager.persist(new ManagementPolicyPO(
                    policyId, tenantId, command.policyCode(), command.name(),
                    command.validFrom(), command.validTo(), command.maximumAssignmentDays(),
                    ManagementPolicyRiskLevelEnum.valueOf(command.maximumRiskLevel()),
                    ManagementPolicyAuthenticationStrengthEnum.valueOf(
                            command.requiredAuthenticationStrength()),
                    command.requireReason(), command.requireTicket(),
                    command.includeInheritedSubjectRoles(),
                    command.requireAllAffiliationsInScope(), command.actorId(), now));
        } else {
            policyId = Long.valueOf(command.policyId());
            ManagementPolicyPO current = entityManager.find(
                    ManagementPolicyPO.class, policyId, LockModeType.PESSIMISTIC_WRITE);
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
                    ManagementPolicyRiskLevelEnum.valueOf(
                            command.maximumRiskLevel()),
                    ManagementPolicyAuthenticationStrengthEnum.valueOf(
                            command.requiredAuthenticationStrength()),
                    command.requireReason(), command.requireTicket(),
                    command.includeInheritedSubjectRoles(),
                    command.requireAllAffiliationsInScope(), command.actorId(), now);
            deleteChildren(tenantId, policyId);
        }
        command.subjects().forEach(subject -> entityManager.persist(
                new ManagementSubjectPO(
                        tenantId, policyId,
                        ManagementSubjectSubjectTypeEnum.valueOf(subject.type()),
                        Long.valueOf(subject.id()))));
        command.scopes().forEach(scope -> insertScope(tenantId, policyId, scope));
        command.activationRootRoleIds().forEach(roleId -> entityManager.persist(
                new ManagementRolePO(tenantId, policyId, Long.valueOf(roleId))));
        command.operations().forEach(operation -> entityManager.persist(
                new ManagementOperationPO(
                        tenantId, policyId,
                        ManagementOperationOperationEnum.valueOf(operation))));
        return policyId.toString();
    }

    /**
     * 方法 `policies` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `policies` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `policies` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `policies` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `policies` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `policies`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PolicyVO> policies(String tenantId) {
        return entityManager.createQuery("""
                        select p from ManagementPolicyEntity p
                         where p.tenantId = :tenantId
                         order by p.policyCode
                        """, ManagementPolicyPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .getResultList().stream()
                .map(this::view)
                .toList();
    }

    /**
     * 方法 `policy` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `policy` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `policy` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `policy` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
    public PolicyVO policy(
            String tenantId,
            String policyId
    ) {
        return view(requirePolicy(
                Long.valueOf(tenantId), Long.valueOf(policyId), false));
    }

    /**
     * 方法 `save` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `save` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `save` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `save` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `save` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `save`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public PolicyVO save(
            SaveCommandDTO command
    ) {
        var restrictions = command.restrictions();
        String policyId = savePolicy(new SavePolicyCommandDTO(
                command.tenantId(), command.policyId(), command.policyCode(),
                command.name(), command.validFrom(), command.validTo(),
                restrictions.maximumAssignmentDays(),
                restrictions.maximumRiskLevel(),
                restrictions.requiredAuthenticationStrength(),
                restrictions.requireReason(), restrictions.requireTicket(),
                restrictions.includeInheritedSubjectRoles(),
                restrictions.requireAllAffiliationsInScope(),
                command.subjects().stream()
                        .map(subject -> new ManagementPolicySubjectPO(subject.type(), subject.id()))
                        .toList(),
                command.scopes().stream()
                        .map(scope -> new ManagementPolicyScopePO(scope.type(), scope.referenceId()))
                        .toList(),
                command.activationRootRoleIds(), command.operations(),
                command.expectedVersion(), command.actorId()));
        entityManager.flush();
        return view(requirePolicy(
                Long.valueOf(command.tenantId()), Long.valueOf(policyId), false));
    }

    /**
     * 方法 `disable` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `disable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `disable` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `disable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
    public PolicyVO disable(
            String tenantId,
            String policyId,
            long expectedVersion,
            String actorId
    ) {
        ManagementPolicyPO policy = requirePolicy(
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
     * 方法 `capabilities` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `capabilities` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `capabilities` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `capabilities` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
    public CapabilityVO capabilities(
            String tenantId,
            String subjectUserId,
            Instant databaseNow
    ) {
        List<Long> policyIds = subjectPolicyIds(
                Long.valueOf(tenantId), Long.valueOf(subjectUserId), databaseNow);
        if (policyIds.isEmpty()) {
            return new CapabilityVO(
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
        return new CapabilityVO(
                policyIds.stream().map(Object::toString).toList(),
                operationRows.stream().map(Object::toString)
                        .collect(java.util.stream.Collectors.toCollection(
                                LinkedHashSet::new)),
                roleRows.stream().map(Object::toString).toList());
    }

    /**
     * 方法 `manageableUsers` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `manageable Users` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `manageableUsers` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `manageable Users` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
    public List<ManagedUserVO> manageableUsers(
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
                .map(row -> new ManagedUserVO(
                        row[0].toString(), row[1].toString(), row[2].toString()))
                .toList();
    }

    /**
     * 方法 `manageableRoles` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `manageable Roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `manageableRoles` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `manageable Roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
    public List<ManagedRoleVO> manageableRoles(
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
                .map(row -> new ManagedRoleVO(
                        row[0].toString(), row[1].toString(), row[2].toString(),
                        row[3].toString(), (Boolean) row[4]))
                .toList();
    }

    /**
     * 方法 `subjectPolicyIds` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `subject Policy Ids` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `subjectPolicyIds` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `subject Policy Ids` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `fact` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `fact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fact` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `fact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
            ManagementPolicyPO policy,
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
                        """, ManagementOperationOperationEnum.class)
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
                policy.getStatus() == ManagementPolicyStatusEnum.ACTIVE);
    }

    /**
     * 方法 `targetInScope` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `target In ManagementPolicyScopePO` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `targetInScope` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `target In ManagementPolicyScopePO` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `deleteChildren` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `delete Children` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `deleteChildren` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `delete Children` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `insertScope` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `insert ManagementPolicyScopePO` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `insertScope` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `insert ManagementPolicyScopePO` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `insertScope` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `insertScope`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scope 输入参数 `scope`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void insertScope(Long tenantId, Long policyId, ManagementPolicyScopePO scope) {
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
     * 方法 `requirePolicy` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `require Policy` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requirePolicy` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `require Policy` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requirePolicy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requirePolicy`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param forUpdate 输入参数 `forUpdate`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ManagementPolicyPO requirePolicy(
            Long tenantId,
            Long policyId,
            boolean forUpdate
    ) {
        ManagementPolicyPO policy = forUpdate
                ? entityManager.find(
                        ManagementPolicyPO.class, policyId,
                        LockModeType.PESSIMISTIC_WRITE)
                : entityManager.find(ManagementPolicyPO.class, policyId);
        if (policy == null || !tenantId.equals(policy.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return policy;
    }

    /**
     * 方法 `view` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `view` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `view` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `view` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `view` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `view`, then continue the business flow using its result, exception, or side effect.
     *
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private PolicyVO view(ManagementPolicyPO policy) {
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
        List<ManagementPolicySubjectVO> subjects = subjectRows.stream()
                .map(row -> new ManagementPolicySubjectVO(
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
        List<ManagementPolicyScopeVO> scopes = scopeRows.stream()
                .map(row -> new ManagementPolicyScopeVO(
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
        return new PolicyVO(
                policy.getId().toString(), policy.getPolicyCode(), policy.getName(),
                policy.getStatus().name(), policy.getValidFrom(), policy.getValidTo(),
                new ManagementPolicyRestrictionsVO(
                        policy.getMaximumAssignmentDays(),
                        policy.getMaximumRiskLevel().name(),
                        policy.getRequiredAuthenticationStrength().name(),
                        policy.isRequireReason(), policy.isRequireTicket(),
                        policy.isIncludeInheritedSubjectRoles(),
                        policy.isRequireAllAffiliationsInScope()),
                subjects, scopes, roles, operations, policy.getVersion());
    }

    /**
     * 方法 `stringSet` 按照 `JpaManagementPolicyRepository` 的职责处理输入，完成 `string Set` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `stringSet` processes its inputs according to `JpaManagementPolicyRepository`'s responsibility, performs the `string Set` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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



    }
