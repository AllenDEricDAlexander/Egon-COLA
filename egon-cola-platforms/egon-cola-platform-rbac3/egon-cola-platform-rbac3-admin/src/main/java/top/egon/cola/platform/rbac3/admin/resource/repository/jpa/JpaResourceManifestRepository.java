package top.egon.cola.platform.rbac3.admin.resource.repository.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationEventPublisher;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.ApplicationPO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.FieldDefinitionPO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.PermissionPO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.PermissionResourcePO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.ResourcePO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.ResourceManifestPO;
import top.egon.cola.platform.rbac3.contract.manifest.ManifestResource;
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import top.egon.cola.platform.rbac3.admin.resource.repository.internal.TypedResource;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ActivationMutation;
import top.egon.cola.platform.rbac3.admin.resource.repository.ApplicationResourceRepository;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ApplicationVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ResourceVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ManifestVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ManifestValidationVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ManifestImpactVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ArchiveResultVO;
import top.egon.cola.platform.rbac3.admin.resource.repository.ResourceManifestRepository;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.StoredManifestVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.FieldDefinitionDataTypeEnum;
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.FieldDefinitionSensitivityEnum;
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.FieldDefinitionDefaultAccessEnum;
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.PermissionStatusEnum;
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.ResourceTypeEnum;
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.ResourceStatusEnum;
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.ResourceManifestStatusEnum;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationEventVO;

