package top.egon.cola.platform.rbac3.admin.shared.domain.vo;

import java.time.Instant;
import java.util.UUID;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeMetaVO;

/**
 * 类型 `ApiEnvelopeVO` 位于当前包内，是记录类型，用于承载 `Api Envelope` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ApiEnvelopeVO` is a record in its package and carries the responsibility, state, or contract for `Api Envelope`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ApiEnvelopeVO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ApiEnvelopeVO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 *
 * @param <T> 类型参数表示响应数据的具体类型；type parameter representing the response data type.
 * @param data 记录组件 `data` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `data` carries constructor data whose meaning is defined by the record contract.
 * @param meta 记录组件 `meta` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `meta` carries constructor data whose meaning is defined by the record contract.
 */
public record ApiEnvelopeVO<T>(/**
 * 字段 `data` 表示 `ApiEnvelopeVO` 中与 `data` 相关的状态、依赖、配置或结果（声明类型 `T`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `data` stores the `data`-related state, dependency, configuration, or result of `ApiEnvelopeVO` (declared type `T`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `data` 时应保持 `ApiEnvelopeVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `data`, preserve `ApiEnvelopeVO`'s lifecycle, immutability, and thread-safety constraints.
 */ T data, /**
 * 字段 `meta` 表示 `ApiEnvelopeVO` 中与 `meta` 相关的状态、依赖、配置或结果（声明类型 `ApiEnvelopeMetaVO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `meta` stores the `meta`-related state, dependency, configuration, or result of `ApiEnvelopeVO` (declared type `ApiEnvelopeMetaVO`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `meta` 时应保持 `ApiEnvelopeVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `meta`, preserve `ApiEnvelopeVO`'s lifecycle, immutability, and thread-safety constraints.
 */ ApiEnvelopeMetaVO meta) {

    /**
     * 方法 `success` 按照 `ApiEnvelopeVO` 的职责处理输入，完成 `success` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `success` processes its inputs according to `ApiEnvelopeVO`'s responsibility, performs the `success` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `success` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `success`, then continue the business flow using its result, exception, or side effect.
     *
     * @param <T> 类型参数表示响应数据的具体类型；type parameter representing the response data type.
     * @param data 输入参数 `data`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public static <T> ApiEnvelopeVO<T> success(T data) {
        String requestId = UUID.randomUUID().toString();
        return new ApiEnvelopeVO<>(data, new ApiEnvelopeMetaVO(requestId, requestId, Instant.now()));
    }

    }
