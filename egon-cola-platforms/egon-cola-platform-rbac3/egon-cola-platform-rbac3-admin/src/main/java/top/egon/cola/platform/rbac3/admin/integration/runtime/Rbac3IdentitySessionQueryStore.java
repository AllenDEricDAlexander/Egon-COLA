package top.egon.cola.platform.rbac3.admin.integration.runtime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.auth.application.AuthenticationFacade;
import top.egon.cola.platform.rbac3.admin.auth.application.RefreshFacade;
import top.egon.cola.platform.rbac3.admin.bootstrap.application.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.directory.domain.DirectorySnapshotEntity;
import top.egon.cola.platform.rbac3.admin.directory.infrastructure.DirectorySnapshotStore;
import top.egon.cola.platform.rbac3.admin.identity.domain.TenantEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserEntity;
import top.egon.cola.platform.rbac3.admin.interfaces.http.AssignmentController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.SessionController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.TenantUserDirectoryController;
import top.egon.cola.platform.rbac3.admin.role.domain.RoleEntity;
import top.egon.cola.platform.rbac3.admin.session.domain.RefreshTokenEntity;
import top.egon.cola.platform.rbac3.admin.session.domain.SessionEntity;
import top.egon.cola.platform.rbac3.admin.snapshot.infrastructure.RedisAuthorizationRuntimeStore;
import top.egon.cola.platform.rbac3.contract.activation.ActivationRoot;
import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.manifest.ManifestResource;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read models and narrow command adapters shared by authentication controllers.
 */
