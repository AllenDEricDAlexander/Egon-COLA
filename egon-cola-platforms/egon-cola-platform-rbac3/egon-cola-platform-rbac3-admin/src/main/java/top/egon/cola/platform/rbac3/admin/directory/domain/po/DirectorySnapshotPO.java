package top.egon.cola.platform.rbac3.admin.directory.domain.po;

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
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.DirectorySnapshotStatusEnum;

/**
 * 类型 `DirectorySnapshotPO` 位于当前包内，是类型，用于承载 `Directory Snapshot Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `DirectorySnapshotPO` is a type in its package and carries the responsibility, state, or contract for `Directory Snapshot Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `DirectorySnapshotPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `DirectorySnapshotPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "DirectorySnapshotEntity")
@Table(name = "rbac3_directory_snapshot")
public class DirectorySnapshotPO extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `DirectorySnapshotPO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `DirectorySnapshotPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `DirectorySnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `DirectorySnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `providerCode` 表示 `DirectorySnapshotPO` 中与 `provider Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `providerCode` stores the `provider Code`-related state, dependency, configuration, or result of `DirectorySnapshotPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `providerCode` 时应保持 `DirectorySnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `providerCode`, preserve `DirectorySnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "provider_code", nullable = false, length = 128)
    private String providerCode;

    /**
     * 字段 `snapshotVersion` 表示 `DirectorySnapshotPO` 中与 `snapshot Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `snapshotVersion` stores the `snapshot Version`-related state, dependency, configuration, or result of `DirectorySnapshotPO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `snapshotVersion` 时应保持 `DirectorySnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `snapshotVersion`, preserve `DirectorySnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "snapshot_version", nullable = false)
    private long snapshotVersion;

    /**
     * 字段 `checksum` 表示 `DirectorySnapshotPO` 中与 `checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `checksum` stores the `checksum`-related state, dependency, configuration, or result of `DirectorySnapshotPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `checksum` 时应保持 `DirectorySnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `checksum`, preserve `DirectorySnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, length = 128)
    private String checksum;

    /**
     * 字段 `status` 表示 `DirectorySnapshotPO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `DirectorySnapshotStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `DirectorySnapshotPO` (declared type `DirectorySnapshotStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `DirectorySnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `DirectorySnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DirectorySnapshotStatusEnum status;

    /**
     * 字段 `generatedAt` 表示 `DirectorySnapshotPO` 中与 `generated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `generatedAt` stores the `generated At`-related state, dependency, configuration, or result of `DirectorySnapshotPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `generatedAt` 时应保持 `DirectorySnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `generatedAt`, preserve `DirectorySnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    /**
     * 字段 `receivedAt` 表示 `DirectorySnapshotPO` 中与 `received At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `receivedAt` stores the `received At`-related state, dependency, configuration, or result of `DirectorySnapshotPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `receivedAt` 时应保持 `DirectorySnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `receivedAt`, preserve `DirectorySnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    /**
     * 字段 `activatedAt` 表示 `DirectorySnapshotPO` 中与 `activated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `activatedAt` stores the `activated At`-related state, dependency, configuration, or result of `DirectorySnapshotPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `activatedAt` 时应保持 `DirectorySnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `activatedAt`, preserve `DirectorySnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "activated_at")
    private Instant activatedAt;

    /**
     * 字段 `payload` 表示 `DirectorySnapshotPO` 中与 `payload` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `payload` stores the `payload`-related state, dependency, configuration, or result of `DirectorySnapshotPO` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `payload` 时应保持 `DirectorySnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `payload`, preserve `DirectorySnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    /**
     * 字段 `counts` 表示 `DirectorySnapshotPO` 中与 `counts` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `counts` stores the `counts`-related state, dependency, configuration, or result of `DirectorySnapshotPO` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `counts` 时应保持 `DirectorySnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `counts`, preserve `DirectorySnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> counts = new LinkedHashMap<>();

    /**
     * 构造器 `DirectorySnapshotPO` 用于创建并初始化 `DirectorySnapshotPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DirectorySnapshotPO` creates and initializes `DirectorySnapshotPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DirectorySnapshotPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DirectorySnapshotPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected DirectorySnapshotPO() {
    }

    /**
     * 构造器 `DirectorySnapshotPO` 用于创建并初始化 `DirectorySnapshotPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DirectorySnapshotPO` creates and initializes `DirectorySnapshotPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DirectorySnapshotPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DirectorySnapshotPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param providerCode 输入参数 `providerCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotVersion 输入参数 `snapshotVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param checksum 输入参数 `checksum`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param payload 输入参数 `payload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public DirectorySnapshotPO(
            Long id,
            Long tenantId,
            String providerCode,
            long snapshotVersion,
            String checksum,
            Instant generatedAt,
            Map<String, Object> payload,
            String actorId,
            Instant now) {
        if (snapshotVersion < 0) {
            throw new IllegalArgumentException("snapshotVersion must not be negative");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.providerCode = required(providerCode, "providerCode");
        this.snapshotVersion = snapshotVersion;
        this.checksum = required(checksum, "checksum");
        this.status = DirectorySnapshotStatusEnum.RECEIVED;
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
        this.receivedAt = Objects.requireNonNull(now, "now");
        this.payload = Map.copyOf(Objects.requireNonNull(payload, "payload"));
        markCreated(actorId, now);
    }

    /**
     * 方法 `validate` 按照 `DirectorySnapshotPO` 的职责处理输入，完成 `validate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validate` processes its inputs according to `DirectorySnapshotPO`'s responsibility, performs the `validate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param validationCounts 输入参数 `validationCounts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void validate(Map<String, Object> validationCounts, String actorId, Instant now) {
        if (status != DirectorySnapshotStatusEnum.RECEIVED) {
            throw new IllegalStateException("only received snapshot can be validated");
        }
        counts = Map.copyOf(Objects.requireNonNull(validationCounts, "validationCounts"));
        status = DirectorySnapshotStatusEnum.VALIDATED;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `activate` 按照 `DirectorySnapshotPO` 的职责处理输入，完成 `activate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activate` processes its inputs according to `DirectorySnapshotPO`'s responsibility, performs the `activate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void activate(String actorId, Instant now) {
        if (status != DirectorySnapshotStatusEnum.VALIDATED) {
            throw new IllegalStateException("only validated snapshot can be activated");
        }
        status = DirectorySnapshotStatusEnum.ACTIVE;
        activatedAt = now;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `archive` 按照 `DirectorySnapshotPO` 的职责处理输入，完成 `archive` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `archive` processes its inputs according to `DirectorySnapshotPO`'s responsibility, performs the `archive` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `archive` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `archive`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void archive(String actorId, Instant now) {
        if (status != DirectorySnapshotStatusEnum.ACTIVE) {
            throw new IllegalStateException("only active snapshot can be archived");
        }
        status = DirectorySnapshotStatusEnum.ARCHIVED;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `getId` 按照 `DirectorySnapshotPO` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `DirectorySnapshotPO`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getProviderCode` 按照 `DirectorySnapshotPO` 的职责处理输入，完成 `get Provider Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getProviderCode` processes its inputs according to `DirectorySnapshotPO`'s responsibility, performs the `get Provider Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getProviderCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getProviderCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getProviderCode() {
        return providerCode;
    }

    /**
     * 方法 `getSnapshotVersion` 按照 `DirectorySnapshotPO` 的职责处理输入，完成 `get Snapshot Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSnapshotVersion` processes its inputs according to `DirectorySnapshotPO`'s responsibility, performs the `get Snapshot Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSnapshotVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSnapshotVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public long getSnapshotVersion() {
        return snapshotVersion;
    }

    /**
     * 方法 `getChecksum` 按照 `DirectorySnapshotPO` 的职责处理输入，完成 `get Checksum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getChecksum` processes its inputs according to `DirectorySnapshotPO`'s responsibility, performs the `get Checksum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getStatus` 按照 `DirectorySnapshotPO` 的职责处理输入，完成 `get DirectorySnapshotStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `DirectorySnapshotPO`'s responsibility, performs the `get DirectorySnapshotStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public DirectorySnapshotStatusEnum getStatus() {
        return status;
    }

    /**
     * 方法 `getGeneratedAt` 按照 `DirectorySnapshotPO` 的职责处理输入，完成 `get Generated At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getGeneratedAt` processes its inputs according to `DirectorySnapshotPO`'s responsibility, performs the `get Generated At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getGeneratedAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getGeneratedAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getGeneratedAt() {
        return generatedAt;
    }

    /**
     * 方法 `getReceivedAt` 按照 `DirectorySnapshotPO` 的职责处理输入，完成 `get Received At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getReceivedAt` processes its inputs according to `DirectorySnapshotPO`'s responsibility, performs the `get Received At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getReceivedAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getReceivedAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getReceivedAt() {
        return receivedAt;
    }

    /**
     * 方法 `getActivatedAt` 按照 `DirectorySnapshotPO` 的职责处理输入，完成 `get Activated At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getActivatedAt` processes its inputs according to `DirectorySnapshotPO`'s responsibility, performs the `get Activated At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getActivatedAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getActivatedAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getActivatedAt() {
        return activatedAt;
    }

    /**
     * 方法 `getCounts` 按照 `DirectorySnapshotPO` 的职责处理输入，完成 `get Counts` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getCounts` processes its inputs according to `DirectorySnapshotPO`'s responsibility, performs the `get Counts` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getCounts` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getCounts`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Map<String, Object> getCounts() {
        return Map.copyOf(counts);
    }

    /**
     * 方法 `required` 按照 `DirectorySnapshotPO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `DirectorySnapshotPO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
