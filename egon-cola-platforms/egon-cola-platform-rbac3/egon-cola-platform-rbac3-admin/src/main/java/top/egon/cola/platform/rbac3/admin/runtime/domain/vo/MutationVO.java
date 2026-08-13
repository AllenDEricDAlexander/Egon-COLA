package top.egon.cola.platform.rbac3.admin.runtime.domain.vo;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
     * 类型 `MutationVO` 位于 `RuntimeQueryService` 内，是记录类型，用于承载 `Mutation View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `MutationVO` is a record inside `RuntimeQueryService` and carries the responsibility, state, or contract for `Mutation View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `MutationVO` 作为 `RuntimeQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `MutationVO` as the responsibility boundary of `RuntimeQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param mutationId 记录组件 `mutationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `mutationId` carries constructor data whose meaning is defined by the record contract.
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param scopeId 记录组件 `scopeId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeId` carries constructor data whose meaning is defined by the record contract.
     * @param commandId 记录组件 `commandId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `commandId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param attempt 记录组件 `attempt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `attempt` carries constructor data whose meaning is defined by the record contract.
     * @param lastErrorCode 记录组件 `lastErrorCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `lastErrorCode` carries constructor data whose meaning is defined by the record contract.
     * @param updatedAt 记录组件 `updatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `updatedAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record MutationVO(
            /**
             * 字段 `mutationId` 表示 `MutationVO` 中与 `mutation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `mutationId` stores the `mutation Id`-related state, dependency, configuration, or result of `MutationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `mutationId` 时应保持 `MutationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `mutationId`, preserve `MutationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String mutationId,
            /**
             * 字段 `scopeType` 表示 `MutationVO` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `MutationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `MutationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `MutationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeType,
            /**
             * 字段 `scopeId` 表示 `MutationVO` 中与 `scope Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeId` stores the `scope Id`-related state, dependency, configuration, or result of `MutationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeId` 时应保持 `MutationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeId`, preserve `MutationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeId,
            /**
             * 字段 `commandId` 表示 `MutationVO` 中与 `command Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `commandId` stores the `command Id`-related state, dependency, configuration, or result of `MutationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `commandId` 时应保持 `MutationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `commandId`, preserve `MutationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String commandId,
            /**
             * 字段 `status` 表示 `MutationVO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `MutationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `MutationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `MutationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `attempt` 表示 `MutationVO` 中与 `attempt` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `attempt` stores the `attempt`-related state, dependency, configuration, or result of `MutationVO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `attempt` 时应保持 `MutationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `attempt`, preserve `MutationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            int attempt,
            /**
             * 字段 `lastErrorCode` 表示 `MutationVO` 中与 `last Error Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `lastErrorCode` stores the `last Error Code`-related state, dependency, configuration, or result of `MutationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `lastErrorCode` 时应保持 `MutationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `lastErrorCode`, preserve `MutationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String lastErrorCode,
            /**
             * 字段 `updatedAt` 表示 `MutationVO` 中与 `updated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `updatedAt` stores the `updated At`-related state, dependency, configuration, or result of `MutationVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `updatedAt` 时应保持 `MutationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `updatedAt`, preserve `MutationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant updatedAt) {
    }
