package top.egon.cola.platform.idp.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.idp.contract.IdpPrincipal;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * 从单个 Bearer 访问令牌建立仅包含身份的 Spring Security 上下文。
 * 缺少令牌时继续过滤链，令牌格式或身份验证失败时返回统一的 401 JSON 响应；端点
 * 认证类型由 {@link IdpEndpointAuthenticationPolicy} 明确选择。
 *
 * <p>Establishes an identity-only Spring Security context from one Bearer access token. Requests
 * without a token continue through the chain, while malformed or invalid tokens receive a uniform
 * JSON 401 response. Endpoint ownership is selected by the explicit policy.</p>
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
     * 访问令牌验证器。
     *
     * <p>Access-token verifier.</p>
     */
    private final UserAccessTokenVerifier userAccessTokenVerifier;

    /**
     * Explicit SERVICE access-token verifier.
     */
    private final ServiceAccessTokenVerifier serviceAccessTokenVerifier;

    /**
     * Endpoint policy selecting PUBLIC, USER or SERVICE handling.
     */
    private final IdpEndpointAuthenticationPolicy endpointAuthenticationPolicy;

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
        this(
                new UserAccessTokenVerifier(jwtVerifier),
                new ServiceAccessTokenVerifier(jwtVerifier),
                new IdpEndpointAuthenticationPolicy(),
                objectMapper);
    }

    /**
     * Creates a filter with explicit USER/SERVICE verifiers and endpoint policy.
     *
     * <p>The filter never falls back from one principal type to the other. This keeps endpoint
     * ownership explicit and prevents a SERVICE token from being accepted on a USER path (or the
     * reverse).</p>
     */
    public IdpBearerAuthenticationFilter(
            UserAccessTokenVerifier userAccessTokenVerifier,
            ServiceAccessTokenVerifier serviceAccessTokenVerifier,
            IdpEndpointAuthenticationPolicy endpointAuthenticationPolicy,
            ObjectMapper objectMapper
    ) {
        this.userAccessTokenVerifier = Objects.requireNonNull(
                userAccessTokenVerifier, "userAccessTokenVerifier");
        this.serviceAccessTokenVerifier = Objects.requireNonNull(
                serviceAccessTokenVerifier, "serviceAccessTokenVerifier");
        this.endpointAuthenticationPolicy = Objects.requireNonNull(
                endpointAuthenticationPolicy, "endpointAuthenticationPolicy");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
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
        IdpEndpointAuthenticationPolicy.Requirement requirement =
                endpointAuthenticationPolicy.requirement(request);
        if (requirement == IdpEndpointAuthenticationPolicy.Requirement.DENY) {
            unauthorized(response, "ENDPOINT_AUTHENTICATION_POLICY_INVALID");
            return;
        }
        if (requirement == IdpEndpointAuthenticationPolicy.Requirement.PUBLIC) {
            filterChain.doFilter(request, response);
            return;
        }
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
            IdpPrincipal principal;
            boolean userToken = requirement == IdpEndpointAuthenticationPolicy.Requirement.USER;
            if (userToken) {
                var verification = userAccessTokenVerifier.verify(token);
                if (verification instanceof AccessTokenVerification.Valid<?> valid) {
                    principal = valid.principal();
                } else {
                    unauthorized(response, reason(verification));
                    return;
                }
            } else {
                var verification = serviceAccessTokenVerifier.verify(token);
                if (verification instanceof AccessTokenVerification.Valid<?> valid) {
                    principal = valid.principal();
                } else {
                    unauthorized(response, reason(verification));
                    return;
                }
            }
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new IdpAuthenticationToken(principal));
            SecurityContextHolder.setContext(context);
            if (userToken) {
                VerifiedUserTokenCarrier.set(request, token);
            }
            filterChain.doFilter(request, response);
        } finally {
            VerifiedUserTokenCarrier.clear(request);
            SecurityContextHolder.clearContext();
        }
    }

    private String reason(AccessTokenVerification<?> verification) {
        if (verification instanceof AccessTokenVerification.Expired<?>) {
            return "JWT_EXPIRED";
        }
        if (verification instanceof AccessTokenVerification.Invalid<?> invalid) {
            return invalid.reasonCode();
        }
        return "JWT_INVALID";
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
