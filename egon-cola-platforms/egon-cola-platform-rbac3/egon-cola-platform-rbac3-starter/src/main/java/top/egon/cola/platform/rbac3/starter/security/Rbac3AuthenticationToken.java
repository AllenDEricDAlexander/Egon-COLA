package top.egon.cola.platform.rbac3.starter.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 类型 `Rbac3AuthenticationToken` 位于当前包内，是类型，用于承载 `Rbac3 Authentication Token` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3AuthenticationToken` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Authentication Token`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Authenticated request principal carrying only the validated runtime context.
 */
public final class Rbac3AuthenticationToken extends AbstractAuthenticationToken
        implements Rbac3ContextAuthentication {

    /**
     * 字段 `context` 表示 `Rbac3AuthenticationToken` 中与 `context` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationService.RuntimeAuthorizationContext`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `context` stores the `context`-related state, dependency, configuration, or result of `Rbac3AuthenticationToken` (declared type `AuthorizationService.RuntimeAuthorizationContext`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `context` 时应保持 `Rbac3AuthenticationToken` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `context`, preserve `Rbac3AuthenticationToken`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationService.RuntimeAuthorizationContext context;

    /**
     * 构造器 `Rbac3AuthenticationToken` 用于创建并初始化 `Rbac3AuthenticationToken` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3AuthenticationToken` creates and initializes `Rbac3AuthenticationToken`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3AuthenticationToken` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3AuthenticationToken`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3AuthenticationToken(
            AuthorizationService.RuntimeAuthorizationContext context
    ) {
        super(authorities(context));
        this.context = Objects.requireNonNull(context, "context");
        setAuthenticated(true);
    }

    /**
     * 方法 `context` 按照 `Rbac3AuthenticationToken` 的职责处理输入，完成 `context` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `context` processes its inputs according to `Rbac3AuthenticationToken`'s responsibility, performs the `context` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `context` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `context`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public AuthorizationService.RuntimeAuthorizationContext context() {
        return context;
    }

    /**
     * 方法 `getCredentials` 按照 `Rbac3AuthenticationToken` 的职责处理输入，完成 `get Credentials` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getCredentials` processes its inputs according to `Rbac3AuthenticationToken`'s responsibility, performs the `get Credentials` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getPrincipal` 按照 `Rbac3AuthenticationToken` 的职责处理输入，完成 `get Principal` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPrincipal` processes its inputs according to `Rbac3AuthenticationToken`'s responsibility, performs the `get Principal` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPrincipal` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPrincipal`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public IdentityPrincipal getPrincipal() {
        return context.identity();
    }

    /**
     * 方法 `getName` 按照 `Rbac3AuthenticationToken` 的职责处理输入，完成 `get Name` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getName` processes its inputs according to `Rbac3AuthenticationToken`'s responsibility, performs the `get Name` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getName` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getName`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public String getName() {
        return context.identity().subject();
    }

    /**
     * 方法 `authorities` 按照 `Rbac3AuthenticationToken` 的职责处理输入，完成 `authorities` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorities` processes its inputs according to `Rbac3AuthenticationToken`'s responsibility, performs the `authorities` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorities` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorities`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static List<GrantedAuthority> authorities(
            AuthorizationService.RuntimeAuthorizationContext context) {
        Objects.requireNonNull(context, "context");
        LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>();
        context.snapshot().permissions().stream().sorted().forEach(permission -> {
            authorities.add(new SimpleGrantedAuthority("RBAC3_" + permission));
            authorities.add(new SimpleGrantedAuthority("CAP_" + permission));
        });
        return List.copyOf(authorities);
    }
}
