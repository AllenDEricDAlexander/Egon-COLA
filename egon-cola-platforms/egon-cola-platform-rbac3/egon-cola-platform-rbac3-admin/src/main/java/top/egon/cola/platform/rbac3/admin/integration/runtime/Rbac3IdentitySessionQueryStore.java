package top.egon.cola.platform.rbac3.admin.integration.runtime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.auth.application.AuthenticationFacade;
import top.egon.cola.platform.rbac3.admin.auth.application.RefreshFacade;
import top.egon.cola.platform.rbac3.admin.auth.application.StepUpFacade;
import top.egon.cola.platform.rbac3.admin.bootstrap.service.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.DirectorySnapshotPO;
import top.egon.cola.platform.rbac3.admin.directory.service.DirectorySnapshotProcessor;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.OrgUnitPO;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.PositionPO;
import top.egon.cola.platform.rbac3.admin.directory.repository.jpa.DirectorySnapshotMaterializer;
import top.egon.cola.platform.rbac3.admin.directory.repository.jpa.JpaDirectorySnapshotRepository;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.interfaces.http.AssignmentController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.SessionController;
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
import top.egon.cola.platform.rbac3.admin.bootstrap.repository.BootstrapSnapshotRepository;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.SnapshotModelVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.DirectorySnapshotStatusEnum;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.OrgUnitUnitTypeEnum;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.OrgUnitStatusEnum;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.PositionStatusEnum;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.MaterializationResultVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.IngestionResultVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.DirectorySnapshotOutcomeEnum;
import top.egon.cola.platform.rbac3.admin.tenant.domain.enums.TenantStatusEnum;
import top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.tenant.domain.dto.CreateTenantCommandDTO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.dto.TenantStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.identity.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.directory.domain.dto.DirectorySnapshotCommandDTO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.DirectorySyncVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.vo.TenantVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.OrgUnitVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.PositionVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.DirectorySnapshotVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.DirectoryPageVO;

/**
 * 类型 `Rbac3IdentitySessionQueryStore` 位于当前包内，是类型，用于承载 `Rbac3 Identity Session Query Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3IdentitySessionQueryStore` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Identity Session Query Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Read models and narrow command adapters shared by authentication controllers.
 */
