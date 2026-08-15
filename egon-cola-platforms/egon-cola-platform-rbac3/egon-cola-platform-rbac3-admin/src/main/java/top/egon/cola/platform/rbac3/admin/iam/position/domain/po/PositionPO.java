package top.egon.cola.platform.rbac3.admin.iam.position.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.enums.PositionStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.DirectorySourceTypeEnum;

/**
 * 类型 `PositionPO` 位于当前包内，是类型，用于承载 `Position Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `PositionPO` is a type in its package and carries the responsibility, state, or contract for `Position Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `PositionPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `PositionPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "PositionEntity")
@Table(name = "rbac3_position")
public class PositionPO extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `PositionPO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `PositionPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `PositionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `PositionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `snapshotId` 表示 `PositionPO` 中与 `snapshot Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `snapshotId` stores the `snapshot Id`-related state, dependency, configuration, or result of `PositionPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `snapshotId` 时应保持 `PositionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `snapshotId`, preserve `PositionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private DirectorySourceTypeEnum sourceType;

    /**
     * 字段 `code` 表示 `PositionPO` 中与 `code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `code` stores the `code`-related state, dependency, configuration, or result of `PositionPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `code` 时应保持 `PositionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `code`, preserve `PositionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, length = 128)
    private String code;

    /**
     * 字段 `name` 表示 `PositionPO` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `name` stores the `name`-related state, dependency, configuration, or result of `PositionPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `name` 时应保持 `PositionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `name`, preserve `PositionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 字段 `orgUnitId` 表示 `PositionPO` 中与 `org Unit Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `orgUnitId` stores the `org Unit Id`-related state, dependency, configuration, or result of `PositionPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `orgUnitId` 时应保持 `PositionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `orgUnitId`, preserve `PositionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    /**
     * 字段 `status` 表示 `PositionPO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `PositionStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `PositionPO` (declared type `PositionStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `PositionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `PositionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PositionStatusEnum status;

    /**
     * 字段 `externalId` 表示 `PositionPO` 中与 `external Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `externalId` stores the `external Id`-related state, dependency, configuration, or result of `PositionPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `externalId` 时应保持 `PositionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `externalId`, preserve `PositionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "external_id", length = 256)
    private String externalId;

    /**
     * 字段 `validFrom` 表示 `PositionPO` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `PositionPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `PositionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `PositionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    /**
     * 字段 `validTo` 表示 `PositionPO` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `PositionPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validTo` 时应保持 `PositionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validTo`, preserve `PositionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_to")
    private Instant validTo;

    /**
     * 构造器 `PositionPO` 用于创建并初始化 `PositionPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PositionPO` creates and initializes `PositionPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PositionPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PositionPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected PositionPO() {
    }

    /**
     * 构造器 `PositionPO` 用于创建并初始化 `PositionPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PositionPO` creates and initializes `PositionPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PositionPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PositionPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param code 输入参数 `code`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public PositionPO(
            Long id,
            Long tenantId,
            Long snapshotId,
            String code,
            String name,
            Long orgUnitId,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        this(id, tenantId, snapshotId, code, name, orgUnitId, null,
                validFrom, validTo, actorId, now);
    }

    /**
     * 构造器 `PositionPO` 用于创建并初始化 `PositionPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PositionPO` creates and initializes `PositionPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PositionPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PositionPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param code 输入参数 `code`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param externalId 输入参数 `externalId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public PositionPO(
            Long id,
            Long tenantId,
            Long snapshotId,
            String code,
            String name,
            Long orgUnitId,
            String externalId,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.snapshotId = Objects.requireNonNull(snapshotId, "snapshotId");
        this.sourceType = DirectorySourceTypeEnum.DIRECTORY_SNAPSHOT;
        this.code = required(code, "code");
        this.name = required(name, "name");
        this.orgUnitId = Objects.requireNonNull(orgUnitId, "orgUnitId");
        this.status = PositionStatusEnum.ACTIVE;
        this.externalId = externalId;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        markCreated(actorId, now);
    }

    public PositionPO(
            Long id,
            Long tenantId,
            DirectorySourceTypeEnum sourceType,
            Long snapshotId,
            String code,
            String name,
            Long orgUnitId,
            String externalId,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.snapshotId = snapshotId;
        this.code = required(code, "code");
        this.name = required(name, "name");
        this.orgUnitId = Objects.requireNonNull(orgUnitId, "orgUnitId");
        this.status = PositionStatusEnum.ACTIVE;
        this.externalId = externalId;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        validateSourceState();
        markCreated(actorId, now);
    }

    /**
     * 方法 `applySnapshot` 按照 `PositionPO` 的职责处理输入，完成 `apply Snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `applySnapshot` processes its inputs according to `PositionPO`'s responsibility, performs the `apply Snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `applySnapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `applySnapshot`, then continue the business flow using its result, exception, or side effect.
     *
     * @param nextSnapshotId 输入参数 `nextSnapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextName 输入参数 `nextName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextOrgUnitId 输入参数 `nextOrgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextExternalId 输入参数 `nextExternalId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextValidFrom 输入参数 `nextValidFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextValidTo 输入参数 `nextValidTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void applySnapshot(
            Long nextSnapshotId,
            String nextName,
            Long nextOrgUnitId,
            String nextExternalId,
            Instant nextValidFrom,
            Instant nextValidTo,
            String actorId,
            Instant now) {
        requireDirectorySnapshotSource();
        if (nextValidTo != null && !nextValidTo.isAfter(nextValidFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        snapshotId = Objects.requireNonNull(nextSnapshotId, "nextSnapshotId");
        name = required(nextName, "nextName");
        orgUnitId = Objects.requireNonNull(nextOrgUnitId, "nextOrgUnitId");
        externalId = nextExternalId;
        validFrom = Objects.requireNonNull(nextValidFrom, "nextValidFrom");
        validTo = nextValidTo;
        status = PositionStatusEnum.ACTIVE;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `inactivate` 按照 `PositionPO` 的职责处理输入，完成 `inactivate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `inactivate` processes its inputs according to `PositionPO`'s responsibility, performs the `inactivate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `inactivate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `inactivate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean inactivate(String actorId, Instant now) {
        if (status != PositionStatusEnum.ACTIVE) {
            return false;
        }
        status = PositionStatusEnum.INACTIVE;
        markUpdated(actorId, now);
        return true;
    }

    public void inactivateManually(String actorId, Instant now) {
        requireManualSource();
        if (!inactivate(actorId, now)) {
            throw new IllegalStateException("position is not active");
        }
    }

    /** Updates the mutable fields owned by a MANUAL position record. */
    public void updateManually(
            String nextName,
            Long nextOrgUnitId,
            String nextExternalId,
            Instant nextValidFrom,
            Instant nextValidTo,
            long expectedVersion,
            String actorId,
            Instant now) {
        requireManualSource();
        if (getVersion() != expectedVersion) {
            throw new IllegalStateException("position version conflict");
        }
        if (nextValidTo != null && !nextValidTo.isAfter(nextValidFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        name = required(nextName, "nextName");
        orgUnitId = Objects.requireNonNull(nextOrgUnitId, "nextOrgUnitId");
        externalId = nextExternalId;
        validFrom = Objects.requireNonNull(nextValidFrom, "nextValidFrom");
        validTo = nextValidTo;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `getId` 按照 `PositionPO` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `PositionPO`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getSnapshotId` 按照 `PositionPO` 的职责处理输入，完成 `get Snapshot Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSnapshotId` processes its inputs according to `PositionPO`'s responsibility, performs the `get Snapshot Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSnapshotId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSnapshotId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getSnapshotId() {
        return snapshotId;
    }

    public DirectorySourceTypeEnum getSourceType() {
        return sourceType;
    }

    /**
     * 方法 `getCode` 按照 `PositionPO` 的职责处理输入，完成 `get Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getCode` processes its inputs according to `PositionPO`'s responsibility, performs the `get Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getName` 按照 `PositionPO` 的职责处理输入，完成 `get Name` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getName` processes its inputs according to `PositionPO`'s responsibility, performs the `get Name` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getOrgUnitId` 按照 `PositionPO` 的职责处理输入，完成 `get Org Unit Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getOrgUnitId` processes its inputs according to `PositionPO`'s responsibility, performs the `get Org Unit Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getOrgUnitId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getOrgUnitId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getOrgUnitId() {
        return orgUnitId;
    }

    /**
     * 方法 `getStatus` 按照 `PositionPO` 的职责处理输入，完成 `get PositionStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `PositionPO`'s responsibility, performs the `get PositionStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public PositionStatusEnum getStatus() {
        return status;
    }

    /**
     * 方法 `getExternalId` 按照 `PositionPO` 的职责处理输入，完成 `get External Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getExternalId` processes its inputs according to `PositionPO`'s responsibility, performs the `get External Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getValidFrom` 按照 `PositionPO` 的职责处理输入，完成 `get Valid From` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getValidFrom` processes its inputs according to `PositionPO`'s responsibility, performs the `get Valid From` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getValidTo` 按照 `PositionPO` 的职责处理输入，完成 `get Valid To` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getValidTo` processes its inputs according to `PositionPO`'s responsibility, performs the `get Valid To` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `required` 按照 `PositionPO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `PositionPO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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

    private void requireDirectorySnapshotSource() {
        if (sourceType != DirectorySourceTypeEnum.DIRECTORY_SNAPSHOT) {
            throw new IllegalStateException("manual position cannot be materialized from a directory snapshot");
        }
    }

    private void requireManualSource() {
        if (sourceType != DirectorySourceTypeEnum.MANUAL) {
            throw new IllegalStateException("directory snapshot position is read-only for manual operations");
        }
    }

    private void validateSourceState() {
        if (sourceType == DirectorySourceTypeEnum.DIRECTORY_SNAPSHOT && snapshotId == null) {
            throw new IllegalArgumentException("directory snapshot position requires snapshotId");
        }
        if (sourceType == DirectorySourceTypeEnum.MANUAL && snapshotId != null) {
            throw new IllegalArgumentException("manual position cannot have snapshotId");
        }
    }

    }
