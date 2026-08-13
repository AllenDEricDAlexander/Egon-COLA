package top.egon.cola.platform.rbac3.admin.directory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;
import java.util.Objects;

/**
 * 类型 `OrgUnitEntity` 位于当前包内，是类型，用于承载 `Org Unit Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `OrgUnitEntity` is a type in its package and carries the responsibility, state, or contract for `Org Unit Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `OrgUnitEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `OrgUnitEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_org_unit")
public class OrgUnitEntity extends TenantScopedEntity {

    /**
     * 字段 `MAX_DEPTH` 表示 `OrgUnitEntity` 中与 `MAX DEPTH` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `MAX_DEPTH` stores the `MAX DEPTH`-related state, dependency, configuration, or result of `OrgUnitEntity` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `MAX_DEPTH` 时应保持 `OrgUnitEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `MAX_DEPTH`, preserve `OrgUnitEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final int MAX_DEPTH = 20;

    /**
     * 字段 `id` 表示 `OrgUnitEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `OrgUnitEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `OrgUnitEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `OrgUnitEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `snapshotId` 表示 `OrgUnitEntity` 中与 `snapshot Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `snapshotId` stores the `snapshot Id`-related state, dependency, configuration, or result of `OrgUnitEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `snapshotId` 时应保持 `OrgUnitEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `snapshotId`, preserve `OrgUnitEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    /**
     * 字段 `unitType` 表示 `OrgUnitEntity` 中与 `unit Type` 相关的状态、依赖、配置或结果（声明类型 `UnitType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `unitType` stores the `unit Type`-related state, dependency, configuration, or result of `OrgUnitEntity` (declared type `UnitType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `unitType` 时应保持 `OrgUnitEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `unitType`, preserve `OrgUnitEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false, length = 32)
    private UnitType unitType;

    /**
     * 字段 `code` 表示 `OrgUnitEntity` 中与 `code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `code` stores the `code`-related state, dependency, configuration, or result of `OrgUnitEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `code` 时应保持 `OrgUnitEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `code`, preserve `OrgUnitEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, length = 128)
    private String code;

    /**
     * 字段 `name` 表示 `OrgUnitEntity` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `name` stores the `name`-related state, dependency, configuration, or result of `OrgUnitEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `name` 时应保持 `OrgUnitEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `name`, preserve `OrgUnitEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 字段 `parentId` 表示 `OrgUnitEntity` 中与 `parent Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `parentId` stores the `parent Id`-related state, dependency, configuration, or result of `OrgUnitEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `parentId` 时应保持 `OrgUnitEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `parentId`, preserve `OrgUnitEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 字段 `path` 表示 `OrgUnitEntity` 中与 `path` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `path` stores the `path`-related state, dependency, configuration, or result of `OrgUnitEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `path` 时应保持 `OrgUnitEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `path`, preserve `OrgUnitEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, columnDefinition = "text")
    private String path;

    /**
     * 字段 `depth` 表示 `OrgUnitEntity` 中与 `depth` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `depth` stores the `depth`-related state, dependency, configuration, or result of `OrgUnitEntity` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `depth` 时应保持 `OrgUnitEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `depth`, preserve `OrgUnitEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false)
    private int depth;

    /**
     * 字段 `status` 表示 `OrgUnitEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `OrgUnitEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `OrgUnitEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `OrgUnitEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    /**
     * 字段 `externalId` 表示 `OrgUnitEntity` 中与 `external Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `externalId` stores the `external Id`-related state, dependency, configuration, or result of `OrgUnitEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `externalId` 时应保持 `OrgUnitEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `externalId`, preserve `OrgUnitEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "external_id", length = 256)
    private String externalId;

    /**
     * 字段 `validFrom` 表示 `OrgUnitEntity` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `OrgUnitEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `OrgUnitEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `OrgUnitEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    /**
     * 字段 `validTo` 表示 `OrgUnitEntity` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `OrgUnitEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validTo` 时应保持 `OrgUnitEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validTo`, preserve `OrgUnitEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_to")
    private Instant validTo;

    /**
     * 构造器 `OrgUnitEntity` 用于创建并初始化 `OrgUnitEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `OrgUnitEntity` creates and initializes `OrgUnitEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `OrgUnitEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `OrgUnitEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected OrgUnitEntity() {
    }

    /**
     * 构造器 `OrgUnitEntity` 用于创建并初始化 `OrgUnitEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `OrgUnitEntity` creates and initializes `OrgUnitEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `OrgUnitEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `OrgUnitEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param unitType 输入参数 `unitType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param code 输入参数 `code`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param parentId 输入参数 `parentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param path 输入参数 `path`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param depth 输入参数 `depth`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public OrgUnitEntity(
            Long id,
            Long tenantId,
            Long snapshotId,
            UnitType unitType,
            String code,
            String name,
            Long parentId,
            String path,
            int depth,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        this(id, tenantId, snapshotId, unitType, code, name, parentId, path, depth,
                null, validFrom, validTo, actorId, now);
    }

    /**
     * 构造器 `OrgUnitEntity` 用于创建并初始化 `OrgUnitEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `OrgUnitEntity` creates and initializes `OrgUnitEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `OrgUnitEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `OrgUnitEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param unitType 输入参数 `unitType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param code 输入参数 `code`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param parentId 输入参数 `parentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param path 输入参数 `path`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param depth 输入参数 `depth`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param externalId 输入参数 `externalId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public OrgUnitEntity(
            Long id,
            Long tenantId,
            Long snapshotId,
            UnitType unitType,
            String code,
            String name,
            Long parentId,
            String path,
            int depth,
            String externalId,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (depth < 0 || depth > MAX_DEPTH) {
            throw new IllegalArgumentException("depth must be between 0 and 20");
        }
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.snapshotId = Objects.requireNonNull(snapshotId, "snapshotId");
        this.unitType = Objects.requireNonNull(unitType, "unitType");
        this.code = required(code, "code");
        this.name = required(name, "name");
        this.parentId = parentId;
        this.path = required(path, "path");
        this.depth = depth;
        this.status = Status.ACTIVE;
        this.externalId = externalId;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        markCreated(actorId, now);
    }

    /**
     * 方法 `applySnapshot` 按照 `OrgUnitEntity` 的职责处理输入，完成 `apply Snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `applySnapshot` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `apply Snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `applySnapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `applySnapshot`, then continue the business flow using its result, exception, or side effect.
     *
     * @param nextSnapshotId 输入参数 `nextSnapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextType 输入参数 `nextType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextName 输入参数 `nextName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextParentId 输入参数 `nextParentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextPath 输入参数 `nextPath`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextDepth 输入参数 `nextDepth`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextExternalId 输入参数 `nextExternalId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextValidFrom 输入参数 `nextValidFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextValidTo 输入参数 `nextValidTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void applySnapshot(
            Long nextSnapshotId,
            UnitType nextType,
            String nextName,
            Long nextParentId,
            String nextPath,
            int nextDepth,
            String nextExternalId,
            Instant nextValidFrom,
            Instant nextValidTo,
            String actorId,
            Instant now) {
        if (nextDepth < 0 || nextDepth > MAX_DEPTH) {
            throw new IllegalArgumentException("depth must be between 0 and 20");
        }
        if (nextValidTo != null && !nextValidTo.isAfter(nextValidFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        snapshotId = Objects.requireNonNull(nextSnapshotId, "nextSnapshotId");
        unitType = Objects.requireNonNull(nextType, "nextType");
        name = required(nextName, "nextName");
        parentId = nextParentId;
        path = required(nextPath, "nextPath");
        depth = nextDepth;
        externalId = nextExternalId;
        validFrom = Objects.requireNonNull(nextValidFrom, "nextValidFrom");
        validTo = nextValidTo;
        status = Status.ACTIVE;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `inactivate` 按照 `OrgUnitEntity` 的职责处理输入，完成 `inactivate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `inactivate` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `inactivate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `inactivate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `inactivate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean inactivate(String actorId, Instant now) {
        if (status != Status.ACTIVE) {
            return false;
        }
        status = Status.INACTIVE;
        markUpdated(actorId, now);
        return true;
    }

    /**
     * 方法 `getId` 按照 `OrgUnitEntity` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getSnapshotId` 按照 `OrgUnitEntity` 的职责处理输入，完成 `get Snapshot Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSnapshotId` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `get Snapshot Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSnapshotId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSnapshotId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getSnapshotId() {
        return snapshotId;
    }

    /**
     * 方法 `getUnitType` 按照 `OrgUnitEntity` 的职责处理输入，完成 `get Unit Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getUnitType` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `get Unit Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getUnitType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getUnitType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public UnitType getUnitType() {
        return unitType;
    }

    /**
     * 方法 `getCode` 按照 `OrgUnitEntity` 的职责处理输入，完成 `get Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getCode` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `get Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getCode() {
        return code;
    }

    /**
     * 方法 `getName` 按照 `OrgUnitEntity` 的职责处理输入，完成 `get Name` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getName` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `get Name` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getName` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getName`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getName() {
        return name;
    }

    /**
     * 方法 `getParentId` 按照 `OrgUnitEntity` 的职责处理输入，完成 `get Parent Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getParentId` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `get Parent Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getParentId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getParentId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getParentId() {
        return parentId;
    }

    /**
     * 方法 `getPath` 按照 `OrgUnitEntity` 的职责处理输入，完成 `get Path` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPath` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `get Path` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPath` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPath`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getPath() {
        return path;
    }

    /**
     * 方法 `getDepth` 按照 `OrgUnitEntity` 的职责处理输入，完成 `get Depth` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getDepth` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `get Depth` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getDepth` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getDepth`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * 方法 `getStatus` 按照 `OrgUnitEntity` 的职责处理输入，完成 `get Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `get Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * 方法 `getExternalId` 按照 `OrgUnitEntity` 的职责处理输入，完成 `get External Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getExternalId` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `get External Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getExternalId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getExternalId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getExternalId() {
        return externalId;
    }

    /**
     * 方法 `getValidFrom` 按照 `OrgUnitEntity` 的职责处理输入，完成 `get Valid From` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getValidFrom` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `get Valid From` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getValidFrom` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getValidFrom`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getValidFrom() {
        return validFrom;
    }

    /**
     * 方法 `getValidTo` 按照 `OrgUnitEntity` 的职责处理输入，完成 `get Valid To` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getValidTo` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `get Valid To` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getValidTo` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getValidTo`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getValidTo() {
        return validTo;
    }

    /**
     * 方法 `required` 按照 `OrgUnitEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `OrgUnitEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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

    /**
     * 类型 `UnitType` 位于 `OrgUnitEntity` 内，是枚举，用于承载 `Unit Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `UnitType` is an enum inside `OrgUnitEntity` and carries the responsibility, state, or contract for `Unit Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `UnitType` 作为 `OrgUnitEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `UnitType` as the responsibility boundary of `OrgUnitEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum UnitType {
        /**
         * 字段 `ORG` 表示 `UnitType` 中与 `ORG` 相关的状态、依赖、配置或结果（声明类型 `UnitType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ORG` stores the `ORG`-related state, dependency, configuration, or result of `UnitType` (declared type `UnitType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ORG` 时应保持 `UnitType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ORG`, preserve `UnitType`'s lifecycle, immutability, and thread-safety constraints.
         */
        ORG,
        /**
         * 字段 `DEPT` 表示 `UnitType` 中与 `DEPT` 相关的状态、依赖、配置或结果（声明类型 `UnitType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `DEPT` stores the `DEPT`-related state, dependency, configuration, or result of `UnitType` (declared type `UnitType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `DEPT` 时应保持 `UnitType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DEPT`, preserve `UnitType`'s lifecycle, immutability, and thread-safety constraints.
         */
        DEPT
    }

    /**
     * 类型 `Status` 位于 `OrgUnitEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `OrgUnitEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `OrgUnitEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `OrgUnitEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum Status {
        /**
         * 字段 `ACTIVE` 表示 `Status` 中与 `ACTIVE` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACTIVE` stores the `ACTIVE`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACTIVE` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACTIVE`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACTIVE,
        /**
         * 字段 `INACTIVE` 表示 `Status` 中与 `INACTIVE` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `INACTIVE` stores the `INACTIVE`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `INACTIVE` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `INACTIVE`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        INACTIVE,
        /**
         * 字段 `ARCHIVED` 表示 `Status` 中与 `ARCHIVED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ARCHIVED` stores the `ARCHIVED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ARCHIVED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ARCHIVED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        ARCHIVED
    }
}
