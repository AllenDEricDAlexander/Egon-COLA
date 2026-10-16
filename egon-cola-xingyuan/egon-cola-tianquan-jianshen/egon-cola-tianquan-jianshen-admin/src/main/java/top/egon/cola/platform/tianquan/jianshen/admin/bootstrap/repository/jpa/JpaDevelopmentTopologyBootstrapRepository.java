package top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Valid;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.domain.dto.ResolvedPlatformBootstrapCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.domain.dto.ResolvedPlatformBootstrapCommandDTO.PlatformCatalogBindingDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.DdcCatalogGateway;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.domain.po.UserRoleAssignmentPO;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.domain.Rbac3DevelopmentTopology;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.state.repository.TenantAuthorizationStateRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.domain.po.UserPO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.application.domain.po.ApplicationPO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.domain.po.PermissionPO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.domain.po.RolePO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.enums.ResourceStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.po.ResourcePO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.enums.RoleResourceGrantStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.po.RoleResourceGrantPO;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Objects;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.repository.DevelopmentBootstrapPort;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.domain.vo.ApplicationDefinitionVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.domain.enums.PermissionRiskLevelEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.domain.enums.RoleTypeEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.domain.enums.RoleRiskLevelEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.domain.enums.UserRoleAssignmentTypeEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.domain.enums.UserRoleAssignmentStatusEnum;

/**
 * 类型 `JpaDevelopmentTopologyBootstrapRepository` 位于当前包内，是类型，用于承载 `Postgresql Development Topology Bootstrap Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaDevelopmentTopologyBootstrapRepository` is a type in its package and carries the responsibility, state, or contract for `Postgresql Development Topology Bootstrap Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * PostgreSQL-backed, idempotent local topology bootstrap guarded by an advisory lock.
 */
