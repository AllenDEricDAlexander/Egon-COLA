package top.egon.cola.component.yuheng.llm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 中文说明：{@code LlmClientCredentialFilter} 只做一件事：让 Anthropic Messages 入口能像官方 SDK 那样用
 * {@code x-api-key} 承载<b>企业签发的 SERVICE token</b>。它注册在既有身份过滤器之前（见 {@link #FILTER_ORDER}），
 * 因此在它眼里请求还没被认证：它把「只有 x-api-key」的请求改写为等价的 {@code Authorization: Bearer <同一个 token>}
 * 交给下游，把「同时带 Authorization 与 x-api-key 且两者不同」的请求直接按原生 Messages 错误形状拒成 400——这既不是
 * 认证也不是授权，验证与赋权仍然完全由 Tianquan 过滤器负责，本类不解析 token 内容、不做任何权限判断，也不向上游转发
 * 客户端凭据（上游的 Authorization/x-api-key 由 engine 用 secretRef 重建）。任何日志与错误正文都不含 token 片段。
 * English summary: {@code LlmClientCredentialFilter} does exactly one thing: it lets the Anthropic Messages entry carry
 * the <b>enterprise-issued SERVICE token</b> in {@code x-api-key}, as official SDKs do. Registered before the existing
 * identity filters (see {@link #FILTER_ORDER}), it sees requests that are not yet authenticated: it rewrites a request
 * carrying only {@code x-api-key} into the equivalent {@code Authorization: Bearer <the same token>} for the downstream
 * chain, and rejects a request carrying both headers with different values as a native-shaped Messages 400. This is
 * neither authentication nor authorization — verification and granting stay entirely with the Tianquan filters; this
 * class never parses the token, makes no access decision, and never forwards client credentials upstream (the engine
 * rebuilds the upstream Authorization/x-api-key from the secretRef). No log line and no error body contains a token fragment.
 *
 * 用法 / Usage: 由 {@code LlmGatewayConfiguration} 以 {@link #FILTER_ORDER} 注册为容器过滤器，不应标注
 * {@code @Component}（那会让它在装配顺序不确定时被重复注册）。/ Register it from {@code LlmGatewayConfiguration} as a
 * servlet filter with {@link #FILTER_ORDER}; it must not carry {@code @Component}, which would register it twice at an
 * unpredictable order.
 */
@Slf4j
@RequiredArgsConstructor
public final class LlmClientCredentialFilter extends OncePerRequestFilter {

    /**
     * 中文说明：注册顺序必须早于 Spring Security 的默认链顺序（{@code -100}），否则企业 token 已经按「无凭据」被判完，
     * 适配就没有意义。
     * English summary: the registered order must precede Spring Security's default chain order ({@code -100}), otherwise
     * the enterprise token would already have been assessed as absent and the adaptation would be meaningless.
     */
    public static final int FILTER_ORDER = -200;

    /** 中文说明：唯一受本适配器影响的入口路径，其余四种协议一律原样放行。 English summary: the only entry path this adapter touches; the other three protocols pass through untouched. */
    private static final String MESSAGES_ENTRY_PATH = "/v1/messages";

    /** 中文说明：Anthropic SDK 承载企业 token 的头名。 English summary: the header name the Anthropic SDK uses to carry the enterprise token. */
    private static final String API_KEY_HEADER = "x-api-key";

    /** 中文说明：{@code Authorization} 的 Bearer 前缀，大小写不敏感比较用。 English summary: the Bearer prefix of {@code Authorization}, used for case-insensitive comparison. */
    private static final String BEARER_PREFIX = "Bearer ";

    /** 中文说明：原生错误编码器，Messages 路由固定使用，不复用 admin 业务错误 wrapper。 English summary: the native error encoder, fixed for the Messages route and never the admin business error wrapper. */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：只处理 Messages 入口的同步派发；其余路径与本类的凭据语义无关，直接放行。
     * English summary: Handles only the synchronous Messages dispatch; other paths are irrelevant to credential adaptation
     * and pass straight through.
     *
     * 用法 / Usage: 由 {@link OncePerRequestFilter} 调度 / Consulted by {@link OncePerRequestFilter}.
     * @param request 参数 当前请求；parameter the current request.
     * @return 返回 是否跳过本过滤器；returns whether this filter should be skipped.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !MESSAGES_ENTRY_PATH.equals(StringUtils.removeEnd(request.getRequestURI(), "/"));
    }

    /**
     * 中文说明：先比较两个承载头：只有 x-api-key 时改写为等价的 Bearer 并停止对外暴露 x-api-key；两者都有且值不同按原生
     * 400 拒绝且不再进入下游；两者都缺省则原样放行给 Tianquan。下游异常如实传播，不吞异常也不伪造成功。
     * English summary: Compares the two carrier headers first: with only x-api-key it rewrites to the equivalent Bearer and
     * stops exposing x-api-key downstream; with both headers carrying different values it answers a native 400 and never
     * reaches the chain; when both are absent it passes through to Tianquan unchanged. Downstream failures propagate
     * unchanged, so nothing is swallowed and no success is fabricated.
     *
     * 用法 / Usage: 容器过滤器入口 / Entry point invoked by the servlet container:
     * {@code LlmClientCredentialFilter.doFilterInternal(request, response, chain)}。
     * @param request 参数 当前请求；parameter the current request.
     * @param response 参数 当前响应；parameter the current response.
     * @param filterChain 参数 下游链；parameter the downstream chain.
     * @throws ServletException 下游抛出的非 IO 受检异常；a checked non-IO failure raised downstream.
     * @throws IOException 下游 IO 失败；an IO failure from downstream.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String apiKey = StringUtils.trimToNull(request.getHeader(API_KEY_HEADER));
        String authorization = StringUtils.trimToNull(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (apiKey == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (authorization != null && !bearerValue(authorization).equals(apiKey)) {
            log.warn("llm messages credential headers conflict path={}", MESSAGES_ENTRY_PATH);
            writeNativeConflict(response);
            return;
        }
        filterChain.doFilter(new BearerCarriedRequest(request, apiKey), response);
    }

    /**
     * 中文说明：按原生 Messages 错误形状写出 400（{@code type=error} 加原生 error 对象），消息只说明冲突，绝不回显任何
     * token 片段。
     * English summary: Writes the 400 in the native Messages error shape ({@code type=error} plus the native error object)
     * naming only the conflict, never echoing any token fragment.
     */
    private void writeNativeConflict(HttpServletResponse response) throws IOException {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("type", "error");
        error.putObject("error")
                .put("type", "invalid_request_error")
                .put("message", "Authorization and x-api-key must not disagree");
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), error);
    }

    /**
     * 中文说明：取 Bearer 值，比较时忽略大小写前缀与首尾空白；不校验 token 内容，那属于 Tianquan。
     * English summary: Extracts the Bearer value, ignoring prefix case and surrounding whitespace, and validates nothing
     * about the token itself, which is Tianquan's job.
     */
    private static String bearerValue(String authorization) {
        String value = authorization;
        if (StringUtils.startsWithIgnoreCase(value, BEARER_PREFIX)) {
            value = value.substring(BEARER_PREFIX.length());
        }
        return StringUtils.trimToEmpty(value);
    }

    /**
     * 中文说明：把企业 token 以 {@code Authorization: Bearer} 呈现给下游，并对下游隐藏 {@code x-api-key}，
     * 使任何组件都不可能把它误当成供应商凭据继续往上游转发。请求的其余部分保持原样。
     * English summary: Presents the enterprise token downstream as {@code Authorization: Bearer} and hides
     * {@code x-api-key} from it, so no component can mistake that header for a vendor credential and relay it upstream.
     * Everything else about the request stays exactly as received.
     */
    private static final class BearerCarriedRequest extends HttpServletRequestWrapper {

        private final String bearerValue;

        private BearerCarriedRequest(HttpServletRequest request, String token) {
            super(request);
            this.bearerValue = BEARER_PREFIX + token;
        }

        @Override
        public String getHeader(String name) {
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                return bearerValue;
            }
            return API_KEY_HEADER.equalsIgnoreCase(name) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(bearerValue));
            }
            if (API_KEY_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of());
            }
            return Collections.enumeration(Collections.list(super.getHeaders(name)));
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()).stream()
                    .filter(name -> !API_KEY_HEADER.equalsIgnoreCase(name))
                    .toList());
            names.add(HttpHeaders.AUTHORIZATION);
            return Collections.enumeration(names);
        }
    }
}
