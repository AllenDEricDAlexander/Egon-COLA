package top.egon.cola.platform.rbac3.admin.runtime.repository;

import java.util.Map;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationEventVO;

/**
 * 类型 `AuthorizationEventPublisher` 位于当前包内，是接口，用于承载 `Authorization Event Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuthorizationEventPublisher` is an interface in its package and carries the responsibility, state, or contract for `Authorization Event Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `AuthorizationEventPublisher` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `AuthorizationEventPublisher` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
public interface AuthorizationEventPublisher {

    /**
     * 方法 `enqueue` 按照 `AuthorizationEventPublisher` 的职责处理输入，完成 `enqueue` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `enqueue` processes its inputs according to `AuthorizationEventPublisher`'s responsibility, performs the `enqueue` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `enqueue` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `enqueue`, then continue the business flow using its result, exception, or side effect.
     *
     * @param event 输入参数 `event`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    String enqueue(AuthorizationEventVO event);

    }
