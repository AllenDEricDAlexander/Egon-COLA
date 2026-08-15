package top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.enums.ResourceManifestStatusEnum;

/**
 * 类型 `ResourceManifestPO` 位于当前包内，是类型，用于承载 `Resource Manifest Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ResourceManifestPO` is a type in its package and carries the responsibility, state, or contract for `Resource Manifest Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ResourceManifestPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ResourceManifestPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "ResourceManifestEntity")
@Table(name = "rbac3_resource_manifest")
public class ResourceManifestPO extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `ResourceManifestPO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `ResourceManifestPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `ResourceManifestPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `ResourceManifestPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `applicationId` 表示 `ResourceManifestPO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `ResourceManifestPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `ResourceManifestPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `ResourceManifestPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /**
     * 字段 `schemaVersion` 表示 `ResourceManifestPO` 中与 `schema Version` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `schemaVersion` stores the `schema Version`-related state, dependency, configuration, or result of `ResourceManifestPO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `schemaVersion` 时应保持 `ResourceManifestPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `schemaVersion`, preserve `ResourceManifestPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    /**
     * 字段 `artifactVersion` 表示 `ResourceManifestPO` 中与 `artifact Version` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `artifactVersion` stores the `artifact Version`-related state, dependency, configuration, or result of `ResourceManifestPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `artifactVersion` 时应保持 `ResourceManifestPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `artifactVersion`, preserve `ResourceManifestPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "artifact_version", nullable = false, length = 128)
    private String artifactVersion;

    /**
     * 字段 `buildId` 表示 `ResourceManifestPO` 中与 `build Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `buildId` stores the `build Id`-related state, dependency, configuration, or result of `ResourceManifestPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `buildId` 时应保持 `ResourceManifestPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `buildId`, preserve `ResourceManifestPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "build_id", nullable = false, length = 256)
    private String buildId;

    /**
     * 字段 `manifestVersion` 表示 `ResourceManifestPO` 中与 `manifest Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `manifestVersion` stores the `manifest Version`-related state, dependency, configuration, or result of `ResourceManifestPO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `manifestVersion` 时应保持 `ResourceManifestPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `manifestVersion`, preserve `ResourceManifestPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "manifest_version", nullable = false)
    private long manifestVersion;

    /**
     * 字段 `checksum` 表示 `ResourceManifestPO` 中与 `checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `checksum` stores the `checksum`-related state, dependency, configuration, or result of `ResourceManifestPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `checksum` 时应保持 `ResourceManifestPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `checksum`, preserve `ResourceManifestPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, length = 128)
    private String checksum;

    /**
     * 字段 `status` 表示 `ResourceManifestPO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `ResourceManifestStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `ResourceManifestPO` (declared type `ResourceManifestStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `ResourceManifestPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `ResourceManifestPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ResourceManifestStatusEnum status;

    /**
     * 字段 `definitionSetId` 表示 `ResourceManifestPO` 中与 `definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `definitionSetId` stores the `definition Set Id`-related state, dependency, configuration, or result of `ResourceManifestPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `definitionSetId` 时应保持 `ResourceManifestPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `definitionSetId`, preserve `ResourceManifestPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "definition_set_id", length = 64)
    private String definitionSetId;

    /**
     * 字段 `payload` 表示 `ResourceManifestPO` 中与 `payload` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `payload` stores the `payload`-related state, dependency, configuration, or result of `ResourceManifestPO` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `payload` 时应保持 `ResourceManifestPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `payload`, preserve `ResourceManifestPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    /**
     * 字段 `validationResult` 表示 `ResourceManifestPO` 中与 `validation Result` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validationResult` stores the `validation Result`-related state, dependency, configuration, or result of `ResourceManifestPO` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validationResult` 时应保持 `ResourceManifestPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validationResult`, preserve `ResourceManifestPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_result", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> validationResult = new LinkedHashMap<>();

    /**
     * 字段 `receivedAt` 表示 `ResourceManifestPO` 中与 `received At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `receivedAt` stores the `received At`-related state, dependency, configuration, or result of `ResourceManifestPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `receivedAt` 时应保持 `ResourceManifestPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `receivedAt`, preserve `ResourceManifestPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    /**
     * 字段 `activatedAt` 表示 `ResourceManifestPO` 中与 `activated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `activatedAt` stores the `activated At`-related state, dependency, configuration, or result of `ResourceManifestPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `activatedAt` 时应保持 `ResourceManifestPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `activatedAt`, preserve `ResourceManifestPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "activated_at")
    private Instant activatedAt;

    /**
     * 构造器 `ResourceManifestPO` 用于创建并初始化 `ResourceManifestPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ResourceManifestPO` creates and initializes `ResourceManifestPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ResourceManifestPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ResourceManifestPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ResourceManifestPO() {
    }

    /**
     * 构造器 `ResourceManifestPO` 用于创建并初始化 `ResourceManifestPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ResourceManifestPO` creates and initializes `ResourceManifestPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ResourceManifestPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ResourceManifestPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param schemaVersion 输入参数 `schemaVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param artifactVersion 输入参数 `artifactVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param buildId 输入参数 `buildId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param manifestVersion 输入参数 `manifestVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param checksum 输入参数 `checksum`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param definitionSetId 输入参数 `definitionSetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param payload 输入参数 `payload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ResourceManifestPO(
            Long id,
            Long tenantId,
            Long applicationId,
            int schemaVersion,
            String artifactVersion,
            String buildId,
            long manifestVersion,
            String checksum,
            String definitionSetId,
            Map<String, Object> payload,
            String actorId,
            Instant now) {
        if (schemaVersion < 1 || manifestVersion < 0) {
            throw new IllegalArgumentException("manifest versions are invalid");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.schemaVersion = schemaVersion;
        this.artifactVersion = required(artifactVersion, "artifactVersion");
        this.buildId = required(buildId, "buildId");
        this.manifestVersion = manifestVersion;
        this.checksum = required(checksum, "checksum");
        this.status = ResourceManifestStatusEnum.PENDING_VALIDATION;
        this.definitionSetId = required(definitionSetId, "definitionSetId");
        this.payload = Map.copyOf(Objects.requireNonNull(payload, "payload"));
        this.receivedAt = Objects.requireNonNull(now, "now");
        markCreated(actorId, now);
    }

    /**
     * 方法 `activate` 按照 `ResourceManifestPO` 的职责处理输入，完成 `activate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activate` processes its inputs according to `ResourceManifestPO`'s responsibility, performs the `activate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void activate(String actorId, Instant now) {
        if (status != ResourceManifestStatusEnum.PENDING_VALIDATION) {
            throw new IllegalStateException("only pending manifest can be activated");
        }
        status = ResourceManifestStatusEnum.ACTIVE;
        activatedAt = now;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `supersede` 按照 `ResourceManifestPO` 的职责处理输入，完成 `supersede` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `supersede` processes its inputs according to `ResourceManifestPO`'s responsibility, performs the `supersede` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `supersede` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `supersede`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void supersede(String actorId, Instant now) {
        if (status == ResourceManifestStatusEnum.ACTIVE) {
            status = ResourceManifestStatusEnum.SUPERSEDED;
            markUpdated(actorId, now);
        }
    }

    /**
     * 方法 `getId` 按照 `ResourceManifestPO` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `ResourceManifestPO`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getId() {
        return id;
    }

    /**
     * 方法 `getApplicationId` 按照 `ResourceManifestPO` 的职责处理输入，完成 `get Application Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getApplicationId` processes its inputs according to `ResourceManifestPO`'s responsibility, performs the `get Application Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getApplicationId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getApplicationId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getApplicationId() {
        return applicationId;
    }

    /**
     * 方法 `getArtifactVersion` 按照 `ResourceManifestPO` 的职责处理输入，完成 `get Artifact Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getArtifactVersion` processes its inputs according to `ResourceManifestPO`'s responsibility, performs the `get Artifact Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getArtifactVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getArtifactVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getArtifactVersion() {
        return artifactVersion;
    }

    /**
     * 方法 `getBuildId` 按照 `ResourceManifestPO` 的职责处理输入，完成 `get Build Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getBuildId` processes its inputs according to `ResourceManifestPO`'s responsibility, performs the `get Build Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getBuildId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getBuildId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getBuildId() {
        return buildId;
    }

    /**
     * 方法 `getManifestVersion` 按照 `ResourceManifestPO` 的职责处理输入，完成 `get Manifest Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getManifestVersion` processes its inputs according to `ResourceManifestPO`'s responsibility, performs the `get Manifest Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getManifestVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getManifestVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public long getManifestVersion() {
        return manifestVersion;
    }

    /**
     * 方法 `getChecksum` 按照 `ResourceManifestPO` 的职责处理输入，完成 `get Checksum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getChecksum` processes its inputs according to `ResourceManifestPO`'s responsibility, performs the `get Checksum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getChecksum` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getChecksum`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * 方法 `getDefinitionSetId` 按照 `ResourceManifestPO` 的职责处理输入，完成 `get Definition Set Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getDefinitionSetId` processes its inputs according to `ResourceManifestPO`'s responsibility, performs the `get Definition Set Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getDefinitionSetId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getDefinitionSetId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getDefinitionSetId() {
        return definitionSetId;
    }

    /**
     * 方法 `getPayload` 按照 `ResourceManifestPO` 的职责处理输入，完成 `get Payload` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPayload` processes its inputs according to `ResourceManifestPO`'s responsibility, performs the `get Payload` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPayload` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPayload`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Map<String, Object> getPayload() {
        return Map.copyOf(payload);
    }

    /**
     * 方法 `getValidationResult` 按照 `ResourceManifestPO` 的职责处理输入，完成 `get Validation Result` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getValidationResult` processes its inputs according to `ResourceManifestPO`'s responsibility, performs the `get Validation Result` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getValidationResult` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getValidationResult`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Map<String, Object> getValidationResult() {
        return Map.copyOf(validationResult);
    }

    /**
     * 方法 `getStatus` 按照 `ResourceManifestPO` 的职责处理输入，完成 `get ResourceManifestStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `ResourceManifestPO`'s responsibility, performs the `get ResourceManifestStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ResourceManifestStatusEnum getStatus() {
        return status;
    }

    /**
     * 方法 `required` 按照 `ResourceManifestPO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `ResourceManifestPO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fieldName 输入参数 `fieldName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    }
