package top.egon.cola.platform.rbac3.admin.session.repository.jpa;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.session.service.AuthorizationContextFacade;
import top.egon.cola.platform.rbac3.admin.session.domain.po.SessionPO;

import java.time.Instant;
import java.util.Optional;
import top.egon.cola.platform.rbac3.admin.session.repository.AuthorizationContextRepository;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.ActiveMembershipVO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.AuthorizationContextVO;
import top.egon.cola.platform.rbac3.admin.session.domain.exception.ConcurrentContextCreationException;

/**
 * 类型 `JpaAuthorizationContextRepository` 位于当前包内，是类型，用于承载 `Authorization Context Repository` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaAuthorizationContextRepository` is a type in its package and carries the responsibility, state, or contract for `Authorization Context Repository`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * PostgreSQL store for IdP-bound RBAC3 authorization contexts.
 */
@Repository
public class JpaAuthorizationContextRepository
        implements AuthorizationContextRepository {

    /**
     * 字段 `sessions` 表示 `JpaAuthorizationContextRepository` 中与 `sessions` 相关的状态、依赖、配置或结果（声明类型 `SessionRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessions` stores the `sessions`-related state, dependency, configuration, or result of `JpaAuthorizationContextRepository` (declared type `SessionRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessions` 时应保持 `JpaAuthorizationContextRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessions`, preserve `JpaAuthorizationContextRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final JpaSessionEntityRepository sessions;

    /**
     * 构造器 `JpaAuthorizationContextRepository` 用于创建并初始化 `JpaAuthorizationContextRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaAuthorizationContextRepository` creates and initializes `JpaAuthorizationContextRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaAuthorizationContextRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaAuthorizationContextRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param sessions 输入参数 `sessions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaAuthorizationContextRepository(JpaSessionEntityRepository sessions) {
        this.sessions = sessions;
    }

    /**
     * 方法 `find` 按照 `JpaAuthorizationContextRepository` 的职责处理输入，完成 `find` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `find` processes its inputs according to `JpaAuthorizationContextRepository`'s responsibility, performs the `find` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `find` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `find`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<AuthorizationContextVO> find(
            String tenantId, String sessionId) {
        return sessions.findByTenantIdAndSessionId(
                        Long.valueOf(tenantId), Long.valueOf(sessionId))
                .map(JpaAuthorizationContextRepository::toContext);
    }

    /**
     * 方法 `create` 按照 `JpaAuthorizationContextRepository` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `create` processes its inputs according to `JpaAuthorizationContextRepository`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
     *
     * @param entityId 输入参数 `entityId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param membership 输入参数 `membership`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expiresAt 输入参数 `expiresAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     * @throws ConcurrentContextCreationException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
     */
    @Override
    @Transactional
    public AuthorizationContextVO create(
            long entityId,
            ActiveMembershipVO membership,
            String sessionId,
            Instant now,
            Instant expiresAt)
            throws ConcurrentContextCreationException {
        SessionPO entity = SessionPO.authorizationContext(
                entityId, Long.valueOf(membership.tenantId()),
                Long.valueOf(membership.rbac3UserId()), Long.valueOf(sessionId),
                membership.identitySub(), membership.authVersion(),
                membership.policyVersion(), now, expiresAt, membership.identitySub());
        try {
            return toContext(sessions.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new ConcurrentContextCreationException(
                    exception);
        }
    }

    /**
     * 方法 `toContext` 按照 `JpaAuthorizationContextRepository` 的职责处理输入，完成 `to Context` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toContext` processes its inputs according to `JpaAuthorizationContextRepository`'s responsibility, performs the `to Context` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toContext` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toContext`, then continue the business flow using its result, exception, or side effect.
     *
     * @param entity 输入参数 `entity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static AuthorizationContextVO toContext(
            SessionPO entity) {
        return new AuthorizationContextVO(
                entity.getId().toString(), entity.getTenantId().toString(),
                entity.getSessionId().toString(), entity.getIdentitySub(),
                entity.getUserId().toString(), entity.getAuthVersionAtIssue(),
                entity.getContextVersion(), entity.getPolicyVersionAtIssue(),
                entity.isActivationRequired(), entity.getStatus().name(),
                entity.getAuthenticatedAt(), entity.getContextExpiresAt());
    }
}
