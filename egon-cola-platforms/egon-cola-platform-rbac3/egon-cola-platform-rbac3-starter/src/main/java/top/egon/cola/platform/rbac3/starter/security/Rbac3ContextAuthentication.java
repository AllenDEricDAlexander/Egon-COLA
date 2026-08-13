package top.egon.cola.platform.rbac3.starter.security;

import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;

/**
 * 类型 `Rbac3ContextAuthentication` 位于当前包内，是接口，用于承载 `Rbac3 Context Authentication` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3ContextAuthentication` is an interface in its package and carries the responsibility, state, or contract for `Rbac3 Context Authentication`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Authentication contract retaining the bound RBAC3 runtime context.
 */
public interface Rbac3ContextAuthentication {

    /**
     * 方法 `context` 按照 `Rbac3ContextAuthentication` 的职责处理输入，完成 `context` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `context` processes its inputs according to `Rbac3ContextAuthentication`'s responsibility, performs the `context` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `context` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `context`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    AuthorizationService.RuntimeAuthorizationContext context();
}
