package top.egon.cola.platform.rbac3.admin.authorization.domain.vo;

import top.egon.cola.platform.rbac3.contract.authorization.Decision;

import java.time.Instant;

/**
 * 用户授权传播 Fence 校验结果。
 * User authorization propagation-fence verification result.
     *
     * @param decision Fence 判定 / fence decision
     * @param reasonCode 稳定原因码 / stable reason code
 * @param identitySub IdP 稳定主体标识 / stable IdP subject
     * @param verifiedAt 校验时间 / verification time
     * 语义与用法：将 `FenceVerificationVO` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FenceVerificationVO` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record FenceVerificationVO(
            /**
             * 字段 `decision` 表示 `FenceVerificationVO` 中与 `decision` 相关的状态、依赖、配置或结果（声明类型 `Decision`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `decision` stores the `decision`-related state, dependency, configuration, or result of `FenceVerificationVO` (declared type `Decision`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `decision` 时应保持 `FenceVerificationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `decision`, preserve `FenceVerificationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Decision decision,
            /**
             * 字段 `reasonCode` 表示 `FenceVerificationVO` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `FenceVerificationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `FenceVerificationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `FenceVerificationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `identitySub` 表示 IdP 稳定主体标识；Field `identitySub` stores the stable IdP subject.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `FenceVerificationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: preserve the lifecycle, immutability, and thread-safety constraints of `FenceVerificationVO` when reading or passing `identitySub`.
             */
            String identitySub,
            /**
             * 字段 `verifiedAt` 表示 `FenceVerificationVO` 中与 `verified At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `verifiedAt` stores the `verified At`-related state, dependency, configuration, or result of `FenceVerificationVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `verifiedAt` 时应保持 `FenceVerificationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `verifiedAt`, preserve `FenceVerificationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant verifiedAt) {
    }
