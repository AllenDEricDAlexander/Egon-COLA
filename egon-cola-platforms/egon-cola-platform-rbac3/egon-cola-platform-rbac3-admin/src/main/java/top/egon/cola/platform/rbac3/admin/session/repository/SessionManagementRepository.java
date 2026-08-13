package top.egon.cola.platform.rbac3.admin.session.repository;

import top.egon.cola.platform.rbac3.admin.session.domain.vo.SessionVO;

import java.time.Instant;
import java.util.List;

/** 会话管理持久化契约。 Persistence contract for session management. */
public interface SessionManagementRepository {

    /**
     * 查询用户的全部会话。 Finds every session belonging to a user.
     *
     * @param tenantId 租户标识；tenant identifier
     * @param userId 用户标识；user identifier
     * @return 会话列表；session list
     */
    List<SessionVO> findByUser(String tenantId, String userId);

    /**
     * 撤销指定会话。 Revokes one session.
     *
     * @param tenantId 租户标识；tenant identifier
     * @param sessionId 会话标识；session identifier
     * @param now 数据库当前时间；current database time
     * @return 是否发生状态变更；whether state changed
     */
    boolean revoke(String tenantId, String sessionId, Instant now);

    /**
     * 撤销用户的全部活动会话。 Revokes all active sessions for a user.
     *
     * @param tenantId 租户标识；tenant identifier
     * @param userId 用户标识；user identifier
     * @param now 数据库当前时间；current database time
     * @return 被撤销的会话数量；number of revoked sessions
     */
    int revokeAll(String tenantId, String userId, Instant now);
}
