package top.egon.cola.platform.rbac3.admin.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.security.Rbac3ContextAuthentication;

import java.util.Objects;

/**
 * 类型 `Rbac3AdminAuthenticationToken` 位于当前包内，是类型，用于承载 `Rbac3 Admin Authentication Token` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3AdminAuthenticationToken` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Admin Authentication Token`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * RBAC3 Admin compatibility principal backed by the unified runtime context.
 */
public final class Rbac3AdminAuthenticationToken extends AbstractAuthenticationToken
        implements Rbac3ContextAuthentication {

    /**
     * 字段 `principal` 表示 `Rbac3AdminAuthenticationToken` 中与 `principal` 相关的状态、依赖、配置或结果（声明类型 `CurrentRbac3Principal`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `principal` stores the `principal`-related state, dependency, configuration, or result of `Rbac3AdminAuthenticationToken` (declared type `CurrentRbac3Principal`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `principal` 时应保持 `Rbac3AdminAuthenticationToken` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `principal`, preserve `Rbac3AdminAuthenticationToken`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final CurrentRbac3Principal principal;
    /**
     * 字段 `context` 表示 `Rbac3AdminAuthenticationToken` 中与 `context` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationService.RuntimeAuthorizationContext`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `context` stores the `context`-related state, dependency, configuration, or result of `Rbac3AdminAuthenticationToken` (declared type `AuthorizationService.RuntimeAuthorizationContext`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `context` 时应保持 `Rbac3AdminAuthenticationToken` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `context`, preserve `Rbac3AdminAuthenticationToken`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationService.RuntimeAuthorizationContext context;

    /**
     * 构造器 `Rbac3AdminAuthenticationToken` 用于创建并初始化 `Rbac3AdminAuthenticationToken` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3AdminAuthenticationToken` creates and initializes `Rbac3AdminAuthenticationToken`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3AdminAuthenticationToken` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3AdminAuthenticationToken`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3AdminAuthenticationToken(
            CurrentRbac3Principal principal,
            AuthorizationService.RuntimeAuthorizationContext context) {
        super(Objects.requireNonNull(principal, "principal").authorities());
        this.principal = principal;
        this.context = Objects.requireNonNull(context, "context");
        setAuthenticated(true);
    }

    /**
     * 方法 `getPrincipal` 按照 `Rbac3AdminAuthenticationToken` 的职责处理输入，完成 `get Principal` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPrincipal` processes its inputs according to `Rbac3AdminAuthenticationToken`'s responsibility, performs the `get Principal` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPrincipal` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPrincipal`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public CurrentRbac3Principal getPrincipal() {
        return principal;
    }

    /**
     * 方法 `getCredentials` 按照 `Rbac3AdminAuthenticationToken` 的职责处理输入，完成 `get Credentials` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getCredentials` processes its inputs according to `Rbac3AdminAuthenticationToken`'s responsibility, performs the `get Credentials` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getCredentials` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getCredentials`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public Object getCredentials() {
        return "";
    }

    /**
     * 方法 `getName` 按照 `Rbac3AdminAuthenticationToken` 的职责处理输入，完成 `get Name` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getName` processes its inputs according to `Rbac3AdminAuthenticationToken`'s responsibility, performs the `get Name` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getName` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getName`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public String getName() {
        return principal.identitySub();
    }

    /**
     * 方法 `context` 按照 `Rbac3AdminAuthenticationToken` 的职责处理输入，完成 `context` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `context` processes its inputs according to `Rbac3AdminAuthenticationToken`'s responsibility, performs the `context` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `context` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `context`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public AuthorizationService.RuntimeAuthorizationContext context() {
        return context;
    }
}
