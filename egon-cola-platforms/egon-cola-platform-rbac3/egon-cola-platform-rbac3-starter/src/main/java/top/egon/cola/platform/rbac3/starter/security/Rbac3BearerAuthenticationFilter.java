package top.egon.cola.platform.rbac3.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.IdpAuthenticationToken;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.cache.SingleFlightSnapshotLoader;
import top.egon.cola.platform.rbac3.starter.client.Rbac3AuthorizationClient;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * 使用本系统的 RBAC3 快照增强 IdP 已认证的用户请求，服务请求保持 IdP 身份不变。
 * Enriches an IdP-authenticated user request with this system's RBAC3 snapshot while leaving
 * service requests under the IdP identity.
 */
public final class Rbac3BearerAuthenticationFilter extends OncePerRequestFilter {

    /**
     * 字段 `snapshotLoader` 表示 `Rbac3BearerAuthenticationFilter` 中与 `snapshot Loader` 相关的状态、依赖、配置或结果（声明类型 `SingleFlightSnapshotLoader`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `snapshotLoader` stores the `snapshot Loader`-related state, dependency, configuration, or result of `Rbac3BearerAuthenticationFilter` (declared type `SingleFlightSnapshotLoader`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `snapshotLoader` 时应保持 `Rbac3BearerAuthenticationFilter` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `snapshotLoader`, preserve `Rbac3BearerAuthenticationFilter`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SingleFlightSnapshotLoader snapshotLoader;
    /**
     * 字段 `objectMapper` 表示 `Rbac3BearerAuthenticationFilter` 中与 `object Mapper` 相关的状态、依赖、配置或结果（声明类型 `ObjectMapper`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `objectMapper` stores the `object Mapper`-related state, dependency, configuration, or result of `Rbac3BearerAuthenticationFilter` (declared type `ObjectMapper`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `objectMapper` 时应保持 `Rbac3BearerAuthenticationFilter` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `objectMapper`, preserve `Rbac3BearerAuthenticationFilter`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造器 `Rbac3BearerAuthenticationFilter` 用于创建并初始化 `Rbac3BearerAuthenticationFilter` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3BearerAuthenticationFilter` creates and initializes `Rbac3BearerAuthenticationFilter`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3BearerAuthenticationFilter` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3BearerAuthenticationFilter`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param snapshotLoader 输入参数 `snapshotLoader`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3BearerAuthenticationFilter(
            SingleFlightSnapshotLoader snapshotLoader,
            ObjectMapper objectMapper
    ) {
        this.snapshotLoader = Objects.requireNonNull(snapshotLoader, "snapshotLoader");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * 方法 `shouldNotFilter` 按照 `Rbac3BearerAuthenticationFilter` 的职责处理输入，完成 `should Not Filter` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `shouldNotFilter` processes its inputs according to `Rbac3BearerAuthenticationFilter`'s responsibility, performs the `should Not Filter` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        // IdP protocol and identity endpoints are authenticated by IdP only;
        // they must not require an RBAC3 authorization snapshot.
        return path.startsWith("/internal/") || path.startsWith("/oauth2/");
    }

    /**
     * 方法 `doFilterInternal` 按照 `Rbac3BearerAuthenticationFilter` 的职责处理输入，完成 `do Filter Internal` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `doFilterInternal` processes its inputs according to `Rbac3BearerAuthenticationFilter`'s responsibility, performs the `do Filter Internal` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (!(authentication instanceof IdpAuthenticationToken idp)
                || !(idp.getPrincipal() instanceof IdentityPrincipal user)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            AuthorizationService.RuntimeAuthorizationContext context =
                    new AuthorizationService.RuntimeAuthorizationContext(
                            user, snapshotLoader.load(user), false);
            var securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new Rbac3AuthenticationToken(context));
            SecurityContextHolder.setContext(securityContext);
            filterChain.doFilter(request, response);
        } catch (Rbac3AuthorizationClient.AuthorizationDeniedException exception) {
            failure(response, HttpServletResponse.SC_FORBIDDEN,
                    exception.getMessage());
        } catch (Rbac3AuthorizationClient.AuthorizationUnavailableException exception) {
            failure(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    exception.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 方法 `failure` 按照 `Rbac3BearerAuthenticationFilter` 的职责处理输入，完成 `failure` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `failure` processes its inputs according to `Rbac3BearerAuthenticationFilter`'s responsibility, performs the `failure` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `failure` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `failure`, then continue the business flow using its result, exception, or side effect.
     *
     * @param response 输入参数 `response`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @throws IOException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
     */
    private void failure(
            HttpServletResponse response,
            int status,
            String reasonCode)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "code", reasonCode,
                "message", "RBAC3 authorization context is unavailable"));
    }
}
