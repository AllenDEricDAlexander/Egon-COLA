package top.egon.cola.platform.rbac3.admin.authorization.repository;

import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationDecision;
import top.egon.cola.platform.rbac3.contract.authorization.DataScopeDecision;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
     * 会话授权传播 Fence 校验端口。
     * Port for checking a session authorization propagation fence.
     * 语义与用法：将 `FenceVerifier` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FenceVerifier` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface FenceVerifier {

        /**
         * 判断指定会话是否仍处于传播 Fence 中。
         * Determines whether the specified session is still fenced.
         *
         * @param tenantId 租户标识 / tenant identifier
         * @param sessionId 会话标识 / session identifier
         * @return 若存在 Fence 则为 {@code true} / {@code true} when fenced
         * 用法：调用 `isFenced` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `isFenced`, then continue the business flow using its result, exception, or side effect.
         */
        boolean isFenced(String tenantId, String sessionId);
    }
