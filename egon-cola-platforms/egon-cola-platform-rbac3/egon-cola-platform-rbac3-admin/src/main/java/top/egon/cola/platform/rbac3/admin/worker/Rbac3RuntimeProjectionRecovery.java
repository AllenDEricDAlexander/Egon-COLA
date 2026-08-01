package top.egon.cola.platform.rbac3.admin.worker;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.activation.infrastructure.RoleActivationFactStore;
import top.egon.cola.platform.rbac3.admin.activation.infrastructure.SessionActiveRoleRepository;
import top.egon.cola.platform.rbac3.admin.integration.outbox.Rbac3RuntimeProjectionDeliveryHandler;
import top.egon.cola.platform.rbac3.admin.session.domain.SessionEntity;
import top.egon.cola.platform.rbac3.admin.session.application.SessionRuntimeSynchronizer;
import top.egon.cola.platform.rbac3.admin.snapshot.application.LoginRuntimeProjectionFactory;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.admin.snapshot.infrastructure.RedisAuthorizationRuntimeStore;
import top.egon.cola.platform.rbac3.core.activation.DefaultRoleActivationResolver;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationInput;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Rebuilds immutable Session projections from committed PostgreSQL facts.
 */
@Component
public class Rbac3RuntimeProjectionRecovery implements
        AuthorizationMutationRecoveryWorker.ProjectionExecutor,
        RuntimeSnapshotRebuildWorker.RebuildPort,
        SessionRuntimeSynchronizer {

    private final EntityManager entityManager;
    private final RoleActivationFactStore factStore;
    private final SessionActiveRoleRepository activeRoleRepository;
    private final SessionSnapshotProjector snapshotProjector;
    private final LoginRuntimeProjectionFactory loginProjectionFactory;
    private final RedisAuthorizationRuntimeStore runtimeStore;
    private final Clock clock;

    public Rbac3RuntimeProjectionRecovery(
            EntityManager entityManager,
            RoleActivationFactStore factStore,
            SessionActiveRoleRepository activeRoleRepository,
            SessionSnapshotProjector snapshotProjector,
            LoginRuntimeProjectionFactory loginProjectionFactory,
            RedisAuthorizationRuntimeStore runtimeStore,
            Clock clock) {
        this.entityManager = entityManager;
        this.factStore = factStore;
        this.activeRoleRepository = activeRoleRepository;
        this.snapshotProjector = snapshotProjector;
        this.loginProjectionFactory = loginProjectionFactory;
        this.runtimeStore = runtimeStore;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public void project(AuthorizationMutationRecoveryWorker.MutationWork mutation) {
        rebuildSessions(
                mutation.tenantId(), mutation.scopeType(), mutation.scopeId());
    }

    @Override
    @Transactional(readOnly = true)
    public void rebuild(Rbac3RuntimeProjectionDeliveryHandler.EventEnvelope event) {
        if ("SESSION".equals(event.aggregateType())) {
            rebuildSessions(event.tenantId(), "SESSION", event.aggregateId());
            return;
        }
        String userId = event.payload().get("userId");
        rebuildSessions(
                event.tenantId(), userId == null ? "TENANT" : "USER",
                userId == null ? event.tenantId() : userId);
    }

    private void rebuildSessions(String tenantId, String scopeType, String scopeId) {
        Instant now = clock.instant();
        for (SessionEntity session : sessions(tenantId, scopeType, scopeId)) {
            synchronize(session, now);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void synchronize(
            String tenantId,
            String userId,
            String sessionId,
            Instant generatedAt) {
        SessionEntity session = entityManager.createQuery("""
                        select s from SessionEntity s
                         where s.tenantId = :tenantId
                           and s.userId = :userId
                           and s.sessionId = :sessionId
                        """, SessionEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("userId", Long.valueOf(userId))
                .setParameter("sessionId", Long.valueOf(sessionId))
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("runtime session is missing"));
        synchronize(session, generatedAt);
    }

    private void synchronize(SessionEntity session, Instant generatedAt) {
        if (session.getStatus() == SessionEntity.Status.ACTIVE
                && session.getIdleExpiresAt().isAfter(generatedAt)
                && session.getAbsoluteExpiresAt().isAfter(generatedAt)
                && !session.isActivationRequired()) {
            rebuild(session, generatedAt);
            return;
        }
        publishMinimum(session, generatedAt);
    }

    private void publishMinimum(SessionEntity session, Instant generatedAt) {
        var state = new LoginRuntimeProjectionFactory.RuntimeState(
                session.getTenantId().toString(), session.getUserId().toString(),
                session.getSessionId().toString(), runtimeStatus(session, generatedAt),
                session.getAuthVersionAtIssue(), session.getSessionVersion(),
                session.getPolicyVersionAtIssue(), session.getAbsoluteExpiresAt());
        var projection = loginProjectionFactory.create(state, generatedAt);
        runtimeStore.publish(new RedisAuthorizationRuntimeStore.PublishCommand(
                state.tenantId(), state.userId(), state.sessionId(), state.authVersion(),
                state.sessionVersion(), state.policyVersion(), projection));
    }

    private String runtimeStatus(SessionEntity session, Instant generatedAt) {
        if (session.getStatus() == SessionEntity.Status.ACTIVE
                && (!session.getIdleExpiresAt().isAfter(generatedAt)
                || !session.getAbsoluteExpiresAt().isAfter(generatedAt))) {
            return SessionEntity.Status.EXPIRED.name();
        }
        return session.getStatus().name();
    }

    private void rebuild(SessionEntity session, Instant now) {
        String tenantId = session.getTenantId().toString();
        String userId = session.getUserId().toString();
        String sessionId = session.getSessionId().toString();
        var current = activeRoleRepository.current(
                tenantId, userId, sessionId, now);
        var requestedRoots = current.rootsByApplication().values().stream()
                .flatMap(java.util.Collection::stream)
                .sorted()
                .toList();
        if (requestedRoots.isEmpty()) {
            return;
        }
        var facts = factStore.load(tenantId, userId, now);
        long resolverSessionVersion = Math.max(0L, current.sessionVersion() - 1L);
        var resolution = new DefaultRoleActivationResolver().resolve(
                new RoleActivationInput(
                        tenantId, userId, sessionId, requestedRoots,
                        facts.assignments(), facts.hierarchy(), facts.dsdSets(),
                        facts.authorizationFacts(), current.authVersion(),
                        resolverSessionVersion, current.policyVersion(), now));
        SessionSnapshotProjector.Projection projection = snapshotProjector.project(
                new SessionSnapshotProjector.ProjectionCommand(
                        tenantId, userId, sessionId, current.authVersion(),
                        current.sessionVersion(), current.policyVersion(),
                        session.getAbsoluteExpiresAt(), resolution, facts, now));
        runtimeStore.publish(new RoleActivationFacade.RuntimePublication(
                tenantId, userId, sessionId, current.authVersion(),
                current.sessionVersion(), current.policyVersion(), projection));
    }

    private List<SessionEntity> sessions(
            String tenantId,
            String scopeType,
            String scopeId) {
        StringBuilder hql = new StringBuilder("""
                select s from SessionEntity s
                 where s.tenantId = :tenantId
                """);
        if ("SESSION".equals(scopeType)) {
            hql.append(" and s.sessionId = :scopeId");
        } else if ("USER".equals(scopeType)) {
            hql.append(" and s.userId = :scopeId");
        } else if (!"TENANT".equals(scopeType)) {
            throw new IllegalArgumentException(
                    "unsupported authorization mutation scope: " + scopeType);
        }
        var query = entityManager.createQuery(hql.toString(), SessionEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId));
        if (!"TENANT".equals(scopeType)) {
            query.setParameter("scopeId", Long.valueOf(scopeId));
        }
        return new ArrayList<>(query.getResultList());
    }
}
