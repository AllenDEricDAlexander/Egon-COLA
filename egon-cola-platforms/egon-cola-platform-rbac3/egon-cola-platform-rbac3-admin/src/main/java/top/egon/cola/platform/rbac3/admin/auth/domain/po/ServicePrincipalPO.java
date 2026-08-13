package top.egon.cola.platform.rbac3.admin.auth.domain.po;

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
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.auth.domain.enums.ServicePrincipalStatusEnum;

/**
 * 类型 `ServicePrincipalPO` 位于当前包内，是类型，用于承载 `Service Principal Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ServicePrincipalPO` is a type in its package and carries the responsibility, state, or contract for `Service Principal Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ServicePrincipalPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ServicePrincipalPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "ServicePrincipalEntity")
@Table(name = "rbac3_service_principal")
public class ServicePrincipalPO extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `ServicePrincipalPO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `ServicePrincipalPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `ServicePrincipalPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `ServicePrincipalPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `serviceCode` 表示 `ServicePrincipalPO` 中与 `service Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `serviceCode` stores the `service Code`-related state, dependency, configuration, or result of `ServicePrincipalPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `serviceCode` 时应保持 `ServicePrincipalPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `serviceCode`, preserve `ServicePrincipalPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "service_code", nullable = false, length = 128)
    private String serviceCode;

    /**
     * 字段 `applicationCode` 表示 `ServicePrincipalPO` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `ServicePrincipalPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `ServicePrincipalPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `ServicePrincipalPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_code", nullable = false, length = 128)
    private String applicationCode;

    /**
     * 字段 `displayName` 表示 `ServicePrincipalPO` 中与 `display Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `displayName` stores the `display Name`-related state, dependency, configuration, or result of `ServicePrincipalPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `displayName` 时应保持 `ServicePrincipalPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `displayName`, preserve `ServicePrincipalPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    /**
     * 字段 `status` 表示 `ServicePrincipalPO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `ServicePrincipalStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `ServicePrincipalPO` (declared type `ServicePrincipalStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `ServicePrincipalPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `ServicePrincipalPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ServicePrincipalStatusEnum status;

    /**
     * 字段 `allowedEnvironments` 表示 `ServicePrincipalPO` 中与 `allowed Environments` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `allowedEnvironments` stores the `allowed Environments`-related state, dependency, configuration, or result of `ServicePrincipalPO` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `allowedEnvironments` 时应保持 `ServicePrincipalPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `allowedEnvironments`, preserve `ServicePrincipalPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_envs", nullable = false, columnDefinition = "jsonb")
    private List<String> allowedEnvironments;

    /**
     * 字段 `allowedNamespaces` 表示 `ServicePrincipalPO` 中与 `allowed Namespaces` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `allowedNamespaces` stores the `allowed Namespaces`-related state, dependency, configuration, or result of `ServicePrincipalPO` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `allowedNamespaces` 时应保持 `ServicePrincipalPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `allowedNamespaces`, preserve `ServicePrincipalPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_namespaces", nullable = false, columnDefinition = "jsonb")
    private List<String> allowedNamespaces;

    /**
     * 构造器 `ServicePrincipalPO` 用于创建并初始化 `ServicePrincipalPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ServicePrincipalPO` creates and initializes `ServicePrincipalPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ServicePrincipalPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ServicePrincipalPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ServicePrincipalPO() {
    }

    /**
     * 构造器 `ServicePrincipalPO` 用于创建并初始化 `ServicePrincipalPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ServicePrincipalPO` creates and initializes `ServicePrincipalPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ServicePrincipalPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ServicePrincipalPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param serviceCode 输入参数 `serviceCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param displayName 输入参数 `displayName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param allowedEnvironments 输入参数 `allowedEnvironments`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param allowedNamespaces 输入参数 `allowedNamespaces`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ServicePrincipalPO(
            Long id,
            Long tenantId,
            String serviceCode,
            String applicationCode,
            String displayName,
            List<String> allowedEnvironments,
            List<String> allowedNamespaces,
            String actorId,
            Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.serviceCode = required(serviceCode, "serviceCode");
        this.applicationCode = required(applicationCode, "applicationCode");
        this.displayName = required(displayName, "displayName");
        this.status = ServicePrincipalStatusEnum.ACTIVE;
        this.allowedEnvironments = List.copyOf(allowedEnvironments);
        this.allowedNamespaces = List.copyOf(allowedNamespaces);
        markCreated(actorId, now);
    }

    /**
     * 方法 `required` 按照 `ServicePrincipalPO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `ServicePrincipalPO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
