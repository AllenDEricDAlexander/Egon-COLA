package top.egon.cola.platform.rbac3.admin.audit.domain.dto;

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
     * 类型 `AuditCommandDTO` 位于 `AuditQueryService` 内，是记录类型，用于承载 `Audit Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuditCommandDTO` is a record inside `AuditQueryService` and carries the responsibility, state, or contract for `Audit Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuditCommandDTO` 作为 `AuditQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuditCommandDTO` as the responsibility boundary of `AuditQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
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
     * @param occurredAt 记录组件 `occurredAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `occurredAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record AuditCommandDTO(
            /**
             * 字段 `tenantId` 表示 `AuditCommandDTO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `eventType` 表示 `AuditCommandDTO` 中与 `event Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `eventType` stores the `event Type`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `eventType` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `eventType`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String eventType,
            /**
             * 字段 `outcome` 表示 `AuditCommandDTO` 中与 `outcome` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `outcome` stores the `outcome`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `outcome` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `outcome`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String outcome,
            /**
             * 字段 `severity` 表示 `AuditCommandDTO` 中与 `severity` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `severity` stores the `severity`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `severity` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `severity`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String severity,
            /**
             * 字段 `actorType` 表示 `AuditCommandDTO` 中与 `actor Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorType` stores the `actor Type`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorType` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorType`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorType,
            /**
             * 字段 `actorId` 表示 `AuditCommandDTO` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `targetType` 表示 `AuditCommandDTO` 中与 `target Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetType` stores the `target Type`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetType` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetType`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetType,
            /**
             * 字段 `targetId` 表示 `AuditCommandDTO` 中与 `target Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetId` stores the `target Id`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetId` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetId`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetId,
            /**
             * 字段 `managementPolicyId` 表示 `AuditCommandDTO` 中与 `management Policy Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `managementPolicyId` stores the `management Policy Id`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `managementPolicyId` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `managementPolicyId`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String managementPolicyId,
            /**
             * 字段 `reasonCode` 表示 `AuditCommandDTO` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `requestId` 表示 `AuditCommandDTO` 中与 `request Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestId` stores the `request Id`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestId` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestId`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requestId,
            /**
             * 字段 `traceId` 表示 `AuditCommandDTO` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId,
            /**
             * 字段 `beforeSnapshot` 表示 `AuditCommandDTO` 中与 `before Snapshot` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, ?&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `beforeSnapshot` stores the `before Snapshot`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `Map&lt;String, ?&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `beforeSnapshot` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `beforeSnapshot`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, ?> beforeSnapshot,
            /**
             * 字段 `afterSnapshot` 表示 `AuditCommandDTO` 中与 `after Snapshot` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, ?&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `afterSnapshot` stores the `after Snapshot`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `Map&lt;String, ?&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `afterSnapshot` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `afterSnapshot`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, ?> afterSnapshot,
            /**
             * 字段 `occurredAt` 表示 `AuditCommandDTO` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `AuditCommandDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `AuditCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `AuditCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant occurredAt) {
        /**
         * 构造器 `AuditCommandDTO` 用于创建并初始化 `AuditCommandDTO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AuditCommandDTO` creates and initializes `AuditCommandDTO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AuditCommandDTO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuditCommandDTO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param eventType 输入参数 `eventType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param outcome 输入参数 `outcome`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param severity 输入参数 `severity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorType 输入参数 `actorType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param targetType 输入参数 `targetType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param targetId 输入参数 `targetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param managementPolicyId 输入参数 `managementPolicyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requestId 输入参数 `requestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param beforeSnapshot 输入参数 `beforeSnapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param afterSnapshot 输入参数 `afterSnapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param occurredAt 输入参数 `occurredAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AuditCommandDTO {
            tenantId = required(tenantId, "tenantId");
            eventType = required(eventType, "eventType");
            outcome = required(outcome, "outcome");
            severity = required(severity, "severity");
            actorType = required(actorType, "actorType");
            actorId = required(actorId, "actorId");
            requestId = required(requestId, "requestId");
            traceId = required(traceId, "traceId");
            beforeSnapshot = Map.copyOf(Objects.requireNonNull(
                    beforeSnapshot, "beforeSnapshot"));
            afterSnapshot = Map.copyOf(Objects.requireNonNull(
                    afterSnapshot, "afterSnapshot"));
            occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        }

        /** 校验必填文本。 / Validates required text. */
        private static String required(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " is required");
            }
            return value.trim();
        }
    }
