package top.egon.cola.platform.rbac3.admin.application;

import java.time.Instant;

/**
 * 类型 `CommandContext` 位于当前包内，是记录类型，用于承载 `Command Context` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `CommandContext` is a record in its package and carries the responsibility, state, or contract for `Command Context`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `CommandContext` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `CommandContext` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 *
 * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
 * @param operatorUserId 记录组件 `operatorUserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operatorUserId` carries constructor data whose meaning is defined by the record contract.
 * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
 * @param requestId 记录组件 `requestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestId` carries constructor data whose meaning is defined by the record contract.
 * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
 * @param databaseNow 记录组件 `databaseNow` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `databaseNow` carries constructor data whose meaning is defined by the record contract.
 */
public record CommandContext(
        /**
         * 字段 `tenantId` 表示 `CommandContext` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `CommandContext` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `CommandContext` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `CommandContext`'s lifecycle, immutability, and thread-safety constraints.
         */
        String tenantId,
        /**
         * 字段 `operatorUserId` 表示 `CommandContext` 中与 `operator User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `operatorUserId` stores the `operator User Id`-related state, dependency, configuration, or result of `CommandContext` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `operatorUserId` 时应保持 `CommandContext` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `operatorUserId`, preserve `CommandContext`'s lifecycle, immutability, and thread-safety constraints.
         */
        String operatorUserId,
        /**
         * 字段 `sessionId` 表示 `CommandContext` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `CommandContext` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `CommandContext` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `CommandContext`'s lifecycle, immutability, and thread-safety constraints.
         */
        String sessionId,
        /**
         * 字段 `requestId` 表示 `CommandContext` 中与 `request Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `requestId` stores the `request Id`-related state, dependency, configuration, or result of `CommandContext` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `requestId` 时应保持 `CommandContext` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `requestId`, preserve `CommandContext`'s lifecycle, immutability, and thread-safety constraints.
         */
        String requestId,
        /**
         * 字段 `traceId` 表示 `CommandContext` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `CommandContext` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `traceId` 时应保持 `CommandContext` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `traceId`, preserve `CommandContext`'s lifecycle, immutability, and thread-safety constraints.
         */
        String traceId,
        /**
         * 字段 `databaseNow` 表示 `CommandContext` 中与 `database Now` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `databaseNow` stores the `database Now`-related state, dependency, configuration, or result of `CommandContext` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `databaseNow` 时应保持 `CommandContext` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `databaseNow`, preserve `CommandContext`'s lifecycle, immutability, and thread-safety constraints.
         */
        Instant databaseNow
) {
}
