package top.egon.cola.platform.rbac3.admin.runtime.domain.vo;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationFenceService;

/**
     * 类型 `AuthorizationFenceVO` 位于 `AuthorizationFenceService` 内，是记录类型，用于承载 `AuthorizationFenceVO` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuthorizationFenceVO` is a record inside `AuthorizationFenceService` and carries the responsibility, state, or contract for `AuthorizationFenceVO`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuthorizationFenceVO` 作为 `AuthorizationFenceService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationFenceVO` as the responsibility boundary of `AuthorizationFenceService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param scopeId 记录组件 `scopeId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeId` carries constructor data whose meaning is defined by the record contract.
     * @param mutationId 记录组件 `mutationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `mutationId` carries constructor data whose meaning is defined by the record contract.
     * @param createdAt 记录组件 `createdAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `createdAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record AuthorizationFenceVO(
            /**
             * 字段 `tenantId` 表示 `AuthorizationFenceVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AuthorizationFenceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AuthorizationFenceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AuthorizationFenceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `scopeType` 表示 `AuthorizationFenceVO` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `AuthorizationFenceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `AuthorizationFenceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `AuthorizationFenceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeType,
            /**
             * 字段 `scopeId` 表示 `AuthorizationFenceVO` 中与 `scope Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeId` stores the `scope Id`-related state, dependency, configuration, or result of `AuthorizationFenceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeId` 时应保持 `AuthorizationFenceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeId`, preserve `AuthorizationFenceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeId,
            /**
             * 字段 `mutationId` 表示 `AuthorizationFenceVO` 中与 `mutation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `mutationId` stores the `mutation Id`-related state, dependency, configuration, or result of `AuthorizationFenceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `mutationId` 时应保持 `AuthorizationFenceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `mutationId`, preserve `AuthorizationFenceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String mutationId,
            /**
             * 字段 `createdAt` 表示 `AuthorizationFenceVO` 中与 `created At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `createdAt` stores the `created At`-related state, dependency, configuration, or result of `AuthorizationFenceVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `createdAt` 时应保持 `AuthorizationFenceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `createdAt`, preserve `AuthorizationFenceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant createdAt
    ) {
    }
