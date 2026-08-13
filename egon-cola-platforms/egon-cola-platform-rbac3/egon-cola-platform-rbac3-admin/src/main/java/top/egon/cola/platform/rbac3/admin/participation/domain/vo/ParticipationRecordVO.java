package top.egon.cola.platform.rbac3.admin.participation.domain.vo;

import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.participation.BusinessParticipationCommand;
import top.egon.cola.platform.rbac3.core.participation.OperationSodSpecification;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import top.egon.cola.platform.rbac3.admin.participation.service.ParticipationFacade;

/**
     * 类型 `ParticipationRecordVO` 位于 `ParticipationFacade` 内，是记录类型，用于承载 `Participation Record` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ParticipationRecordVO` is a record inside `ParticipationFacade` and carries the responsibility, state, or contract for `Participation Record`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ParticipationRecordVO` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ParticipationRecordVO` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationCode 记录组件 `applicationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessResource 记录组件 `businessResource` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessResource` carries constructor data whose meaning is defined by the record contract.
     * @param businessId 记录组件 `businessId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessId` carries constructor data whose meaning is defined by the record contract.
     * @param actorUserId 记录组件 `actorUserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorUserId` carries constructor data whose meaning is defined by the record contract.
     * @param actionCode 记录组件 `actionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actionCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessEventId 记录组件 `businessEventId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessEventId` carries constructor data whose meaning is defined by the record contract.
     * @param occurredAt 记录组件 `occurredAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `occurredAt` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     * @param payloadDigest 记录组件 `payloadDigest` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `payloadDigest` carries constructor data whose meaning is defined by the record contract.
     */
    public record ParticipationRecordVO(
            /**
             * 字段 `tenantId` 表示 `ParticipationRecordVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ParticipationRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ParticipationRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ParticipationRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationCode` 表示 `ParticipationRecordVO` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `ParticipationRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `ParticipationRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `ParticipationRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationCode,
            /**
             * 字段 `businessResource` 表示 `ParticipationRecordVO` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `ParticipationRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `ParticipationRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `ParticipationRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessResource,
            /**
             * 字段 `businessId` 表示 `ParticipationRecordVO` 中与 `business Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessId` stores the `business Id`-related state, dependency, configuration, or result of `ParticipationRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessId` 时应保持 `ParticipationRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessId`, preserve `ParticipationRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessId,
            /**
             * 字段 `actorUserId` 表示 `ParticipationRecordVO` 中与 `actor User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorUserId` stores the `actor User Id`-related state, dependency, configuration, or result of `ParticipationRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorUserId` 时应保持 `ParticipationRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorUserId`, preserve `ParticipationRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorUserId,
            /**
             * 字段 `actionCode` 表示 `ParticipationRecordVO` 中与 `action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actionCode` stores the `action Code`-related state, dependency, configuration, or result of `ParticipationRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actionCode` 时应保持 `ParticipationRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actionCode`, preserve `ParticipationRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actionCode,
            /**
             * 字段 `businessEventId` 表示 `ParticipationRecordVO` 中与 `business Event Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessEventId` stores the `business Event Id`-related state, dependency, configuration, or result of `ParticipationRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessEventId` 时应保持 `ParticipationRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessEventId`, preserve `ParticipationRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessEventId,
            /**
             * 字段 `occurredAt` 表示 `ParticipationRecordVO` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `ParticipationRecordVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `ParticipationRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `ParticipationRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant occurredAt,
            /**
             * 字段 `traceId` 表示 `ParticipationRecordVO` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `ParticipationRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `ParticipationRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `ParticipationRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId,
            /**
             * 字段 `payloadDigest` 表示 `ParticipationRecordVO` 中与 `payload Digest` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `payloadDigest` stores the `payload Digest`-related state, dependency, configuration, or result of `ParticipationRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `payloadDigest` 时应保持 `ParticipationRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `payloadDigest`, preserve `ParticipationRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String payloadDigest) {
    }
