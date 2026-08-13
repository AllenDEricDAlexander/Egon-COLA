package top.egon.cola.platform.rbac3.admin.integration.ddc;

import top.egon.cola.component.ddc.annotation.DdcValue;

/**
 * 类型 `Rbac3DdcValueDeclarations` 位于当前包内，是类型，用于承载 `Rbac3 Ddc Value Declarations` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3DdcValueDeclarations` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Ddc Value Declarations`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Declares the RBAC3 configuration catalog reported to DDC.
 */
public final class Rbac3DdcValueDeclarations {

    /**
     * 字段 `accessTokenTtlSeconds` 表示 `Rbac3DdcValueDeclarations` 中与 `access Token Ttl Seconds` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `accessTokenTtlSeconds` stores the `access Token Ttl Seconds`-related state, dependency, configuration, or result of `Rbac3DdcValueDeclarations` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `accessTokenTtlSeconds` 时应保持 `Rbac3DdcValueDeclarations` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `accessTokenTtlSeconds`, preserve `Rbac3DdcValueDeclarations`'s lifecycle, immutability, and thread-safety constraints.
     */
    @DdcValue(
            value = "${rbac3.access-token-ttl-seconds:900}",
            refreshable = false)
    private Long accessTokenTtlSeconds = 900L;

    /**
     * 字段 `refreshTokenTtlSeconds` 表示 `Rbac3DdcValueDeclarations` 中与 `refresh Token Ttl Seconds` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `refreshTokenTtlSeconds` stores the `refresh Token Ttl Seconds`-related state, dependency, configuration, or result of `Rbac3DdcValueDeclarations` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `refreshTokenTtlSeconds` 时应保持 `Rbac3DdcValueDeclarations` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `refreshTokenTtlSeconds`, preserve `Rbac3DdcValueDeclarations`'s lifecycle, immutability, and thread-safety constraints.
     */
    @DdcValue(
            value = "${rbac3.refresh-token-ttl-seconds:604800}",
            refreshable = false)
    private Long refreshTokenTtlSeconds = 604_800L;

    /**
     * 字段 `sessionIdleTimeoutSeconds` 表示 `Rbac3DdcValueDeclarations` 中与 `session Idle Timeout Seconds` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionIdleTimeoutSeconds` stores the `session Idle Timeout Seconds`-related state, dependency, configuration, or result of `Rbac3DdcValueDeclarations` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionIdleTimeoutSeconds` 时应保持 `Rbac3DdcValueDeclarations` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionIdleTimeoutSeconds`, preserve `Rbac3DdcValueDeclarations`'s lifecycle, immutability, and thread-safety constraints.
     */
    @DdcValue(
            value = "${rbac3.session-idle-timeout-seconds:1800}",
            refreshable = false)
    private Long sessionIdleTimeoutSeconds = 1_800L;

    /**
     * 字段 `sessionAbsoluteTimeoutSeconds` 表示 `Rbac3DdcValueDeclarations` 中与 `session Absolute Timeout Seconds` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionAbsoluteTimeoutSeconds` stores the `session Absolute Timeout Seconds`-related state, dependency, configuration, or result of `Rbac3DdcValueDeclarations` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionAbsoluteTimeoutSeconds` 时应保持 `Rbac3DdcValueDeclarations` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionAbsoluteTimeoutSeconds`, preserve `Rbac3DdcValueDeclarations`'s lifecycle, immutability, and thread-safety constraints.
     */
    @DdcValue(
            value = "${rbac3.session-absolute-timeout-seconds:43200}",
            refreshable = false)
    private Long sessionAbsoluteTimeoutSeconds = 43_200L;

    /**
     * 字段 `maximumActiveRoots` 表示 `Rbac3DdcValueDeclarations` 中与 `maximum Active Roots` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `maximumActiveRoots` stores the `maximum Active Roots`-related state, dependency, configuration, or result of `Rbac3DdcValueDeclarations` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `maximumActiveRoots` 时应保持 `Rbac3DdcValueDeclarations` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `maximumActiveRoots`, preserve `Rbac3DdcValueDeclarations`'s lifecycle, immutability, and thread-safety constraints.
     */
    @DdcValue(
            value = "${rbac3.maximum-active-roots:16}",
            refreshable = false)
    private Integer maximumActiveRoots = 16;
}
