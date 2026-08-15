package top.egon.cola.platform.rbac3.admin.iam.policy.domain.vo;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
     * 类型 `FieldRuleVO` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Field Rule View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `FieldRuleVO` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Field Rule View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `FieldRuleVO` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FieldRuleVO` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param ruleId 记录组件 `ruleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ruleId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param permissionId 记录组件 `permissionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionId` carries constructor data whose meaning is defined by the record contract.
     * @param fieldDefinitionId 记录组件 `fieldDefinitionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `fieldDefinitionId` carries constructor data whose meaning is defined by the record contract.
     * @param accessLevel 记录组件 `accessLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `accessLevel` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record FieldRuleVO(
            /**
             * 字段 `ruleId` 表示 `FieldRuleVO` 中与 `rule Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ruleId` stores the `rule Id`-related state, dependency, configuration, or result of `FieldRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ruleId` 时应保持 `FieldRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ruleId`, preserve `FieldRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ruleId,
            /**
             * 字段 `applicationId` 表示 `FieldRuleVO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `FieldRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `FieldRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `FieldRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleId` 表示 `FieldRuleVO` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `FieldRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `FieldRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `FieldRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `permissionId` 表示 `FieldRuleVO` 中与 `permission Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionId` stores the `permission Id`-related state, dependency, configuration, or result of `FieldRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionId` 时应保持 `FieldRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionId`, preserve `FieldRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionId,
            /**
             * 字段 `fieldDefinitionId` 表示 `FieldRuleVO` 中与 `field Definition Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `fieldDefinitionId` stores the `field Definition Id`-related state, dependency, configuration, or result of `FieldRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `fieldDefinitionId` 时应保持 `FieldRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `fieldDefinitionId`, preserve `FieldRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String fieldDefinitionId,
            /**
             * 字段 `accessLevel` 表示 `FieldRuleVO` 中与 `access Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `accessLevel` stores the `access Level`-related state, dependency, configuration, or result of `FieldRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `accessLevel` 时应保持 `FieldRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `accessLevel`, preserve `FieldRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String accessLevel,
            /**
             * 字段 `status` 表示 `FieldRuleVO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `FieldRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `FieldRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `FieldRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `version` 表示 `FieldRuleVO` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `FieldRuleVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `FieldRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `FieldRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version
    ) {
    }
