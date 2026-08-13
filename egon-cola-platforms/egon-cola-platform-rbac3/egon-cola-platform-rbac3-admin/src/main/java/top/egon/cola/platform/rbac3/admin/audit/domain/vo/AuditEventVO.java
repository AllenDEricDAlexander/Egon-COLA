package top.egon.cola.platform.rbac3.admin.audit.domain.vo;

import java.time.Instant;
import java.util.Map;

/**
     * 类型 `AuditEventVO` 位于 `AuditPort` 内，是记录类型，用于承载 `Audit Event` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuditEventVO` is a record inside `AuditPort` and carries the responsibility, state, or contract for `Audit Event`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuditEventVO` 作为 `AuditPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuditEventVO` as the responsibility boundary of `AuditPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param eventType 记录组件 `eventType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `eventType` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     * @param targetType 记录组件 `targetType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetType` carries constructor data whose meaning is defined by the record contract.
     * @param targetId 记录组件 `targetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetId` carries constructor data whose meaning is defined by the record contract.
     * @param requestId 记录组件 `requestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestId` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     * @param safeEvidence 记录组件 `safeEvidence` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `safeEvidence` carries constructor data whose meaning is defined by the record contract.
     * @param occurredAt 记录组件 `occurredAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `occurredAt` carries constructor data whose meaning is defined by the record contract.
     * @param outcome 记录组件 `outcome` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `outcome` carries constructor data whose meaning is defined by the record contract.
     * @param severity 记录组件 `severity` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `severity` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     */
    public record AuditEventVO(
            /**
             * 字段 `tenantId` 表示 `AuditEventVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AuditEventVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AuditEventVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AuditEventVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `eventType` 表示 `AuditEventVO` 中与 `event Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `eventType` stores the `event Type`-related state, dependency, configuration, or result of `AuditEventVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `eventType` 时应保持 `AuditEventVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `eventType`, preserve `AuditEventVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String eventType,
            /**
             * 字段 `actorId` 表示 `AuditEventVO` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `AuditEventVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `AuditEventVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `AuditEventVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `targetType` 表示 `AuditEventVO` 中与 `target Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetType` stores the `target Type`-related state, dependency, configuration, or result of `AuditEventVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetType` 时应保持 `AuditEventVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetType`, preserve `AuditEventVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetType,
            /**
             * 字段 `targetId` 表示 `AuditEventVO` 中与 `target Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetId` stores the `target Id`-related state, dependency, configuration, or result of `AuditEventVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetId` 时应保持 `AuditEventVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetId`, preserve `AuditEventVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetId,
            /**
             * 字段 `requestId` 表示 `AuditEventVO` 中与 `request Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestId` stores the `request Id`-related state, dependency, configuration, or result of `AuditEventVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestId` 时应保持 `AuditEventVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestId`, preserve `AuditEventVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requestId,
            /**
             * 字段 `traceId` 表示 `AuditEventVO` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `AuditEventVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `AuditEventVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `AuditEventVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId,
            /**
             * 字段 `safeEvidence` 表示 `AuditEventVO` 中与 `safe Evidence` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `safeEvidence` stores the `safe Evidence`-related state, dependency, configuration, or result of `AuditEventVO` (declared type `Map&lt;String, String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `safeEvidence` 时应保持 `AuditEventVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `safeEvidence`, preserve `AuditEventVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, String> safeEvidence,
            /**
             * 字段 `occurredAt` 表示 `AuditEventVO` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `AuditEventVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `AuditEventVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `AuditEventVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant occurredAt,
            /**
             * 字段 `outcome` 表示 `AuditEventVO` 中与 `outcome` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `outcome` stores the `outcome`-related state, dependency, configuration, or result of `AuditEventVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `outcome` 时应保持 `AuditEventVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `outcome`, preserve `AuditEventVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String outcome,
            /**
             * 字段 `severity` 表示 `AuditEventVO` 中与 `severity` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `severity` stores the `severity`-related state, dependency, configuration, or result of `AuditEventVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `severity` 时应保持 `AuditEventVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `severity`, preserve `AuditEventVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String severity,
            /**
             * 字段 `reasonCode` 表示 `AuditEventVO` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `AuditEventVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `AuditEventVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `AuditEventVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode
    ) {
        /**
         * 构造器 `AuditEventVO` 用于创建并初始化 `AuditEventVO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AuditEventVO` creates and initializes `AuditEventVO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AuditEventVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuditEventVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param eventType 输入参数 `eventType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param targetType 输入参数 `targetType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param targetId 输入参数 `targetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requestId 输入参数 `requestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param safeEvidence 输入参数 `safeEvidence`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param occurredAt 输入参数 `occurredAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AuditEventVO(
                String tenantId,
                String eventType,
                String actorId,
                String targetType,
                String targetId,
                String requestId,
                String traceId,
                Map<String, String> safeEvidence,
                Instant occurredAt) {
            this(tenantId, eventType, actorId, targetType, targetId,
                    requestId, traceId, safeEvidence, occurredAt,
                    "SUCCESS", "INFO", "ALLOW");
        }

        /**
         * 构造器 `AuditEventVO` 用于创建并初始化 `AuditEventVO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AuditEventVO` creates and initializes `AuditEventVO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AuditEventVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuditEventVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param eventType 输入参数 `eventType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param targetType 输入参数 `targetType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param targetId 输入参数 `targetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requestId 输入参数 `requestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param safeEvidence 输入参数 `safeEvidence`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param occurredAt 输入参数 `occurredAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param outcome 输入参数 `outcome`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param severity 输入参数 `severity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AuditEventVO {
            safeEvidence = Map.copyOf(safeEvidence);
        }
    }
