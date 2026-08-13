package top.egon.cola.platform.rbac3.admin.constraint.domain.vo;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
     * 类型 `OperationSodRuleVO` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Operation Sod Rule View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `OperationSodRuleVO` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Operation Sod Rule View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `OperationSodRuleVO` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `OperationSodRuleVO` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param ruleId 记录组件 `ruleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ruleId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationCode 记录组件 `applicationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessResource 记录组件 `businessResource` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessResource` carries constructor data whose meaning is defined by the record contract.
     * @param priorActionCode 记录组件 `priorActionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `priorActionCode` carries constructor data whose meaning is defined by the record contract.
     * @param forbiddenLaterActionCode 记录组件 `forbiddenLaterActionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `forbiddenLaterActionCode` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record OperationSodRuleVO(
            /**
             * 字段 `ruleId` 表示 `OperationSodRuleVO` 中与 `rule Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ruleId` stores the `rule Id`-related state, dependency, configuration, or result of `OperationSodRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ruleId` 时应保持 `OperationSodRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ruleId`, preserve `OperationSodRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ruleId,
            /**
             * 字段 `applicationCode` 表示 `OperationSodRuleVO` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `OperationSodRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `OperationSodRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `OperationSodRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationCode,
            /**
             * 字段 `businessResource` 表示 `OperationSodRuleVO` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `OperationSodRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `OperationSodRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `OperationSodRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessResource,
            /**
             * 字段 `priorActionCode` 表示 `OperationSodRuleVO` 中与 `prior Action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `priorActionCode` stores the `prior Action Code`-related state, dependency, configuration, or result of `OperationSodRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `priorActionCode` 时应保持 `OperationSodRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `priorActionCode`, preserve `OperationSodRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String priorActionCode,
            /**
             * 字段 `forbiddenLaterActionCode` 表示 `OperationSodRuleVO` 中与 `forbidden Later Action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `forbiddenLaterActionCode` stores the `forbidden Later Action Code`-related state, dependency, configuration, or result of `OperationSodRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `forbiddenLaterActionCode` 时应保持 `OperationSodRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `forbiddenLaterActionCode`, preserve `OperationSodRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String forbiddenLaterActionCode,
            /**
             * 字段 `status` 表示 `OperationSodRuleVO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `OperationSodRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `OperationSodRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `OperationSodRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `version` 表示 `OperationSodRuleVO` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `OperationSodRuleVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `OperationSodRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `OperationSodRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version
    ) {
    }