@Repository
public class Rbac3IdentitySessionQueryStore implements
        AuthenticationFacade.LoginStateSource,
        RefreshFacade.RefreshStateSource,
        BootstrapQueryService.BootstrapSnapshotSource,
        SessionController.SessionManagementPort,
        AssignmentController.SessionStrengthPort,
        TenantUserDirectoryController.DirectoryCommandPort,
        TenantUserDirectoryController.DirectoryQueryPort {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;
    private final RoleActivationCandidateService candidateService;
    private final RedisAuthorizationRuntimeStore runtimeStore;
    private final DirectorySnapshotStore directorySnapshotStore;

    public Rbac3IdentitySessionQueryStore(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock,
            RoleActivationCandidateService candidateService,
            RedisAuthorizationRuntimeStore runtimeStore,
            DirectorySnapshotStore directorySnapshotStore) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
        this.candidateService = candidateService;
        this.runtimeStore = runtimeStore;
        this.directorySnapshotStore = directorySnapshotStore;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationFacade.LoginState load(
            String tenantCode,
            String userId,
            Instant now) {
        TenantEntity tenant = entityManager.createQuery("""
                        select t from TenantEntity t where lower(t.code) = :code
                        """, TenantEntity.class)
                .setParameter("code", tenantCode.toLowerCase(java.util.Locale.ROOT))
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new Rbac3RuleViolation("AUTHENTICATION_FAILED"));
        UserEntity user = requireUser(tenant.getId(), Long.valueOf(userId));
        int candidates = candidateService.candidates(
                        tenant.getId().toString(), userId, now)
                .applications().stream()
                .mapToInt(application -> application.candidates().size())
                .sum();
        return new AuthenticationFacade.LoginState(
                tenant.getId().toString(), user.getAuthVersion(),
                tenant.getPolicyVersion(), candidates);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshFacade.RefreshState load(String familyId) {
        List<Object[]> rows = entityManager.createQuery("""
                        select token, session
                          from RefreshTokenEntity token, SessionEntity session
                         where token.familyId = :familyId
                           and session.tenantId = token.tenantId
                           and session.sessionId = token.sessionId
                         order by token.generation desc
                        """, Object[].class)
                .setParameter("familyId", familyId)
                .setMaxResults(1)
                .getResultList();
        if (rows.isEmpty()) {
            throw new Rbac3RuleViolation("AUTHENTICATION_FAILED");
        }
        RefreshTokenEntity token = (RefreshTokenEntity) rows.getFirst()[0];
        SessionEntity session = (SessionEntity) rows.getFirst()[1];
        return new RefreshFacade.RefreshState(
                session.getTenantId().toString(), session.getUserId().toString(),
                session.getSessionId().toString(), session.getAuthVersionAtIssue(),
                session.getSessionVersion(), session.getPolicyVersionAtIssue(),
                token.getExpiresAt(), session.isActivationRequired(),
                session.isActivationRequired() ? "ROLE_ACTIVATION_REQUIRED" : null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BootstrapView> find(
            String tenantId,
            String userId,
            String sessionId) {
        UserEntity user = requireUser(Long.valueOf(tenantId), Long.valueOf(userId));
        var record = runtimeStore.load(tenantId, sessionId);
        if (!userId.equals(record.userId())) {
            return Optional.empty();
        }
        var snapshot = record.snapshot();
        var contexts = new ArrayList<BootstrapView.ActiveRoleContext>();
        var permissions = new LinkedHashSet<String>();
        var resources = new ArrayList<ManifestResource>();
        var fieldPolicies = new LinkedHashMap<String,
                top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision>();
        for (AppAuthorizationContext context : snapshot.appContexts()) {
            permissions.addAll(context.permissions());
            resources.addAll(context.resources());
            fieldPolicies.putAll(context.fieldPolicies());
            for (String rootRoleId : context.activationRootRoleIds()) {
                RoleEntity role = entityManager.find(
                        RoleEntity.class, Long.valueOf(rootRoleId));
                if (role == null || !role.getTenantId().equals(Long.valueOf(tenantId))) {
                    throw new Rbac3RuleViolation("AUTH_SNAPSHOT_NOT_READY");
                }
                contexts.add(new BootstrapView.ActiveRoleContext(
                        context.applicationCode(),
                        new ActivationRoot(
                                rootRoleId, role.getApplicationId().toString(),
                                role.getRoleCode()),
                        context.effectiveRoleIds(), context.eligibleAssignmentIds(),
                        context.landingRouteCode()));
            }
        }
        String defaultApplication = contexts.isEmpty()
                ? null : contexts.getFirst().applicationCode();
        String defaultRoute = contexts.stream()
                .map(BootstrapView.ActiveRoleContext::landingRoute)
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse(null);
        return Optional.of(new BootstrapView(
                new BootstrapView.User(
                        userId, tenantId, user.getUsername(), user.getDisplayName()),
                contexts, permissions,
                resources(resources, "APP"), resources(resources, "MENU"),
                resources(resources, "ROUTE"), resources(resources, "ACTION"),
                fieldPolicies, defaultApplication, defaultRoute, sessionId,
                snapshot.authVersion(), snapshot.sessionVersion(),
                snapshot.policyVersion()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionController.SessionView> findByUser(
            String tenantId,
            String userId) {
        return entityManager.createQuery("""
                        select s from SessionEntity s
                         where s.tenantId = :tenantId and s.userId = :userId
                         order by s.authenticatedAt desc, s.sessionId desc
                        """, SessionEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("userId", Long.valueOf(userId))
                .getResultList().stream()
                .map(session -> new SessionController.SessionView(
                        session.getSessionId().toString(), session.getStatus().name(),
                        session.getSessionVersion(), session.getAuthenticatedAt(),
                        session.getLastSeenAt(), session.getAbsoluteExpiresAt()))
                .toList();
    }

    @Override
    @Transactional
    public boolean revoke(String tenantId, String sessionId, Instant now) {
        List<SessionEntity> rows = entityManager.createQuery("""
                        select s from SessionEntity s
                         where s.tenantId = :tenantId and s.sessionId = :sessionId
                        """, SessionEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("sessionId", Long.valueOf(sessionId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        return !rows.isEmpty() && rows.getFirst().revoke(
                "ADMIN_REVOKE", "session-administration", now);
    }

    @Override
    @Transactional(readOnly = true)
    public String authenticationStrength(String tenantId, String sessionId) {
        List<SessionEntity.AuthenticationStrength> rows = entityManager.createQuery("""
                        select s.authenticationStrength from SessionEntity s
                         where s.tenantId = :tenantId and s.sessionId = :sessionId
                        """, SessionEntity.AuthenticationStrength.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("sessionId", Long.valueOf(sessionId))
                .getResultList();
        if (rows.size() != 1) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return rows.getFirst().name();
    }

    @Override
    @Transactional
    public TenantUserDirectoryController.DirectorySyncView submit(
            String tenantId,
            TenantUserDirectoryController.DirectorySnapshotCommand command) {
        Instant now = databaseClock.transactionNow();
        DirectorySnapshotEntity entity = new DirectorySnapshotEntity(
                idGenerator.nextLongId(), Long.valueOf(tenantId), command.providerCode(),
                command.snapshotVersion(), command.checksum(), command.generatedAt(),
                command.payload(), "directory-sync", now);
        DirectorySnapshotStore.IngestionResult result = directorySnapshotStore.accept(entity);
        if (result.outcome() == DirectorySnapshotStore.Outcome.ACCEPTED) {
            Map<String, Object> rawCounts = rawCounts(command.payload());
            entity.validate(rawCounts, "directory-sync", now);
            entity.activate("directory-sync", now);
        }
        Map<String, Long> counts = longCounts(rawCounts(command.payload()));
        long users = collectionSize(command.payload().get("users"));
        return new TenantUserDirectoryController.DirectorySyncView(
                result.snapshotId().toString(), result.outcome().name(), counts, users);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantUserDirectoryController.UserDirectoryView findUser(
            String tenantId,
            String userId) {
        UserEntity user = requireUser(Long.valueOf(tenantId), Long.valueOf(userId));
        return new TenantUserDirectoryController.UserDirectoryView(
                userId, user.getUsername(), user.getDisplayName(),
                user.getStatus().name(), user.getAuthVersion(),
                user.getDirectorySnapshotVersion());
    }

    private UserEntity requireUser(Long tenantId, Long userId) {
        UserEntity user = entityManager.find(UserEntity.class, userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return user;
    }

    private List<ManifestResource> resources(
            List<ManifestResource> resources,
            String type) {
        return resources.stream()
                .filter(resource -> type.equalsIgnoreCase(
                        resource.metadata().getOrDefault("resourceType", "")))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> rawCounts(Map<String, Object> payload) {
        Object value = payload.get("counts");
        return value instanceof Map<?, ?> map
                ? new LinkedHashMap<>((Map<String, Object>) map)
                : Map.of();
    }

    private Map<String, Long> longCounts(Map<String, Object> values) {
        Map<String, Long> result = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (value instanceof Number number) {
                result.put(key, number.longValue());
            }
        });
        return Map.copyOf(result);
    }

    private long collectionSize(Object value) {
        return value instanceof Collection<?> collection ? collection.size() : 0L;
    }
}
