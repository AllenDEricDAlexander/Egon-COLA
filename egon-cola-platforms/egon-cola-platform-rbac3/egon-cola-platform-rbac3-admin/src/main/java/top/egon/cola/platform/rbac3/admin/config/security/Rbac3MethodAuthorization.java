package top.egon.cola.platform.rbac3.admin.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 类型 `Rbac3MethodAuthorization` 位于当前包内，是类型，用于承载 `Rbac3 Method Authorization` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3MethodAuthorization` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Method Authorization`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `Rbac3MethodAuthorization` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `Rbac3MethodAuthorization` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Component("rbac3MethodAuthorization")
public final class Rbac3MethodAuthorization {

    /**
     * 方法 `hasPermission` 按照 `Rbac3MethodAuthorization` 的职责处理输入，完成 `has Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `hasPermission` processes its inputs according to `Rbac3MethodAuthorization`'s responsibility, performs the `has Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `hasPermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `hasPermission`, then continue the business flow using its result, exception, or side effect.
     *
     * @param authentication 输入参数 `authentication`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requiredPermission 输入参数 `requiredPermission`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean hasPermission(
            Authentication authentication,
            String requiredPermission) {
        if (authentication == null) {
            return false;
        }
        return authentication.getPrincipal() instanceof CurrentRbac3Principal principal
                && principal.hasPermission(requiredPermission);
    }
}
