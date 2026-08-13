package top.egon.cola.platform.rbac3.admin.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类型 `RequiresRbac3Permission` 位于当前包内，是注解类型，用于承载 `Requires Rbac3 Permission` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RequiresRbac3Permission` is an annotation type in its package and carries the responsibility, state, or contract for `Requires Rbac3 Permission`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `RequiresRbac3Permission` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `RequiresRbac3Permission` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@rbac3MethodAuthorization.hasPermission(authentication, '{permission}')")
public @interface RequiresRbac3Permission {

    /**
     * 方法 `permission` 按照 `RequiresRbac3Permission` 的职责处理输入，完成 `permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `permission` processes its inputs according to `RequiresRbac3Permission`'s responsibility, performs the `permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `permission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `permission`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    String permission();
}