@Repository("jpaDevelopmentTopologyBootstrapRepository")
@Validated
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class JpaDevelopmentTopologyBootstrapRepository
        implements DevelopmentBootstrapPort {

    /**
     * 字段 `BOOTSTRAP_LOCK_KEY` 表示 `JpaDevelopmentTopologyBootstrapRepository` 中与 `BOOTSTRAP LOCK KEY` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `BOOTSTRAP_LOCK_KEY` stores the `BOOTSTRAP LOCK KEY`-related state, dependency, configuration, or result of `JpaDevelopmentTopologyBootstrapRepository` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `BOOTSTRAP_LOCK_KEY` 时应保持 `JpaDevelopmentTopologyBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `BOOTSTRAP_LOCK_KEY`, preserve `JpaDevelopmentTopologyBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final long BOOTSTRAP_LOCK_KEY = 0x5242414333494450L;
    /**
     * 字段 `ACTOR` 表示 `JpaDevelopmentTopologyBootstrapRepository` 中与 `ACTOR` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `ACTOR` stores the `ACTOR`-related state, dependency, configuration, or result of `JpaDevelopmentTopologyBootstrapRepository` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `ACTOR` 时应保持 `JpaDevelopmentTopologyBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ACTOR`, preserve `JpaDevelopmentTopologyBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String ACTOR = "tianquan-jianshen-development-bootstrap";

    /**
     * 字段 `entityManager` 表示 `JpaDevelopmentTopologyBootstrapRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaDevelopmentTopologyBootstrapRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaDevelopmentTopologyBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaDevelopmentTopologyBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Qualifier("jpaSharedEM_entityManagerFactory") private final EntityManager entityManager;
    /**
     * 字段 `clock` 表示 `JpaDevelopmentTopologyBootstrapRepository` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `JpaDevelopmentTopologyBootstrapRepository` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `JpaDevelopmentTopologyBootstrapRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `JpaDevelopmentTopologyBootstrapRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Qualifier("idpClock") private final Clock clock;
    @Qualifier("jpaTenantAuthorizationStateRepository") private final TenantAuthorizationStateRepository authorizationState;
    private static final List<String> CURRENT_PLATFORM_CODES = List.of(
            "tianquan-jianshen-admin", "tianquan-shoubing-admin", "yuheng-admin", "tianshu-admin");
    private static final List<String> LEGACY_PLATFORM_CODES = List.of(
            "rbac3-admin", "idp-admin", "gateway-admin", "ddc-admin");
    private static final List<String> LEGACY_APPLICATION_IDS = List.of("rbac3", "idp", "gateway-admin", "ddc");
    private static final List<String> PLATFORM_BUSINESS_CODES = List.of("permission", "identity", "platform", "platform");

    @Qualifier("ddcCatalogGateway") private final DdcCatalogGateway catalog;
    @Qualifier("egonColaValidationUtils") private final ValidationUtils validation;

    /**
     * 构造器 `JpaDevelopmentTopologyBootstrapRepository` 用于创建并初始化 `JpaDevelopmentTopologyBootstrapRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaDevelopmentTopologyBootstrapRepository` creates and initializes `JpaDevelopmentTopologyBootstrapRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaDevelopmentTopologyBootstrapRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaDevelopmentTopologyBootstrapRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param authorizationState 输入参数 `authorizationState`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaDevelopmentTopologyBootstrapRepository(
            EntityManager entityManager,
            Clock clock,
            TenantAuthorizationStateRepository authorizationState) {
        this(Objects.requireNonNull(entityManager, "entityManager"),
                Objects.requireNonNull(clock, "clock"),
                Objects.requireNonNull(authorizationState, "authorizationState"), null, null);
    }

    /**
     * 方法 `bootstrap` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `bootstrap` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bootstrap` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `bootstrap` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bootstrap` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bootstrap`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void bootstrap(String tenantId, String identitySub) {
        bootstrap(tenantId, identitySub, Rbac3DevelopmentTopology.applications());
    }

    private void bootstrap(String tenantId, String identitySub, List<ApplicationDefinitionVO> definitions) {
        acquireLock();
        Instant now = clock.instant();
        Long normalizedTenantId = normalizeTenantId(tenantId);
        authorizationState.ensureVerifiedTenant(
                TenantAuthorizationStateRepository.VerifiedTenant.of(normalizedTenantId),
                ACTOR);
        boolean changed = false;
        String normalizedIdentitySub = required(identitySub, "identitySub");
        UserPO user = findUser(normalizedTenantId, normalizedIdentitySub);
        if (user == null) {
            user = new UserPO(
                    SnowflakeIdGenerator.nextLongId(),
                    normalizedTenantId,
                    normalizedIdentitySub,
                    top.egon.cola.platform.tianquan.jianshen.admin.iam.user.domain.enums.UserStatusEnum.ACTIVE,
                    ACTOR,
                    now
            );
            entityManager.persist(user);
            changed = true;
        }

        for (var definition : definitions) {
            changed |= ensureApplication(normalizedTenantId, user.getId(), definition, now);
        }
        if (changed) {
            authorizationState.increment(normalizedTenantId, ACTOR);
            user.advanceAuthorizationVersion(user.getAuthVersion(), ACTOR, now);
        }
        entityManager.flush();
    }

    /**
     * Resolves all catalog facts before any write. Locks also fence concurrent role,
     * entitlement and Business grants while unused system placeholders are rebound.
     */
    @Override
    @Transactional(timeout = 30)
    public void bindResolvedApplications(@NotNull @Valid ResolvedPlatformBootstrapCommandDTO command) {
        List<PlatformCatalogBindingDTO> bindings = verifiedBindings(command);
        acquireLock();
        entityManager.createNativeQuery("""
                lock table rbac3_business_participation, rbac3_data_rule, rbac3_field_definition,
                    rbac3_field_rule, rbac3_operation_sod_rule, rbac3_permission, rbac3_resource,
                    rbac3_resource_api_binding, rbac3_role, rbac3_role_closure, rbac3_role_inheritance,
                    rbac3_role_resource_grant, rbac3_service_permission, rbac3_service_principal,
                    rbac3_sod_set, rbac3_tenant_application, rbac3_user_active_role, rbac3_user_business_access
                    in share row exclusive mode
                """).executeUpdate();
        Map<String, ApplicationPO> applications = new LinkedHashMap<>();
        for (var binding : bindings) {
            ApplicationPO application = findApplication(null, binding.getApplicationCode());
            String legacyCode = LEGACY_PLATFORM_CODES.get(platformIndex(binding.getApplicationCode()));
            if (!legacyCode.equals(binding.getApplicationCode())) {
                ApplicationPO initial = entityManager.find(ApplicationPO.class,
                        8000000000000000001L + platformIndex(binding.getApplicationCode()));
                if (initial == null) { initial = findApplication(null, legacyCode); }
                if (application != null && initial != null && !application.getId().equals(initial.getId())) {
                    throw new IllegalStateException("RESOLVED_BOOTSTRAP_BINDING_CONFLICT");
                }
                if (application == null) { application = initial; }
            }
            if (application != null) {
                entityManager.lock(application, LockModeType.PESSIMISTIC_WRITE);
                requireBindableApplication(application, binding);
            }
            applications.put(binding.getApplicationCode(), application);
        }
        Instant now = clock.instant();
        for (var binding : bindings) {
            ApplicationPO application = applications.get(binding.getApplicationCode());
            if (application == null) {
                var definition = platformDefinition(binding.getApplicationCode());
                entityManager.persist(new ApplicationPO(SnowflakeIdGenerator.nextLongId(),
                        normalizeTenantId(command.getTenantId()), binding.getDdcApplicationId(), binding.getDdcBusinessId(),
                        binding.getApplicationCode(), definition.applicationName(), definition.displayPriority(), ACTOR, now));
            } else if (!sameCatalogBinding(application, binding)) {
                int updated = entityManager.createNativeQuery("""
                        update rbac3_application
                           set application_code = :canonicalApplicationCode,
                               ddc_application_id = :applicationPublicId, ddc_business_id = :businessPublicId,
                               version = version + 1, updated_by = :actor, updated_at = :now
                         where id = :applicationId and application_code = :applicationCode
                           and version = :expectedVersion and created_by = :seedActor and updated_by = :seedActor
                           and ddc_application_id = :oldApplicationId and ddc_business_id = :oldBusinessId
                           and status = 'ACTIVE'
                        """)
                        .setParameter("canonicalApplicationCode", binding.getApplicationCode())
                        .setParameter("applicationPublicId", binding.getDdcApplicationId())
                        .setParameter("businessPublicId", binding.getDdcBusinessId())
                        .setParameter("actor", ACTOR).setParameter("now", now)
                        .setParameter("applicationId", application.getId())
                        .setParameter("applicationCode", application.getApplicationCode())
                        .setParameter("expectedVersion", application.getVersion()).setParameter("seedActor", "flyway-v10")
                        .setParameter("oldApplicationId", application.getDdcApplicationId())
                        .setParameter("oldBusinessId", application.getDdcBusinessId()).executeUpdate();
                if (updated != 1) {
                    throw new IllegalStateException("RESOLVED_BOOTSTRAP_BINDING_CONFLICT");
                }
                entityManager.refresh(application);
            }
        }
        entityManager.flush();
    }

    /** Reuses the existing role/grant algorithm only after the platform resources exist. */
    @Override
    @Transactional(timeout = 30)
    public void bootstrapResolved(@NotNull @Valid ResolvedPlatformBootstrapCommandDTO command) {
        bindResolvedApplications(command);
        List<ApplicationDefinitionVO> definitions = command.getApplications().stream()
                .map(binding -> platformDefinition(binding.getApplicationCode())).toList();
        bootstrap(command.getTenantId(), command.getIdentitySub(), definitions);
    }

    private List<PlatformCatalogBindingDTO> verifiedBindings(ResolvedPlatformBootstrapCommandDTO command) {
        if (catalog == null || validation == null) {
            throw new IllegalStateException("RESOLVED_BOOTSTRAP_UNAVAILABLE");
        }
        validation.validate(Objects.requireNonNull(command, "command"));
        normalizeTenantId(command.getTenantId());
        required(command.getIdentitySub(), "identitySub");
        List<PlatformCatalogBindingDTO> bindings = command.getApplications().stream()
                .map(binding -> new PlatformCatalogBindingDTO(binding.getApplicationCode(), binding.getBusinessCode(),
                        binding.getDdcApplicationId(), binding.getDdcBusinessId())).toList();
        Set<String> codes = new HashSet<>();
        Set<String> publicIds = new HashSet<>();
        for (var binding : bindings) {
            if (!codes.add(binding.getApplicationCode()) || !publicIds.add(binding.getDdcApplicationId())) {
                throw new IllegalArgumentException("RESOLVED_BOOTSTRAP_INVALID_PLATFORMS");
            }
            int index = platformIndex(binding.getApplicationCode());
            if (!PLATFORM_BUSINESS_CODES.get(index).equals(binding.getBusinessCode())) {
                throw new IllegalArgumentException("RESOLVED_BOOTSTRAP_INVALID_BUSINESS");
            }
        }
        if (!codes.equals(Set.copyOf(CURRENT_PLATFORM_CODES)) && !codes.equals(Set.copyOf(LEGACY_PLATFORM_CODES))) {
            throw new IllegalArgumentException("RESOLVED_BOOTSTRAP_INVALID_PLATFORMS");
        }
        for (var binding : bindings) {
            var business = catalog.findBusiness(binding.getDdcBusinessId())
                    .orElseThrow(() -> new IllegalStateException("RESOLVED_BOOTSTRAP_CATALOG_UNAVAILABLE"));
            var application = catalog.findApplication(binding.getDdcApplicationId())
                    .orElseThrow(() -> new IllegalStateException("RESOLVED_BOOTSTRAP_CATALOG_UNAVAILABLE"));
            if (!business.enabled() || !application.applicationEnabled() || !application.businessEnabled()
                    || !binding.getDdcBusinessId().equals(business.ddcBusinessId())
                    || !binding.getBusinessCode().equals(business.bizCode())
                    || !binding.getDdcApplicationId().equals(application.ddcApplicationId())
                    || !binding.getDdcBusinessId().equals(application.ddcBusinessId())
                    || !binding.getBusinessCode().equals(application.bizCode())
                    || !binding.getApplicationCode().equals(application.appCode())) {
                throw new IllegalStateException("RESOLVED_BOOTSTRAP_CATALOG_MISMATCH");
            }
        }
        return bindings;
    }

    private void requireBindableApplication(ApplicationPO application, PlatformCatalogBindingDTO binding) {
        if (application.getStatus() != top.egon.cola.platform.tianquan.jianshen.admin.iam.application.domain.enums.ApplicationStatusEnum.ACTIVE) {
            throw new IllegalStateException("RESOLVED_BOOTSTRAP_BINDING_CONFLICT");
        }
        int index = platformIndex(binding.getApplicationCode());
        boolean systemSeed = application.getId().equals(8000000000000000001L + index)
                && (LEGACY_PLATFORM_CODES.get(index).equals(application.getApplicationCode())
                    || CURRENT_PLATFORM_CODES.get(index).equals(application.getApplicationCode()))
                && "flyway-v10".equals(application.getCreatedBy());
        if (sameCatalogBinding(application, binding)) {
            if (!systemSeed && !ACTOR.equals(application.getCreatedBy())) {
                throw new IllegalStateException("RESOLVED_BOOTSTRAP_BINDING_CONFLICT");
            }
            return;
        }
        if (!systemSeed || !LEGACY_PLATFORM_CODES.get(index).equals(application.getApplicationCode())
                || application.getVersion() != 0 || !"flyway-v10".equals(application.getUpdatedBy())
                || !LEGACY_APPLICATION_IDS.get(index).equals(application.getDdcApplicationId())
                || !PLATFORM_BUSINESS_CODES.get(index).equals(application.getDdcBusinessId())) {
            throw new IllegalStateException("RESOLVED_BOOTSTRAP_BINDING_CONFLICT");
        }
        Number dependencies = (Number) entityManager.createNativeQuery("""
                select (select count(*) from rbac3_role where application_id = :applicationId)
                     + (select count(*) from rbac3_tenant_application where application_id = :applicationId)
                     + (select count(*) from rbac3_user_business_access where ddc_business_id = :businessId)
                     + (select count(*) from rbac3_business_participation where application_id = :applicationId)
                     + (select count(*) from rbac3_data_rule where application_id = :applicationId)
                     + (select count(*) from rbac3_field_definition where application_id = :applicationId)
                     + (select count(*) from rbac3_field_rule where application_id = :applicationId)
                     + (select count(*) from rbac3_operation_sod_rule where application_id = :applicationId)
                     + (select count(*) from rbac3_resource where application_id = :applicationId)
                     + (select count(*) from rbac3_resource_api_binding where application_id = :applicationId)
                     + (select count(*) from rbac3_role_closure where application_id = :applicationId)
                     + (select count(*) from rbac3_role_inheritance where application_id = :applicationId)
                     + (select count(*) from rbac3_role_resource_grant where application_id = :applicationId)
                     + (select count(*) from rbac3_service_permission where application_id = :applicationId)
                     + (select count(*) from rbac3_service_principal where application_id = :applicationId)
                     + (select count(*) from rbac3_sod_set where application_id = :applicationId)
                     + (select count(*) from rbac3_user_active_role where application_id = :applicationId)
                """).setParameter("applicationId", application.getId())
                .setParameter("businessId", application.getDdcBusinessId()).getSingleResult();
        if (dependencies.longValue() != 0) {
            throw new IllegalStateException("RESOLVED_BOOTSTRAP_APPLICATION_IN_USE");
        }
        Number changedPermissions = (Number) entityManager.createNativeQuery("""
                select count(*) from rbac3_permission
                 where application_id = :applicationId and (
                     version <> 0 or status <> 'ACTIVE' or source_type <> 'MANUAL'
                     or permission_code not in (:seedPermissionCodes) or permission_name <> permission_code
                     or not (
                         (id > 8100000000000000000 and id < 8100000000000001000
                          and created_by = 'flyway-v10' and updated_by = 'flyway-v10'
                          and description = 'Built-in local platform capability')
                         or (id = 8110000000000000001 and permission_code = 'system:about:read'
                             and created_by = 'flyway-v12' and updated_by = 'flyway-v12'
                             and description = 'Read the current RBAC3 authorization context for the administration web client')
                     ))
                """).setParameter("applicationId", application.getId())
                .setParameter("seedPermissionCodes", initialPermissionCodes(index)).getSingleResult();
        if (changedPermissions.longValue() != 0) {
            throw new IllegalStateException("RESOLVED_BOOTSTRAP_INITIAL_PERMISSION_CHANGED");
        }
    }

    private static List<String> initialPermissionCodes(int index) {
        var permissions = new java.util.ArrayList<>(Rbac3DevelopmentTopology.applications().get(index).permissions().stream()
                .map(code -> switch (index) {
                    case 1 -> code.replace("tianquan-shoubing:", "idp:");
                    case 2 -> code.replace("yuheng:", "gateway:");
                    case 3 -> code.replace("TIANSHU_", "DDC_");
                    default -> code;
                }).toList());
        if (index == 0) { permissions.add("system:about:read"); }
        return List.copyOf(permissions);
    }

    private static boolean sameCatalogBinding(ApplicationPO application, PlatformCatalogBindingDTO binding) {
        return binding.getApplicationCode().equals(application.getApplicationCode())
                && binding.getDdcApplicationId().equals(application.getDdcApplicationId())
                && binding.getDdcBusinessId().equals(application.getDdcBusinessId());
    }

    private static int platformIndex(String code) {
        int index = CURRENT_PLATFORM_CODES.indexOf(code);
        if (index < 0) {
            index = LEGACY_PLATFORM_CODES.indexOf(code);
        }
        if (index < 0) {
            throw new IllegalArgumentException("RESOLVED_BOOTSTRAP_INVALID_PLATFORM");
        }
        return index;
    }

    private static ApplicationDefinitionVO platformDefinition(String code) {
        var definition = Rbac3DevelopmentTopology.applications().get(platformIndex(code));
        return new ApplicationDefinitionVO(code, definition.applicationName(), definition.roleCode(),
                definition.displayPriority(), definition.permissions());
    }

    /**
     * 方法 `acquireLock` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `acquire Lock` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `acquireLock` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `acquire Lock` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `acquireLock` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `acquireLock`, then continue the business flow using its result, exception, or side effect.
     */
    private void acquireLock() {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", BOOTSTRAP_LOCK_KEY)
                .getSingleResult();
    }

    /**
     * 方法 `findUser` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `find User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findUser` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `find User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findUser`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param normalizedUsername 输入参数 `normalizedUsername`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private UserPO findUser(Long tenantId, String identitySub) {
        return singleOrNull(entityManager.createQuery("""
                        select user from UserEntity user
                         where user.tenantId = :tenantId
                           and user.identitySub = :identitySub
                        """, UserPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("identitySub", identitySub)
                .getResultList(), "user");
    }

    /**
     * 方法 `ensureApplication` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `ensure Application` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `ensureApplication` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `ensure Application` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `ensureApplication` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `ensureApplication`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param definition 输入参数 `definition`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean ensureApplication(
            Long tenantId,
            Long userId,
            ApplicationDefinitionVO definition,
            Instant now) {
        ApplicationPO application = findApplication(
                tenantId, definition.applicationCode());
        boolean changed = false;
        if (application == null) {
            application = new ApplicationPO(
                    SnowflakeIdGenerator.nextLongId(), tenantId,
                    definition.applicationCode(), definition.applicationName(),
                    definition.displayPriority(), ACTOR, now);
            entityManager.persist(application);
            changed = true;
        }
        RolePO role = findRole(
                tenantId, application.getId(), definition.roleCode());
        if (role == null) {
            role = new RolePO(
                    SnowflakeIdGenerator.nextLongId(), tenantId, application.getId(),
                    definition.roleCode(), definition.applicationName() + " Administrator",
                    RoleTypeEnum.MANAGEMENT, RoleRiskLevelEnum.MEDIUM,
                    false, null, 0, null, ACTOR, now);
            entityManager.persist(role);
            entityManager.flush();
            insertSelfClosure(tenantId, application.getId(), role.getId());
            changed = true;
        }
        for (String permissionCode : definition.permissions()) {
            changed |= ensureResourceGrants(
                    tenantId, application.getId(), role.getId(), permissionCode, now);
        }
        if (definition.roleCode().endsWith("_LOCAL_ADMIN")) {
            changed |= ensureAllResourceGrants(
                    tenantId, application.getId(), role.getId(), now);
        }
        if (!hasAssignment(tenantId, userId, role.getId())) {
            entityManager.persist(new UserRoleAssignmentPO(
                    SnowflakeIdGenerator.nextLongId(), tenantId, userId, role.getId(),
                    UserRoleAssignmentTypeEnum.DIRECT, now, null,
                    "DEVELOPMENT", definition.applicationCode(),
                    "Unified identity local administrator", null, ACTOR, now));
            changed = true;
        }
        return changed;
    }

    /**
     * 方法 `findApplication` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `find Application` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findApplication` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `find Application` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findApplication` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findApplication`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ApplicationPO findApplication(Long tenantId, String applicationCode) {
        return singleOrNull(entityManager.createQuery("""
                        select application from ApplicationEntity application
                         where application.applicationCode = :applicationCode
                        """, ApplicationPO.class)
                .setParameter("applicationCode", applicationCode)
                .getResultList(), "application");
    }

    /**
     * 方法 `findRole` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `find Role` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findRole` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `find Role` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findRole` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findRole`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleCode 输入参数 `roleCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RolePO findRole(Long tenantId, Long applicationId, String roleCode) {
        return singleOrNull(entityManager.createQuery("""
                        select role from RoleEntity role
                         where role.tenantId = :tenantId
                           and role.applicationId = :applicationId
                           and role.roleCode = :roleCode
                        """, RolePO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationId", applicationId)
                .setParameter("roleCode", roleCode)
                .getResultList(), "role");
    }

    /**
     * 方法 `ensureResourceGrants` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `ensure Resource Grants` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `ensureResourceGrants` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `ensure Resource Grants` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `ensureResourceGrants` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `ensureResourceGrants`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean ensureResourceGrants(
            Long tenantId,
            Long applicationId,
            Long roleId,
            String permissionCode,
            Instant now) {
        PermissionPO permission = singleOrNull(entityManager.createQuery("""
                        select permission from PermissionEntity permission
                         where permission.permissionCode = :permissionCode
                        """, PermissionPO.class)
                .setParameter("permissionCode", permissionCode)
                .getResultList(), "permission");
        boolean changed = false;
        if (permission == null) {
            permission = new PermissionPO(
                    SnowflakeIdGenerator.nextLongId(), tenantId, applicationId,
                    permissionCode, permissionCode, risk(permissionCode),
                    "Unified identity local administrative capability", ACTOR, now);
            entityManager.persist(permission);
            changed = true;
        } else if (!permission.getApplicationId().equals(applicationId)) {
            throw new IllegalStateException(
                    "permission code is already owned by another application: "
                            + permissionCode);
        }
        List<ResourcePO> resources = entityManager.createQuery("""
                        select resource from ResourceEntity resource
                         where resource.applicationId = :applicationId
                           and resource.requiredPermissionId = :permissionId
                           and resource.status = :status
                        """, ResourcePO.class)
                .setParameter("applicationId", applicationId)
                .setParameter("permissionId", permission.getId())
                .setParameter("status", ResourceStatusEnum.ACTIVE)
                .getResultList();
        if (resources.isEmpty()) {
            throw new IllegalStateException(
                    "required mapped development resource is missing: " + permissionCode);
        }
        for (ResourcePO resource : resources) {
            if (!hasRoleResourceGrant(tenantId, roleId, resource.getId(), now)) {
                entityManager.persist(new RoleResourceGrantPO(
                        SnowflakeIdGenerator.nextLongId(), tenantId, applicationId,
                        roleId, resource.getId(), now, null, ACTOR, now));
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 为 local administrator role 补齐当前应用的全部 ACTIVE 资源。
     *
     * <p>Local resource bootstrap may add menu/page/action/API facts beyond the minimum
     * capability list. The administrator role is intentionally the only built-in role that
     * receives those newly registered resources; the mock entry role remains least-privilege.</p>
     *
     * @param tenantId 租户标识；tenant identifier
     * @param applicationId 应用标识；application identifier
     * @param roleId 角色标识；role identifier
     * @param now 当前数据库时间；current database time
     * @return 是否产生持久化变更；whether persistence changed
     */
    private boolean ensureAllResourceGrants(
            Long tenantId,
            Long applicationId,
            Long roleId,
            Instant now) {
        boolean changed = false;
        List<ResourcePO> resources = entityManager.createQuery("""
                        select resource from ResourceEntity resource
                         where resource.applicationId = :applicationId
                           and resource.status = :status
                         order by resource.id
                        """, ResourcePO.class)
                .setParameter("applicationId", applicationId)
                .setParameter("status", ResourceStatusEnum.ACTIVE)
                .getResultList();
        for (ResourcePO resource : resources) {
            if (!hasRoleResourceGrant(tenantId, roleId, resource.getId(), now)) {
                entityManager.persist(new RoleResourceGrantPO(
                        SnowflakeIdGenerator.nextLongId(), tenantId, applicationId,
                        roleId, resource.getId(), now, null, ACTOR, now));
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 方法 `hasRoleResourceGrant` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `has Role Resource Grant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `hasRoleResourceGrant` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `has Role Resource Grant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `hasRoleResourceGrant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `hasRoleResourceGrant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceId 输入参数 `resourceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定有效窗口；input value used to determine the effective window.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean hasRoleResourceGrant(
            Long tenantId,
            Long roleId,
            Long resourceId,
            Instant now) {
        Number count = (Number) entityManager.createQuery("""
                        select count(grant) from RoleResourceGrantEntity grant
                         where grant.tenantId = :tenantId
                           and grant.roleId = :roleId
                           and grant.resourceId = :resourceId
                           and grant.status = :status
                           and grant.validFrom <= :now
                           and (grant.validTo is null or grant.validTo > :now)
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("roleId", roleId)
                .setParameter("resourceId", resourceId)
                .setParameter("status", RoleResourceGrantStatusEnum.ACTIVE)
                .setParameter("now", now)
                .getSingleResult();
        return count.longValue() > 0;
    }

    /**
     * 方法 `hasAssignment` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `has Assignment` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `hasAssignment` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `has Assignment` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `hasAssignment` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `hasAssignment`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean hasAssignment(Long tenantId, Long userId, Long roleId) {
        Number count = (Number) entityManager.createQuery("""
                        select count(assignment) from UserRoleAssignmentEntity assignment
                         where assignment.tenantId = :tenantId
                           and assignment.userId = :userId
                           and assignment.roleId = :roleId
                           and assignment.status in :statuses
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("userId", userId)
                .setParameter("roleId", roleId)
                .setParameter("statuses", List.of(
                        UserRoleAssignmentStatusEnum.ACTIVE,
                        UserRoleAssignmentStatusEnum.PENDING))
                .getSingleResult();
        return count.longValue() > 0;
    }

    /**
     * 方法 `insertSelfClosure` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `insert Self Closure` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `insertSelfClosure` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `insert Self Closure` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `insertSelfClosure` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `insertSelfClosure`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void insertSelfClosure(Long tenantId, Long applicationId, Long roleId) {
        entityManager.createNativeQuery("""
                        insert into rbac3_role_closure (
                            tenant_id, application_id, ancestor_role_id,
                            descendant_role_id, depth
                        ) values (:tenantId, :applicationId, :roleId, :roleId, 0)
                        on conflict do nothing
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationId", applicationId)
                .setParameter("roleId", roleId)
                .executeUpdate();
    }

    /**
     * 方法 `risk` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `risk` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `risk` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `risk` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `risk` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `risk`, then continue the business flow using its result, exception, or side effect.
     *
     * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static PermissionRiskLevelEnum risk(String permissionCode) {
        if (permissionCode.endsWith(":read") || permissionCode.equals("TIANSHU_READ")) {
            return PermissionRiskLevelEnum.MEDIUM;
        }
        return permissionCode.endsWith(":admin")
                || permissionCode.endsWith(":manage")
                || permissionCode.endsWith(":activate")
                || permissionCode.endsWith(":revoke")
                ? PermissionRiskLevelEnum.CRITICAL
                : PermissionRiskLevelEnum.HIGH;
    }

    /**
     * 方法 `normalizeTenantId` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `normalizeTenantId` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `normalizeTenantId` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `normalizeTenantId` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `normalizeTenantId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `normalizeTenantId`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Long normalizeTenantId(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException("tenantId is invalid");
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0L) {
                throw new NumberFormatException("tenant id must be positive");
            }
            return parsed;
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("tenantId is invalid", invalid);
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    /**
     * 方法 `singleOrNull` 按照 `JpaDevelopmentTopologyBootstrapRepository` 的职责处理输入，完成 `single Or Null` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `singleOrNull` processes its inputs according to `JpaDevelopmentTopologyBootstrapRepository`'s responsibility, performs the `single Or Null` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `singleOrNull` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `singleOrNull`, then continue the business flow using its result, exception, or side effect.
     *
     * @param <T> 类型参数表示查询结果的具体类型；type parameter representing the query result type.
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static <T> T singleOrNull(List<T> values, String name) {
        if (values.size() > 1) {
            throw new IllegalStateException("duplicate development " + name);
        }
        return values.isEmpty() ? null : values.getFirst();
    }
}
