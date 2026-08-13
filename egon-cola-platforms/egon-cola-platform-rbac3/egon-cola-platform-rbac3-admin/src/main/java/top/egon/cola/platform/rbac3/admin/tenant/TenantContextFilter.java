package top.egon.cola.platform.rbac3.admin.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 类型 `TenantContextFilter` 位于当前包内，是类型，用于承载 `Tenant Context Filter` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `TenantContextFilter` is a type in its package and carries the responsibility, state, or contract for `Tenant Context Filter`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `TenantContextFilter` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `TenantContextFilter` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
public final class TenantContextFilter extends OncePerRequestFilter {

    /**
     * 字段 `TENANT_ATTRIBUTE` 表示 `TenantContextFilter` 中与 `TENANT ATTRIBUTE` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `TENANT_ATTRIBUTE` stores the `TENANT ATTRIBUTE`-related state, dependency, configuration, or result of `TenantContextFilter` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `TENANT_ATTRIBUTE` 时应保持 `TenantContextFilter` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `TENANT_ATTRIBUTE`, preserve `TenantContextFilter`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String TENANT_ATTRIBUTE = TenantContext.class.getName();
    /**
     * 字段 `resolver` 表示 `TenantContextFilter` 中与 `resolver` 相关的状态、依赖、配置或结果（声明类型 `TenantContextResolver`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resolver` stores the `resolver`-related state, dependency, configuration, or result of `TenantContextFilter` (declared type `TenantContextResolver`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `resolver` 时应保持 `TenantContextFilter` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `resolver`, preserve `TenantContextFilter`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final TenantContextResolver resolver;

    /**
     * 构造器 `TenantContextFilter` 用于创建并初始化 `TenantContextFilter` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `TenantContextFilter` creates and initializes `TenantContextFilter`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `TenantContextFilter` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `TenantContextFilter`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param resolver 输入参数 `resolver`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public TenantContextFilter(TenantContextResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 方法 `doFilterInternal` 按照 `TenantContextFilter` 的职责处理输入，完成 `do Filter Internal` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `doFilterInternal` processes its inputs according to `TenantContextFilter`'s responsibility, performs the `do Filter Internal` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `doFilterInternal` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `doFilterInternal`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param response 输入参数 `response`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param filterChain 输入参数 `filterChain`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @throws ServletException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
     * @throws IOException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            TenantContext context = resolver.resolve(
                    request, SecurityContextHolder.getContext().getAuthentication());
            TenantContext.set(context);
            request.setAttribute(TENANT_ATTRIBUTE, context.effectiveTenantId());
            filterChain.doFilter(request, response);
        } catch (TenantContextResolver.TenantContextResolutionException error) {
            response.setStatus(error.status());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":{\"code\":\""
                    + error.reasonCode() + "\"}}");
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 方法 `shouldNotFilter` 按照 `TenantContextFilter` 的职责处理输入，完成 `should Not Filter` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `shouldNotFilter` processes its inputs according to `TenantContextFilter`'s responsibility, performs the `should Not Filter` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `shouldNotFilter` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `shouldNotFilter`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health/");
    }
}
