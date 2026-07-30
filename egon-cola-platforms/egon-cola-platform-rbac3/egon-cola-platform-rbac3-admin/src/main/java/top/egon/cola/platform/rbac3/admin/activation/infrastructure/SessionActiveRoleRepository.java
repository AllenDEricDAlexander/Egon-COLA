package top.egon.cola.platform.rbac3.admin.activation.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.activation.application.ActiveRoleSetRevalidator;
import top.egon.cola.platform.rbac3.admin.activation.domain.SessionActiveRoleEntity;
import top.egon.cola.platform.rbac3.admin.application.port.AuditPort;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;
import top.egon.cola.platform.rbac3.admin.runtime.domain.AuthorizationMutationEntity;
import top.egon.cola.platform.rbac3.admin.session.domain.SessionEntity;
import top.egon.cola.platform.rbac3.admin.session.infrastructure.SessionRepository;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationCandidateResolver;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Serializes activation and refresh on the same session row lock.
 */
@Repository
public class SessionActiveRoleRepository
        implements RoleActivationFacade.ActivationTransaction,
        ActiveRoleSetRevalidator.ReselectionStore {

    private final SessionRepository sessionRepository;
    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final AuditPort auditPort;
    private final AuthorizationEventPort eventPort;

    public SessionActiveRoleRepository(
            SessionRepository sessionRepository,
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            AuditPort auditPort,
            AuthorizationEventPort eventPort
    ) {
        this.sessionRepository = sessionRepository;
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.auditPort = auditPort;
        this.eventPort = eventPort;
    }

    @Override
    @Transactional
    public RoleActivationFacade.TransactionResult replace(
            RoleActivationFacade.ReplaceCommand command,
            Instant now,
            Function<RoleActivationFacade.SessionState,
                    RoleActivationFacade.ResolvedActivation> resolutionFactory
    ) {
        SessionEntity session = locked(command.tenantId(), command.userId(),
                command.sessionId(), now);
        if (session.getSessionVersion() != command.expectedSessionVersion()) {
            throw new Rbac3RuleViolation("SESSION_VERSION_CONFLICT");
        }
        List<SessionActiveRoleEntity> currentEntities = activeRoles(
                Long.valueOf(command.tenantId()), Long.valueOf(command.sessionId()));
        Map<String, Set<String>> current = roots(currentEntities);
        var state = new RoleActivationFacade.SessionState(
                command.tenantId(), command.userId(), command.sessionId(), current,
                session.getAuthVersionAtIssue(), session.getSessionVersion(),
                session.getPolicyVersionAtIssue(), session.getActiveRootChecksum(),
                session.isActivationRequired(), session.getAbsoluteExpiresAt());
        RoleActivationFacade.ResolvedActivation resolved = resolutionFactory.apply(state);
        Map<String, Set<String>> requested = resolved.resolution()
                .activeRoleSet().rootsByApplication();
        boolean unchanged = !session.isActivationRequired()
                && current.equals(requested)
                && session.getAuthVersionAtIssue() == resolved.facts().authVersion()
                && session.getPolicyVersionAtIssue() == resolved.facts().policyVersion();
        if (unchanged) {
            return result(resolved, false, null, current, session);
        }

        String mutationId = Long.toString(idGenerator.nextLongId());
        var mutation = new AuthorizationMutationEntity(
                Long.valueOf(mutationId),
                Long.valueOf(command.tenantId()),
                Long.valueOf(command.userId()),
                Long.valueOf(command.sessionId()),
                AuthorizationMutationEntity.ScopeType.SESSION,
                command.commandId(),
                session.getSessionVersion(),
                Math.incrementExact(session.getSessionVersion()),
                session.getAuthVersionAtIssue(),
                resolved.facts().authVersion(),
                session.getPolicyVersionAtIssue(),
                resolved.facts().policyVersion(),
                command.actorId(),
                now);
        entityManager.persist(mutation);
        session.activateRoles(
                resolved.facts().authVersion(),
                resolved.facts().policyVersion(),
                resolved.resolution().snapshot().checksum(),
                command.actorId(),
                now);
        currentEntities.forEach(entityManager::remove);
        Map<String, Set<String>> assignmentIdsByRoot =
                new RoleActivationCandidateResolver().resolve(
                        resolved.facts().assignments(),
                        resolved.facts().hierarchy(), now);
        for (Map.Entry<String, Set<String>> application : requested.entrySet()) {
            for (String rootRoleId : application.getValue()) {
                Set<String> assignmentIds = assignmentIdsByRoot.get(rootRoleId);
                if (assignmentIds == null || assignmentIds.isEmpty()) {
                    throw new IllegalStateException(
                            "missing eligible assignment evidence for root " + rootRoleId);
                }
                entityManager.persist(new SessionActiveRoleEntity(
                        Long.valueOf(command.tenantId()),
                        Long.valueOf(command.sessionId()),
                        Long.valueOf(application.getKey()),
                        Long.valueOf(rootRoleId),
                        session.getSessionVersion(),
                        new ArrayList<>(assignmentIds),
                        now));
            }
        }
        mutation.committed(now, command.actorId());
        auditPort.append(new AuditPort.AuditEvent(
                command.tenantId(), "SESSION_ACTIVE_ROLES_REPLACED",
                command.actorId(), "SESSION", command.sessionId(),
                command.commandId(), command.commandId(),
                Map.of(
                        "oldSessionVersion", Long.toString(command.expectedSessionVersion()),
                        "newSessionVersion", Long.toString(session.getSessionVersion()),
                        "snapshotChecksum", session.getActiveRootChecksum()),
                now));
        eventPort.enqueue(new AuthorizationEventPort.AuthorizationEvent(
                command.tenantId(), "SESSION", command.sessionId(),
                "RBAC3_SESSION_ACTIVE_ROLES_REPLACED",
                Map.of(
                        "mutationId", mutationId,
                        "sessionVersion", Long.toString(session.getSessionVersion()),
                        "authVersion", Long.toString(session.getAuthVersionAtIssue()),
                        "policyVersion", Long.toString(session.getPolicyVersionAtIssue())),
                command.commandId()));
        entityManager.flush();
        return result(resolved, true, mutationId, requested, session);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleActivationFacade.CurrentState current(
            String tenantId,
            String userId,
            String sessionId,
            Instant now
    ) {
        SessionEntity session = sessionRepository.findByTenantIdAndSessionId(
                        Long.valueOf(tenantId), Long.valueOf(sessionId))
                .orElseThrow(() -> new Rbac3RuleViolation("RESOURCE_NOT_FOUND"));
        if (!session.getUserId().equals(Long.valueOf(userId))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        session.requireActive(now);
        return new RoleActivationFacade.CurrentState(
                roots(activeRoles(Long.valueOf(tenantId), Long.valueOf(sessionId))),
                session.getAuthVersionAtIssue(),
                session.getSessionVersion(),
                session.getPolicyVersionAtIssue(),
                session.getActiveRootChecksum(),
                session.isActivationRequired());
    }

    private RoleActivationFacade.TransactionResult result(
            RoleActivationFacade.ResolvedActivation resolved,
            boolean changed,
            String mutationId,
            Map<String, Set<String>> roots,
            SessionEntity session
    ) {
        return new RoleActivationFacade.TransactionResult(
                resolved, changed, mutationId, immutable(roots),
                session.getAuthVersionAtIssue(), session.getSessionVersion(),
                session.getPolicyVersionAtIssue(), session.getActiveRootChecksum(),
                session.getAbsoluteExpiresAt());
    }

    @Override
    @Transactional
    public void markFenced(String mutationId, Instant now) {
        mutation(mutationId).fenced(now, "role-activation");
    }

    @Override
    @Transactional
    public void markCompleted(String mutationId, Instant now) {
        AuthorizationMutationEntity mutation = mutation(mutationId);
        mutation.projected(now, "role-activation");
        mutation.completed(now, "role-activation");
    }

    @Override
    @Transactional
    public void markRecoveryRequired(
            String mutationId,
            String reasonCode,
            Instant now
    ) {
        mutation(mutationId).recoveryRequired(
                reasonCode, now, "role-activation-recovery");
    }

    @Override
    @Transactional
    public void requireReselection(
            String tenantId,
            String sessionId,
            long expectedSessionVersion,
            Instant now,
            String actorId
    ) {
        SessionEntity session = sessionRepository.lockByTenantIdAndSessionId(
                        Long.valueOf(tenantId), Long.valueOf(sessionId))
                .orElseThrow(() -> new Rbac3RuleViolation("RESOURCE_NOT_FOUND"));
        if (session.getSessionVersion() != expectedSessionVersion) {
            throw new Rbac3RuleViolation("SESSION_VERSION_CONFLICT");
        }
        session.requireRoleReselection(actorId, now);
        activeRoles(Long.valueOf(tenantId), Long.valueOf(sessionId))
                .forEach(entityManager::remove);
    }

    private AuthorizationMutationEntity mutation(String mutationId) {
        AuthorizationMutationEntity mutation = entityManager.find(
                AuthorizationMutationEntity.class,
                Long.valueOf(mutationId),
                LockModeType.PESSIMISTIC_WRITE);
        if (mutation == null) {
            throw new IllegalStateException("authorization mutation is missing");
        }
        return mutation;
    }

    private SessionEntity locked(
            String tenantId,
            String userId,
            String sessionId,
            Instant now
    ) {
        SessionEntity session = sessionRepository.lockByTenantIdAndSessionId(
                        Long.valueOf(tenantId), Long.valueOf(sessionId))
                .orElseThrow(() -> new Rbac3RuleViolation("RESOURCE_NOT_FOUND"));
        if (!session.getUserId().equals(Long.valueOf(userId))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        session.requireActive(now);
        return session;
    }

    private List<SessionActiveRoleEntity> activeRoles(Long tenantId, Long sessionId) {
        return entityManager.createQuery("""
                        select active from SessionActiveRoleEntity active
                         where active.tenantId = :tenantId
                           and active.sessionId = :sessionId
                         order by active.applicationId, active.rootRoleId
                        """, SessionActiveRoleEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("sessionId", sessionId)
                .getResultList();
    }

    private Map<String, Set<String>> roots(
            List<SessionActiveRoleEntity> activeRoles
    ) {
        var result = new TreeMap<String, Set<String>>();
        for (SessionActiveRoleEntity active : activeRoles) {
            result.computeIfAbsent(
                    active.getApplicationId().toString(), ignored -> new TreeSet<>())
                    .add(active.getRootRoleId().toString());
        }
        return immutable(result);
    }

    private Map<String, Set<String>> immutable(Map<String, Set<String>> values) {
        var result = new TreeMap<String, Set<String>>();
        values.forEach((key, roots) -> result.put(
                key, Collections.unmodifiableSet(new TreeSet<>(roots))));
        return Collections.unmodifiableMap(result);
    }
}
