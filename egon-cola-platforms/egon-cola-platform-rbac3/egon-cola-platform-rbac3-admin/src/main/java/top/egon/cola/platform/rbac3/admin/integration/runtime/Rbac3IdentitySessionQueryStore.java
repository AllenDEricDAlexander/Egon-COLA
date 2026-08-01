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
import top.egon.cola.platform.rbac3.admin.auth.application.StepUpFacade;
import top.egon.cola.platform.rbac3.admin.bootstrap.application.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.directory.domain.DirectorySnapshotEntity;
import top.egon.cola.platform.rbac3.admin.directory.application.DirectorySnapshotProcessor;
import top.egon.cola.platform.rbac3.admin.directory.domain.OrgUnitEntity;
import top.egon.cola.platform.rbac3.admin.directory.domain.PositionEntity;
import top.egon.cola.platform.rbac3.admin.directory.infrastructure.DirectorySnapshotMaterializer;
import top.egon.cola.platform.rbac3.admin.directory.infrastructure.DirectorySnapshotStore;
import top.egon.cola.platform.rbac3.admin.identity.domain.TenantEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserEntity;
import top.egon.cola.platform.rbac3.admin.interfaces.http.AssignmentController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.SessionController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.TenantUserDirectoryController;
import top.egon.cola.platform.rbac3.admin.role.domain.RoleEntity;
import top.egon.cola.platform.rbac3.admin.session.domain.RefreshTokenEntity;
import top.egon.cola.platform.rbac3.admin.session.domain.SessionEntity;
import top.egon.cola.platform.rbac3.admin.session.application.SessionRuntimeSynchronizer;
import top.egon.cola.platform.rbac3.admin.session.application.SessionSecurityEventRecorder;
import top.egon.cola.platform.rbac3.admin.snapshot.infrastructure.RedisAuthorizationRuntimeStore;
import top.egon.cola.platform.rbac3.contract.activation.ActivationRoot;
import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.manifest.ManifestResource;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;

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
        StepUpFacade.IdentitySource,
        StepUpFacade.SessionStrengthStore,
        TenantUserDirectoryController.DirectoryCommandPort,
        TenantUserDirectoryController.DirectoryQueryPort {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;
    private final RoleActivationCandidateService candidateService;
    private final RedisAuthorizationRuntimeStore runtimeStore;
    private final DirectorySnapshotStore directorySnapshotStore;
    private final DirectorySnapshotMaterializer directorySnapshotMaterializer;
    private final SessionRuntimeSynchronizer runtimeSynchronizer;
    private final SessionSecurityEventRecorder securityEventRecorder;
    private final DirectorySnapshotProcessor directorySnapshotProcessor =
            new DirectorySnapshotProcessor();

    public Rbac3IdentitySessionQueryStore(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock,
            RoleActivationCandidateService candidateService,
            RedisAuthorizationRuntimeStore runtimeStore,
            DirectorySnapshotStore directorySnapshotStore,
            DirectorySnapshotMaterializer directorySnapshotMaterializer,
            SessionRuntimeSynchronizer runtimeSynchronizer,
            SessionSecurityEventRecorder securityEventRecorder) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
        this.candidateService = candidateService;
        this.runtimeStore = runtimeStore;
        this.directorySnapshotStore = directorySnapshotStore;
        this.directorySnapshotMaterializer = directorySnapshotMaterializer;
        this.runtimeSynchronizer = runtimeSynchronizer;
        this.securityEventRecorder = securityEventRecorder;
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
                        session.getSessionVersion(),
                        session.getAuthenticationStrength().name(),
                        session.getAuthenticatedAt(), session.getStrongAuthenticatedAt(),
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
        if (rows.isEmpty()) {
            return false;
        }
        boolean changed = rows.getFirst().revoke(
                "ADMIN_REVOKE", "session-administration", now);
        if (changed) {
            revokeRefreshTokens(Long.valueOf(tenantId), List.of(Long.valueOf(sessionId)),
                    "session-administration", now);
            SessionEntity session = rows.getFirst();
            recordTermination(session, "session-administration", now);
            runtimeSynchronizer.synchronize(
                    tenantId, session.getUserId().toString(), sessionId, now);
        }
        return changed;
    }

    @Override
    @Transactional
    public int revokeAll(String tenantId, String userId, Instant now) {
        List<SessionEntity> sessions = entityManager.createQuery("""
                        select s from SessionEntity s
                         where s.tenantId = :tenantId and s.userId = :userId
                        """, SessionEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("userId", Long.valueOf(userId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        List<Long> changedSessionIds = new ArrayList<>();
        for (SessionEntity session : sessions) {
            if (session.revoke("ADMIN_REVOKE_ALL", "session-administration", now)) {
                changedSessionIds.add(session.getSessionId());
            }
        }
        revokeRefreshTokens(Long.valueOf(tenantId), changedSessionIds,
                "session-administration", now);
        sessions.stream()
                .filter(session -> changedSessionIds.contains(session.getSessionId()))
                .forEach(session -> {
                    recordTermination(session, "session-administration", now);
                    runtimeSynchronizer.synchronize(
                            tenantId, userId, session.getSessionId().toString(), now);
                });
        return changedSessionIds.size();
    }

    @Override
    @Transactional(readOnly = true)
    public String authenticationStrength(String tenantId, String sessionId, Instant now) {
        List<SessionEntity> rows = entityManager.createQuery("""
                        select s from SessionEntity s
                         where s.tenantId = :tenantId and s.sessionId = :sessionId
                        """, SessionEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("sessionId", Long.valueOf(sessionId))
                .getResultList();
        if (rows.size() != 1) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        SessionEntity session = rows.getFirst();
        if (session.getAuthenticationStrength() == SessionEntity.AuthenticationStrength.STRONG
                && !session.isStrongAuthenticationRecent(now, Duration.ofMinutes(10))) {
            return SessionEntity.AuthenticationStrength.PASSWORD.name();
        }
        return session.getAuthenticationStrength().name();
    }

    @Override
    @Transactional(readOnly = true)
    public StepUpFacade.Identity load(String tenantId, String userId) {
        TenantEntity tenant = entityManager.find(TenantEntity.class, Long.valueOf(tenantId));
        UserEntity user = requireUser(Long.valueOf(tenantId), Long.valueOf(userId));
        if (tenant == null) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return new StepUpFacade.Identity(tenant.getCode(), user.getUsername());
    }

    @Override
    @Transactional
    public StepUpFacade.StepUpResult strengthen(
            String tenantId,
            String userId,
            String sessionId,
            Instant now) {
        List<SessionEntity> sessions = entityManager.createQuery("""
                        select s from SessionEntity s
                         where s.tenantId = :tenantId
                           and s.userId = :userId
                           and s.sessionId = :sessionId
                        """, SessionEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("userId", Long.valueOf(userId))
                .setParameter("sessionId", Long.valueOf(sessionId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (sessions.size() != 1) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        SessionEntity session = sessions.getFirst();
        session.stepUp(userId, now);
        return new StepUpFacade.StepUpResult(
                sessionId, session.getAuthenticationStrength().name(),
                session.getStrongAuthenticatedAt());
    }

    @Override
    @Transactional
    public TenantUserDirectoryController.DirectorySyncView submit(
            String tenantId,
            TenantUserDirectoryController.DirectorySnapshotCommand command) {
        Instant now = databaseClock.transactionNow();
        DirectorySnapshotProcessor.SnapshotModel model =
                directorySnapshotProcessor.validate(command.payload(), command.generatedAt());
        DirectorySnapshotEntity entity = new DirectorySnapshotEntity(
                idGenerator.nextLongId(), Long.valueOf(tenantId), command.providerCode(),
                command.snapshotVersion(), command.checksum(), command.generatedAt(),
                command.payload(), "directory-sync", now);
        DirectorySnapshotStore.IngestionResult result = directorySnapshotStore.accept(entity);
        Map<String, Object> counts;
        long affectedUsers;
        if (result.outcome() == DirectorySnapshotStore.Outcome.ACCEPTED) {
            DirectorySnapshotMaterializer.MaterializationResult materialization =
                    directorySnapshotMaterializer.apply(
                            Long.valueOf(tenantId), entity.getId(), command.snapshotVersion(),
                            model, "directory-sync", now);
            counts = new LinkedHashMap<>(model.counts());
            counts.putAll(materialization.counts());
            entity.validate(counts, "directory-sync", now);
            archiveCurrentSnapshot(
                    Long.valueOf(tenantId), command.providerCode(), entity.getId(), now);
            entity.activate("directory-sync", now);
            affectedUsers = materialization.affectedUserCount();
        } else {
            DirectorySnapshotEntity existing = entityManager.find(
                    DirectorySnapshotEntity.class, result.snapshotId());
            counts = existing == null ? Map.of() : existing.getCounts();
            affectedUsers = numericCount(counts, "affectedUsers");
        }
        return new TenantUserDirectoryController.DirectorySyncView(
                result.snapshotId().toString(), result.outcome().name(),
                longCounts(counts), affectedUsers);
    }

    @Override
    @Transactional
    public TenantUserDirectoryController.TenantView createTenant(
            TenantUserDirectoryController.CreateTenantCommand command,
            String actorId) {
        String normalizedCode = command.code().trim().toLowerCase(Locale.ROOT);
        boolean exists = !entityManager.createQuery("""
                        select t.id from TenantEntity t where lower(t.code) = :code
                        """, Long.class)
                .setParameter("code", normalizedCode)
                .setMaxResults(1)
                .getResultList().isEmpty();
        if (exists) {
            throw new Rbac3RuleViolation("TENANT_CODE_CONFLICT");
        }
        Instant now = databaseClock.transactionNow();
        TenantEntity tenant = new TenantEntity(
                idGenerator.nextLongId(), normalizedCode, command.name(), actorId, now);
        tenant.configure(command.settings(), actorId, now);
        entityManager.persist(tenant);
        return tenantView(tenant);
    }

    @Override
    @Transactional
    public TenantUserDirectoryController.TenantView changeTenantStatus(
            String tenantId,
            TenantUserDirectoryController.TenantStatusCommand command,
            String actorId) {
        TenantEntity tenant = entityManager.find(
                TenantEntity.class, Long.valueOf(tenantId), LockModeType.PESSIMISTIC_WRITE);
        if (tenant == null) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        TenantEntity.Status nextStatus = enumValue(
                TenantEntity.Status.class, command.status(), "TENANT_STATUS_INVALID");
        Instant now = databaseClock.transactionNow();
        tenant.changeStatus(nextStatus, command.expectedVersion(), command.reason(), actorId, now);
        if (nextStatus == TenantEntity.Status.SUSPENDED
                || nextStatus == TenantEntity.Status.CLOSED) {
            revokeTenantSessions(tenant.getId(), actorId, now);
        }
        return tenantView(tenant);
    }

    @Override
    @Transactional
    public TenantUserDirectoryController.UserDirectoryView changeUserStatus(
            String tenantId,
            String userId,
            TenantUserDirectoryController.UserStatusCommand command,
            String actorId) {
        UserEntity user = entityManager.find(
                UserEntity.class, Long.valueOf(userId), LockModeType.PESSIMISTIC_WRITE);
        if (user == null || !Long.valueOf(tenantId).equals(user.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        UserEntity.Status nextStatus = enumValue(
                UserEntity.Status.class, command.status(), "USER_STATUS_INVALID");
        Instant now = databaseClock.transactionNow();
        user.changeStatus(nextStatus, command.reason(), command.expectedAuthVersion(),
                actorId, now);
        if (nextStatus != UserEntity.Status.ACTIVE) {
            revokeAll(tenantId, userId, now);
        }
        return userView(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantUserDirectoryController.UserDirectoryView findUser(
            String tenantId,
            String userId) {
        UserEntity user = requireUser(Long.valueOf(tenantId), Long.valueOf(userId));
        return userView(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantUserDirectoryController.TenantView findTenant(String tenantId) {
        TenantEntity tenant = entityManager.find(
                TenantEntity.class, Long.valueOf(tenantId));
        if (tenant == null) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return tenantView(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantUserDirectoryController.PageView<TenantUserDirectoryController.TenantView>
            findTenants(String query, String status, int page, int size) {
        String normalizedQuery = nullableText(query);
        TenantEntity.Status requiredStatus = nullableEnum(
                TenantEntity.Status.class, status, "TENANT_STATUS_INVALID");
        String predicates = "";
        if (normalizedQuery != null) {
            predicates += " and (lower(t.code) like :query or lower(t.name) like :query)";
        }
        if (requiredStatus != null) {
            predicates += " and t.status = :status";
        }
        var dataQuery = entityManager.createQuery(
                "select t from TenantEntity t where 1 = 1" + predicates
                        + " order by t.code, t.id", TenantEntity.class);
        var countQuery = entityManager.createQuery(
                "select count(t) from TenantEntity t where 1 = 1" + predicates,
                Long.class);
        if (normalizedQuery != null) {
            dataQuery.setParameter("query", '%' + normalizedQuery.toLowerCase(Locale.ROOT) + '%');
            countQuery.setParameter("query", '%' + normalizedQuery.toLowerCase(Locale.ROOT) + '%');
        }
        if (requiredStatus != null) {
            dataQuery.setParameter("status", requiredStatus);
            countQuery.setParameter("status", requiredStatus);
        }
        List<TenantUserDirectoryController.TenantView> items = dataQuery
                .setFirstResult(Math.multiplyExact(page, size))
                .setMaxResults(size)
                .getResultList().stream().map(this::tenantView).toList();
        return new TenantUserDirectoryController.PageView<>(
                items, page, size, countQuery.getSingleResult());
    }

    @Override
    @Transactional(readOnly = true)
    public TenantUserDirectoryController.PageView<TenantUserDirectoryController.UserDirectoryView>
            findUsers(
                    String tenantId, String query, String status, String orgUnitId,
                    String positionId, int page, int size) {
        String normalizedQuery = nullableText(query);
        UserEntity.Status requiredStatus = nullableEnum(
                UserEntity.Status.class, status, "USER_STATUS_INVALID");
        Long requiredOrgUnit = nullableLong(orgUnitId, "ORG_UNIT_ID_INVALID");
        Long requiredPosition = nullableLong(positionId, "POSITION_ID_INVALID");
        String predicates = " where u.tenantId = :tenantId";
        if (normalizedQuery != null) {
            predicates += " and (lower(u.username) like :query or lower(u.displayName) like :query)";
        }
        if (requiredStatus != null) {
            predicates += " and u.status = :status";
        }
        if (requiredOrgUnit != null) {
            predicates += " and u.primaryOrgUnitId = :orgUnitId";
        }
        if (requiredPosition != null) {
            predicates += " and u.primaryPositionId = :positionId";
        }
        var dataQuery = entityManager.createQuery(
                "select u from UserEntity u" + predicates
                        + " order by u.normalizedUsername, u.id", UserEntity.class);
        var countQuery = entityManager.createQuery(
                "select count(u) from UserEntity u" + predicates, Long.class);
        dataQuery.setParameter("tenantId", Long.valueOf(tenantId));
        countQuery.setParameter("tenantId", Long.valueOf(tenantId));
        if (normalizedQuery != null) {
            String pattern = '%' + normalizedQuery.toLowerCase(Locale.ROOT) + '%';
            dataQuery.setParameter("query", pattern);
            countQuery.setParameter("query", pattern);
        }
        if (requiredStatus != null) {
            dataQuery.setParameter("status", requiredStatus);
            countQuery.setParameter("status", requiredStatus);
        }
        if (requiredOrgUnit != null) {
            dataQuery.setParameter("orgUnitId", requiredOrgUnit);
            countQuery.setParameter("orgUnitId", requiredOrgUnit);
        }
        if (requiredPosition != null) {
            dataQuery.setParameter("positionId", requiredPosition);
            countQuery.setParameter("positionId", requiredPosition);
        }
        List<TenantUserDirectoryController.UserDirectoryView> items = dataQuery
                .setFirstResult(Math.multiplyExact(page, size))
                .setMaxResults(size)
                .getResultList().stream().map(this::userView).toList();
        return new TenantUserDirectoryController.PageView<>(
                items, page, size, countQuery.getSingleResult());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantUserDirectoryController.OrgUnitView> findOrgUnits(
            String tenantId, String parentId, String type, String status) {
        Long requiredParent = nullableLong(parentId, "ORG_UNIT_ID_INVALID");
        OrgUnitEntity.UnitType requiredType = nullableEnum(
                OrgUnitEntity.UnitType.class, type, "ORG_UNIT_TYPE_INVALID");
        OrgUnitEntity.Status requiredStatus = nullableEnum(
                OrgUnitEntity.Status.class, status, "ORG_UNIT_STATUS_INVALID");
        String predicates = " where o.tenantId = :tenantId";
        if (requiredParent != null) {
            predicates += " and o.parentId = :parentId";
        }
        if (requiredType != null) {
            predicates += " and o.unitType = :type";
        }
        if (requiredStatus != null) {
            predicates += " and o.status = :status";
        }
        var query = entityManager.createQuery(
                "select o from OrgUnitEntity o" + predicates
                        + " order by o.path, o.id", OrgUnitEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId));
        if (requiredParent != null) {
            query.setParameter("parentId", requiredParent);
        }
        if (requiredType != null) {
            query.setParameter("type", requiredType);
        }
        if (requiredStatus != null) {
            query.setParameter("status", requiredStatus);
        }
        return query.getResultList().stream().map(this::orgUnitView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantUserDirectoryController.PositionView> findPositions(
            String tenantId, String orgUnitId, String status) {
        Long requiredOrgUnit = nullableLong(orgUnitId, "ORG_UNIT_ID_INVALID");
        PositionEntity.Status requiredStatus = nullableEnum(
                PositionEntity.Status.class, status, "POSITION_STATUS_INVALID");
        String predicates = " where p.tenantId = :tenantId";
        if (requiredOrgUnit != null) {
            predicates += " and p.orgUnitId = :orgUnitId";
        }
        if (requiredStatus != null) {
            predicates += " and p.status = :status";
        }
        var query = entityManager.createQuery(
                "select p from PositionEntity p" + predicates
                        + " order by p.code, p.id", PositionEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId));
        if (requiredOrgUnit != null) {
            query.setParameter("orgUnitId", requiredOrgUnit);
        }
        if (requiredStatus != null) {
            query.setParameter("status", requiredStatus);
        }
        return query.getResultList().stream().map(this::positionView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantUserDirectoryController.DirectorySnapshotView findSnapshot(
            String tenantId, String snapshotId) {
        DirectorySnapshotEntity snapshot = entityManager.find(
                DirectorySnapshotEntity.class, Long.valueOf(snapshotId));
        if (snapshot == null || !Long.valueOf(tenantId).equals(snapshot.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return new TenantUserDirectoryController.DirectorySnapshotView(
                snapshot.getId().toString(), snapshot.getProviderCode(),
                snapshot.getSnapshotVersion(), snapshot.getChecksum(),
                snapshot.getStatus().name(), snapshot.getGeneratedAt(),
                snapshot.getReceivedAt(), snapshot.getActivatedAt(),
                snapshot.getCounts());
    }

    private UserEntity requireUser(Long tenantId, Long userId) {
        UserEntity user = entityManager.find(UserEntity.class, userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return user;
    }

    private TenantUserDirectoryController.TenantView tenantView(TenantEntity tenant) {
        return new TenantUserDirectoryController.TenantView(
                tenant.getId().toString(), tenant.getCode(), tenant.getName(),
                tenant.getStatus().name(), tenant.getSettings(), tenant.getVersion());
    }

    private TenantUserDirectoryController.UserDirectoryView userView(UserEntity user) {
        return new TenantUserDirectoryController.UserDirectoryView(
                user.getId().toString(), user.getUsername(), user.getDisplayName(),
                user.getStatus().name(), user.getAuthVersion(),
                stringId(user.getPrimaryOrgUnitId()), stringId(user.getPrimaryPositionId()),
                user.getDirectorySnapshotVersion());
    }

    private TenantUserDirectoryController.OrgUnitView orgUnitView(OrgUnitEntity unit) {
        return new TenantUserDirectoryController.OrgUnitView(
                unit.getId().toString(), unit.getSnapshotId().toString(),
                unit.getUnitType().name(), unit.getCode(), unit.getName(),
                stringId(unit.getParentId()), unit.getPath(), unit.getDepth(),
                unit.getStatus().name());
    }

    private TenantUserDirectoryController.PositionView positionView(PositionEntity position) {
        return new TenantUserDirectoryController.PositionView(
                position.getId().toString(), position.getSnapshotId().toString(),
                position.getCode(), position.getName(), position.getOrgUnitId().toString(),
                position.getStatus().name());
    }

    private void revokeTenantSessions(Long tenantId, String actorId, Instant now) {
        List<SessionEntity> sessions = entityManager.createQuery("""
                        select s from SessionEntity s where s.tenantId = :tenantId
                        """, SessionEntity.class)
                .setParameter("tenantId", tenantId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        List<Long> changedSessionIds = new ArrayList<>();
        for (SessionEntity session : sessions) {
            if (session.revoke("TENANT_STATUS_CHANGED", actorId, now)) {
                changedSessionIds.add(session.getSessionId());
            }
        }
        revokeRefreshTokens(tenantId, changedSessionIds, actorId, now);
        sessions.stream()
                .filter(session -> changedSessionIds.contains(session.getSessionId()))
                .forEach(session -> {
                    recordTermination(session, actorId, now);
                    runtimeSynchronizer.synchronize(
                            tenantId.toString(), session.getUserId().toString(),
                            session.getSessionId().toString(), now);
                });
    }

    private void archiveCurrentSnapshot(
            Long tenantId,
            String providerCode,
            Long incomingSnapshotId,
            Instant now) {
        List<DirectorySnapshotEntity> active = entityManager.createQuery("""
                        select s from DirectorySnapshotEntity s
                         where s.tenantId = :tenantId
                           and s.providerCode = :providerCode
                           and s.status = :status
                           and s.id <> :incomingSnapshotId
                        """, DirectorySnapshotEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("providerCode", providerCode)
                .setParameter("status", DirectorySnapshotEntity.Status.ACTIVE)
                .setParameter("incomingSnapshotId", incomingSnapshotId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        active.forEach(snapshot -> snapshot.archive("directory-sync", now));
    }

    private void revokeRefreshTokens(
            Long tenantId,
            List<Long> sessionIds,
            String actorId,
            Instant now) {
        if (sessionIds.isEmpty()) {
            return;
        }
        List<RefreshTokenEntity> tokens = entityManager.createQuery("""
                        select t from RefreshTokenEntity t
                         where t.tenantId = :tenantId and t.sessionId in :sessionIds
                        """, RefreshTokenEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("sessionIds", sessionIds)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        tokens.forEach(token -> token.revoke(now, actorId));
    }

    private void recordTermination(
            SessionEntity session,
            String actorId,
            Instant occurredAt) {
        securityEventRecorder.record(new SessionSecurityEventRecorder.Termination(
                session.getTenantId().toString(), session.getUserId().toString(),
                session.getSessionId().toString(), session.getSessionVersion(),
                session.getStatus().name(), session.getRevokeReason(), actorId, occurredAt));
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type, String value, String reasonCode) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new Rbac3RuleViolation(reasonCode);
        }
    }

    private static <E extends Enum<E>> E nullableEnum(
            Class<E> type, String value, String reasonCode) {
        return nullableText(value) == null ? null : enumValue(type, value, reasonCode);
    }

    private static Long nullableLong(String value, String reasonCode) {
        String normalized = nullableText(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException exception) {
            throw new Rbac3RuleViolation(reasonCode);
        }
    }

    private static String nullableText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String stringId(Long value) {
        return value == null ? null : value.toString();
    }

    private List<ManifestResource> resources(
            List<ManifestResource> resources,
            String type) {
        return resources.stream()
                .filter(resource -> type.equalsIgnoreCase(
                        resource.metadata().getOrDefault("resourceType", "")))
                .toList();
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

    private long numericCount(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.longValue() : 0;
    }

}
