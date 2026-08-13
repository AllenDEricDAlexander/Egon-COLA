package top.egon.cola.platform.rbac3.admin.auth.domain.vo;

import top.egon.cola.platform.rbac3.admin.session.service.RefreshTokenService;
import top.egon.cola.platform.rbac3.contract.auth.RefreshResult;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import top.egon.cola.platform.rbac3.admin.auth.service.RefreshFacade;

/**
     * 类型 `RefreshAuditVO` 位于 `RefreshFacade` 内，是记录类型，用于承载 `Refresh Audit` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RefreshAuditVO` is a record inside `RefreshFacade` and carries the responsibility, state, or contract for `Refresh Audit`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RefreshAuditVO` 作为 `RefreshFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RefreshAuditVO` as the responsibility boundary of `RefreshFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param occurredAt 记录组件 `occurredAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `occurredAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record RefreshAuditVO(
            /**
             * 字段 `tenantId` 表示 `RefreshAuditVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `RefreshAuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `RefreshAuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `RefreshAuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `RefreshAuditVO` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `RefreshAuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `RefreshAuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `RefreshAuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `RefreshAuditVO` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `RefreshAuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `RefreshAuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `RefreshAuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `sessionVersion` 表示 `RefreshAuditVO` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `RefreshAuditVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `RefreshAuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `RefreshAuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `RefreshAuditVO` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `RefreshAuditVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `RefreshAuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `RefreshAuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `occurredAt` 表示 `RefreshAuditVO` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `RefreshAuditVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `RefreshAuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `RefreshAuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant occurredAt
    ) {
    }
