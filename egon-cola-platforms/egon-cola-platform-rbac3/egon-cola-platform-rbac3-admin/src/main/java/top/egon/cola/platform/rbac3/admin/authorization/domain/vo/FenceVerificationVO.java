package top.egon.cola.platform.rbac3.admin.authorization.domain.vo;

import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationDecision;
import top.egon.cola.platform.rbac3.contract.authorization.DataScopeDecision;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.authorization.service.AuthorizationDecisionService;

/**
     * 会话授权传播 Fence 校验结果。
     * Session authorization propagation-fence verification result.
     *
     * @param decision Fence 判定 / fence decision
     * @param reasonCode 稳定原因码 / stable reason code
     * @param sessionId 会话标识 / session identifier
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
             * 字段 `sessionId` 表示 `FenceVerificationVO` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `FenceVerificationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `FenceVerificationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `FenceVerificationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `verifiedAt` 表示 `FenceVerificationVO` 中与 `verified At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `verifiedAt` stores the `verified At`-related state, dependency, configuration, or result of `FenceVerificationVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `verifiedAt` 时应保持 `FenceVerificationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `verifiedAt`, preserve `FenceVerificationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant verifiedAt) {
    }
