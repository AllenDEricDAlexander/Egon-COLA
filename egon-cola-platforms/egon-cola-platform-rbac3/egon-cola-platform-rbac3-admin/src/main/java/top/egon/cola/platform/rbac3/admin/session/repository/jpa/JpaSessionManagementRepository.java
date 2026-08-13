package top.egon.cola.platform.rbac3.admin.session.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.session.domain.po.RefreshTokenPO;
import top.egon.cola.platform.rbac3.admin.session.domain.po.SessionPO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.SessionVO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.TerminationVO;
import top.egon.cola.platform.rbac3.admin.session.repository.SessionManagementRepository;
import top.egon.cola.platform.rbac3.admin.session.service.SessionRuntimeSynchronizer;
import top.egon.cola.platform.rbac3.admin.session.service.SessionSecurityEventRecorder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** 会话管理命令与查询的 JPA 适配器。 JPA adapter for session-management commands and queries. */
@Repository
public class JpaSessionManagementRepository implements SessionManagementRepository {

    /** 会话管理操作的审计主体。 Audit actor used for session-management operations. */
    private static final String ACTOR_ID = "session-administration";
    /** JPA 实体管理器。 JPA entity manager. */
    private final EntityManager entityManager;
    /** 会话运行时同步器。 Session runtime synchronizer. */
    private final SessionRuntimeSynchronizer runtimeSynchronizer;
    /** 会话安全事件记录器。 Session security-event recorder. */
    private final SessionSecurityEventRecorder securityEventRecorder;

    /**
     * 创建会话管理持久化适配器。 Creates the session-management persistence adapter.
     *
     * @param entityManager JPA 实体管理器；JPA entity manager
     * @param runtimeSynchronizer 会话运行时同步器；session runtime synchronizer
     * @param securityEventRecorder 会话安全事件记录器；session security-event recorder
     */
    public JpaSessionManagementRepository(
            EntityManager entityManager,
            SessionRuntimeSynchronizer runtimeSynchronizer,
            SessionSecurityEventRecorder securityEventRecorder) {
        this.entityManager = entityManager;
        this.runtimeSynchronizer = runtimeSynchronizer;
        this.securityEventRecorder = securityEventRecorder;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<SessionVO> findByUser(String tenantId, String userId) {
        return entityManager.createQuery("""
                        select s from SessionEntity s
                         where s.tenantId = :tenantId and s.userId = :userId
                         order by s.authenticatedAt desc, s.sessionId desc
                        """, SessionPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("userId", Long.valueOf(userId))
                .getResultList().stream()
                .map(session -> new SessionVO(
                        session.getSessionId().toString(), session.getStatus().name(),
                        session.getSessionVersion(),
                        session.getAuthenticationStrength().name(),
                        session.getAuthenticatedAt(), session.getStrongAuthenticatedAt(),
                        session.getLastSeenAt(), session.getAbsoluteExpiresAt()))
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public boolean revoke(String tenantId, String sessionId, Instant now) {
        List<SessionPO> rows = entityManager.createQuery("""
                        select s from SessionEntity s
                         where s.tenantId = :tenantId and s.sessionId = :sessionId
                        """, SessionPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("sessionId", Long.valueOf(sessionId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (rows.isEmpty()) {
            return false;
        }
        SessionPO session = rows.getFirst();
        boolean changed = session.revoke("ADMIN_REVOKE", ACTOR_ID, now);
        if (changed) {
            revokeRefreshTokens(
                    Long.valueOf(tenantId), List.of(Long.valueOf(sessionId)), now);
            recordTermination(session, now);
            runtimeSynchronizer.synchronize(
                    tenantId, session.getUserId().toString(), sessionId, now);
        }
        return changed;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public int revokeAll(String tenantId, String userId, Instant now) {
        List<SessionPO> sessions = entityManager.createQuery("""
                        select s from SessionEntity s
                         where s.tenantId = :tenantId and s.userId = :userId
                        """, SessionPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("userId", Long.valueOf(userId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        List<Long> changedSessionIds = new ArrayList<>();
        for (SessionPO session : sessions) {
            if (session.revoke("ADMIN_REVOKE_ALL", ACTOR_ID, now)) {
                changedSessionIds.add(session.getSessionId());
            }
        }
        revokeRefreshTokens(Long.valueOf(tenantId), changedSessionIds, now);
        sessions.stream()
                .filter(session -> changedSessionIds.contains(session.getSessionId()))
                .forEach(session -> {
                    recordTermination(session, now);
                    runtimeSynchronizer.synchronize(
                            tenantId, userId, session.getSessionId().toString(), now);
                });
        return changedSessionIds.size();
    }

    /** 撤销指定会话族的刷新令牌。 Revokes refresh tokens for the selected sessions. */
    private void revokeRefreshTokens(
            Long tenantId,
            List<Long> sessionIds,
            Instant now) {
        if (sessionIds.isEmpty()) {
            return;
        }
        List<RefreshTokenPO> tokens = entityManager.createQuery("""
                        select t from RefreshTokenEntity t
                         where t.tenantId = :tenantId and t.sessionId in :sessionIds
                        """, RefreshTokenPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("sessionIds", sessionIds)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        tokens.forEach(token -> token.revoke(now, ACTOR_ID));
    }

    /** 记录会话终止事件。 Records a session-termination event. */
    private void recordTermination(SessionPO session, Instant occurredAt) {
        securityEventRecorder.record(new TerminationVO(
                session.getTenantId().toString(), session.getUserId().toString(),
                session.getSessionId().toString(), session.getSessionVersion(),
                session.getStatus().name(), session.getRevokeReason(), ACTOR_ID,
                occurredAt));
    }
}
