package top.egon.cola.platform.rbac3.admin.activation.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.activation.service.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.activation.service.ActiveRoleSetRevalidator;
import top.egon.cola.platform.rbac3.admin.activation.domain.po.SessionActiveRolePO;
import top.egon.cola.platform.rbac3.admin.audit.repository.AuditPort;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;
import top.egon.cola.platform.rbac3.admin.runtime.domain.AuthorizationMutationEntity;
import top.egon.cola.platform.rbac3.admin.session.domain.po.SessionPO;
import top.egon.cola.platform.rbac3.admin.session.repository.jpa.JpaSessionEntityRepository;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationCandidateResolver;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import top.egon.cola.platform.rbac3.admin.activation.repository.ReselectionRepository;
import top.egon.cola.platform.rbac3.admin.activation.repository.ActivationTransaction;
import top.egon.cola.platform.rbac3.admin.activation.domain.dto.ReplaceCommandDTO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.SessionStateVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ResolvedActivationVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.TransactionResultVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.CurrentStateVO;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditEventVO;

/**
 * 类型 `JpaSessionActiveRoleRepository` 位于当前包内，是类型，用于承载 `Session Active Role Repository` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaSessionActiveRoleRepository` is a type in its package and carries the responsibility, state, or contract for `Session Active Role Repository`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Serializes activation and refresh on the same session row lock.
 */
