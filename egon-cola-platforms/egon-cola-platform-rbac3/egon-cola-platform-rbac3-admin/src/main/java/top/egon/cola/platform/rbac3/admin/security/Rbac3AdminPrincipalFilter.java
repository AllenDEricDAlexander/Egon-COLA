package top.egon.cola.platform.rbac3.admin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.rbac3.starter.security.Rbac3ContextAuthentication;

import java.io.IOException;

/**
 * 类型 `Rbac3AdminPrincipalFilter` 位于当前包内，是类型，用于承载 `Rbac3 Admin Principal Filter` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3AdminPrincipalFilter` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Admin Principal Filter`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Projects the generic Starter context into RBAC3 Admin's established principal.
 */
public final class Rbac3AdminPrincipalFilter extends OncePerRequestFilter {

    /**
     * 方法 `shouldNotFilter` 按照 `Rbac3AdminPrincipalFilter` 的职责处理输入，完成 `should Not Filter` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `shouldNotFilter` processes its inputs according to `Rbac3AdminPrincipalFilter`'s responsibility, performs the `should Not Filter` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `shouldNotFilter` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `shouldNotFilter`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/internal/");
    }

    /**
     * 方法 `doFilterInternal` 按照 `Rbac3AdminPrincipalFilter` 的职责处理输入，完成 `do Filter Internal` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `doFilterInternal` processes its inputs according to `Rbac3AdminPrincipalFilter`'s responsibility, performs the `do Filter Internal` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
            FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof Rbac3ContextAuthentication source
                && !(authentication instanceof Rbac3AdminAuthenticationToken)) {
            var context = source.context();
            var snapshot = context.snapshot();
            var principal = new CurrentRbac3Principal(
                    snapshot.tenantId(), snapshot.identitySub(),
                    snapshot.rbac3UserId(), snapshot.sessionId(),
                    snapshot.authVersion(), snapshot.contextVersion(),
                    snapshot.policyVersion(), snapshot.permissions(),
                    snapshot.permissions().contains("system:platform:admin"));
            var projected = SecurityContextHolder.createEmptyContext();
            projected.setAuthentication(new Rbac3AdminAuthenticationToken(
                    principal, context));
            SecurityContextHolder.setContext(projected);
        }
        filterChain.doFilter(request, response);
    }
}
