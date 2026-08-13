package top.egon.cola.platform.rbac3.admin.interfaces.http;

import java.time.Instant;
import java.util.UUID;

/**
 * 类型 `ApiEnvelope` 位于当前包内，是记录类型，用于承载 `Api Envelope` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ApiEnvelope` is a record in its package and carries the responsibility, state, or contract for `Api Envelope`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ApiEnvelope` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ApiEnvelope` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 *
 * @param <T> 类型参数表示响应数据的具体类型；type parameter representing the response data type.
 * @param data 记录组件 `data` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `data` carries constructor data whose meaning is defined by the record contract.
 * @param meta 记录组件 `meta` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `meta` carries constructor data whose meaning is defined by the record contract.
 */
public record ApiEnvelope<T>(/**
 * 字段 `data` 表示 `ApiEnvelope` 中与 `data` 相关的状态、依赖、配置或结果（声明类型 `T`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `data` stores the `data`-related state, dependency, configuration, or result of `ApiEnvelope` (declared type `T`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `data` 时应保持 `ApiEnvelope` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `data`, preserve `ApiEnvelope`'s lifecycle, immutability, and thread-safety constraints.
 */ T data, /**
 * 字段 `meta` 表示 `ApiEnvelope` 中与 `meta` 相关的状态、依赖、配置或结果（声明类型 `Meta`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `meta` stores the `meta`-related state, dependency, configuration, or result of `ApiEnvelope` (declared type `Meta`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `meta` 时应保持 `ApiEnvelope` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `meta`, preserve `ApiEnvelope`'s lifecycle, immutability, and thread-safety constraints.
 */ Meta meta) {

    /**
     * 方法 `success` 按照 `ApiEnvelope` 的职责处理输入，完成 `success` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `success` processes its inputs according to `ApiEnvelope`'s responsibility, performs the `success` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `success` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `success`, then continue the business flow using its result, exception, or side effect.
     *
     * @param <T> 类型参数表示响应数据的具体类型；type parameter representing the response data type.
     * @param data 输入参数 `data`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public static <T> ApiEnvelope<T> success(T data) {
        String requestId = UUID.randomUUID().toString();
        return new ApiEnvelope<>(data, new Meta(requestId, requestId, Instant.now()));
    }

    /**
     * 类型 `Meta` 位于 `ApiEnvelope` 内，是记录类型，用于承载 `Meta` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Meta` is a record inside `ApiEnvelope` and carries the responsibility, state, or contract for `Meta`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Meta` 作为 `ApiEnvelope` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Meta` as the responsibility boundary of `ApiEnvelope`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param requestId 记录组件 `requestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestId` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     * @param timestamp 记录组件 `timestamp` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `timestamp` carries constructor data whose meaning is defined by the record contract.
     */
    public record Meta(/**
 * 字段 `requestId` 表示 `Meta` 中与 `request Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `requestId` stores the `request Id`-related state, dependency, configuration, or result of `Meta` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `requestId` 时应保持 `Meta` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `requestId`, preserve `Meta`'s lifecycle, immutability, and thread-safety constraints.
 */ String requestId, /**
 * 字段 `traceId` 表示 `Meta` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `Meta` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `traceId` 时应保持 `Meta` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `traceId`, preserve `Meta`'s lifecycle, immutability, and thread-safety constraints.
 */ String traceId, /**
 * 字段 `timestamp` 表示 `Meta` 中与 `timestamp` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `timestamp` stores the `timestamp`-related state, dependency, configuration, or result of `Meta` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `timestamp` 时应保持 `Meta` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `timestamp`, preserve `Meta`'s lifecycle, immutability, and thread-safety constraints.
 */ Instant timestamp) {
    }
}
