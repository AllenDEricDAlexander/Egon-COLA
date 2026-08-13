package top.egon.cola.platform.rbac3.admin.auth.repository;

import top.egon.cola.platform.rbac3.admin.session.service.SessionFacade;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;
import top.egon.cola.platform.rbac3.contract.auth.LoginResult;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.auth.service.AuthenticationFacade;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.SessionRecordVO;

/**
     * 类型 `LoginRuntimePublisher` 位于 `AuthenticationFacade` 内，是接口，用于承载 `Login Runtime Publisher` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `LoginRuntimePublisher` is an interface inside `AuthenticationFacade` and carries the responsibility, state, or contract for `Login Runtime Publisher`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `LoginRuntimePublisher` 作为 `AuthenticationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `LoginRuntimePublisher` as the responsibility boundary of `AuthenticationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface LoginRuntimePublisher {

        /**
         * 方法 `publish` 按照 `LoginRuntimePublisher` 的职责处理输入，完成 `publish` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `publish` processes its inputs according to `LoginRuntimePublisher`'s responsibility, performs the `publish` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `publish` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `publish`, then continue the business flow using its result, exception, or side effect.
         *
         * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void publish(SessionRecordVO session, Instant generatedAt);
    }
