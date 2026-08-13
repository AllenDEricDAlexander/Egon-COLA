package top.egon.cola.platform.rbac3.admin.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * 类型 `UserEntity` 位于当前包内，是类型，用于承载 `User Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `UserEntity` is a type in its package and carries the responsibility, state, or contract for `User Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `UserEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `UserEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_user")
public class UserEntity extends TenantScopedEntity {

    /**
     * 字段 `id` 表示 `UserEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `UserEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `UserEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `UserEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `username` 表示 `UserEntity` 中与 `username` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `username` stores the `username`-related state, dependency, configuration, or result of `UserEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `username` 时应保持 `UserEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `username`, preserve `UserEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, length = 128)
    private String username;

    /**
     * 字段 `normalizedUsername` 表示 `UserEntity` 中与 `normalized Username` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `normalizedUsername` stores the `normalized Username`-related state, dependency, configuration, or result of `UserEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `normalizedUsername` 时应保持 `UserEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `normalizedUsername`, preserve `UserEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "normalized_username", nullable = false, length = 128)
    private String normalizedUsername;

    /**
     * 字段 `displayName` 表示 `UserEntity` 中与 `display Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `displayName` stores the `display Name`-related state, dependency, configuration, or result of `UserEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `displayName` 时应保持 `UserEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `displayName`, preserve `UserEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    /**
     * 字段 `status` 表示 `UserEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `UserEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `UserEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `UserEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    /**
     * 字段 `authVersion` 表示 `UserEntity` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `UserEntity` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `UserEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `UserEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    /**
     * 字段 `primaryOrgUnitId` 表示 `UserEntity` 中与 `primary Org Unit Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `primaryOrgUnitId` stores the `primary Org Unit Id`-related state, dependency, configuration, or result of `UserEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `primaryOrgUnitId` 时应保持 `UserEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `primaryOrgUnitId`, preserve `UserEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "primary_org_unit_id")
    private Long primaryOrgUnitId;

    /**
     * 字段 `primaryPositionId` 表示 `UserEntity` 中与 `primary Position Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `primaryPositionId` stores the `primary Position Id`-related state, dependency, configuration, or result of `UserEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `primaryPositionId` 时应保持 `UserEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `primaryPositionId`, preserve `UserEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "primary_position_id")
    private Long primaryPositionId;

    /**
     * 字段 `directorySnapshotVersion` 表示 `UserEntity` 中与 `directory Snapshot Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `directorySnapshotVersion` stores the `directory Snapshot Version`-related state, dependency, configuration, or result of `UserEntity` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `directorySnapshotVersion` 时应保持 `UserEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `directorySnapshotVersion`, preserve `UserEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "directory_snapshot_version", nullable = false)
    private long directorySnapshotVersion;

    /**
     * 字段 `lockedUntil` 表示 `UserEntity` 中与 `locked Until` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `lockedUntil` stores the `locked Until`-related state, dependency, configuration, or result of `UserEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `lockedUntil` 时应保持 `UserEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `lockedUntil`, preserve `UserEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    /**
     * 字段 `archivedAt` 表示 `UserEntity` 中与 `archived At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `archivedAt` stores the `archived At`-related state, dependency, configuration, or result of `UserEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `archivedAt` 时应保持 `UserEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `archivedAt`, preserve `UserEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "archived_at")
    private Instant archivedAt;

    /**
     * 构造器 `UserEntity` 用于创建并初始化 `UserEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `UserEntity` creates and initializes `UserEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `UserEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `UserEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected UserEntity() {
    }

    /**
     * 构造器 `UserEntity` 用于创建并初始化 `UserEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `UserEntity` creates and initializes `UserEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `UserEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `UserEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param username 输入参数 `username`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param displayName 输入参数 `displayName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public UserEntity(
            Long id,
            Long tenantId,
            String username,
            String displayName,
            String actorId,
            Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.username = required(username, "username");
        this.normalizedUsername = normalize(username);
        this.displayName = required(displayName, "displayName");
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    /**
     * 方法 `changeStatus` 按照 `UserEntity` 的职责处理输入，完成 `change Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `changeStatus` processes its inputs according to `UserEntity`'s responsibility, performs the `change Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `changeStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `changeStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param nextStatus 输入参数 `nextStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reason 输入参数 `reason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void changeStatus(Status nextStatus, String reason, String actorId, Instant now) {
        Objects.requireNonNull(nextStatus, "nextStatus");
        required(reason, "reason");
        if (status == Status.ARCHIVED) {
            throw new IllegalStateException("archived user is terminal");
        }
        status = nextStatus;
        authVersion = Math.incrementExact(authVersion);
        if (nextStatus == Status.ARCHIVED) {
            archivedAt = now;
        }
        markUpdated(actorId, now);
    }

    /**
     * 方法 `changeStatus` 按照 `UserEntity` 的职责处理输入，完成 `change Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `changeStatus` processes its inputs according to `UserEntity`'s responsibility, performs the `change Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `changeStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `changeStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param nextStatus 输入参数 `nextStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reason 输入参数 `reason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedAuthVersion 输入参数 `expectedAuthVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void changeStatus(
            Status nextStatus,
            String reason,
            long expectedAuthVersion,
            String actorId,
            Instant now) {
        if (authVersion != expectedAuthVersion) {
            throw new IllegalStateException("user authorization version conflict");
        }
        changeStatus(nextStatus, reason, actorId, now);
    }

    /**
     * 方法 `getId` 按照 `UserEntity` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `UserEntity`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getNormalizedUsername` 按照 `UserEntity` 的职责处理输入，完成 `get Normalized Username` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getNormalizedUsername` processes its inputs according to `UserEntity`'s responsibility, performs the `get Normalized Username` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getNormalizedUsername` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getNormalizedUsername`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getNormalizedUsername() {
        return normalizedUsername;
    }

    /**
     * 方法 `getUsername` 按照 `UserEntity` 的职责处理输入，完成 `get Username` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getUsername` processes its inputs according to `UserEntity`'s responsibility, performs the `get Username` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getUsername` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getUsername`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getUsername() {
        return username;
    }

    /**
     * 方法 `getDisplayName` 按照 `UserEntity` 的职责处理输入，完成 `get Display Name` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getDisplayName` processes its inputs according to `UserEntity`'s responsibility, performs the `get Display Name` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getDisplayName` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getDisplayName`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 方法 `getStatus` 按照 `UserEntity` 的职责处理输入，完成 `get Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `UserEntity`'s responsibility, performs the `get Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getAuthVersion` 按照 `UserEntity` 的职责处理输入，完成 `get Auth Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getAuthVersion` processes its inputs according to `UserEntity`'s responsibility, performs the `get Auth Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getAuthVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getAuthVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public long getAuthVersion() {
        return authVersion;
    }

    /**
     * 方法 `getDirectorySnapshotVersion` 按照 `UserEntity` 的职责处理输入，完成 `get Directory Snapshot Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getDirectorySnapshotVersion` processes its inputs according to `UserEntity`'s responsibility, performs the `get Directory Snapshot Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getDirectorySnapshotVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getDirectorySnapshotVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public long getDirectorySnapshotVersion() {
        return directorySnapshotVersion;
    }

    /**
     * 方法 `getPrimaryOrgUnitId` 按照 `UserEntity` 的职责处理输入，完成 `get Primary Org Unit Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPrimaryOrgUnitId` processes its inputs according to `UserEntity`'s responsibility, performs the `get Primary Org Unit Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPrimaryOrgUnitId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPrimaryOrgUnitId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getPrimaryOrgUnitId() {
        return primaryOrgUnitId;
    }

    /**
     * 方法 `getPrimaryPositionId` 按照 `UserEntity` 的职责处理输入，完成 `get Primary Position Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPrimaryPositionId` processes its inputs according to `UserEntity`'s responsibility, performs the `get Primary Position Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPrimaryPositionId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPrimaryPositionId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getPrimaryPositionId() {
        return primaryPositionId;
    }

    /**
     * 方法 `applyDirectorySnapshot` 按照 `UserEntity` 的职责处理输入，完成 `apply Directory Snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `applyDirectorySnapshot` processes its inputs according to `UserEntity`'s responsibility, performs the `apply Directory Snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `applyDirectorySnapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `applyDirectorySnapshot`, then continue the business flow using its result, exception, or side effect.
     *
     * @param snapshotVersion 输入参数 `snapshotVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextPrimaryOrgUnitId 输入参数 `nextPrimaryOrgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextPrimaryPositionId 输入参数 `nextPrimaryPositionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param authorizationChanged 输入参数 `authorizationChanged`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void applyDirectorySnapshot(
            long snapshotVersion,
            Long nextPrimaryOrgUnitId,
            Long nextPrimaryPositionId,
            boolean authorizationChanged,
            String actorId,
            Instant now) {
        directorySnapshotVersion = Math.max(directorySnapshotVersion, snapshotVersion);
        primaryOrgUnitId = nextPrimaryOrgUnitId;
        primaryPositionId = nextPrimaryPositionId;
        if (authorizationChanged) {
            authVersion = Math.incrementExact(authVersion);
        }
        markUpdated(actorId, now);
    }

    /**
     * 方法 `advanceAuthorizationVersion` 按照 `UserEntity` 的职责处理输入，完成 `advance Authorization Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `advanceAuthorizationVersion` processes its inputs according to `UserEntity`'s responsibility, performs the `advance Authorization Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `advanceAuthorizationVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `advanceAuthorizationVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public long advanceAuthorizationVersion(
            long expectedVersion,
            String actorId,
            Instant now
    ) {
        if (authVersion != expectedVersion) {
            throw new IllegalStateException("user authorization version conflict");
        }
        authVersion = Math.incrementExact(authVersion);
        markUpdated(actorId, now);
        return authVersion;
    }

    /**
     * 方法 `normalize` 按照 `UserEntity` 的职责处理输入，完成 `normalize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `normalize` processes its inputs according to `UserEntity`'s responsibility, performs the `normalize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `normalize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `normalize`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public static String normalize(String value) {
        return Normalizer.normalize(required(value, "username"), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
    }

    /**
     * 方法 `required` 按照 `UserEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `UserEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 类型 `Status` 位于 `UserEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `UserEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `UserEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `UserEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum Status {
        /**
         * 字段 `INVITED` 表示 `Status` 中与 `INVITED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `INVITED` stores the `INVITED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `INVITED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `INVITED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        INVITED,
        /**
         * 字段 `ACTIVE` 表示 `Status` 中与 `ACTIVE` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACTIVE` stores the `ACTIVE`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACTIVE` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACTIVE`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACTIVE,
        /**
         * 字段 `LOCKED` 表示 `Status` 中与 `LOCKED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `LOCKED` stores the `LOCKED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `LOCKED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `LOCKED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        LOCKED,
        /**
         * 字段 `DISABLED` 表示 `Status` 中与 `DISABLED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `DISABLED` stores the `DISABLED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `DISABLED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DISABLED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        DISABLED,
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
