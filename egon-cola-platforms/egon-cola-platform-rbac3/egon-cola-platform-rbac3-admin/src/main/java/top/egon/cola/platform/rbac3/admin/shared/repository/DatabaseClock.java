package top.egon.cola.platform.rbac3.admin.shared.repository;

import java.time.Instant;

/**
 * 类型 `DatabaseClock` 位于当前包内，是接口，用于承载 `Database Clock` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `DatabaseClock` is an interface in its package and carries the responsibility, state, or contract for `Database Clock`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `DatabaseClock` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `DatabaseClock` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
public interface DatabaseClock {

    /**
     * 方法 `transactionNow` 按照 `DatabaseClock` 的职责处理输入，完成 `transaction Now` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `transactionNow` processes its inputs according to `DatabaseClock`'s responsibility, performs the `transaction Now` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `transactionNow` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `transactionNow`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    Instant transactionNow();
}
