package top.egon.cola.platform.rbac3.admin.shared.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * 类型 `TenantScopedPO` 位于当前包内，是类型，用于承载 `Tenant Scoped Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `TenantScopedPO` is a type in its package and carries the responsibility, state, or contract for `Tenant Scoped Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `TenantScopedPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `TenantScopedPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@MappedSuperclass
public abstract class TenantScopedPO {

    /**
     * 字段 `tenantId` 表示 `TenantScopedPO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `TenantScopedPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `TenantScopedPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `TenantScopedPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    /**
     * 字段 `version` 表示 `TenantScopedPO` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `version` stores the `version`-related state, dependency, configuration, or result of `TenantScopedPO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `version` 时应保持 `TenantScopedPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `version`, preserve `TenantScopedPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /**
     * 字段 `createdAt` 表示 `TenantScopedPO` 中与 `created At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `createdAt` stores the `created At`-related state, dependency, configuration, or result of `TenantScopedPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `createdAt` 时应保持 `TenantScopedPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `createdAt`, preserve `TenantScopedPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * 字段 `createdBy` 表示 `TenantScopedPO` 中与 `created By` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `createdBy` stores the `created By`-related state, dependency, configuration, or result of `TenantScopedPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `createdBy` 时应保持 `TenantScopedPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `createdBy`, preserve `TenantScopedPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "created_by", nullable = false, updatable = false, length = 128)
    private String createdBy;

    /**
     * 字段 `updatedAt` 表示 `TenantScopedPO` 中与 `updated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `updatedAt` stores the `updated At`-related state, dependency, configuration, or result of `TenantScopedPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `updatedAt` 时应保持 `TenantScopedPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `updatedAt`, preserve `TenantScopedPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 字段 `updatedBy` 表示 `TenantScopedPO` 中与 `updated By` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `updatedBy` stores the `updated By`-related state, dependency, configuration, or result of `TenantScopedPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `updatedBy` 时应保持 `TenantScopedPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `updatedBy`, preserve `TenantScopedPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    /**
     * 方法 `getTenantId` 按照 `TenantScopedPO` 的职责处理输入，完成 `get Tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getTenantId` processes its inputs according to `TenantScopedPO`'s responsibility, performs the `get Tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getTenantId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getTenantId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getTenantId() {
        return tenantId;
    }

    /**
     * 方法 `setTenantId` 按照 `TenantScopedPO` 的职责处理输入，完成 `set Tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setTenantId` processes its inputs according to `TenantScopedPO`'s responsibility, performs the `set Tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setTenantId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setTenantId`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    protected void setTenantId(Long tenantId) {
        if (this.tenantId != null && !this.tenantId.equals(tenantId)) {
            throw new IllegalStateException("tenantId is immutable");
        }
        this.tenantId = tenantId;
    }

    /**
     * 方法 `getVersion` 按照 `TenantScopedPO` 的职责处理输入，完成 `get Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getVersion` processes its inputs according to `TenantScopedPO`'s responsibility, performs the `get Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public long getVersion() {
        return version;
    }

    /**
     * 方法 `getCreatedAt` 按照 `TenantScopedPO` 的职责处理输入，完成 `get Created At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getCreatedAt` processes its inputs according to `TenantScopedPO`'s responsibility, performs the `get Created At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getCreatedAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getCreatedAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 方法 `getCreatedBy` 按照 `TenantScopedPO` 的职责处理输入，完成 `get Created By` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getCreatedBy` processes its inputs according to `TenantScopedPO`'s responsibility, performs the `get Created By` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getCreatedBy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getCreatedBy`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 方法 `getUpdatedAt` 按照 `TenantScopedPO` 的职责处理输入，完成 `get Updated At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getUpdatedAt` processes its inputs according to `TenantScopedPO`'s responsibility, performs the `get Updated At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getUpdatedAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getUpdatedAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 方法 `getUpdatedBy` 按照 `TenantScopedPO` 的职责处理输入，完成 `get Updated By` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getUpdatedBy` processes its inputs according to `TenantScopedPO`'s responsibility, performs the `get Updated By` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getUpdatedBy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getUpdatedBy`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * 方法 `markCreated` 按照 `TenantScopedPO` 的职责处理输入，完成 `mark Created` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `markCreated` processes its inputs according to `TenantScopedPO`'s responsibility, performs the `mark Created` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `markCreated` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `markCreated`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void markCreated(String actorId, Instant now) {
        if (createdAt != null) {
            throw new IllegalStateException("creation audit is immutable");
        }
        createdAt = now;
        createdBy = requiredActor(actorId);
        updatedAt = now;
        updatedBy = createdBy;
    }

    /**
     * 方法 `markUpdated` 按照 `TenantScopedPO` 的职责处理输入，完成 `mark Updated` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `markUpdated` processes its inputs according to `TenantScopedPO`'s responsibility, performs the `mark Updated` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `markUpdated` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `markUpdated`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void markUpdated(String actorId, Instant now) {
        updatedAt = now;
        updatedBy = requiredActor(actorId);
    }

    /**
     * 方法 `requireCreationAudit` 按照 `TenantScopedPO` 的职责处理输入，完成 `require Creation Audit` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireCreationAudit` processes its inputs according to `TenantScopedPO`'s responsibility, performs the `require Creation Audit` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireCreationAudit` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireCreationAudit`, then continue the business flow using its result, exception, or side effect.
     */
    @PrePersist
    void requireCreationAudit() {
        if (createdAt == null || createdBy == null || updatedAt == null || updatedBy == null) {
            throw new IllegalStateException("creation audit must be initialized before persistence");
        }
    }

    /**
     * 方法 `requireUpdateAudit` 按照 `TenantScopedPO` 的职责处理输入，完成 `require Update Audit` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireUpdateAudit` processes its inputs according to `TenantScopedPO`'s responsibility, performs the `require Update Audit` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireUpdateAudit` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireUpdateAudit`, then continue the business flow using its result, exception, or side effect.
     */
    @PreUpdate
    void requireUpdateAudit() {
        if (updatedAt == null || updatedBy == null) {
            throw new IllegalStateException("update audit must be initialized before persistence");
        }
    }

    /**
     * 方法 `requiredActor` 按照 `TenantScopedPO` 的职责处理输入，完成 `required Actor` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requiredActor` processes its inputs according to `TenantScopedPO`'s responsibility, performs the `required Actor` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requiredActor` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requiredActor`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String requiredActor(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId is required");
        }
        return actorId.trim();
    }
}
