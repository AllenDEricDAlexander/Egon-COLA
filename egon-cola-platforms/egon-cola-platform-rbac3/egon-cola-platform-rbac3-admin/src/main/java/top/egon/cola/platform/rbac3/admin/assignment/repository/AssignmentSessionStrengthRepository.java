package top.egon.cola.platform.rbac3.admin.assignment.repository;

import java.time.Instant;

/**
 * 提供角色分配场景所需的会话认证强度查询。
 * Provides session authentication-strength lookup for role-assignment use cases.
 */
@FunctionalInterface
public interface AssignmentSessionStrengthRepository {

    /**
     * 查询指定会话在给定时刻可使用的认证强度。
     * Resolves the authentication strength available to the session at the given time.
     *
     * @param tenantId 租户标识；tenant identifier
     * @param sessionId 会话标识；session identifier
     * @param now 当前数据库时间；current database time
     * @return 认证强度；authentication strength
     */
    String authenticationStrength(String tenantId, String sessionId, Instant now);
}
