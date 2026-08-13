package top.egon.cola.platform.rbac3.admin.audit.domain.vo;

import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
     * 类型 `AuditVO` 位于 `AuditQueryService` 内，是记录类型，用于承载 `Audit View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuditVO` is a record inside `AuditQueryService` and carries the responsibility, state, or contract for `Audit View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuditVO` 作为 `AuditQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuditVO` as the responsibility boundary of `AuditQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param id 记录组件 `id` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `id` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param eventType 记录组件 `eventType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `eventType` carries constructor data whose meaning is defined by the record contract.
     * @param outcome 记录组件 `outcome` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `outcome` carries constructor data whose meaning is defined by the record contract.
     * @param severity 记录组件 `severity` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `severity` carries constructor data whose meaning is defined by the record contract.
     * @param actorType 记录组件 `actorType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorType` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     * @param targetType 记录组件 `targetType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetType` carries constructor data whose meaning is defined by the record contract.
     * @param targetId 记录组件 `targetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetId` carries constructor data whose meaning is defined by the record contract.
     * @param managementPolicyId 记录组件 `managementPolicyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `managementPolicyId` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     * @param requestId 记录组件 `requestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestId` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     * @param beforeSnapshot 记录组件 `beforeSnapshot` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `beforeSnapshot` carries constructor data whose meaning is defined by the record contract.
     * @param afterSnapshot 记录组件 `afterSnapshot` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `afterSnapshot` carries constructor data whose meaning is defined by the record contract.
     * @param payloadChecksum 记录组件 `payloadChecksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `payloadChecksum` carries constructor data whose meaning is defined by the record contract.
     * @param createdAt 记录组件 `createdAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `createdAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record AuditVO(
            /**
             * 字段 `id` 表示 `AuditVO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `id` stores the `id`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `id` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `id`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String id,
            /**
             * 字段 `tenantId` 表示 `AuditVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `eventType` 表示 `AuditVO` 中与 `event Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `eventType` stores the `event Type`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `eventType` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `eventType`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String eventType,
            /**
             * 字段 `outcome` 表示 `AuditVO` 中与 `outcome` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `outcome` stores the `outcome`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `outcome` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `outcome`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String outcome,
            /**
             * 字段 `severity` 表示 `AuditVO` 中与 `severity` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `severity` stores the `severity`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `severity` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `severity`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String severity,
            /**
             * 字段 `actorType` 表示 `AuditVO` 中与 `actor Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorType` stores the `actor Type`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorType` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorType`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorType,
            /**
             * 字段 `actorId` 表示 `AuditVO` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `targetType` 表示 `AuditVO` 中与 `target Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetType` stores the `target Type`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetType` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetType`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetType,
            /**
             * 字段 `targetId` 表示 `AuditVO` 中与 `target Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetId` stores the `target Id`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetId` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetId`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetId,
            /**
             * 字段 `managementPolicyId` 表示 `AuditVO` 中与 `management Policy Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `managementPolicyId` stores the `management Policy Id`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `managementPolicyId` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `managementPolicyId`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String managementPolicyId,
            /**
             * 字段 `reasonCode` 表示 `AuditVO` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `requestId` 表示 `AuditVO` 中与 `request Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestId` stores the `request Id`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestId` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestId`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requestId,
            /**
             * 字段 `traceId` 表示 `AuditVO` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId,
            /**
             * 字段 `beforeSnapshot` 表示 `AuditVO` 中与 `before Snapshot` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `beforeSnapshot` stores the `before Snapshot`-related state, dependency, configuration, or result of `AuditVO` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `beforeSnapshot` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `beforeSnapshot`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @GatewaySchemaField(allowArbitraryJson = true)
            Map<String, Object> beforeSnapshot,
            /**
             * 字段 `afterSnapshot` 表示 `AuditVO` 中与 `after Snapshot` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `afterSnapshot` stores the `after Snapshot`-related state, dependency, configuration, or result of `AuditVO` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `afterSnapshot` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `afterSnapshot`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @GatewaySchemaField(allowArbitraryJson = true)
            Map<String, Object> afterSnapshot,
            /**
             * 字段 `payloadChecksum` 表示 `AuditVO` 中与 `payload Checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `payloadChecksum` stores the `payload Checksum`-related state, dependency, configuration, or result of `AuditVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `payloadChecksum` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `payloadChecksum`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String payloadChecksum,
            /**
             * 字段 `createdAt` 表示 `AuditVO` 中与 `created At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `createdAt` stores the `created At`-related state, dependency, configuration, or result of `AuditVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `createdAt` 时应保持 `AuditVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `createdAt`, preserve `AuditVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant createdAt) {
    }
