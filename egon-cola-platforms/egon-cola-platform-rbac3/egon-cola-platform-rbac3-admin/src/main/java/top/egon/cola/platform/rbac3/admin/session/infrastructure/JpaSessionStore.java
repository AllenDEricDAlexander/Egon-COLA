package top.egon.cola.platform.rbac3.admin.session.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.session.application.RefreshTokenService;
import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;
import top.egon.cola.platform.rbac3.admin.session.application.SessionRuntimeSynchronizer;
import top.egon.cola.platform.rbac3.admin.session.application.SessionSecurityEventRecorder;
import top.egon.cola.platform.rbac3.admin.session.domain.RefreshTokenEntity;
import top.egon.cola.platform.rbac3.admin.session.domain.SessionEntity;

import java.time.Instant;
import java.util.List;

@Repository
public class JpaSessionStore implements SessionFacade.SessionStore {

    private final SessionRepository sessionRepository;
    private final EntityManager entityManager;
    private final SessionRuntimeSynchronizer runtimeSynchronizer;
    private final SessionSecurityEventRecorder securityEventRecorder;

    public JpaSessionStore(
            SessionRepository sessionRepository,
            EntityManager entityManager,
            SessionRuntimeSynchronizer runtimeSynchronizer,
            SessionSecurityEventRecorder securityEventRecorder) {
        this.sessionRepository = sessionRepository;
        this.entityManager = entityManager;
        this.runtimeSynchronizer = runtimeSynchronizer;
        this.securityEventRecorder = securityEventRecorder;
    }

    @Override
    @Transactional
    public void create(
            SessionFacade.SessionRecord session,
            RefreshTokenService.TokenRecord refreshToken,
            Instant now) {
        SessionEntity entity = new SessionEntity(
                Long.valueOf(session.entityId()),
                Long.valueOf(session.tenantId()),
                Long.valueOf(session.userId()),
                Long.valueOf(session.sessionId()),
                session.authVersion(),
                session.policyVersion(),
                session.tokenFamilyId(),
                session.deviceIdHash(),
                SessionEntity.AuthenticationStrength.PASSWORD,
                session.authenticatedAt(),
                session.idleExpiresAt(),
                session.absoluteExpiresAt(),
                session.userId());
        RefreshTokenEntity token = new RefreshTokenEntity(
                Long.valueOf(refreshToken.tokenId()),
                Long.valueOf(refreshToken.tenantId()),
                Long.valueOf(refreshToken.sessionId()),
                refreshToken.familyId(),
                refreshToken.generation(),
                refreshToken.tokenHash(),
                now,
                refreshToken.expiresAt(),
                session.userId());
        sessionRepository.save(entity);
        entityManager.persist(token);
    }

    @Override
    @Transactional
    public boolean logout(String tenantId, String userId, String sessionId, Instant now) {
        SessionEntity session = sessionRepository.lockByTenantIdAndSessionId(
                        Long.valueOf(tenantId), Long.valueOf(sessionId))
                .orElse(null);
        if (session == null || !session.getUserId().equals(Long.valueOf(userId))) {
            return false;
        }
        boolean changed = session.logout(userId, now);
        if (!changed) {
            return false;
        }
        List<RefreshTokenEntity> tokens = entityManager.createQuery(
                        "select t from RefreshTokenEntity t "
                                + "where t.tenantId = :tenantId and t.sessionId = :sessionId",
                        RefreshTokenEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("sessionId", Long.valueOf(sessionId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        tokens.forEach(token -> token.revoke(now, userId));
        securityEventRecorder.record(termination(session, userId, now));
        runtimeSynchronizer.synchronize(tenantId, userId, sessionId, now);
        return true;
    }

    private SessionSecurityEventRecorder.Termination termination(
            SessionEntity session,
            String actorId,
            Instant occurredAt) {
        return new SessionSecurityEventRecorder.Termination(
                session.getTenantId().toString(), session.getUserId().toString(),
                session.getSessionId().toString(), session.getSessionVersion(),
                session.getStatus().name(), session.getRevokeReason(), actorId, occurredAt);
    }
}