@Repository
public class JpaSessionActiveRoleRepository
        implements ActivationTransaction,
        ReselectionRepository {

    /**
     * 字段 `sessionRepository` 表示 `JpaSessionActiveRoleRepository` 中与 `session Repository` 相关的状态、依赖、配置或结果（声明类型 `JpaSessionEntityRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionRepository` stores the `session Repository`-related state, dependency, configuration, or result of `JpaSessionActiveRoleRepository` (declared type `JpaSessionEntityRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionRepository` 时应保持 `JpaSessionActiveRoleRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionRepository`, preserve `JpaSessionActiveRoleRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final JpaSessionEntityRepository sessionRepository;
    /**
     * 字段 `entityManager` 表示 `JpaSessionActiveRoleRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaSessionActiveRoleRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaSessionActiveRoleRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaSessionActiveRoleRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `idGenerator` 表示 `JpaSessionActiveRoleRepository` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `JpaSessionActiveRoleRepository` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `JpaSessionActiveRoleRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `JpaSessionActiveRoleRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `auditPort` 表示 `JpaSessionActiveRoleRepository` 中与 `audit Port` 相关的状态、依赖、配置或结果（声明类型 `AuditPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `auditPort` stores the `audit Port`-related state, dependency, configuration, or result of `JpaSessionActiveRoleRepository` (declared type `AuditPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `auditPort` 时应保持 `JpaSessionActiveRoleRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `auditPort`, preserve `JpaSessionActiveRoleRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuditPort auditPort;
    /**
     * 字段 `eventPort` 表示 `JpaSessionActiveRoleRepository` 中与 `event Port` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationEventPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `eventPort` stores the `event Port`-related state, dependency, configuration, or result of `JpaSessionActiveRoleRepository` (declared type `AuthorizationEventPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `eventPort` 时应保持 `JpaSessionActiveRoleRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `eventPort`, preserve `JpaSessionActiveRoleRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationEventPort eventPort;

    /**
     * 构造器 `JpaSessionActiveRoleRepository` 用于创建并初始化 `JpaSessionActiveRoleRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaSessionActiveRoleRepository` creates and initializes `JpaSessionActiveRoleRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaSessionActiveRoleRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaSessionActiveRoleRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param sessionRepository 输入参数 `sessionRepository`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param auditPort 输入参数 `auditPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param eventPort 输入参数 `eventPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaSessionActiveRoleRepository(
            JpaSessionEntityRepository sessionRepository,
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            AuditPort auditPort,
            AuthorizationEventPort eventPort
    ) {
        this.sessionRepository = sessionRepository;
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.auditPort = auditPort;
        this.eventPort = eventPort;
    }

    /**
     * 方法 `replace` 按照 `JpaSessionActiveRoleRepository` 的职责处理输入，完成 `replace` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `replace` processes its inputs according to `JpaSessionActiveRoleRepository`'s responsibility, performs the `replace` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `replace` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `replace`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resolutionFactory 输入参数 `resolutionFactory`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public TransactionResultVO replace(
            ReplaceCommandDTO command,
            Instant now,
            Function<SessionStateVO,
                    ResolvedActivationVO> resolutionFactory
    ) {
        SessionPO session = locked(command.tenantId(), command.identitySub(),
                command.userId(), command.sessionId(), now);
        if (session.getContextVersion() != command.expectedContextVersion()) {
            throw new Rbac3RuleViolation("ROLE_ACTIVATION_VERSION_CONFLICT");
        }
        List<SessionActiveRolePO> currentEntities = activeRoles(
                Long.valueOf(command.tenantId()), Long.valueOf(command.sessionId()));
        Map<String, Set<String>> current = roots(currentEntities);
        var state = new SessionStateVO(
                command.tenantId(), command.userId(), command.sessionId(), current,
                session.getAuthVersionAtIssue(), session.getSessionVersion(),
                session.getPolicyVersionAtIssue(), session.getActiveRootChecksum(),
                session.isActivationRequired(), session.getAbsoluteExpiresAt(),
                session.getAuthenticationStrength().name(),
                session.getStrongAuthenticatedAt());
        ResolvedActivationVO resolved = resolutionFactory.apply(state);
        Map<String, Set<String>> requested = resolved.resolution()
                .activeRoleSet().rootsByApplication();
        boolean unchanged = !session.isActivationRequired()
                && current.equals(requested)
                && session.getAuthVersionAtIssue() == resolved.facts().authVersion()
                && session.getPolicyVersionAtIssue() == resolved.facts().policyVersion();
        if (unchanged) {
            return result(resolved, false, null, current, session);
        }

        String mutationId = Long.toString(idGenerator.nextLongId());
        var mutation = new AuthorizationMutationEntity(
                Long.valueOf(mutationId),
                Long.valueOf(command.tenantId()),
                Long.valueOf(command.userId()),
                Long.valueOf(command.sessionId()),
                AuthorizationMutationEntity.ScopeType.SESSION,
                command.commandId(),
                session.getSessionVersion(),
                Math.incrementExact(session.getSessionVersion()),
                session.getAuthVersionAtIssue(),
                resolved.facts().authVersion(),
                session.getPolicyVersionAtIssue(),
                resolved.facts().policyVersion(),
                command.actorId(),
                now);
        entityManager.persist(mutation);
        session.activateRoles(
                resolved.facts().authVersion(),
                resolved.facts().policyVersion(),
                resolved.resolution().snapshot().checksum(),
                command.actorId(),
                now);
        currentEntities.forEach(entityManager::remove);
        Map<String, Set<String>> assignmentIdsByRoot =
                new RoleActivationCandidateResolver().resolve(
                        resolved.facts().assignments(),
                        resolved.facts().hierarchy(), now);
        for (Map.Entry<String, Set<String>> application : requested.entrySet()) {
            for (String rootRoleId : application.getValue()) {
                Set<String> assignmentIds = assignmentIdsByRoot.get(rootRoleId);
                if (assignmentIds == null || assignmentIds.isEmpty()) {
                    throw new IllegalStateException(
                            "missing eligible assignment evidence for root " + rootRoleId);
                }
                entityManager.persist(new SessionActiveRolePO(
                        Long.valueOf(command.tenantId()),
                        Long.valueOf(command.sessionId()),
                        Long.valueOf(application.getKey()),
                        Long.valueOf(rootRoleId),
                        session.getSessionVersion(),
                        new ArrayList<>(assignmentIds),
                        now));
            }
        }
        mutation.committed(now, command.actorId());
        auditPort.append(new AuditEventVO(
                command.tenantId(), "SESSION_ACTIVE_ROLES_REPLACED",
                command.actorId(), "SESSION", command.sessionId(),
                command.commandId(), command.commandId(),
                Map.of(
                        "oldContextVersion", Long.toString(command.expectedContextVersion()),
                        "newContextVersion", Long.toString(session.getContextVersion()),
                        "snapshotChecksum", session.getActiveRootChecksum()),
                now));
        eventPort.enqueue(new AuthorizationEventPort.AuthorizationEvent(
                command.tenantId(), "SESSION", command.sessionId(),
                "RBAC3_SESSION_ACTIVE_ROLES_REPLACED",
                Map.of(
                        "mutationId", mutationId,
                        "contextVersion", Long.toString(session.getContextVersion()),
                        "authVersion", Long.toString(session.getAuthVersionAtIssue()),
                        "policyVersion", Long.toString(session.getPolicyVersionAtIssue())),
                command.commandId()));
        entityManager.flush();
        return result(resolved, true, mutationId, requested, session);
    }

    /**
     * 方法 `current` 按照 `JpaSessionActiveRoleRepository` 的职责处理输入，完成 `current` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `current` processes its inputs according to `JpaSessionActiveRoleRepository`'s responsibility, performs the `current` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `current` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `current`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public CurrentStateVO current(
            String tenantId,
            String identitySub,
            String userId,
            String sessionId,
            Instant now
    ) {
        SessionPO session = sessionRepository.findByTenantIdAndSessionId(
                        Long.valueOf(tenantId), Long.valueOf(sessionId))
                .orElseThrow(() -> new Rbac3RuleViolation("RESOURCE_NOT_FOUND"));
        if (!session.getIdentitySub().equals(identitySub)
                || !session.getUserId().equals(Long.valueOf(userId))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        session.requireActive(now);
        return new CurrentStateVO(
                roots(activeRoles(Long.valueOf(tenantId), Long.valueOf(sessionId))),
                session.getAuthVersionAtIssue(),
                session.getSessionVersion(),
                session.getPolicyVersionAtIssue(),
                session.getActiveRootChecksum(),
                session.isActivationRequired());
    }

    /**
     * 方法 `result` 按照 `JpaSessionActiveRoleRepository` 的职责处理输入，完成 `result` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `result` processes its inputs according to `JpaSessionActiveRoleRepository`'s responsibility, performs the `result` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `result` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `result`, then continue the business flow using its result, exception, or side effect.
     *
     * @param resolved 输入参数 `resolved`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param changed 输入参数 `changed`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roots 输入参数 `roots`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private TransactionResultVO result(
            ResolvedActivationVO resolved,
            boolean changed,
            String mutationId,
            Map<String, Set<String>> roots,
            SessionPO session
    ) {
        return new TransactionResultVO(
                resolved, changed, mutationId, immutable(roots),
                session.getAuthVersionAtIssue(), session.getSessionVersion(),
                session.getPolicyVersionAtIssue(), session.getActiveRootChecksum(),
                session.getAbsoluteExpiresAt());
    }

    /**
     * 方法 `markFenced` 按照 `JpaSessionActiveRoleRepository` 的职责处理输入，完成 `mark Fenced` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `markFenced` processes its inputs according to `JpaSessionActiveRoleRepository`'s responsibility, performs the `mark Fenced` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `markFenced` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `markFenced`, then continue the business flow using its result, exception, or side effect.
     *
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void markFenced(String mutationId, Instant now) {
        mutation(mutationId).fenced(now, "role-activation");
    }

    /**
     * 方法 `markCompleted` 按照 `JpaSessionActiveRoleRepository` 的职责处理输入，完成 `mark Completed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `markCompleted` processes its inputs according to `JpaSessionActiveRoleRepository`'s responsibility, performs the `mark Completed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `markCompleted` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `markCompleted`, then continue the business flow using its result, exception, or side effect.
     *
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void markCompleted(String mutationId, Instant now) {
        AuthorizationMutationEntity mutation = mutation(mutationId);
        mutation.projected(now, "role-activation");
        mutation.completed(now, "role-activation");
    }

    /**
     * 方法 `markRecoveryRequired` 按照 `JpaSessionActiveRoleRepository` 的职责处理输入，完成 `mark Recovery Required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `markRecoveryRequired` processes its inputs according to `JpaSessionActiveRoleRepository`'s responsibility, performs the `mark Recovery Required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `markRecoveryRequired` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `markRecoveryRequired`, then continue the business flow using its result, exception, or side effect.
     *
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void markRecoveryRequired(
            String mutationId,
            String reasonCode,
            Instant now
    ) {
        mutation(mutationId).recoveryRequired(
                reasonCode, now, "role-activation-recovery");
    }

    /**
     * 方法 `requireReselection` 按照 `JpaSessionActiveRoleRepository` 的职责处理输入，完成 `require Reselection` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireReselection` processes its inputs according to `JpaSessionActiveRoleRepository`'s responsibility, performs the `require Reselection` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireReselection` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireReselection`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedSessionVersion 输入参数 `expectedSessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void requireReselection(
            String tenantId,
            String sessionId,
            long expectedSessionVersion,
            Instant now,
            String actorId
    ) {
        SessionPO session = sessionRepository.lockByTenantIdAndSessionId(
                        Long.valueOf(tenantId), Long.valueOf(sessionId))
                .orElseThrow(() -> new Rbac3RuleViolation("RESOURCE_NOT_FOUND"));
        if (session.getSessionVersion() != expectedSessionVersion) {
            throw new Rbac3RuleViolation("ROLE_ACTIVATION_VERSION_CONFLICT");
        }
        session.requireRoleReselection(actorId, now);
        activeRoles(Long.valueOf(tenantId), Long.valueOf(sessionId))
                .forEach(entityManager::remove);
    }

    /**
     * 方法 `mutation` 按照 `JpaSessionActiveRoleRepository` 的职责处理输入，完成 `mutation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `mutation` processes its inputs according to `JpaSessionActiveRoleRepository`'s responsibility, performs the `mutation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `mutation` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `mutation`, then continue the business flow using its result, exception, or side effect.
     *
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private AuthorizationMutationEntity mutation(String mutationId) {
        AuthorizationMutationEntity mutation = entityManager.find(
                AuthorizationMutationEntity.class,
                Long.valueOf(mutationId),
                LockModeType.PESSIMISTIC_WRITE);
        if (mutation == null) {
            throw new IllegalStateException("authorization mutation is missing");
        }
        return mutation;
    }

    /**
     * 方法 `locked` 按照 `JpaSessionActiveRoleRepository` 的职责处理输入，完成 `locked` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `locked` processes its inputs according to `JpaSessionActiveRoleRepository`'s responsibility, performs the `locked` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `locked` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `locked`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private SessionPO locked(
            String tenantId,
            String identitySub,
            String userId,
            String sessionId,
            Instant now
    ) {
        SessionPO session = sessionRepository.lockByTenantIdAndSessionId(
                        Long.valueOf(tenantId), Long.valueOf(sessionId))
                .orElseThrow(() -> new Rbac3RuleViolation("RESOURCE_NOT_FOUND"));
        if (!session.getIdentitySub().equals(identitySub)
                || !session.getUserId().equals(Long.valueOf(userId))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        session.requireActive(now);
        return session;
    }

    /**
     * 方法 `activeRoles` 按照 `JpaSessionActiveRoleRepository` 的职责处理输入，完成 `active Roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activeRoles` processes its inputs according to `JpaSessionActiveRoleRepository`'s responsibility, performs the `active Roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activeRoles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activeRoles`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private List<SessionActiveRolePO> activeRoles(Long tenantId, Long sessionId) {
        return entityManager.createQuery("""
                        select active from SessionActiveRoleEntity active
                         where active.tenantId = :tenantId
                           and active.sessionId = :sessionId
                         order by active.applicationId, active.rootRoleId
                        """, SessionActiveRolePO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("sessionId", sessionId)
                .getResultList();
    }

    /**
     * 方法 `roots` 按照 `JpaSessionActiveRoleRepository` 的职责处理输入，完成 `roots` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `roots` processes its inputs according to `JpaSessionActiveRoleRepository`'s responsibility, performs the `roots` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `roots` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `roots`, then continue the business flow using its result, exception, or side effect.
     *
     * @param activeRoles 输入参数 `activeRoles`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Map<String, Set<String>> roots(
            List<SessionActiveRolePO> activeRoles
    ) {
        var result = new TreeMap<String, Set<String>>();
        for (SessionActiveRolePO active : activeRoles) {
            result.computeIfAbsent(
                    active.getApplicationId().toString(), ignored -> new TreeSet<>())
                    .add(active.getRootRoleId().toString());
        }
        return immutable(result);
    }

    /**
     * 方法 `immutable` 按照 `JpaSessionActiveRoleRepository` 的职责处理输入，完成 `immutable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `immutable` processes its inputs according to `JpaSessionActiveRoleRepository`'s responsibility, performs the `immutable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `immutable` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `immutable`, then continue the business flow using its result, exception, or side effect.
     *
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Map<String, Set<String>> immutable(Map<String, Set<String>> values) {
        var result = new TreeMap<String, Set<String>>();
        values.forEach((key, roots) -> result.put(
                key, Collections.unmodifiableSet(new TreeSet<>(roots))));
        return Collections.unmodifiableMap(result);
    }
}
