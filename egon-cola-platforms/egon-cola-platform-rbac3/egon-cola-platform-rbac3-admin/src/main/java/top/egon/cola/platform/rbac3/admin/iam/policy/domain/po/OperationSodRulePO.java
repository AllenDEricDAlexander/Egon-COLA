package top.egon.cola.platform.rbac3.admin.iam.policy.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.iam.policy.domain.enums.OperationSodRuleStatusEnum;

/**
 * 类型 `OperationSodRulePO` 位于当前包内，是类型，用于承载 `Operation Sod Rule Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `OperationSodRulePO` is a type in its package and carries the responsibility, state, or contract for `Operation Sod Rule Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `OperationSodRulePO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `OperationSodRulePO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "OperationSodRuleEntity")
@Table(name = "rbac3_operation_sod_rule")
public class OperationSodRulePO extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `OperationSodRulePO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `OperationSodRulePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `OperationSodRulePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `OperationSodRulePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `applicationCode` 表示 `OperationSodRulePO` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `OperationSodRulePO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `OperationSodRulePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `OperationSodRulePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_code", nullable = false, length = 128)
    private String applicationCode;

    /**
     * 字段 `businessResource` 表示 `OperationSodRulePO` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `OperationSodRulePO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `OperationSodRulePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `OperationSodRulePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "business_resource", nullable = false, length = 128)
    private String businessResource;

    /**
     * 字段 `priorActionCode` 表示 `OperationSodRulePO` 中与 `prior Action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `priorActionCode` stores the `prior Action Code`-related state, dependency, configuration, or result of `OperationSodRulePO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `priorActionCode` 时应保持 `OperationSodRulePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `priorActionCode`, preserve `OperationSodRulePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "prior_action_code", nullable = false, length = 128)
    private String priorActionCode;

    /**
     * 字段 `forbiddenLaterActionCode` 表示 `OperationSodRulePO` 中与 `forbidden Later Action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `forbiddenLaterActionCode` stores the `forbidden Later Action Code`-related state, dependency, configuration, or result of `OperationSodRulePO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `forbiddenLaterActionCode` 时应保持 `OperationSodRulePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `forbiddenLaterActionCode`, preserve `OperationSodRulePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "forbidden_later_action_code", nullable = false, length = 128)
    private String forbiddenLaterActionCode;

    /**
     * 字段 `lookbackFrom` 表示 `OperationSodRulePO` 中与 `lookback From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `lookbackFrom` stores the `lookback From`-related state, dependency, configuration, or result of `OperationSodRulePO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `lookbackFrom` 时应保持 `OperationSodRulePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `lookbackFrom`, preserve `OperationSodRulePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "lookback_from")
    private Instant lookbackFrom;

    /**
     * 字段 `status` 表示 `OperationSodRulePO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `OperationSodRuleStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `OperationSodRulePO` (declared type `OperationSodRuleStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `OperationSodRulePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `OperationSodRulePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OperationSodRuleStatusEnum status;

    /**
     * 字段 `validFrom` 表示 `OperationSodRulePO` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `OperationSodRulePO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `OperationSodRulePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `OperationSodRulePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    /**
     * 字段 `validTo` 表示 `OperationSodRulePO` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `OperationSodRulePO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validTo` 时应保持 `OperationSodRulePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validTo`, preserve `OperationSodRulePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_to")
    private Instant validTo;

    /**
     * 构造器 `OperationSodRulePO` 用于创建并初始化 `OperationSodRulePO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `OperationSodRulePO` creates and initializes `OperationSodRulePO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `OperationSodRulePO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `OperationSodRulePO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected OperationSodRulePO() {
    }

    /**
     * 构造器 `OperationSodRulePO` 用于创建并初始化 `OperationSodRulePO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `OperationSodRulePO` creates and initializes `OperationSodRulePO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `OperationSodRulePO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `OperationSodRulePO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param businessResource 输入参数 `businessResource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param priorActionCode 输入参数 `priorActionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param forbiddenLaterActionCode 输入参数 `forbiddenLaterActionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param lookbackFrom 输入参数 `lookbackFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public OperationSodRulePO(
            Long id,
            Long tenantId,
            String applicationCode,
            String businessResource,
            String priorActionCode,
            String forbiddenLaterActionCode,
            Instant lookbackFrom,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        validate(priorActionCode, forbiddenLaterActionCode, validFrom, validTo);
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationCode = required(applicationCode, "applicationCode");
        this.businessResource = required(businessResource, "businessResource");
        this.priorActionCode = priorActionCode.trim();
        this.forbiddenLaterActionCode = forbiddenLaterActionCode.trim();
        this.lookbackFrom = lookbackFrom;
        this.status = OperationSodRuleStatusEnum.ACTIVE;
        this.validFrom = validFrom;
        this.validTo = validTo;
        markCreated(actorId, now);
    }

    /**
     * 方法 `update` 按照 `OperationSodRulePO` 的职责处理输入，完成 `update` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `update` processes its inputs according to `OperationSodRulePO`'s responsibility, performs the `update` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `update` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `update`, then continue the business flow using its result, exception, or side effect.
     *
     * @param businessResource 输入参数 `businessResource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param priorActionCode 输入参数 `priorActionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param forbiddenLaterActionCode 输入参数 `forbiddenLaterActionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param lookbackFrom 输入参数 `lookbackFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void update(
            String businessResource,
            String priorActionCode,
            String forbiddenLaterActionCode,
            Instant lookbackFrom,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        validate(priorActionCode, forbiddenLaterActionCode, validFrom, validTo);
        this.businessResource = required(businessResource, "businessResource");
        this.priorActionCode = priorActionCode.trim();
        this.forbiddenLaterActionCode = forbiddenLaterActionCode.trim();
        this.lookbackFrom = lookbackFrom;
        this.validFrom = validFrom;
        this.validTo = validTo;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `getId` 按照 `OperationSodRulePO` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `OperationSodRulePO`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getApplicationCode` 按照 `OperationSodRulePO` 的职责处理输入，完成 `get Application Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getApplicationCode` processes its inputs according to `OperationSodRulePO`'s responsibility, performs the `get Application Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getApplicationCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getApplicationCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getApplicationCode() {
        return applicationCode;
    }

    /**
     * 方法 `getBusinessResource` 按照 `OperationSodRulePO` 的职责处理输入，完成 `get Business Resource` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getBusinessResource` processes its inputs according to `OperationSodRulePO`'s responsibility, performs the `get Business Resource` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getBusinessResource` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getBusinessResource`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getBusinessResource() {
        return businessResource;
    }

    /**
     * 方法 `getPriorActionCode` 按照 `OperationSodRulePO` 的职责处理输入，完成 `get Prior Action Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPriorActionCode` processes its inputs according to `OperationSodRulePO`'s responsibility, performs the `get Prior Action Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPriorActionCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPriorActionCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getPriorActionCode() {
        return priorActionCode;
    }

    /**
     * 方法 `getForbiddenLaterActionCode` 按照 `OperationSodRulePO` 的职责处理输入，完成 `get Forbidden Later Action Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getForbiddenLaterActionCode` processes its inputs according to `OperationSodRulePO`'s responsibility, performs the `get Forbidden Later Action Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getForbiddenLaterActionCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getForbiddenLaterActionCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getForbiddenLaterActionCode() {
        return forbiddenLaterActionCode;
    }

    /**
     * 方法 `getStatus` 按照 `OperationSodRulePO` 的职责处理输入，完成 `get OperationSodRuleStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `OperationSodRulePO`'s responsibility, performs the `get OperationSodRuleStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public OperationSodRuleStatusEnum getStatus() {
        return status;
    }


    /**
     * 方法 `validate` 按照 `OperationSodRulePO` 的职责处理输入，完成 `validate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validate` processes its inputs according to `OperationSodRulePO`'s responsibility, performs the `validate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param priorActionCode 输入参数 `priorActionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param forbiddenLaterActionCode 输入参数 `forbiddenLaterActionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private static void validate(
            String priorActionCode,
            String forbiddenLaterActionCode,
            Instant validFrom,
            Instant validTo) {
        String prior = required(priorActionCode, "priorActionCode");
        String later = required(forbiddenLaterActionCode, "forbiddenLaterActionCode");
        if (prior.equals(later)) {
            throw new IllegalArgumentException("SOD actions must be different");
        }
        Objects.requireNonNull(validFrom, "validFrom");
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
    }

    /**
     * 方法 `required` 按照 `OperationSodRulePO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `OperationSodRulePO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