/**
 * 类型 `JpaResourceManifestRepository` 位于当前包内，是类型，用于承载 `Resource Manifest Repository` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaResourceManifestRepository` is a type in its package and carries the responsibility, state, or contract for `Resource Manifest Repository`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `JpaResourceManifestRepository` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `JpaResourceManifestRepository` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Repository
public class JpaResourceManifestRepository implements
        ResourceManifestRepository,
        ApplicationResourceRepository {

    /**
     * 字段 `entityManager` 表示 `JpaResourceManifestRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaResourceManifestRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaResourceManifestRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaResourceManifestRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `objectMapper` 表示 `JpaResourceManifestRepository` 中与 `object Mapper` 相关的状态、依赖、配置或结果（声明类型 `ObjectMapper`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `objectMapper` stores the `object Mapper`-related state, dependency, configuration, or result of `JpaResourceManifestRepository` (declared type `ObjectMapper`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `objectMapper` 时应保持 `JpaResourceManifestRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `objectMapper`, preserve `JpaResourceManifestRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ObjectMapper objectMapper;
    /**
     * 字段 `idGenerator` 表示 `JpaResourceManifestRepository` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `JpaResourceManifestRepository` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `JpaResourceManifestRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `JpaResourceManifestRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `databaseClock` 表示 `JpaResourceManifestRepository` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `JpaResourceManifestRepository` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `JpaResourceManifestRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `JpaResourceManifestRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;
    /**
     * 字段 `eventPort` 表示 `JpaResourceManifestRepository` 中与 `event Port` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationEventPublisher`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `eventPort` stores the `event Port`-related state, dependency, configuration, or result of `JpaResourceManifestRepository` (declared type `AuthorizationEventPublisher`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `eventPort` 时应保持 `JpaResourceManifestRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `eventPort`, preserve `JpaResourceManifestRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationEventPublisher eventPort;

    /**
     * 构造器 `JpaResourceManifestRepository` 用于创建并初始化 `JpaResourceManifestRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaResourceManifestRepository` creates and initializes `JpaResourceManifestRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaResourceManifestRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaResourceManifestRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param eventPort 输入参数 `eventPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaResourceManifestRepository(
            EntityManager entityManager,
            ObjectMapper objectMapper,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock,
            AuthorizationEventPublisher eventPort) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
        this.eventPort = eventPort;
    }

    /**
     * 方法 `findByBuild` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `find By Build` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findByBuild` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `find By Build` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findByBuild` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findByBuild`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param artifactVersion 输入参数 `artifactVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param buildId 输入参数 `buildId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<StoredManifestVO> findByBuild(
            String tenantId,
            String applicationId,
            String artifactVersion,
            String buildId) {
        return entityManager.createQuery("""
                        select m from ResourceManifestEntity m
                         where m.tenantId = :tenantId
                           and m.applicationId = :applicationId
                           and m.artifactVersion = :artifactVersion
                           and m.buildId = :buildId
                        """, ResourceManifestPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", Long.valueOf(applicationId))
                .setParameter("artifactVersion", artifactVersion)
                .setParameter("buildId", buildId)
                .getResultStream()
                .findFirst()
                .map(this::toStoredManifest);
    }

    /**
     * 方法 `insert` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `insert` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `insert` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `insert` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `insert` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `insert`, then continue the business flow using its result, exception, or side effect.
     *
     * @param manifest 输入参数 `manifest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void insert(StoredManifestVO manifest) {
        Instant now = databaseClock.transactionNow();
        Map<String, Object> payload = objectMapper.convertValue(
                manifest.manifest(), new TypeReference<>() { });
        ResourceManifestPO entity = new ResourceManifestPO(
                Long.valueOf(manifest.manifestId()),
                Long.valueOf(manifest.tenantId()),
                Long.valueOf(manifest.applicationId()),
                Integer.parseInt(manifest.manifest().schemaVersion()),
                manifest.artifactVersion(),
                manifest.buildId(),
                manifest.manifestVersion(),
                manifest.checksum(),
                manifest.definitionSetId(),
                payload,
                "gateway-report",
                now);
        entityManager.persist(entity);
        materializeResources(manifest, now);
    }

    /**
     * 方法 `activate` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `activate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activate` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `activate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedApplicationVersion 输入参数 `expectedApplicationVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedCurrentManifestVersion 输入参数 `expectedCurrentManifestVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedDefinitionSetId 输入参数 `expectedDefinitionSetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reason 输入参数 `reason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ignoredNow 输入参数 `ignoredNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public ActivationMutation activate(
            String tenantId,
            String applicationId,
            String manifestId,
            long expectedApplicationVersion,
            long expectedCurrentManifestVersion,
            String expectedDefinitionSetId,
            String actorId,
            String idempotencyKey,
            String reason,
            Instant ignoredNow) {
        Instant now = databaseClock.transactionNow();
        ApplicationPO application = entityManager.find(
                ApplicationPO.class,
                Long.valueOf(applicationId),
                LockModeType.PESSIMISTIC_WRITE);
        ResourceManifestPO manifest = entityManager.find(
                ResourceManifestPO.class,
                Long.valueOf(manifestId),
                LockModeType.PESSIMISTIC_WRITE);
        TenantPO tenant = entityManager.find(
                TenantPO.class,
                Long.valueOf(tenantId),
                LockModeType.PESSIMISTIC_WRITE);
        if (application == null || manifest == null || tenant == null
                || !application.getTenantId().equals(Long.valueOf(tenantId))
                || !manifest.getTenantId().equals(Long.valueOf(tenantId))
                || !manifest.getApplicationId().equals(Long.valueOf(applicationId))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        if (application.getVersion() != expectedApplicationVersion) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        long currentManifestVersion = application.getCurrentManifestVersion() == null
                ? 0L : application.getCurrentManifestVersion();
        if (currentManifestVersion != expectedCurrentManifestVersion) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        if (!manifest.getDefinitionSetId().equals(expectedDefinitionSetId)) {
            throw new Rbac3RuleViolation("RESOURCE_MANIFEST_CONFLICT");
        }
        entityManager.createQuery("""
                        select m from ResourceManifestEntity m
                         where m.tenantId = :tenantId
                           and m.applicationId = :applicationId
                           and m.status = :status
                        """, ResourceManifestPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", Long.valueOf(applicationId))
                .setParameter("status", ResourceManifestStatusEnum.ACTIVE)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList()
                .forEach(current -> current.supersede(actorId, now));
        manifest.activate(actorId, now);
        application.activateManifest(
                manifest.getId(), manifest.getManifestVersion(), actorId, now);
        tenant.incrementPolicyVersion(actorId, now);
        activateResourceProjection(
                Long.valueOf(tenantId), Long.valueOf(applicationId), manifest.getId(), actorId, now);
        String propagationId = eventPort.enqueue(new AuthorizationEventVO(
                tenantId,
                "RESOURCE_MANIFEST",
                manifestId,
                "RESOURCE_MANIFEST_ACTIVATED",
                Map.of(
                        "applicationId", applicationId,
                        "manifestVersion", Long.toString(manifest.getManifestVersion()),
                        "reason", reason),
                idempotencyKey));
        return new ActivationMutation(tenant.getPolicyVersion(), propagationId, true);
    }

    /**
     * 方法 `applications` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `applications` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `applications` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `applications` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `applications` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `applications`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ApplicationVO> applications(String tenantId) {
        return entityManager.createQuery("""
                        select a from ApplicationEntity a
                         where a.tenantId = :tenantId
                         order by a.displayPriority, a.applicationCode
                        """, ApplicationPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .getResultList()
                .stream()
                .map(application -> new ApplicationVO(
                        application.getId().toString(),
                        application.getApplicationCode(),
                        application.getApplicationName(),
                        application.getStatus().name(),
                        application.getVersion()))
                .toList();
    }

    /**
     * 方法 `resources` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `resources` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resources` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `resources` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `resources` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resources`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ResourceVO> resources(
            String tenantId,
            String applicationId) {
        return entityManager.createQuery("""
                        select r from ResourceEntity r
                         where r.tenantId = :tenantId and r.applicationId = :applicationId
                         order by r.resourceType, r.resourceCode
                        """, ResourcePO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", Long.valueOf(applicationId))
                .getResultList()
                .stream()
                .map(this::toResourceView)
                .toList();
    }

    /**
     * 方法 `manifest` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `manifest` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `manifest` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `manifest` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `manifest` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `manifest`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public ManifestVO manifest(
            String tenantId,
            String manifestId) {
        ResourceManifestPO manifest = requireManifest(tenantId, manifestId);
        return new ManifestVO(
                manifest.getId().toString(),
                manifest.getApplicationId().toString(),
                manifest.getStatus().name(),
                manifest.getChecksum(),
                manifest.getManifestVersion());
    }

    /**
     * 方法 `validation` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `validation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validation` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `validation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validation` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validation`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public ManifestValidationVO validation(
            String tenantId,
            String manifestId) {
        ResourceManifestPO manifest = requireManifest(tenantId, manifestId);
        Map<String, Object> validation = manifest.getValidationResult();
        return new ManifestValidationVO(
                manifestId,
                !Boolean.FALSE.equals(validation.get("valid")),
                strings(validation.get("errors")),
                strings(validation.get("warnings")));
    }

    /**
     * 方法 `impact` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `impact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `impact` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `impact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `impact` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `impact`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public ManifestImpactVO impact(
            String tenantId,
            String manifestId) {
        ResourceManifestPO manifest = requireManifest(tenantId, manifestId);
        long pending = entityManager.createQuery("""
                        select count(r) from ResourceEntity r
                         where r.tenantId = :tenantId
                           and r.applicationId = :applicationId
                           and r.sourceManifestId = :manifestId
                        """, Long.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", manifest.getApplicationId())
                .setParameter("manifestId", manifest.getId())
                .getSingleResult();
        long stale = entityManager.createQuery("""
                        select count(r) from ResourceEntity r
                         where r.tenantId = :tenantId
                           and r.applicationId = :applicationId
                           and r.status = :status
                        """, Long.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", manifest.getApplicationId())
                .setParameter("status", ResourceStatusEnum.STALE)
                .getSingleResult();
        return new ManifestImpactVO(
                manifestId, pending, 0L, stale, 0L, List.of());
    }

    /**
     * 方法 `archive` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `archive` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `archive` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `archive` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `archive` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `archive`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceId 输入参数 `resourceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ignoredNow 输入参数 `ignoredNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public ArchiveResultVO archive(
            String tenantId,
            String resourceId,
            long expectedVersion,
            String actorId,
            Instant ignoredNow) {
        Instant now = databaseClock.transactionNow();
        ResourcePO resource = entityManager.find(
                ResourcePO.class, Long.valueOf(resourceId), LockModeType.PESSIMISTIC_WRITE);
        TenantPO tenant = entityManager.find(
                TenantPO.class, Long.valueOf(tenantId), LockModeType.PESSIMISTIC_WRITE);
        if (resource == null || tenant == null
                || !resource.getTenantId().equals(Long.valueOf(tenantId))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        if (resource.getVersion() != expectedVersion) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        resource.archive(actorId, now);
        tenant.incrementPolicyVersion(actorId, now);
        eventPort.enqueue(new AuthorizationEventVO(
                tenantId,
                "RESOURCE",
                resourceId,
                "RESOURCE_ARCHIVED",
                Map.of("policyVersion", Long.toString(tenant.getPolicyVersion())),
                "resource-archive-" + resourceId));
        return new ArchiveResultVO(
                resourceId, resource.getStatus().name(), tenant.getPolicyVersion());
    }

    /**
     * 方法 `materializeResources` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `materialize Resources` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `materializeResources` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `materialize Resources` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `materializeResources` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `materializeResources`, then continue the business flow using its result, exception, or side effect.
     *
     * @param stored 输入参数 `stored`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void materializeResources(StoredManifestVO stored, Instant now) {
        Map<String, Long> resourceIds = new LinkedHashMap<>();
        List<TypedResource> resources = resources(stored.manifest());
        resources.forEach(resource -> resourceIds.put(
                resource.type() + ":" + resource.value().code(), idGenerator.nextLongId()));
        for (TypedResource typed : resources) {
            ManifestResource source = typed.value();
            Long parentId = findParentId(resourceIds, source.parentCode());
            Long permissionId = requiredPermissionId(
                    stored.tenantId(), stored.applicationId(), source);
            Map<String, Object> mechanical = new LinkedHashMap<>();
            putIfPresent(mechanical, "path", source.path());
            putIfPresent(mechanical, "routeCode", source.routeCode());
            putIfPresent(mechanical, "gatewayOperationId", source.gatewayOperationId());
            putIfPresent(mechanical, "httpMethod", source.httpMethod());
            putIfPresent(mechanical, "pathPattern", source.pathPattern());
            Long resourceId = resourceIds.get(typed.type() + ":" + source.code());
            ResourcePO entity = new ResourcePO(
                    resourceId,
                    Long.valueOf(stored.tenantId()),
                    Long.valueOf(stored.applicationId()),
                    typed.type(),
                    source.code(),
                    source.name() == null ? source.code() : source.name(),
                    parentId,
                    permissionId,
                    Long.valueOf(stored.manifestId()),
                    stored.buildId(),
                    mechanical,
                    Map.copyOf(source.metadata()),
                    "gateway-report",
                    now);
            entityManager.persist(entity);
            if (permissionId != null) {
                entityManager.persist(new PermissionResourcePO(
                        idGenerator.nextLongId(),
                        Long.valueOf(stored.tenantId()),
                        Long.valueOf(stored.applicationId()),
                        permissionId,
                        resourceId,
                        typed.type(),
                        typed.type() == ResourceTypeEnum.API
                                ? stored.definitionSetId() : null,
                        typed.type() == ResourceTypeEnum.API
                                ? source.gatewayOperationId() : null,
                        source.metadata().get("securityPolicyId"),
                        stored.manifestVersion(),
                        "gateway-report",
                        now));
            }
        }
        materializeFieldDefinitions(stored, resourceIds, now);
    }

    /**
     * 方法 `materializeFieldDefinitions` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `materialize Field Definitions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `materializeFieldDefinitions` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `materialize Field Definitions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `materializeFieldDefinitions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `materializeFieldDefinitions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param stored 输入参数 `stored`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceIds 输入参数 `resourceIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void materializeFieldDefinitions(
            StoredManifestVO stored,
            Map<String, Long> resourceIds,
            Instant now) {
        for (ResourceManifest.FieldDefinition field : stored.manifest().fieldDefinitions()) {
            Long resourceId = findParentId(resourceIds, field.resourceCode());
            entityManager.persist(new FieldDefinitionPO(
                    idGenerator.nextLongId(),
                    Long.valueOf(stored.tenantId()),
                    Long.valueOf(stored.applicationId()),
                    resourceId,
                    field.fieldCode(),
                    field.jsonPath(),
                    FieldDefinitionDataTypeEnum.valueOf(field.dataType()),
                    FieldDefinitionSensitivityEnum.valueOf(field.sensitivity()),
                    FieldDefinitionDefaultAccessEnum.valueOf(field.defaultAccess()),
                    field.maskingStrategy(),
                    field.writable(),
                    field.exportable(),
                    Long.valueOf(stored.manifestId()),
                    "gateway-report",
                    now));
        }
    }

    /**
     * 方法 `activateResourceProjection` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `activate Resource Projection` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activateResourceProjection` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `activate Resource Projection` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activateResourceProjection` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activateResourceProjection`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void activateResourceProjection(
            Long tenantId,
            Long applicationId,
            Long manifestId,
            String actorId,
            Instant now) {
        List<ResourcePO> resources = entityManager.createQuery("""
                        select r from ResourceEntity r
                         where r.tenantId = :tenantId and r.applicationId = :applicationId
                        """, ResourcePO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationId", applicationId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        for (ResourcePO resource : resources) {
            if (manifestId.equals(resource.getSourceManifestId())) {
                resource.activate(actorId, now);
            } else {
                resource.markStale(actorId, now);
            }
        }
    }

    /**
     * 方法 `toStoredManifest` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `to Stored Manifest` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toStoredManifest` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `to Stored Manifest` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toStoredManifest` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toStoredManifest`, then continue the business flow using its result, exception, or side effect.
     *
     * @param entity 输入参数 `entity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private StoredManifestVO toStoredManifest(ResourceManifestPO entity) {
        ResourceManifest manifest = objectMapper.convertValue(
                entity.getPayload(), ResourceManifest.class);
        return new StoredManifestVO(
                entity.getTenantId().toString(),
                entity.getApplicationId().toString(),
                entity.getId().toString(),
                entity.getDefinitionSetId(),
                entity.getArtifactVersion(),
                entity.getBuildId(),
                entity.getManifestVersion(),
                entity.getChecksum(),
                manifest);
    }

    /**
     * 方法 `requireManifest` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `require Manifest` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireManifest` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `require Manifest` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireManifest` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireManifest`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ResourceManifestPO requireManifest(String tenantId, String manifestId) {
        ResourceManifestPO manifest = entityManager.find(
                ResourceManifestPO.class, Long.valueOf(manifestId));
        if (manifest == null || !manifest.getTenantId().equals(Long.valueOf(tenantId))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return manifest;
    }

    /**
     * 方法 `toResourceView` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `to Resource View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toResourceView` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `to Resource View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toResourceView` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toResourceView`, then continue the business flow using its result, exception, or side effect.
     *
     * @param resource 输入参数 `resource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ResourceVO toResourceView(ResourcePO resource) {
        return new ResourceVO(
                resource.getId().toString(),
                resource.getApplicationId().toString(),
                resource.getResourceType().name(),
                resource.getResourceCode(),
                resource.getResourceName(),
                value(resource.getParentResourceId()),
                value(resource.getRequiredPermissionId()),
                resource.getStatus().name(),
                resource.getVersion());
    }

    /**
     * 方法 `requiredPermissionId` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `required Permission Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requiredPermissionId` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `required Permission Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requiredPermissionId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requiredPermissionId`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resource 输入参数 `resource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Long requiredPermissionId(
            String tenantId,
            String applicationId,
            ManifestResource resource) {
        if (resource.requiredPermissionCode() == null) {
            return null;
        }
        return entityManager.createQuery("""
                        select p from PermissionEntity p
                         where p.tenantId = :tenantId
                           and p.applicationId = :applicationId
                           and p.permissionCode = :permissionCode
                           and p.status <> :archived
                        """, PermissionPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", Long.valueOf(applicationId))
                .setParameter("permissionCode", resource.requiredPermissionCode())
                .setParameter("archived", PermissionStatusEnum.ARCHIVED)
                .getResultStream()
                .map(PermissionPO::getId)
                .findFirst()
                .orElseThrow(() -> new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID"));
    }

    /**
     * 方法 `strings` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `strings` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `strings` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `strings` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `strings` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `strings`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().map(String::valueOf).toList();
    }

    /**
     * 方法 `value` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `value` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `value` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `value` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `value` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `value`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String value(Long value) {
        return value == null ? null : value.toString();
    }

    /**
     * 方法 `resources` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `resources` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resources` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `resources` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `resources` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resources`, then continue the business flow using its result, exception, or side effect.
     *
     * @param manifest 输入参数 `manifest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static List<TypedResource> resources(ResourceManifest manifest) {
        List<TypedResource> result = new ArrayList<>();
        add(result, ResourceTypeEnum.APP, manifest.apps());
        add(result, ResourceTypeEnum.MENU, manifest.menus());
        add(result, ResourceTypeEnum.ROUTE, manifest.routes());
        add(result, ResourceTypeEnum.ACTION, manifest.actions());
        add(result, ResourceTypeEnum.API, manifest.apis());
        return result;
    }

    /**
     * 方法 `add` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `add` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `add` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `add` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `add` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `add`, then continue the business flow using its result, exception, or side effect.
     *
     * @param target 输入参数 `target`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private static void add(
            List<TypedResource> target,
            ResourceTypeEnum type,
            List<ManifestResource> values) {
        values.forEach(value -> target.add(new TypedResource(type, value)));
    }

    /**
     * 方法 `findParentId` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `find Parent Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findParentId` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `find Parent Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findParentId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findParentId`, then continue the business flow using its result, exception, or side effect.
     *
     * @param ids 输入参数 `ids`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param parentCode 输入参数 `parentCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Long findParentId(Map<String, Long> ids, String parentCode) {
        if (parentCode == null) {
            return null;
        }
        return ids.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith(":" + parentCode))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID"));
    }

    /**
     * 方法 `putIfPresent` 按照 `JpaResourceManifestRepository` 的职责处理输入，完成 `put If Present` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `putIfPresent` processes its inputs according to `JpaResourceManifestRepository`'s responsibility, performs the `put If Present` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `putIfPresent` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `putIfPresent`, then continue the business flow using its result, exception, or side effect.
     *
     * @param target 输入参数 `target`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    }
