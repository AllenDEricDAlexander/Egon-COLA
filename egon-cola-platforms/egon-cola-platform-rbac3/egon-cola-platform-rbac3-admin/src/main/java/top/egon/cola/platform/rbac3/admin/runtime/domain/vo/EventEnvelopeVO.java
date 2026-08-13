package top.egon.cola.platform.rbac3.admin.runtime.domain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
     * 类型 `EventEnvelopeVO` 位于 `Rbac3RuntimeProjectionDeliveryHandler` 内，是记录类型，用于承载 `Event Envelope` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `EventEnvelopeVO` is a record inside `Rbac3RuntimeProjectionDeliveryHandler` and carries the responsibility, state, or contract for `Event Envelope`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `EventEnvelopeVO` 作为 `Rbac3RuntimeProjectionDeliveryHandler` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `EventEnvelopeVO` as the responsibility boundary of `Rbac3RuntimeProjectionDeliveryHandler`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param eventId 记录组件 `eventId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `eventId` carries constructor data whose meaning is defined by the record contract.
     * @param eventType 记录组件 `eventType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `eventType` carries constructor data whose meaning is defined by the record contract.
     * @param schemaVersion 记录组件 `schemaVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `schemaVersion` carries constructor data whose meaning is defined by the record contract.
     * @param occurredAt 记录组件 `occurredAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `occurredAt` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param aggregateType 记录组件 `aggregateType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `aggregateType` carries constructor data whose meaning is defined by the record contract.
     * @param aggregateId 记录组件 `aggregateId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `aggregateId` carries constructor data whose meaning is defined by the record contract.
     * @param aggregateVersion 记录组件 `aggregateVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `aggregateVersion` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     * @param payload 记录组件 `payload` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `payload` carries constructor data whose meaning is defined by the record contract.
     */
    public record EventEnvelopeVO(
            /**
             * 字段 `eventId` 表示 `EventEnvelopeVO` 中与 `event Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `eventId` stores the `event Id`-related state, dependency, configuration, or result of `EventEnvelopeVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `eventId` 时应保持 `EventEnvelopeVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `eventId`, preserve `EventEnvelopeVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String eventId,
            /**
             * 字段 `eventType` 表示 `EventEnvelopeVO` 中与 `event Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `eventType` stores the `event Type`-related state, dependency, configuration, or result of `EventEnvelopeVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `eventType` 时应保持 `EventEnvelopeVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `eventType`, preserve `EventEnvelopeVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String eventType,
            /**
             * 字段 `schemaVersion` 表示 `EventEnvelopeVO` 中与 `schema Version` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `schemaVersion` stores the `schema Version`-related state, dependency, configuration, or result of `EventEnvelopeVO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `schemaVersion` 时应保持 `EventEnvelopeVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `schemaVersion`, preserve `EventEnvelopeVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            int schemaVersion,
            /**
             * 字段 `occurredAt` 表示 `EventEnvelopeVO` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `EventEnvelopeVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `EventEnvelopeVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `EventEnvelopeVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant occurredAt,
            /**
             * 字段 `tenantId` 表示 `EventEnvelopeVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `EventEnvelopeVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `EventEnvelopeVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `EventEnvelopeVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `aggregateType` 表示 `EventEnvelopeVO` 中与 `aggregate Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `aggregateType` stores the `aggregate Type`-related state, dependency, configuration, or result of `EventEnvelopeVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `aggregateType` 时应保持 `EventEnvelopeVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `aggregateType`, preserve `EventEnvelopeVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String aggregateType,
            /**
             * 字段 `aggregateId` 表示 `EventEnvelopeVO` 中与 `aggregate Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `aggregateId` stores the `aggregate Id`-related state, dependency, configuration, or result of `EventEnvelopeVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `aggregateId` 时应保持 `EventEnvelopeVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `aggregateId`, preserve `EventEnvelopeVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String aggregateId,
            /**
             * 字段 `aggregateVersion` 表示 `EventEnvelopeVO` 中与 `aggregate Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `aggregateVersion` stores the `aggregate Version`-related state, dependency, configuration, or result of `EventEnvelopeVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `aggregateVersion` 时应保持 `EventEnvelopeVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `aggregateVersion`, preserve `EventEnvelopeVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long aggregateVersion,
            /**
             * 字段 `traceId` 表示 `EventEnvelopeVO` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `EventEnvelopeVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `EventEnvelopeVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `EventEnvelopeVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId,
            /**
             * 字段 `payload` 表示 `EventEnvelopeVO` 中与 `payload` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `payload` stores the `payload`-related state, dependency, configuration, or result of `EventEnvelopeVO` (declared type `Map&lt;String, String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `payload` 时应保持 `EventEnvelopeVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `payload`, preserve `EventEnvelopeVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, String> payload) {

        /**
         * 构造器 `EventEnvelopeVO` 用于创建并初始化 `EventEnvelopeVO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `EventEnvelopeVO` creates and initializes `EventEnvelopeVO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `EventEnvelopeVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `EventEnvelopeVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param eventId 输入参数 `eventId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param eventType 输入参数 `eventType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param schemaVersion 输入参数 `schemaVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param occurredAt 输入参数 `occurredAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param aggregateType 输入参数 `aggregateType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param aggregateId 输入参数 `aggregateId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param aggregateVersion 输入参数 `aggregateVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param payload 输入参数 `payload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public EventEnvelopeVO {
            payload = Map.copyOf(payload);
        }
    }
