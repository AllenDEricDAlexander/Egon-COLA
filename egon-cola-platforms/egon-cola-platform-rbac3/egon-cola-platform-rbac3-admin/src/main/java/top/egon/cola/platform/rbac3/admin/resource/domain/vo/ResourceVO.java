package top.egon.cola.platform.rbac3.admin.resource.domain.vo;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.resource.service.ApplicationResourceFacade;

/**
     * 类型 `ResourceVO` 位于 `ApplicationResourceFacade` 内，是记录类型，用于承载 `Resource View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ResourceVO` is a record inside `ApplicationResourceFacade` and carries the responsibility, state, or contract for `Resource View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ResourceVO` 作为 `ApplicationResourceFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResourceVO` as the responsibility boundary of `ApplicationResourceFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param resourceId 记录组件 `resourceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `resourceId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param resourceType 记录组件 `resourceType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `resourceType` carries constructor data whose meaning is defined by the record contract.
     * @param resourceCode 记录组件 `resourceCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `resourceCode` carries constructor data whose meaning is defined by the record contract.
     * @param resourceName 记录组件 `resourceName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `resourceName` carries constructor data whose meaning is defined by the record contract.
     * @param parentResourceId 记录组件 `parentResourceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `parentResourceId` carries constructor data whose meaning is defined by the record contract.
     * @param requiredPermissionId 记录组件 `requiredPermissionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requiredPermissionId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record ResourceVO(
            /**
             * 字段 `resourceId` 表示 `ResourceVO` 中与 `resource Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resourceId` stores the `resource Id`-related state, dependency, configuration, or result of `ResourceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resourceId` 时应保持 `ResourceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resourceId`, preserve `ResourceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String resourceId,
            /**
             * 字段 `applicationId` 表示 `ResourceVO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `ResourceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `ResourceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `ResourceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `resourceType` 表示 `ResourceVO` 中与 `resource Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resourceType` stores the `resource Type`-related state, dependency, configuration, or result of `ResourceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resourceType` 时应保持 `ResourceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resourceType`, preserve `ResourceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String resourceType,
            /**
             * 字段 `resourceCode` 表示 `ResourceVO` 中与 `resource Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resourceCode` stores the `resource Code`-related state, dependency, configuration, or result of `ResourceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resourceCode` 时应保持 `ResourceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resourceCode`, preserve `ResourceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String resourceCode,
            /**
             * 字段 `resourceName` 表示 `ResourceVO` 中与 `resource Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resourceName` stores the `resource Name`-related state, dependency, configuration, or result of `ResourceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resourceName` 时应保持 `ResourceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resourceName`, preserve `ResourceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String resourceName,
            /**
             * 字段 `parentResourceId` 表示 `ResourceVO` 中与 `parent Resource Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `parentResourceId` stores the `parent Resource Id`-related state, dependency, configuration, or result of `ResourceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `parentResourceId` 时应保持 `ResourceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `parentResourceId`, preserve `ResourceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String parentResourceId,
            /**
             * 字段 `requiredPermissionId` 表示 `ResourceVO` 中与 `required Permission Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requiredPermissionId` stores the `required Permission Id`-related state, dependency, configuration, or result of `ResourceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requiredPermissionId` 时应保持 `ResourceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requiredPermissionId`, preserve `ResourceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requiredPermissionId,
            /**
             * 字段 `status` 表示 `ResourceVO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `ResourceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `ResourceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `ResourceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `version` 表示 `ResourceVO` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `ResourceVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `ResourceVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `ResourceVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version) {
    }
