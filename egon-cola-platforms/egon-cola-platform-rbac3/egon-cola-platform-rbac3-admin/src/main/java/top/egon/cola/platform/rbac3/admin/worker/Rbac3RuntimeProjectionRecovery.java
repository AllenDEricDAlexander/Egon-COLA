package top.egon.cola.platform.rbac3.admin.worker;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.activation.service.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.activation.repository.jpa.JpaRoleActivationFactRepository;
import top.egon.cola.platform.rbac3.admin.activation.repository.jpa.JpaSessionActiveRoleRepository;
import top.egon.cola.platform.rbac3.admin.integration.outbox.Rbac3RuntimeProjectionDeliveryHandler;
import top.egon.cola.platform.rbac3.admin.session.domain.po.SessionPO;
import top.egon.cola.platform.rbac3.admin.session.service.SessionRuntimeSynchronizer;
import top.egon.cola.platform.rbac3.admin.snapshot.application.LoginRuntimeProjectionFactory;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.admin.snapshot.infrastructure.RedisAuthorizationRuntimeStore;
import top.egon.cola.platform.rbac3.core.activation.DefaultRoleActivationResolver;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationInput;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.SessionStatusEnum;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.RuntimePublicationVO;

/**
 * 类型 `Rbac3RuntimeProjectionRecovery` 位于当前包内，是类型，用于承载 `Rbac3 Runtime Projection Recovery` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3RuntimeProjectionRecovery` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Runtime Projection Recovery`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Rebuilds immutable Session projections from committed PostgreSQL facts.
 */
