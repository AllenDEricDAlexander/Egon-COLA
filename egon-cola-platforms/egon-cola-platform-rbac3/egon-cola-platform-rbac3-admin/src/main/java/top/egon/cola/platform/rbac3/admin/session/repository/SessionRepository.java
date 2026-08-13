package top.egon.cola.platform.rbac3.admin.session.repository;

import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.Rbac3RuntimePolicy;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.SessionRecordVO;
import top.egon.cola.platform.rbac3.admin.session.service.SessionFacade;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.TokenRecordVO;

/**
     * 类型 `SessionRepository` 位于 `SessionFacade` 内，是接口，用于承载 `Session Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionRepository` is an interface inside `SessionFacade` and carries the responsibility, state, or contract for `Session Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionRepository` 作为 `SessionFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionRepository` as the responsibility boundary of `SessionFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface SessionRepository {

        /**
         * 方法 `create` 按照 `SessionRepository` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `create` processes its inputs according to `SessionRepository`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
         *
         * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param refreshToken 输入参数 `refreshToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void create(
                SessionRecordVO session,
                TokenRecordVO refreshToken,
                Instant now);

        /**
         * 方法 `logout` 按照 `SessionRepository` 的职责处理输入，完成 `logout` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `logout` processes its inputs according to `SessionRepository`'s responsibility, performs the `logout` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        boolean logout(String tenantId, String userId, String sessionId, Instant now);
    }