@Repository
public class Rbac3IdentitySessionQueryStore implements
        AuthenticationFacade.LoginStateSource,
        RefreshFacade.RefreshStateSource,
        BootstrapSnapshotRepository,
        SessionController.SessionManagementPort,
        AssignmentController.SessionStrengthPort,
        StepUpFacade.IdentitySource,
        StepUpFacade.SessionStrengthStore {

    private final EntityManager entityManager;
    private final RoleActivationCandidateService candidateService;
    private final RedisAuthorizationRuntimeStore runtimeStore;
    private final SessionRuntimeSynchronizer runtimeSynchronizer;
    private final SessionSecurityEventRecorder securityEventRecorder;


    /**
     * 构造器 `Rbac3IdentitySessionQueryStore` 用于创建并初始化 `Rbac3IdentitySessionQueryStore` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3IdentitySessionQueryStore` creates and initializes `Rbac3IdentitySessionQueryStore`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3IdentitySessionQueryStore` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3IdentitySessionQueryStore`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param candidateService 输入参数 `candidateService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimeStore 输入参数 `runtimeStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimeSynchronizer 输入参数 `runtimeSynchronizer`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param securityEventRecorder 输入参数 `securityEventRecorder`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3IdentitySessionQueryStore(
            EntityManager entityManager,
            RoleActivationCandidateService candidateService,
            RedisAuthorizationRuntimeStore runtimeStore,
            SessionRuntimeSynchronizer runtimeSynchronizer,
            SessionSecurityEventRecorder securityEventRecorder) {
        this.entityManager = entityManager;
        this.candidateService = candidateService;
        this.runtimeStore = runtimeStore;
        this.runtimeSynchronizer = runtimeSynchronizer;
        this.securityEventRecorder = securityEventRecorder;
    }

    /**
     * 方法 `load` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `load` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthenticationFacade.LoginState load(
            String tenantCode,
            String userId,
            Instant now) {
        TenantPO tenant = entityManager.createQuery("""
                        select t from TenantEntity t where lower(t.code) = :code
                        """, TenantPO.class)
                .setParameter("code", tenantCode.toLowerCase(java.util.Locale.ROOT))
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new Rbac3RuleViolation("AUTHENTICATION_FAILED"));
        UserPO user = requireUser(tenant.getId(), Long.valueOf(userId));
        int candidates = candidateService.candidates(
                        tenant.getId().toString(), userId, now)
                .applications().stream()
                .mapToInt(application -> application.candidates().size())
                .sum();
        return new AuthenticationFacade.LoginState(
                tenant.getId().toString(), user.getAuthVersion(),
                tenant.getPolicyVersion(), candidates);
    }

    /**
     * 方法 `load` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `load` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
     *
     * @param familyId 输入参数 `familyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `find` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `find` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `find` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `find` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `find` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `find`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<BootstrapView> find(
            String tenantId,
            String userId,
            String sessionId) {
        UserPO user = requireUser(Long.valueOf(tenantId), Long.valueOf(userId));
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

    /**
     * 方法 `findByUser` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `find By User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findByUser` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `find By User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findByUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findByUser`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `revoke` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `revoke` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `revoke` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `revoke` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `revoke` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `revoke`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `revokeAll` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `revoke All` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `revokeAll` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `revoke All` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `revokeAll` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `revokeAll`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `authenticationStrength` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `authentication Strength` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authenticationStrength` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `authentication Strength` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authenticationStrength` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authenticationStrength`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `load` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `load` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public StepUpFacade.Identity load(String tenantId, String userId) {
        TenantPO tenant = entityManager.find(TenantPO.class, Long.valueOf(tenantId));
        UserPO user = requireUser(Long.valueOf(tenantId), Long.valueOf(userId));
        if (tenant == null) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return new StepUpFacade.Identity(tenant.getCode(), user.getUsername());
    }

    /**
     * 方法 `strengthen` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `strengthen` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `strengthen` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `strengthen` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `strengthen` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `strengthen`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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























    /**
     * 方法 `requireUser` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `require User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireUser` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `require User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireUser`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private UserPO requireUser(Long tenantId, Long userId) {
        UserPO user = entityManager.find(UserPO.class, userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return user;
    }

    /**
     * 方法 `tenantView` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `tenant View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantView` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `tenant View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenantView` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenantView`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenant 输入参数 `tenant`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private TenantVO tenantView(TenantPO tenant) {
        return new TenantVO(
                tenant.getId().toString(), tenant.getCode(), tenant.getName(),
                tenant.getStatus().name(), tenant.getSettings(), tenant.getVersion());
    }

    /**
     * 方法 `userView` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `user View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `userView` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `user View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `userView` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `userView`, then continue the business flow using its result, exception, or side effect.
     *
     * @param user 输入参数 `user`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private UserDirectoryVO userView(UserPO user) {
        return new UserDirectoryVO(
                user.getId().toString(), user.getUsername(), user.getDisplayName(),
                user.getStatus().name(), user.getAuthVersion(),
                stringId(user.getPrimaryOrgUnitId()), stringId(user.getPrimaryPositionId()),
                user.getDirectorySnapshotVersion());
    }

    /**
     * 方法 `orgUnitView` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `org Unit View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `orgUnitView` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `org Unit View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `orgUnitView` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `orgUnitView`, then continue the business flow using its result, exception, or side effect.
     *
     * @param unit 输入参数 `unit`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private OrgUnitVO orgUnitView(OrgUnitPO unit) {
        return new OrgUnitVO(
                unit.getId().toString(), unit.getSnapshotId().toString(),
                unit.getUnitType().name(), unit.getCode(), unit.getName(),
                stringId(unit.getParentId()), unit.getPath(), unit.getDepth(),
                unit.getStatus().name());
    }

    /**
     * 方法 `positionView` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `position View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `positionView` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `position View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `positionView` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `positionView`, then continue the business flow using its result, exception, or side effect.
     *
     * @param position 输入参数 `position`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private PositionVO positionView(PositionPO position) {
        return new PositionVO(
                position.getId().toString(), position.getSnapshotId().toString(),
                position.getCode(), position.getName(), position.getOrgUnitId().toString(),
                position.getStatus().name());
    }

    /**
     * 方法 `revokeTenantSessions` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `revoke Tenant Sessions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `revokeTenantSessions` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `revoke Tenant Sessions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `revokeTenantSessions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `revokeTenantSessions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `archiveCurrentSnapshot` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `archive Current Snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `archiveCurrentSnapshot` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `archive Current Snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `archiveCurrentSnapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `archiveCurrentSnapshot`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param providerCode 输入参数 `providerCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param incomingSnapshotId 输入参数 `incomingSnapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void archiveCurrentSnapshot(
            Long tenantId,
            String providerCode,
            Long incomingSnapshotId,
            Instant now) {
        List<DirectorySnapshotPO> active = entityManager.createQuery("""
                        select s from DirectorySnapshotEntity s
                         where s.tenantId = :tenantId
                           and s.providerCode = :providerCode
                           and s.status = :status
                           and s.id <> :incomingSnapshotId
                        """, DirectorySnapshotPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("providerCode", providerCode)
                .setParameter("status", DirectorySnapshotStatusEnum.ACTIVE)
                .setParameter("incomingSnapshotId", incomingSnapshotId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        active.forEach(snapshot -> snapshot.archive("directory-sync", now));
    }

    /**
     * 方法 `revokeRefreshTokens` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `revoke Refresh Tokens` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `revokeRefreshTokens` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `revoke Refresh Tokens` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `revokeRefreshTokens` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `revokeRefreshTokens`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionIds 输入参数 `sessionIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `recordTermination` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `record Termination` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `recordTermination` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `record Termination` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `recordTermination` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `recordTermination`, then continue the business flow using its result, exception, or side effect.
     *
     * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param occurredAt 输入参数 `occurredAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void recordTermination(
            SessionEntity session,
            String actorId,
            Instant occurredAt) {
        securityEventRecorder.record(new SessionSecurityEventRecorder.Termination(
                session.getTenantId().toString(), session.getUserId().toString(),
                session.getSessionId().toString(), session.getSessionVersion(),
                session.getStatus().name(), session.getRevokeReason(), actorId, occurredAt));
    }

    /**
     * 方法 `enumValue` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `enum Value` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `enumValue` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `enum Value` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `enumValue` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `enumValue`, then continue the business flow using its result, exception, or side effect.
     *
     * @param <E> 类型参数表示待解析的枚举类型；type parameter representing the enum type to resolve.
     * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static <E extends Enum<E>> E enumValue(
            Class<E> type, String value, String reasonCode) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new Rbac3RuleViolation(reasonCode);
        }
    }

    /**
     * 方法 `nullableEnum` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `nullable Enum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `nullableEnum` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `nullable Enum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `nullableEnum` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `nullableEnum`, then continue the business flow using its result, exception, or side effect.
     *
     * @param <E> 类型参数表示可选枚举的具体类型；type parameter representing the optional enum type.
     * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static <E extends Enum<E>> E nullableEnum(
            Class<E> type, String value, String reasonCode) {
        return nullableText(value) == null ? null : enumValue(type, value, reasonCode);
    }

    /**
     * 方法 `nullableLong` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `nullable Long` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `nullableLong` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `nullable Long` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `nullableLong` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `nullableLong`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `nullableText` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `nullable Text` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `nullableText` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `nullable Text` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `nullableText` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `nullableText`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String nullableText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 方法 `stringId` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `string Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `stringId` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `string Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `stringId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `stringId`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String stringId(Long value) {
        return value == null ? null : value.toString();
    }

    /**
     * 方法 `resources` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `resources` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resources` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `resources` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `resources` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resources`, then continue the business flow using its result, exception, or side effect.
     *
     * @param resources 输入参数 `resources`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private List<ManifestResource> resources(
            List<ManifestResource> resources,
            String type) {
        return resources.stream()
                .filter(resource -> type.equalsIgnoreCase(
                        resource.metadata().getOrDefault("resourceType", "")))
                .toList();
    }

    /**
     * 方法 `longCounts` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `long Counts` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `longCounts` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `long Counts` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `longCounts` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `longCounts`, then continue the business flow using its result, exception, or side effect.
     *
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Map<String, Long> longCounts(Map<String, Object> values) {
        Map<String, Long> result = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (value instanceof Number number) {
                result.put(key, number.longValue());
            }
        });
        return Map.copyOf(result);
    }

    /**
     * 方法 `numericCount` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `numeric Count` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `numericCount` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `numeric Count` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `numericCount` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `numericCount`, then continue the business flow using its result, exception, or side effect.
     *
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private long numericCount(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.longValue() : 0;
    }

}
