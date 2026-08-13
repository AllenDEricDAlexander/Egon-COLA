package top.egon.cola.platform.rbac3.admin.session.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.session.application.RefreshTokenService;
import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;
import top.egon.cola.platform.rbac3.admin.session.application.SessionRuntimeSynchronizer;
import top.egon.cola.platform.rbac3.admin.session.application.SessionSecurityEventRecorder;
import top.egon.cola.platform.rbac3.admin.session.domain.RefreshTokenEntity;
import top.egon.cola.platform.rbac3.admin.session.domain.SessionEntity;

import java.time.Instant;
import java.util.List;

/**
 * 类型 `JpaSessionStore` 位于当前包内，是类型，用于承载 `Jpa Session Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaSessionStore` is a type in its package and carries the responsibility, state, or contract for `Jpa Session Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `JpaSessionStore` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `JpaSessionStore` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Repository
public class JpaSessionStore implements SessionFacade.SessionStore {

    /**
     * 字段 `sessionRepository` 表示 `JpaSessionStore` 中与 `session Repository` 相关的状态、依赖、配置或结果（声明类型 `SessionRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionRepository` stores the `session Repository`-related state, dependency, configuration, or result of `JpaSessionStore` (declared type `SessionRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionRepository` 时应保持 `JpaSessionStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionRepository`, preserve `JpaSessionStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionRepository sessionRepository;
    /**
     * 字段 `entityManager` 表示 `JpaSessionStore` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaSessionStore` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaSessionStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaSessionStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `runtimeSynchronizer` 表示 `JpaSessionStore` 中与 `runtime Synchronizer` 相关的状态、依赖、配置或结果（声明类型 `SessionRuntimeSynchronizer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimeSynchronizer` stores the `runtime Synchronizer`-related state, dependency, configuration, or result of `JpaSessionStore` (declared type `SessionRuntimeSynchronizer`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimeSynchronizer` 时应保持 `JpaSessionStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimeSynchronizer`, preserve `JpaSessionStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionRuntimeSynchronizer runtimeSynchronizer;
    /**
     * 字段 `securityEventRecorder` 表示 `JpaSessionStore` 中与 `security Event Recorder` 相关的状态、依赖、配置或结果（声明类型 `SessionSecurityEventRecorder`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `securityEventRecorder` stores the `security Event Recorder`-related state, dependency, configuration, or result of `JpaSessionStore` (declared type `SessionSecurityEventRecorder`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `securityEventRecorder` 时应保持 `JpaSessionStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `securityEventRecorder`, preserve `JpaSessionStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionSecurityEventRecorder securityEventRecorder;

    /**
     * 构造器 `JpaSessionStore` 用于创建并初始化 `JpaSessionStore` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaSessionStore` creates and initializes `JpaSessionStore`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaSessionStore` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaSessionStore`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param sessionRepository 输入参数 `sessionRepository`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimeSynchronizer 输入参数 `runtimeSynchronizer`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param securityEventRecorder 输入参数 `securityEventRecorder`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaSessionStore(
            SessionRepository sessionRepository,
            EntityManager entityManager,
            SessionRuntimeSynchronizer runtimeSynchronizer,
            SessionSecurityEventRecorder securityEventRecorder) {
        this.sessionRepository = sessionRepository;
        this.entityManager = entityManager;
        this.runtimeSynchronizer = runtimeSynchronizer;
        this.securityEventRecorder = securityEventRecorder;
    }

    /**
     * 方法 `create` 按照 `JpaSessionStore` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `create` processes its inputs according to `JpaSessionStore`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
     *
     * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param refreshToken 输入参数 `refreshToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void create(
            SessionFacade.SessionRecord session,
            RefreshTokenService.TokenRecord refreshToken,
            Instant now) {
        SessionEntity entity = new SessionEntity(
                Long.valueOf(session.entityId()),
                Long.valueOf(session.tenantId()),
                Long.valueOf(session.userId()),
                Long.valueOf(session.sessionId()),
                session.authVersion(),
                session.policyVersion(),
                session.tokenFamilyId(),
                session.deviceIdHash(),
                SessionEntity.AuthenticationStrength.PASSWORD,
                session.authenticatedAt(),
                session.idleExpiresAt(),
                session.absoluteExpiresAt(),
                session.userId());
        RefreshTokenEntity token = new RefreshTokenEntity(
                Long.valueOf(refreshToken.tokenId()),
                Long.valueOf(refreshToken.tenantId()),
                Long.valueOf(refreshToken.sessionId()),
                refreshToken.familyId(),
                refreshToken.generation(),
                refreshToken.tokenHash(),
                now,
                refreshToken.expiresAt(),
                session.userId());
        sessionRepository.save(entity);
        entityManager.persist(token);
    }

    /**
     * 方法 `logout` 按照 `JpaSessionStore` 的职责处理输入，完成 `logout` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `logout` processes its inputs according to `JpaSessionStore`'s responsibility, performs the `logout` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `logout` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `logout`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public boolean logout(String tenantId, String userId, String sessionId, Instant now) {
        SessionEntity session = sessionRepository.lockByTenantIdAndSessionId(
                        Long.valueOf(tenantId), Long.valueOf(sessionId))
                .orElse(null);
        if (session == null || !session.getUserId().equals(Long.valueOf(userId))) {
            return false;
        }
        boolean changed = session.logout(userId, now);
        if (!changed) {
            return false;
        }
        List<RefreshTokenEntity> tokens = entityManager.createQuery(
                        "select t from RefreshTokenEntity t "
                                + "where t.tenantId = :tenantId and t.sessionId = :sessionId",
                        RefreshTokenEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("sessionId", Long.valueOf(sessionId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        tokens.forEach(token -> token.revoke(now, userId));
        securityEventRecorder.record(termination(session, userId, now));
        runtimeSynchronizer.synchronize(tenantId, userId, sessionId, now);
        return true;
    }

    /**
     * 方法 `termination` 按照 `JpaSessionStore` 的职责处理输入，完成 `termination` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `termination` processes its inputs according to `JpaSessionStore`'s responsibility, performs the `termination` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `termination` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `termination`, then continue the business flow using its result, exception, or side effect.
     *
     * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param occurredAt 输入参数 `occurredAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private SessionSecurityEventRecorder.Termination termination(
            SessionEntity session,
            String actorId,
            Instant occurredAt) {
        return new SessionSecurityEventRecorder.Termination(
                session.getTenantId().toString(), session.getUserId().toString(),
                session.getSessionId().toString(), session.getSessionVersion(),
                session.getStatus().name(), session.getRevokeReason(), actorId, occurredAt);
    }
}
