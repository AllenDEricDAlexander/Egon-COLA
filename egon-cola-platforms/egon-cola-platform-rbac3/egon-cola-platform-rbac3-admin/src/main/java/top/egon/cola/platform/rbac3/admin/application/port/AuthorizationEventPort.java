package top.egon.cola.platform.rbac3.admin.application.port;

import java.util.Map;

/**
 * 类型 `AuthorizationEventPort` 位于当前包内，是接口，用于承载 `Authorization Event Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuthorizationEventPort` is an interface in its package and carries the responsibility, state, or contract for `Authorization Event Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `AuthorizationEventPort` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `AuthorizationEventPort` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
public interface AuthorizationEventPort {

    /**
     * 方法 `enqueue` 按照 `AuthorizationEventPort` 的职责处理输入，完成 `enqueue` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `enqueue` processes its inputs according to `AuthorizationEventPort`'s responsibility, performs the `enqueue` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `enqueue` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `enqueue`, then continue the business flow using its result, exception, or side effect.
     *
     * @param event 输入参数 `event`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    String enqueue(AuthorizationEvent event);

    /**
     * 类型 `AuthorizationEvent` 位于 `AuthorizationEventPort` 内，是记录类型，用于承载 `Authorization Event` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuthorizationEvent` is a record inside `AuthorizationEventPort` and carries the responsibility, state, or contract for `Authorization Event`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuthorizationEvent` 作为 `AuthorizationEventPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationEvent` as the responsibility boundary of `AuthorizationEventPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param aggregateType 记录组件 `aggregateType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `aggregateType` carries constructor data whose meaning is defined by the record contract.
     * @param aggregateId 记录组件 `aggregateId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `aggregateId` carries constructor data whose meaning is defined by the record contract.
     * @param eventType 记录组件 `eventType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `eventType` carries constructor data whose meaning is defined by the record contract.
     * @param safePayload 记录组件 `safePayload` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `safePayload` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     */
    record AuthorizationEvent(
            /**
             * 字段 `tenantId` 表示 `AuthorizationEvent` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AuthorizationEvent` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AuthorizationEvent` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AuthorizationEvent`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `aggregateType` 表示 `AuthorizationEvent` 中与 `aggregate Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `aggregateType` stores the `aggregate Type`-related state, dependency, configuration, or result of `AuthorizationEvent` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `aggregateType` 时应保持 `AuthorizationEvent` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `aggregateType`, preserve `AuthorizationEvent`'s lifecycle, immutability, and thread-safety constraints.
             */
            String aggregateType,
            /**
             * 字段 `aggregateId` 表示 `AuthorizationEvent` 中与 `aggregate Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `aggregateId` stores the `aggregate Id`-related state, dependency, configuration, or result of `AuthorizationEvent` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `aggregateId` 时应保持 `AuthorizationEvent` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `aggregateId`, preserve `AuthorizationEvent`'s lifecycle, immutability, and thread-safety constraints.
             */
            String aggregateId,
            /**
             * 字段 `eventType` 表示 `AuthorizationEvent` 中与 `event Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `eventType` stores the `event Type`-related state, dependency, configuration, or result of `AuthorizationEvent` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `eventType` 时应保持 `AuthorizationEvent` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `eventType`, preserve `AuthorizationEvent`'s lifecycle, immutability, and thread-safety constraints.
             */
            String eventType,
            /**
             * 字段 `safePayload` 表示 `AuthorizationEvent` 中与 `safe Payload` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `safePayload` stores the `safe Payload`-related state, dependency, configuration, or result of `AuthorizationEvent` (declared type `Map&lt;String, String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `safePayload` 时应保持 `AuthorizationEvent` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `safePayload`, preserve `AuthorizationEvent`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, String> safePayload,
            /**
             * 字段 `traceId` 表示 `AuthorizationEvent` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `AuthorizationEvent` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `AuthorizationEvent` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `AuthorizationEvent`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId
    ) {
        /**
         * 构造器 `AuthorizationEvent` 用于创建并初始化 `AuthorizationEvent` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AuthorizationEvent` creates and initializes `AuthorizationEvent`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AuthorizationEvent` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuthorizationEvent`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param aggregateType 输入参数 `aggregateType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param aggregateId 输入参数 `aggregateId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param eventType 输入参数 `eventType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param safePayload 输入参数 `safePayload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AuthorizationEvent {
            safePayload = Map.copyOf(safePayload);
        }
    }
}
