package top.egon.cola.platform.rbac3.admin.iam.role.assignment.domain.vo;

import top.egon.cola.platform.rbac3.core.constraint.PrerequisiteRoleSpecification;
import top.egon.cola.platform.rbac3.core.constraint.RoleCardinalitySpecification;
import top.egon.cola.platform.rbac3.core.constraint.SsdSpecification;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import top.egon.cola.platform.rbac3.core.rule.RuleResult;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
     * 类型 `AssignmentVO` 位于 `AssignmentFacade` 内，是记录类型，用于承载 `Assignment View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignmentVO` is a record inside `AssignmentFacade` and carries the responsibility, state, or contract for `Assignment View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignmentVO` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignmentVO` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param assignmentId 记录组件 `assignmentId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `assignmentId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param assignmentType 记录组件 `assignmentType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `assignmentType` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param sourceType 记录组件 `sourceType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sourceType` carries constructor data whose meaning is defined by the record contract.
     * @param sourceId 记录组件 `sourceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sourceId` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record AssignmentVO(
            /**
             * 字段 `assignmentId` 表示 `AssignmentVO` 中与 `assignment Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `assignmentId` stores the `assignment Id`-related state, dependency, configuration, or result of `AssignmentVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `assignmentId` 时应保持 `AssignmentVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `assignmentId`, preserve `AssignmentVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String assignmentId,
            /**
             * 字段 `roleId` 表示 `AssignmentVO` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `AssignmentVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `AssignmentVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `AssignmentVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `assignmentType` 表示 `AssignmentVO` 中与 `assignment Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `assignmentType` stores the `assignment Type`-related state, dependency, configuration, or result of `AssignmentVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `assignmentType` 时应保持 `AssignmentVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `assignmentType`, preserve `AssignmentVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String assignmentType,
            /**
             * 字段 `status` 表示 `AssignmentVO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `AssignmentVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `AssignmentVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `AssignmentVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `validFrom` 表示 `AssignmentVO` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `AssignmentVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `AssignmentVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `AssignmentVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `AssignmentVO` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `AssignmentVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `AssignmentVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `AssignmentVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `sourceType` 表示 `AssignmentVO` 中与 `source Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sourceType` stores the `source Type`-related state, dependency, configuration, or result of `AssignmentVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sourceType` 时应保持 `AssignmentVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sourceType`, preserve `AssignmentVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sourceType,
            /**
             * 字段 `sourceId` 表示 `AssignmentVO` 中与 `source Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sourceId` stores the `source Id`-related state, dependency, configuration, or result of `AssignmentVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sourceId` 时应保持 `AssignmentVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sourceId`, preserve `AssignmentVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sourceId,
            /**
             * 字段 `version` 表示 `AssignmentVO` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `AssignmentVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `AssignmentVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `AssignmentVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version
    ) {
    }
