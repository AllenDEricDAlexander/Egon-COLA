package top.egon.cola.platform.rbac3.admin.session.infrastructure;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.session.application.AuthorizationContextFacade;
import top.egon.cola.platform.rbac3.admin.session.domain.SessionEntity;

import java.time.Instant;
import java.util.Optional;

/** PostgreSQL store for IdP-bound RBAC3 authorization contexts. */
@Repository
public class AuthorizationContextRepository
        implements AuthorizationContextFacade.AuthorizationContextStore {

    private final SessionRepository sessions;

    public AuthorizationContextRepository(SessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<AuthorizationContextFacade.AuthorizationContext> find(
            String tenantId, String sessionId) {
        return sessions.findByTenantIdAndSessionId(
                        Long.valueOf(tenantId), Long.valueOf(sessionId))
                .map(AuthorizationContextRepository::toContext);
    }

    @Override
    @Transactional
    public AuthorizationContextFacade.AuthorizationContext create(
            long entityId,
            AuthorizationContextFacade.ActiveMembership membership,
            String sessionId,
            Instant now,
            Instant expiresAt)
            throws AuthorizationContextFacade.ConcurrentContextCreationException {
        SessionEntity entity = SessionEntity.authorizationContext(
                entityId, Long.valueOf(membership.tenantId()),
                Long.valueOf(membership.rbac3UserId()), Long.valueOf(sessionId),
                membership.identitySub(), membership.authVersion(),
                membership.policyVersion(), now, expiresAt, membership.identitySub());
        try {
            return toContext(sessions.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new AuthorizationContextFacade.ConcurrentContextCreationException(
                    exception);
        }
    }

    private static AuthorizationContextFacade.AuthorizationContext toContext(
            SessionEntity entity) {
        return new AuthorizationContextFacade.AuthorizationContext(
                entity.getId().toString(), entity.getTenantId().toString(),
                entity.getSessionId().toString(), entity.getIdentitySub(),
                entity.getUserId().toString(), entity.getAuthVersionAtIssue(),
                entity.getContextVersion(), entity.getPolicyVersionAtIssue(),
                entity.isActivationRequired(), entity.getStatus().name(),
                entity.getAuthenticatedAt(), entity.getContextExpiresAt());
    }
}