@Component
public class Rbac3RuntimeProjectionRecovery implements
        AuthorizationMutationRecoveryWorker.ProjectionExecutor,
        RuntimeSnapshotRebuildWorker.RebuildPort,
        SessionRuntimeSynchronizer {

    /**
     * 字段 `entityManager` 表示 `Rbac3RuntimeProjectionRecovery` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `Rbac3RuntimeProjectionRecovery` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `Rbac3RuntimeProjectionRecovery` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `Rbac3RuntimeProjectionRecovery`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `factStore` 表示 `Rbac3RuntimeProjectionRecovery` 中与 `fact Store` 相关的状态、依赖、配置或结果（声明类型 `JpaRoleActivationFactRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `factStore` stores the `fact Store`-related state, dependency, configuration, or result of `Rbac3RuntimeProjectionRecovery` (declared type `JpaRoleActivationFactRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `factStore` 时应保持 `Rbac3RuntimeProjectionRecovery` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `factStore`, preserve `Rbac3RuntimeProjectionRecovery`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final JpaRoleActivationFactRepository factStore;
    /**
     * 字段 `activeRoleRepository` 表示 `Rbac3RuntimeProjectionRecovery` 中与 `active Role Repository` 相关的状态、依赖、配置或结果（声明类型 `JpaSessionActiveRoleRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `activeRoleRepository` stores the `active Role Repository`-related state, dependency, configuration, or result of `Rbac3RuntimeProjectionRecovery` (declared type `JpaSessionActiveRoleRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `activeRoleRepository` 时应保持 `Rbac3RuntimeProjectionRecovery` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `activeRoleRepository`, preserve `Rbac3RuntimeProjectionRecovery`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final JpaSessionActiveRoleRepository activeRoleRepository;
    /**
     * 字段 `snapshotProjector` 表示 `Rbac3RuntimeProjectionRecovery` 中与 `snapshot Projector` 相关的状态、依赖、配置或结果（声明类型 `SessionSnapshotProjector`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `snapshotProjector` stores the `snapshot Projector`-related state, dependency, configuration, or result of `Rbac3RuntimeProjectionRecovery` (declared type `SessionSnapshotProjector`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `snapshotProjector` 时应保持 `Rbac3RuntimeProjectionRecovery` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `snapshotProjector`, preserve `Rbac3RuntimeProjectionRecovery`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionSnapshotProjector snapshotProjector;
    /**
     * 字段 `loginProjectionFactory` 表示 `Rbac3RuntimeProjectionRecovery` 中与 `login Projection Factory` 相关的状态、依赖、配置或结果（声明类型 `LoginRuntimeProjectionFactory`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `loginProjectionFactory` stores the `login Projection Factory`-related state, dependency, configuration, or result of `Rbac3RuntimeProjectionRecovery` (declared type `LoginRuntimeProjectionFactory`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `loginProjectionFactory` 时应保持 `Rbac3RuntimeProjectionRecovery` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `loginProjectionFactory`, preserve `Rbac3RuntimeProjectionRecovery`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LoginRuntimeProjectionFactory loginProjectionFactory;
    /**
     * 字段 `runtimeStore` 表示 `Rbac3RuntimeProjectionRecovery` 中与 `runtime Store` 相关的状态、依赖、配置或结果（声明类型 `RedisAuthorizationRuntimeStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimeStore` stores the `runtime Store`-related state, dependency, configuration, or result of `Rbac3RuntimeProjectionRecovery` (declared type `RedisAuthorizationRuntimeStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimeStore` 时应保持 `Rbac3RuntimeProjectionRecovery` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimeStore`, preserve `Rbac3RuntimeProjectionRecovery`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RedisAuthorizationRuntimeStore runtimeStore;
    /**
     * 字段 `clock` 表示 `Rbac3RuntimeProjectionRecovery` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `Rbac3RuntimeProjectionRecovery` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `Rbac3RuntimeProjectionRecovery` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `Rbac3RuntimeProjectionRecovery`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 构造器 `Rbac3RuntimeProjectionRecovery` 用于创建并初始化 `Rbac3RuntimeProjectionRecovery` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3RuntimeProjectionRecovery` creates and initializes `Rbac3RuntimeProjectionRecovery`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3RuntimeProjectionRecovery` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3RuntimeProjectionRecovery`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param factStore 输入参数 `factStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param activeRoleRepository 输入参数 `activeRoleRepository`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotProjector 输入参数 `snapshotProjector`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param loginProjectionFactory 输入参数 `loginProjectionFactory`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimeStore 输入参数 `runtimeStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3RuntimeProjectionRecovery(
            EntityManager entityManager,
            JpaRoleActivationFactRepository factStore,
            JpaSessionActiveRoleRepository activeRoleRepository,
            SessionSnapshotProjector snapshotProjector,
            LoginRuntimeProjectionFactory loginProjectionFactory,
            RedisAuthorizationRuntimeStore runtimeStore,
            Clock clock) {
        this.entityManager = entityManager;
        this.factStore = factStore;
        this.activeRoleRepository = activeRoleRepository;
        this.snapshotProjector = snapshotProjector;
        this.loginProjectionFactory = loginProjectionFactory;
        this.runtimeStore = runtimeStore;
        this.clock = clock;
    }

    /**
     * 方法 `project` 按照 `Rbac3RuntimeProjectionRecovery` 的职责处理输入，完成 `project` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `project` processes its inputs according to `Rbac3RuntimeProjectionRecovery`'s responsibility, performs the `project` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `project` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `project`, then continue the business flow using its result, exception, or side effect.
     *
     * @param mutation 输入参数 `mutation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional(readOnly = true)
    public void project(AuthorizationMutationRecoveryWorker.MutationWork mutation) {
        rebuildSessions(
                mutation.tenantId(), mutation.scopeType(), mutation.scopeId());
    }

    /**
     * 方法 `rebuild` 按照 `Rbac3RuntimeProjectionRecovery` 的职责处理输入，完成 `rebuild` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rebuild` processes its inputs according to `Rbac3RuntimeProjectionRecovery`'s responsibility, performs the `rebuild` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rebuild` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rebuild`, then continue the business flow using its result, exception, or side effect.
     *
     * @param event 输入参数 `event`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional(readOnly = true)
    public void rebuild(Rbac3RuntimeProjectionDeliveryHandler.EventEnvelope event) {
        if ("SESSION".equals(event.aggregateType())) {
            rebuildSessions(event.tenantId(), "SESSION", event.aggregateId());
            return;
        }
        String userId = event.payload().get("userId");
        rebuildSessions(
                event.tenantId(), userId == null ? "TENANT" : "USER",
                userId == null ? event.tenantId() : userId);
    }

    /**
     * 方法 `rebuildSessions` 按照 `Rbac3RuntimeProjectionRecovery` 的职责处理输入，完成 `rebuild Sessions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rebuildSessions` processes its inputs according to `Rbac3RuntimeProjectionRecovery`'s responsibility, performs the `rebuild Sessions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rebuildSessions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rebuildSessions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeId 输入参数 `scopeId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void rebuildSessions(String tenantId, String scopeType, String scopeId) {
        Instant now = clock.instant();
        for (SessionPO session : sessions(tenantId, scopeType, scopeId)) {
            synchronize(session, now);
        }
    }

    /**
     * 方法 `synchronize` 按照 `Rbac3RuntimeProjectionRecovery` 的职责处理输入，完成 `synchronize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `synchronize` processes its inputs according to `Rbac3RuntimeProjectionRecovery`'s responsibility, performs the `synchronize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `synchronize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `synchronize`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional(readOnly = true)
    public void synchronize(
            String tenantId,
            String userId,
            String sessionId,
            Instant generatedAt) {
        SessionPO session = entityManager.createQuery("""
                        select s from SessionEntity s
                         where s.tenantId = :tenantId
                           and s.userId = :userId
                           and s.sessionId = :sessionId
                        """, SessionPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("userId", Long.valueOf(userId))
                .setParameter("sessionId", Long.valueOf(sessionId))
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("runtime session is missing"));
        synchronize(session, generatedAt);
    }

    /**
     * 方法 `synchronize` 按照 `Rbac3RuntimeProjectionRecovery` 的职责处理输入，完成 `synchronize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `synchronize` processes its inputs according to `Rbac3RuntimeProjectionRecovery`'s responsibility, performs the `synchronize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `synchronize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `synchronize`, then continue the business flow using its result, exception, or side effect.
     *
     * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void synchronize(SessionPO session, Instant generatedAt) {
        if (session.getStatus() == SessionStatusEnum.ACTIVE
                && session.getIdleExpiresAt().isAfter(generatedAt)
                && session.getAbsoluteExpiresAt().isAfter(generatedAt)
                && !session.isActivationRequired()) {
            rebuild(session, generatedAt);
            return;
        }
        publishMinimum(session, generatedAt);
    }

    /**
     * 方法 `publishMinimum` 按照 `Rbac3RuntimeProjectionRecovery` 的职责处理输入，完成 `publish Minimum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `publishMinimum` processes its inputs according to `Rbac3RuntimeProjectionRecovery`'s responsibility, performs the `publish Minimum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `publishMinimum` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `publishMinimum`, then continue the business flow using its result, exception, or side effect.
     *
     * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void publishMinimum(SessionPO session, Instant generatedAt) {
        var state = new LoginRuntimeProjectionFactory.RuntimeState(
                session.getTenantId().toString(), session.getUserId().toString(),
                session.getSessionId().toString(), runtimeStatus(session, generatedAt),
                session.getAuthVersionAtIssue(), session.getSessionVersion(),
                session.getPolicyVersionAtIssue(), session.getAbsoluteExpiresAt());
        var projection = loginProjectionFactory.create(state, generatedAt);
        runtimeStore.publish(new RedisAuthorizationRuntimeStore.PublishCommand(
                state.tenantId(), state.userId(), state.sessionId(), state.authVersion(),
                state.sessionVersion(), state.policyVersion(), projection));
    }

    /**
     * 方法 `runtimeStatus` 按照 `Rbac3RuntimeProjectionRecovery` 的职责处理输入，完成 `runtime Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `runtimeStatus` processes its inputs according to `Rbac3RuntimeProjectionRecovery`'s responsibility, performs the `runtime Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `runtimeStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `runtimeStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String runtimeStatus(SessionPO session, Instant generatedAt) {
        if (session.getStatus() == SessionStatusEnum.ACTIVE
                && (!session.getIdleExpiresAt().isAfter(generatedAt)
                || !session.getAbsoluteExpiresAt().isAfter(generatedAt))) {
            return SessionStatusEnum.EXPIRED.name();
        }
        return session.getStatus().name();
    }

    /**
     * 方法 `rebuild` 按照 `Rbac3RuntimeProjectionRecovery` 的职责处理输入，完成 `rebuild` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rebuild` processes its inputs according to `Rbac3RuntimeProjectionRecovery`'s responsibility, performs the `rebuild` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rebuild` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rebuild`, then continue the business flow using its result, exception, or side effect.
     *
     * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void rebuild(SessionPO session, Instant now) {
        String tenantId = session.getTenantId().toString();
        String userId = session.getUserId().toString();
        String sessionId = session.getSessionId().toString();
        var current = activeRoleRepository.current(
                tenantId, session.getIdentitySub(), userId, sessionId, now);
        var requestedRoots = current.rootsByApplication().values().stream()
                .flatMap(java.util.Collection::stream)
                .sorted()
                .toList();
        if (requestedRoots.isEmpty()) {
            return;
        }
        var facts = factStore.load(tenantId, userId, now);
        long resolverSessionVersion = Math.max(0L, current.sessionVersion() - 1L);
        var resolution = new DefaultRoleActivationResolver().resolve(
                new RoleActivationInput(
                        tenantId, userId, sessionId, requestedRoots,
                        facts.assignments(), facts.hierarchy(), facts.dsdSets(),
                        facts.authorizationFacts(), current.authVersion(),
                        resolverSessionVersion, current.policyVersion(), now));
        SessionSnapshotProjector.Projection projection = snapshotProjector.project(
                new SessionSnapshotProjector.ProjectionCommand(
                        tenantId, session.getIdentitySub(), userId, sessionId,
                        current.authVersion(),
                        current.sessionVersion(), current.policyVersion(),
                        session.getAbsoluteExpiresAt(), resolution, facts, now));
        runtimeStore.publish(new RuntimePublicationVO(
                tenantId, userId, sessionId, current.authVersion(),
                current.sessionVersion(), current.policyVersion(), projection));
    }

    /**
     * 方法 `sessions` 按照 `Rbac3RuntimeProjectionRecovery` 的职责处理输入，完成 `sessions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sessions` processes its inputs according to `Rbac3RuntimeProjectionRecovery`'s responsibility, performs the `sessions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `sessions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `sessions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeId 输入参数 `scopeId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private List<SessionPO> sessions(
            String tenantId,
            String scopeType,
            String scopeId) {
        StringBuilder hql = new StringBuilder("""
                select s from SessionEntity s
                 where s.tenantId = :tenantId
                """);
        if ("SESSION".equals(scopeType)) {
            hql.append(" and s.sessionId = :scopeId");
        } else if ("USER".equals(scopeType)) {
            hql.append(" and s.userId = :scopeId");
        } else if (!"TENANT".equals(scopeType)) {
            throw new IllegalArgumentException(
                    "unsupported authorization mutation scope: " + scopeType);
        }
        var query = entityManager.createQuery(hql.toString(), SessionPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId));
        if (!"TENANT".equals(scopeType)) {
            query.setParameter("scopeId", Long.valueOf(scopeId));
        }
        return new ArrayList<>(query.getResultList());
    }
}
