package top.egon.cola.platform.idp.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * 从单个 Bearer 访问令牌建立仅包含身份的 Spring Security 上下文。
 * 缺少令牌时继续过滤链，令牌格式或身份验证失败时返回统一的 401 JSON 响应；
 * {@code /internal/} 路径由内部调用机制处理，因此本过滤器跳过该路径。
 *
 * <p>Establishes an identity-only Spring Security context from one Bearer access token. Requests
 * without a token continue through the chain, while malformed or invalid tokens receive a uniform
 * JSON 401 response. Paths under {@code /internal/} are skipped because internal-call mechanisms
 * own their authentication.</p>
 */
public final class IdpBearerAuthenticationFilter extends OncePerRequestFilter {

    /**
     * 接受的 Bearer 凭据最大字符数，限制异常超长认证头。
     *
     * <p>Maximum accepted Bearer credential length, limiting abnormally large authorization
     * headers.</p>
     */
    private static final int MAX_CREDENTIAL_LENGTH = 8192;

    /**
     * 访问令牌与实时用户状态验证器。
     *
     * <p>Access-token and current-user-state verifier.</p>
     */
    private final IdpJwtVerifier jwtVerifier;

    /**
     * 认证失败响应的 JSON 序列化器。
     *
     * <p>JSON serializer for authentication failure responses.</p>
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建 IdP Bearer 身份过滤器。
     *
     * <p>Creates the IdP Bearer identity filter.</p>
     *
     * @param jwtVerifier 访问令牌验证器；access-token verifier
     * @param objectMapper 失败响应序列化器；failure-response serializer
     */
    public IdpBearerAuthenticationFilter(
            IdpJwtVerifier jwtVerifier,
            ObjectMapper objectMapper
    ) {
        this.jwtVerifier = Objects.requireNonNull(jwtVerifier, "jwtVerifier");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * 判断请求是否属于应跳过的内部调用路径。
     *
     * <p>Determines whether the request belongs to an internal path that this filter must skip.</p>
     *
     * @param request 当前 HTTP 请求；current HTTP request
     * @return {@code true} 表示跳过；{@code true} when filtering should be skipped
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/internal/");
    }

    /**
     * 提取并验证 Authorization Bearer 凭据，在调用链期间建立身份上下文，并在结束后清理。
     *
     * <p>Extracts and verifies the Authorization Bearer credential, establishes the identity
     * context for the downstream chain, and clears it afterward.</p>
     *
     * @param request 当前 HTTP 请求；current HTTP request
     * @param response 当前 HTTP 响应；current HTTP response
     * @param filterChain 后续 Servlet 过滤链；remaining Servlet filter chain
     * @throws ServletException 当后续过滤器处理失败时；when downstream filter processing fails
     * @throws IOException 当读取请求或写入响应失败时；when request reading or response writing
     *                     fails
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (headers.size() != 1 || !isBearer(headers.getFirst())) {
            unauthorized(response, "AUTHORIZATION_HEADER_INVALID");
            return;
        }
        String token = headers.getFirst().substring("Bearer ".length()).trim();
        if (token.isEmpty() || token.contains(",")
                || token.length() > MAX_CREDENTIAL_LENGTH) {
            unauthorized(response, "AUTHORIZATION_HEADER_INVALID");
            return;
        }
        try {
            var principal = jwtVerifier.verify(token);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new IdpAuthenticationToken(principal));
            SecurityContextHolder.setContext(context);
            filterChain.doFilter(request, response);
        } catch (IdpJwtVerifier.InvalidTokenException exception) {
            unauthorized(response, exception.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 判断认证头是否使用不区分大小写的 Bearer 方案。
     *
     * <p>Checks whether an authorization header uses the case-insensitive Bearer scheme.</p>
     *
     * @param header Authorization 头值；Authorization header value
     * @return {@code true} 表示 Bearer 方案格式有效；{@code true} when the Bearer scheme prefix
     *         is valid
     */
    private boolean isBearer(String header) {
        return header != null
                && header.length() >= "Bearer ".length()
                && header.regionMatches(
                        true, 0, "Bearer ", 0, "Bearer ".length());
    }

    /**
     * 写入统一的身份验证失败响应。
     *
     * <p>Writes the uniform identity-authentication failure response.</p>
     *
     * @param response 当前 HTTP 响应；current HTTP response
     * @param reasonCode 可稳定识别的失败原因码；stable failure reason code
     * @throws IOException 当 JSON 响应无法写出时；when the JSON response cannot be written
     */
    private void unauthorized(HttpServletResponse response, String reasonCode)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "code", reasonCode,
                "message", "IdP authentication failed"));
    }
}
