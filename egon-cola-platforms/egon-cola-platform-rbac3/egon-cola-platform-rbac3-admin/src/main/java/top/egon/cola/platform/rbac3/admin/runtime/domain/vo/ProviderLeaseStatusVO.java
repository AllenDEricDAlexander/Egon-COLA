package top.egon.cola.platform.rbac3.admin.runtime.domain.vo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
     * 类型 `ProviderLeaseStatusVO` 位于 `ControlPlaneRuntimeStatusPort` 内，是记录类型，用于承载 `Provider Lease Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ProviderLeaseStatusVO` is a record inside `ControlPlaneRuntimeStatusPort` and carries the responsibility, state, or contract for `Provider Lease Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ProviderLeaseStatusVO` 作为 `ControlPlaneRuntimeStatusPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ProviderLeaseStatusVO` as the responsibility boundary of `ControlPlaneRuntimeStatusPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param state 记录组件 `state` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `state` carries constructor data whose meaning is defined by the record contract.
     * @param instanceId 记录组件 `instanceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `instanceId` carries constructor data whose meaning is defined by the record contract.
     * @param leaseExpireAt 记录组件 `leaseExpireAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `leaseExpireAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record ProviderLeaseStatusVO(
            /**
             * 字段 `state` 表示 `ProviderLeaseStatusVO` 中与 `state` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `state` stores the `state`-related state, dependency, configuration, or result of `ProviderLeaseStatusVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `state` 时应保持 `ProviderLeaseStatusVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `state`, preserve `ProviderLeaseStatusVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String state,
            /**
             * 字段 `instanceId` 表示 `ProviderLeaseStatusVO` 中与 `instance Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `instanceId` stores the `instance Id`-related state, dependency, configuration, or result of `ProviderLeaseStatusVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `instanceId` 时应保持 `ProviderLeaseStatusVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `instanceId`, preserve `ProviderLeaseStatusVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String instanceId,
            /**
             * 字段 `leaseExpireAt` 表示 `ProviderLeaseStatusVO` 中与 `lease Expire At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `leaseExpireAt` stores the `lease Expire At`-related state, dependency, configuration, or result of `ProviderLeaseStatusVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `leaseExpireAt` 时应保持 `ProviderLeaseStatusVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `leaseExpireAt`, preserve `ProviderLeaseStatusVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant leaseExpireAt) {
    }
