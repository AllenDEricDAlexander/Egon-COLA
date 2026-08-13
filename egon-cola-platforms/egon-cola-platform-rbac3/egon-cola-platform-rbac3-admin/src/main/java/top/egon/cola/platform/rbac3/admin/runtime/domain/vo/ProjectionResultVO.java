package top.egon.cola.platform.rbac3.admin.runtime.domain.vo;



/**
     * 类型 `ProjectionResultVO` 位于 `RuntimeProjectionPort` 内，是记录类型，用于承载 `Projection Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ProjectionResultVO` is a record inside `RuntimeProjectionPort` and carries the responsibility, state, or contract for `Projection Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ProjectionResultVO` 作为 `RuntimeProjectionPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ProjectionResultVO` as the responsibility boundary of `RuntimeProjectionPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param published 记录组件 `published` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `published` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     */
    public record ProjectionResultVO(/**
 * 字段 `published` 表示 `ProjectionResultVO` 中与 `published` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `published` stores the `published`-related state, dependency, configuration, or result of `ProjectionResultVO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `published` 时应保持 `ProjectionResultVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `published`, preserve `ProjectionResultVO`'s lifecycle, immutability, and thread-safety constraints.
 */ boolean published, /**
 * 字段 `reasonCode` 表示 `ProjectionResultVO` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `ProjectionResultVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `ProjectionResultVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `ProjectionResultVO`'s lifecycle, immutability, and thread-safety constraints.
 */ String reasonCode) {
    }
