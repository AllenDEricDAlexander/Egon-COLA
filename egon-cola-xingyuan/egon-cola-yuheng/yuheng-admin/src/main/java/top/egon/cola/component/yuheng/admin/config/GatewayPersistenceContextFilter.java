package top.egon.cola.component.yuheng.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayPersistenceContextFilter} 是管理面 HTTP 请求线程上的持久化身份入口过滤器。它注册在 Spring Security
 * 过滤器链之后（见 {@link #FILTER_ORDER}），因此读到的 {@code Authentication} 必然是「已验证」结果。它只做两件事：
 * 从已验证身份上取出它自带的租户声明（可能没有），再把整条下游链连同该声明交给 {@link GatewayPersistenceContextComponent}；
 * 由该组件负责与部署配置 {@code yuheng.persistence.identity-tenant-id} 比对（不一致即 fail closed）、在当前执行线程写入 MDC
 * {@code tenantId}/{@code userId} 并在 finally 原样恢复。请求头或请求体里的租户值永远不是事实来源，本过滤器根本不读取它们。
 * English summary: {@code GatewayPersistenceContextFilter} is the persistence-identity entry point for admin HTTP request
 * threads. It is registered after the Spring Security filter chain (see {@link #FILTER_ORDER}), so any
 * {@code Authentication} it observes has already been verified. It does two things only: read the tenant an already
 * verified identity carries (it may carry none), and hand the whole downstream chain plus that claim to
 * {@link GatewayPersistenceContextComponent}, which compares it against the deployment value
 * {@code yuheng.persistence.identity-tenant-id} (fail closed on mismatch), installs the MDC {@code tenantId}/{@code userId}
 * keys on the executing thread, and restores them exactly in a finally block. Tenant values in headers or bodies are never
 * a source of truth and are not read here at all.
 *
 * 用法 / Usage: 由 {@link GatewayPersistenceConfiguration} 以 {@link #FILTER_ORDER} 注册为容器过滤器，不应标注
 * {@code @Component}（那会让它先于认证链执行）。/ Register it from {@link GatewayPersistenceConfiguration} as a servlet
 * filter with {@link #FILTER_ORDER}; it must not carry {@code @Component}, which would run it before the authentication
 * chain.
 */
@Slf4j
@RequiredArgsConstructor
public final class GatewayPersistenceContextFilter extends OncePerRequestFilter {

    /**
     * 中文说明：过滤器注册顺序，必须晚于 Spring Security 的默认链顺序（{@code -100}），否则身份尚未认证，比对与租户绑定都无意义。
     * English summary: the registered filter order; it must be later than Spring Security's default chain order
     * ({@code -100}), otherwise the identity would not be authenticated yet and neither the comparison nor the tenant
     * binding would mean anything.
     */
    public static final int FILTER_ORDER = Ordered.LOWEST_PRECEDENCE - 100;

    /**
     * 中文说明：受信任身份可能携带租户声明的claim名，覆盖签发方使用的三种写法；全部缺失即「身份不带租户」。
     * English summary: the claim names a trusted identity may carry its tenant under, covering the three spellings the
     * issuer uses; when all are absent the identity simply carries no tenant.
     */
    private static final List<String> TENANT_CLAIMS = List.of("tenantId", "tenant_id", "tenant");

    /** 中文说明：部署绑定的单企业持久化上下文组件，唯一被注入的协作者。 English summary: the deployment-bound single-installation persistence context, the only injected collaborator. */
    @Qualifier("gatewayPersistenceContextComponent")
    private final GatewayPersistenceContextComponent persistenceContext;

    /**
     * 中文说明：先取出已验证身份的租户声明，再把下游链放入部署持久化上下文执行；下游抛出的
     * {@link IOException}/{@link ServletException} 与运行期异常如实传播，不吞异常也不伪造成功。
     * English summary: reads the tenant carried by the verified identity first, then runs the downstream chain inside the
     * deployment persistence context; {@link IOException}/{@link ServletException} and runtime failures raised downstream
     * propagate unchanged — nothing is swallowed and no success is fabricated.
     *
     * 用法 / Usage: 容器过滤器入口 / Entry point invoked by the servlet container:
     * {@code GatewayPersistenceContextFilter.doFilterInternal(request, response, chain)}。
     * @param request 参数 当前请求；parameter the current request.
     * @param response 参数 当前响应；parameter the current response.
     * @param filterChain 参数 下游链；parameter the downstream chain.
     * @throws ServletException 下游或工作抛出的非 IO 受检异常；a checked non-IO failure raised downstream or by the work.
     * @throws IOException 下游 IO 失败；an IO failure from downstream.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String identityTenantId = verifiedIdentityTenantId();
        try {
            persistenceContext.call(identityTenantId, () -> {
                filterChain.doFilter(request, response);
                return null;
            });
        } catch (IOException io) {
            throw io;
        } catch (ServletException servlet) {
            throw servlet;
        } catch (RuntimeException runtime) {
            throw runtime;
        } catch (Exception unexpected) {
            throw new ServletException(
                    "YUHENG_ADMIN_PERSISTENCE_CONTEXT_FAILED", unexpected);
        }
    }

    /**
     * 中文说明：异步派发同样需要持久化上下文（{@code WebAsync} 的结果写出发生在 ASYNC 派发线程上），因此不跳过 ASYNC 派发。
     * English summary: async dispatch also needs the persistence context, because a {@code WebAsync} result is written out on
     * the ASYNC dispatch thread, so ASYNC dispatches are not skipped.
     *
     * 用法 / Usage: 由 {@link OncePerRequestFilter} 调度 / Consulted by {@link OncePerRequestFilter}.
     * @return 返回 {@code false}，表示 ASYNC 派发仍需执行本过滤器；returns {@code false}, so the filter still runs on ASYNC dispatches.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    /**
     * 中文说明：只在身份已通过认证时提取其自带租户；匿名或未认证请求返回 {@code null}，由组件按单企业安装域处理，
     * 绝不从请求头/请求体推断租户。
     * English summary: extracts the identity's own tenant only once the identity has actually been authenticated; anonymous
     * or unauthenticated requests yield {@code null} and the component treats them as the single-enterprise installation
     * domain — tenancy is never inferred from a header or body.
     *
     * 用法 / Usage: 内部辅助 / Internal helper: {@code GatewayPersistenceContextFilter.verifiedIdentityTenantId()}。
     * @return 返回 已验证身份携带的租户字符串，或 {@code null}；returns the tenant carried by the verified identity, or {@code null}.
     */
    private String verifiedIdentityTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return TENANT_CLAIMS.stream()
                .map(claim -> jwt.getToken().getClaim(claim))
                .filter(Objects::nonNull)
                .findFirst()
                .map(String::valueOf)
                .orElse(null);
    }
}
