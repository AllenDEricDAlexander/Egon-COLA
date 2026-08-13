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
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.SnapshotRecordVO;

/**
     * 会话授权快照读取端口。
     * Port for loading session authorization snapshots.
     * 语义与用法：将 `AuthorizationSnapshotRepository` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationSnapshotRepository` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface AuthorizationSnapshotRepository {

        /**
         * 按租户和会话读取授权快照记录。
         * Loads an authorization snapshot record by tenant and session.
         *
         * @param tenantId 租户标识 / tenant identifier
         * @param sessionId 会话标识 / session identifier
         * @return 授权快照记录 / authorization snapshot record
         * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
         */
        SnapshotRecordVO load(String tenantId, String sessionId);
    }
