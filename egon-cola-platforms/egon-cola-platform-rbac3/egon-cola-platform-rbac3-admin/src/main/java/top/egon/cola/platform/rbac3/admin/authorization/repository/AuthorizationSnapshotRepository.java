package top.egon.cola.platform.rbac3.admin.authorization.repository;

import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.SnapshotRecordVO;

/**
 * 用户授权快照读取端口。
 * Port for loading user authorization snapshots.
     * 语义与用法：将 `AuthorizationSnapshotRepository` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationSnapshotRepository` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface AuthorizationSnapshotRepository {

        /**
         * 按租户和 IdP 主体读取授权快照记录。
         * Loads an authorization snapshot record by tenant and IdP subject.
         *
         * @param tenantId 租户标识 / tenant identifier
         * @param identitySub IdP 稳定主体标识 / stable IdP subject
         * @return 授权快照记录 / authorization snapshot record
         * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
         */
        SnapshotRecordVO load(String tenantId, String identitySub);
    }
