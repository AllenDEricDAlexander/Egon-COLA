package top.egon.cola.platform.tianquan.shoubing.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal;

import java.io.IOException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 从单个 Bearer 访问令牌建立仅包含身份的 Spring Security 上下文。
 * 缺少令牌时继续过滤链，令牌格式或身份验证失败时返回统一的 401 JSON 响应；端点
 * 认证类型由 {@link IdpEndpointAuthenticationPolicy} 明确选择。
 *
 * <p>Establishes an identity-only Spring Security context from one Bearer access token. Requests
 * without a token continue through the chain, while malformed or invalid tokens receive a uniform
 * JSON 401 response. Endpoint ownership is selected by the explicit policy.</p>
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
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
    @Qualifier("userAccessTokenVerifier")
    private final UserAccessTokenVerifier userAccessTokenVerifier;

    /**
     * Explicit SERVICE access-token verifier.
     */
    @Qualifier("serviceAccessTokenVerifier")
    private final ServiceAccessTokenVerifier serviceAccessTokenVerifier;

    /**
     * Endpoint policy selecting PUBLIC, USER or SERVICE handling.
     */
    @Qualifier("idpEndpointAuthenticationPolicy")
    private final IdpEndpointAuthenticationPolicy endpointAuthenticationPolicy;

    /**
     * 认证失败响应的 JSON 序列化器。
     *
     * <p>JSON serializer for authentication failure responses.</p>
     */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /** Combined verifier used on explicit USER-or-SERVICE paths to avoid a second decode. */
    @Qualifier("idpJwtVerifier")
    private final IdpJwtVerifier combinedVerifier;

    /** Binds verified identity MDC while the downstream authorization chain runs. */
    @Qualifier("verifiedTenantContextFilter")
    private final VerifiedTenantContextFilter verifiedTenantContextFilter;

    /**
     * 创建 Tianquan-Shoubing Bearer 身份过滤器。
     *
     * <p>Creates the Tianquan-Shoubing Bearer identity filter.</p>
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
                objectMapper,
                jwtVerifier,
                new VerifiedTenantContextFilter());
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
        this(userAccessTokenVerifier,
                serviceAccessTokenVerifier,
                endpointAuthenticationPolicy,
                objectMapper,
                null,
                new VerifiedTenantContextFilter());
    }

    /** Creates the filter with the shared verified identity scope and single-pass verifier. */
    public static IdpBearerAuthenticationFilter create(
            UserAccessTokenVerifier userAccessTokenVerifier,
            ServiceAccessTokenVerifier serviceAccessTokenVerifier,
            IdpEndpointAuthenticationPolicy endpointAuthenticationPolicy,
            ObjectMapper objectMapper,
            VerifiedTenantContextFilter verifiedTenantContextFilter,
            IdpJwtVerifier combinedVerifier
    ) {
        return new IdpBearerAuthenticationFilter(
                Objects.requireNonNull(userAccessTokenVerifier, "userAccessTokenVerifier"),
                Objects.requireNonNull(serviceAccessTokenVerifier, "serviceAccessTokenVerifier"),
                Objects.requireNonNull(endpointAuthenticationPolicy, "endpointAuthenticationPolicy"),
                Objects.requireNonNull(objectMapper, "objectMapper"),
                combinedVerifier,
                Objects.requireNonNull(verifiedTenantContextFilter, "verifiedTenantContextFilter"));
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
            preAuthenticationFailure(request, response, "ENDPOINT_AUTHENTICATION_POLICY_INVALID");
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
            preAuthenticationFailure(request, response, "AUTHORIZATION_HEADER_INVALID");
            return;
        }
        String token = headers.getFirst().substring("Bearer ".length()).trim();
        if (token.isEmpty() || token.contains(",")
                || token.length() > MAX_CREDENTIAL_LENGTH) {
            preAuthenticationFailure(request, response, "AUTHORIZATION_HEADER_INVALID");
            return;
        }
        SecurityContext previousContext = null;
        boolean restorePreviousContext = false;
        boolean contextInstalled = false;
        try {
            IdpPrincipal principal;
            boolean userToken;
            Set<String> verifiedScopes;
            top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO proof = null;
            if (requirement
                    == IdpEndpointAuthenticationPolicy.Requirement.USER_OR_SERVICE) {
                if (combinedVerifier != null) {
                    var verification = combinedVerifier.verifyAny(token);
                    if (verification instanceof AccessTokenVerification.Valid<?> valid) {
                        principal = valid.principal();
                        userToken = principal.principalType()
                                == top.egon.cola.platform.tianquan.shoubing.contract.PrincipalType.USER;
                        verifiedScopes = valid.scopes();
                        proof = valid.proof();
                    } else {
                        authenticationFailure(response, verification);
                        return;
                    }
                } else {
                    var verification = userAccessTokenVerifier.verify(token);
                    if (verification instanceof AccessTokenVerification.Valid<?> valid) {
                        principal = valid.principal();
                        userToken = true;
                        verifiedScopes = valid.scopes();
                        proof = valid.proof();
                    } else if (principalTypeMismatch(verification)) {
                        var serviceVerification = serviceAccessTokenVerifier.verify(token);
                        if (serviceVerification instanceof AccessTokenVerification.Valid<?> valid) {
                            principal = valid.principal();
                            userToken = false;
                            verifiedScopes = valid.scopes();
                            proof = valid.proof();
                        } else {
                            authenticationFailure(response, serviceVerification);
                            return;
                        }
                    } else {
                        authenticationFailure(response, verification);
                        return;
                    }
                }
            } else if (requirement
                    == IdpEndpointAuthenticationPolicy.Requirement.USER) {
                var verification = userAccessTokenVerifier.verify(token);
                if (verification instanceof AccessTokenVerification.Valid<?> valid) {
                    principal = valid.principal();
                    userToken = true;
                    verifiedScopes = valid.scopes();
                    proof = valid.proof();
                } else {
                    authenticationFailure(response, verification);
                    return;
                }
            } else {
                var verification = serviceAccessTokenVerifier.verify(token);
                if (verification instanceof AccessTokenVerification.Valid<?> valid) {
                    principal = valid.principal();
                    userToken = false;
                    verifiedScopes = valid.scopes();
                    proof = valid.proof();
                } else {
                    authenticationFailure(response, verification);
                    return;
                }
            }
            previousContext = SecurityContextHolder.getContext();
            restorePreviousContext = previousContext.getAuthentication() != null;
            var context = SecurityContextHolder.createEmptyContext();
            var authentication = new IdpAuthenticationToken(principal, scopeAuthorities(verifiedScopes));
            authentication.setDetails(proof);
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            contextInstalled = true;
            if (userToken) {
                VerifiedUserTokenCarrier.set(request, token);
            }
            verifiedTenantContextFilter.doFilter(request, response, filterChain);
        } finally {
            VerifiedUserTokenCarrier.clear(request);
            if (contextInstalled && restorePreviousContext) {
                SecurityContextHolder.setContext(previousContext);
            } else if (contextInstalled) {
                SecurityContextHolder.clearContext();
            }
        }
    }

    private List<GrantedAuthority> scopeAuthorities(Set<String> scopes) {
        List<GrantedAuthority> authorities = new ArrayList<>(scopes.size());
        for (String scope : scopes) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
        }
        return List.copyOf(authorities);
    }

    private void preAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, String reason) throws IOException {
        if (!IdpSecurityObservationFilter.beforeAuthenticationFailure(request)) {
            temporarilyUnavailable(response, "OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
            return;
        }
        unauthorized(response, reason);
    }

    private void authenticationFailure(
            HttpServletResponse response,
            AccessTokenVerification<?> verification) throws IOException {
        if (verification instanceof AccessTokenVerification.Unavailable<?> unavailable) {
            temporarilyUnavailable(response, unavailable.reasonCode());
        } else {
            unauthorized(response, reason(verification));
        }
    }

    private boolean principalTypeMismatch(
            AccessTokenVerification<?> verification) {
        return verification instanceof AccessTokenVerification.Invalid<?> invalid
                && "JWT_PRINCIPAL_TYPE_INVALID".equals(invalid.reasonCode());
    }

    private String reason(AccessTokenVerification<?> verification) {
        if (verification instanceof AccessTokenVerification.Unavailable<?> unavailable) {
            return unavailable.reasonCode();
        }
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
        response.setHeader("Cache-Control", "no-store");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "code", reasonCode,
                "message", "Tianquan-Shoubing authentication failed"));
    }

    private void temporarilyUnavailable(HttpServletResponse response, String reasonCode)
            throws IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "code", reasonCode,
                "message", "Tianquan-Shoubing authentication state is temporarily unavailable"));
    }
}
