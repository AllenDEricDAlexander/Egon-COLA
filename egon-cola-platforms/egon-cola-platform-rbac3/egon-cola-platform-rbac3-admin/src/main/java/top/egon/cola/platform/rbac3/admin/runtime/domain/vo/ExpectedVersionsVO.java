package top.egon.cola.platform.rbac3.admin.runtime.domain.vo;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
     * 类型 `ExpectedVersionsVO` 位于 `AuthorizationMutationCoordinator` 内，是记录类型，用于承载 `Expected Versions` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ExpectedVersionsVO` is a record inside `AuthorizationMutationCoordinator` and carries the responsibility, state, or contract for `Expected Versions`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ExpectedVersionsVO` 作为 `AuthorizationMutationCoordinator` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ExpectedVersionsVO` as the responsibility boundary of `AuthorizationMutationCoordinator`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param oldSessionVersion 记录组件 `oldSessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `oldSessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param newSessionVersion 记录组件 `newSessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `newSessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param oldAuthVersion 记录组件 `oldAuthVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `oldAuthVersion` carries constructor data whose meaning is defined by the record contract.
     * @param newAuthVersion 记录组件 `newAuthVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `newAuthVersion` carries constructor data whose meaning is defined by the record contract.
     * @param oldPolicyVersion 记录组件 `oldPolicyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `oldPolicyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param newPolicyVersion 记录组件 `newPolicyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `newPolicyVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record ExpectedVersionsVO(
            /**
             * 字段 `oldSessionVersion` 表示 `ExpectedVersionsVO` 中与 `old Session Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `oldSessionVersion` stores the `old Session Version`-related state, dependency, configuration, or result of `ExpectedVersionsVO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `oldSessionVersion` 时应保持 `ExpectedVersionsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `oldSessionVersion`, preserve `ExpectedVersionsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long oldSessionVersion,
            /**
             * 字段 `newSessionVersion` 表示 `ExpectedVersionsVO` 中与 `new Session Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `newSessionVersion` stores the `new Session Version`-related state, dependency, configuration, or result of `ExpectedVersionsVO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `newSessionVersion` 时应保持 `ExpectedVersionsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `newSessionVersion`, preserve `ExpectedVersionsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long newSessionVersion,
            /**
             * 字段 `oldAuthVersion` 表示 `ExpectedVersionsVO` 中与 `old Auth Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `oldAuthVersion` stores the `old Auth Version`-related state, dependency, configuration, or result of `ExpectedVersionsVO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `oldAuthVersion` 时应保持 `ExpectedVersionsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `oldAuthVersion`, preserve `ExpectedVersionsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long oldAuthVersion,
            /**
             * 字段 `newAuthVersion` 表示 `ExpectedVersionsVO` 中与 `new Auth Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `newAuthVersion` stores the `new Auth Version`-related state, dependency, configuration, or result of `ExpectedVersionsVO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `newAuthVersion` 时应保持 `ExpectedVersionsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `newAuthVersion`, preserve `ExpectedVersionsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long newAuthVersion,
            /**
             * 字段 `oldPolicyVersion` 表示 `ExpectedVersionsVO` 中与 `old Policy Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `oldPolicyVersion` stores the `old Policy Version`-related state, dependency, configuration, or result of `ExpectedVersionsVO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `oldPolicyVersion` 时应保持 `ExpectedVersionsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `oldPolicyVersion`, preserve `ExpectedVersionsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long oldPolicyVersion,
            /**
             * 字段 `newPolicyVersion` 表示 `ExpectedVersionsVO` 中与 `new Policy Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `newPolicyVersion` stores the `new Policy Version`-related state, dependency, configuration, or result of `ExpectedVersionsVO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `newPolicyVersion` 时应保持 `ExpectedVersionsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `newPolicyVersion`, preserve `ExpectedVersionsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long newPolicyVersion
    ) {
    }
