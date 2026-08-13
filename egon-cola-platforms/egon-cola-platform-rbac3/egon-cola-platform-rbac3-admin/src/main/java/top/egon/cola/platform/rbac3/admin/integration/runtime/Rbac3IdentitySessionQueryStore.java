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
import top.egon.cola.platform.rbac3.admin.bootstrap.repository.BootstrapSnapshotRepository;

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
        StepUpFacade.SessionStrengthStore,
        TenantUserDirectoryController.DirectoryCommandPort,
        TenantUserDirectoryController.DirectoryQueryPort {

    /**
     * 字段 `entityManager` 表示 `Rbac3IdentitySessionQueryStore` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `Rbac3IdentitySessionQueryStore` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `Rbac3IdentitySessionQueryStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `Rbac3IdentitySessionQueryStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `idGenerator` 表示 `Rbac3IdentitySessionQueryStore` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `Rbac3IdentitySessionQueryStore` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `Rbac3IdentitySessionQueryStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `Rbac3IdentitySessionQueryStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `databaseClock` 表示 `Rbac3IdentitySessionQueryStore` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `Rbac3IdentitySessionQueryStore` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `Rbac3IdentitySessionQueryStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `Rbac3IdentitySessionQueryStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;
    /**
     * 字段 `candidateService` 表示 `Rbac3IdentitySessionQueryStore` 中与 `candidate Service` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationCandidateService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `candidateService` stores the `candidate Service`-related state, dependency, configuration, or result of `Rbac3IdentitySessionQueryStore` (declared type `RoleActivationCandidateService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `candidateService` 时应保持 `Rbac3IdentitySessionQueryStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `candidateService`, preserve `Rbac3IdentitySessionQueryStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleActivationCandidateService candidateService;
    /**
     * 字段 `runtimeStore` 表示 `Rbac3IdentitySessionQueryStore` 中与 `runtime Store` 相关的状态、依赖、配置或结果（声明类型 `RedisAuthorizationRuntimeStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimeStore` stores the `runtime Store`-related state, dependency, configuration, or result of `Rbac3IdentitySessionQueryStore` (declared type `RedisAuthorizationRuntimeStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimeStore` 时应保持 `Rbac3IdentitySessionQueryStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimeStore`, preserve `Rbac3IdentitySessionQueryStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RedisAuthorizationRuntimeStore runtimeStore;
    /**
     * 字段 `directorySnapshotStore` 表示 `Rbac3IdentitySessionQueryStore` 中与 `directory Snapshot Store` 相关的状态、依赖、配置或结果（声明类型 `DirectorySnapshotStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `directorySnapshotStore` stores the `directory Snapshot Store`-related state, dependency, configuration, or result of `Rbac3IdentitySessionQueryStore` (declared type `DirectorySnapshotStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `directorySnapshotStore` 时应保持 `Rbac3IdentitySessionQueryStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `directorySnapshotStore`, preserve `Rbac3IdentitySessionQueryStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DirectorySnapshotStore directorySnapshotStore;
    /**
     * 字段 `directorySnapshotMaterializer` 表示 `Rbac3IdentitySessionQueryStore` 中与 `directory Snapshot Materializer` 相关的状态、依赖、配置或结果（声明类型 `DirectorySnapshotMaterializer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `directorySnapshotMaterializer` stores the `directory Snapshot Materializer`-related state, dependency, configuration, or result of `Rbac3IdentitySessionQueryStore` (declared type `DirectorySnapshotMaterializer`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `directorySnapshotMaterializer` 时应保持 `Rbac3IdentitySessionQueryStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `directorySnapshotMaterializer`, preserve `Rbac3IdentitySessionQueryStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DirectorySnapshotMaterializer directorySnapshotMaterializer;
    /**
     * 字段 `runtimeSynchronizer` 表示 `Rbac3IdentitySessionQueryStore` 中与 `runtime Synchronizer` 相关的状态、依赖、配置或结果（声明类型 `SessionRuntimeSynchronizer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimeSynchronizer` stores the `runtime Synchronizer`-related state, dependency, configuration, or result of `Rbac3IdentitySessionQueryStore` (declared type `SessionRuntimeSynchronizer`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimeSynchronizer` 时应保持 `Rbac3IdentitySessionQueryStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimeSynchronizer`, preserve `Rbac3IdentitySessionQueryStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionRuntimeSynchronizer runtimeSynchronizer;
    /**
     * 字段 `securityEventRecorder` 表示 `Rbac3IdentitySessionQueryStore` 中与 `security Event Recorder` 相关的状态、依赖、配置或结果（声明类型 `SessionSecurityEventRecorder`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `securityEventRecorder` stores the `security Event Recorder`-related state, dependency, configuration, or result of `Rbac3IdentitySessionQueryStore` (declared type `SessionSecurityEventRecorder`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `securityEventRecorder` 时应保持 `Rbac3IdentitySessionQueryStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `securityEventRecorder`, preserve `Rbac3IdentitySessionQueryStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionSecurityEventRecorder securityEventRecorder;
    /**
     * 字段 `directorySnapshotProcessor` 表示 `Rbac3IdentitySessionQueryStore` 中与 `directory Snapshot Processor` 相关的状态、依赖、配置或结果（声明类型 `DirectorySnapshotProcessor`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `directorySnapshotProcessor` stores the `directory Snapshot Processor`-related state, dependency, configuration, or result of `Rbac3IdentitySessionQueryStore` (declared type `DirectorySnapshotProcessor`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `directorySnapshotProcessor` 时应保持 `Rbac3IdentitySessionQueryStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `directorySnapshotProcessor`, preserve `Rbac3IdentitySessionQueryStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DirectorySnapshotProcessor directorySnapshotProcessor =
            new DirectorySnapshotProcessor();

    /**
     * 构造器 `Rbac3IdentitySessionQueryStore` 用于创建并初始化 `Rbac3IdentitySessionQueryStore` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3IdentitySessionQueryStore` creates and initializes `Rbac3IdentitySessionQueryStore`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3IdentitySessionQueryStore` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3IdentitySessionQueryStore`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param candidateService 输入参数 `candidateService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimeStore 输入参数 `runtimeStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param directorySnapshotStore 输入参数 `directorySnapshotStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param directorySnapshotMaterializer 输入参数 `directorySnapshotMaterializer`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimeSynchronizer 输入参数 `runtimeSynchronizer`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param securityEventRecorder 输入参数 `securityEventRecorder`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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
        TenantEntity tenant = entityManager.find(TenantEntity.class, Long.valueOf(tenantId));
        UserEntity user = requireUser(Long.valueOf(tenantId), Long.valueOf(userId));
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
     * 方法 `submit` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `submit` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `submit` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `submit` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `submit` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `submit`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `createTenant` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `create Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `createTenant` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `create Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `createTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `createTenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `changeTenantStatus` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `change Tenant Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `changeTenantStatus` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `change Tenant Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `changeTenantStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `changeTenantStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `changeUserStatus` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `change User Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `changeUserStatus` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `change User Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `changeUserStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `changeUserStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `findUser` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `find User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findUser` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `find User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findUser`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public TenantUserDirectoryController.UserDirectoryView findUser(
            String tenantId,
            String userId) {
        UserEntity user = requireUser(Long.valueOf(tenantId), Long.valueOf(userId));
        return userView(user);
    }

    /**
     * 方法 `findTenant` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `find Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findTenant` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `find Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findTenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `findTenants` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `find Tenants` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findTenants` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `find Tenants` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findTenants` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findTenants`, then continue the business flow using its result, exception, or side effect.
     *
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param page 输入参数 `page`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param size 输入参数 `size`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `findUsers` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `find Users` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findUsers` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `find Users` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findUsers` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findUsers`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param positionId 输入参数 `positionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param page 输入参数 `page`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param size 输入参数 `size`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `findOrgUnits` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `find Org Units` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findOrgUnits` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `find Org Units` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findOrgUnits` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findOrgUnits`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param parentId 输入参数 `parentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `findPositions` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `find Positions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findPositions` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `find Positions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findPositions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findPositions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `findSnapshot` 按照 `Rbac3IdentitySessionQueryStore` 的职责处理输入，完成 `find Snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findSnapshot` processes its inputs according to `Rbac3IdentitySessionQueryStore`'s responsibility, performs the `find Snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findSnapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findSnapshot`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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
    private UserEntity requireUser(Long tenantId, Long userId) {
        UserEntity user = entityManager.find(UserEntity.class, userId);
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
    private TenantUserDirectoryController.TenantView tenantView(TenantEntity tenant) {
        return new TenantUserDirectoryController.TenantView(
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
    private TenantUserDirectoryController.UserDirectoryView userView(UserEntity user) {
        return new TenantUserDirectoryController.UserDirectoryView(
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
    private TenantUserDirectoryController.OrgUnitView orgUnitView(OrgUnitEntity unit) {
        return new TenantUserDirectoryController.OrgUnitView(
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
    private TenantUserDirectoryController.PositionView positionView(PositionEntity position) {
        return new TenantUserDirectoryController.PositionView(
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
