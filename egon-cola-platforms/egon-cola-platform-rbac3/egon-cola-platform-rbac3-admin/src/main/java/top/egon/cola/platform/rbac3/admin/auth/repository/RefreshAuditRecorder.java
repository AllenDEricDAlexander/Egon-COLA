package top.egon.cola.platform.rbac3.admin.auth.repository;

import top.egon.cola.platform.rbac3.admin.session.service.RefreshTokenService;
import top.egon.cola.platform.rbac3.contract.auth.RefreshResult;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.RefreshAuditVO;
import top.egon.cola.platform.rbac3.admin.auth.service.RefreshFacade;

/**
     * 类型 `RefreshAuditRecorder` 位于 `RefreshFacade` 内，是接口，用于承载 `Refresh Audit Recorder` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RefreshAuditRecorder` is an interface inside `RefreshFacade` and carries the responsibility, state, or contract for `Refresh Audit Recorder`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RefreshAuditRecorder` 作为 `RefreshFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RefreshAuditRecorder` as the responsibility boundary of `RefreshFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface RefreshAuditRecorder {

        /**
         * 方法 `record` 按照 `RefreshAuditRecorder` 的职责处理输入，完成 `record` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `record` processes its inputs according to `RefreshAuditRecorder`'s responsibility, performs the `record` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `record` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `record`, then continue the business flow using its result, exception, or side effect.
         *
         * @param audit 输入参数 `audit`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void record(RefreshAuditVO audit);
    }
