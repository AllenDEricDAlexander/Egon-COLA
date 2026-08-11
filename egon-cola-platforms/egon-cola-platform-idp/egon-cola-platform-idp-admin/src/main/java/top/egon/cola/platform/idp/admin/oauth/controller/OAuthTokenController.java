package top.egon.cola.platform.idp.admin.oauth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthErrorVO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthTokenVO;
import top.egon.cola.platform.idp.admin.oauth.repo.IdpSsoSessionStore;
import top.egon.cola.platform.idp.admin.oauth.service.impl.PrivateKeyJwtAuthenticator;
import top.egon.cola.platform.idp.admin.support.ddc.IdpRuntimePolicy;
import top.egon.cola.platform.idp.admin.support.security.IdpSsoAuthenticationFilter;
import top.egon.cola.platform.idp.admin.token.service.impl.ClientCredentialsTokenService;
import top.egon.cola.platform.idp.core.oauth.AuthorizationCode;
import top.egon.cola.platform.idp.core.oauth.AuthorizationFacade;
import top.egon.cola.platform.idp.core.oauth.ClientAssertionAuthentication;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.oauth.OAuthException;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.token.TokenException;
import top.egon.cola.platform.idp.core.token.TokenFacade;
import top.egon.cola.platform.idp.core.token.ServiceAccessToken;

import java.net.URI;
import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * OAuth Token、撤销与退出端点控制器。
 *
 * <p>Controller for the OAuth token, revocation, and logout endpoints.</p>
 */
@RestController
public class OAuthTokenController {

    /** 每个 Client 独立 Refresh Cookie 的名称前缀；per-Client refresh-cookie name prefix. */
    public static final String REFRESH_COOKIE_PREFIX = "EGON_IDP_REFRESH_";

    /** Refresh 与 SSO Cookie 的受限路径；restricted path for refresh and SSO cookies. */
    private static final String REFRESH_COOKIE_PATH = "/oauth2";

    /** 授权码用例门面；authorization-code use-case facade. */
    private final AuthorizationFacade authorizations;

    /** Token 生命周期门面；token-lifecycle facade. */
    private final TokenFacade tokens;

    /** {@code private_key_jwt} Client 认证器；Client authenticator. */
    private final PrivateKeyJwtAuthenticator clientAuthenticator;

    /** Client Credentials SERVICE Token 签发服务；SERVICE token issuance service. */
    private final ClientCredentialsTokenService clientCredentialsTokens;

    /** OAuth Client 查询端口；OAuth Client lookup port. */
    private final OAuthClientStore clients;

    /** SSO 会话存储；SSO-session store. */
    private final IdpSsoSessionStore ssoSessions;

    /** 动态 OAuth 策略；dynamic OAuth policy. */
    private final IdpRuntimePolicy runtimePolicy;

    /** UTC 业务时钟；UTC business clock. */
    private final Clock clock;

    /** Cookie 是否强制 Secure；whether cookies require Secure. */
    private final boolean secureCookie;

    /**
     * 创建 OAuth Token 传输控制器。
     *
     * <p>Creates the OAuth token transport controller.</p>
     *
     * @param authorizations 授权码用例门面；authorization-code use-case facade
     * @param tokens Token 生命周期门面；token-lifecycle facade
     * @param clientAuthenticator {@code private_key_jwt} 认证器；Client authenticator
     * @param clientCredentialsTokens SERVICE Token 签发服务；SERVICE token issuance service
     * @param clients OAuth Client 查询端口；OAuth Client lookup port
     * @param ssoSessions SSO 会话存储；SSO-session store
     * @param runtimePolicy 动态 OAuth 策略；dynamic OAuth policy
     * @param clock UTC 业务时钟；UTC business clock
     * @param secureCookie Cookie 是否强制 Secure；whether cookies require Secure
     */
    public OAuthTokenController(
            AuthorizationFacade authorizations,
            TokenFacade tokens,
            PrivateKeyJwtAuthenticator clientAuthenticator,
            ClientCredentialsTokenService clientCredentialsTokens,
            OAuthClientStore clients,
            IdpSsoSessionStore ssoSessions,
            IdpRuntimePolicy runtimePolicy,
            @Qualifier("idpClock") Clock clock,
            @Value("${egon.idp.oauth.refresh-cookie-secure:true}")
            boolean secureCookie
    ) {
        this.authorizations = Objects.requireNonNull(
                authorizations,
                "authorizations"
        );
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.clientAuthenticator = Objects.requireNonNull(
                clientAuthenticator,
                "clientAuthenticator"
        );
        this.clientCredentialsTokens = Objects.requireNonNull(
                clientCredentialsTokens,
                "clientCredentialsTokens"
        );
        this.clients = Objects.requireNonNull(clients, "clients");
        this.ssoSessions = Objects.requireNonNull(ssoSessions, "ssoSessions");
        this.runtimePolicy = Objects.requireNonNull(
                runtimePolicy,
                "runtimePolicy"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.secureCookie = secureCookie;
    }

    /**
     * 使用授权码、Refresh Cookie 或 Client Credentials 签发单 Resource Access Token。
     *
     * <p>Issues a single-Resource access token from an authorization code, refresh cookie, or
     * Client Credentials.</p>
     *
     * @param form 原始表单参数；raw form parameters
     * @param request HTTP 请求，用于读取 HttpOnly Cookie；HTTP request used to read HttpOnly cookies
     * @return Access Token 响应；USER 流程同时轮换 Refresh Cookie；access-token response, with
     * a rotated refresh cookie for USER flows
     */
    @PostMapping(
            value = "/oauth2/token",
            consumes = "application/x-www-form-urlencoded"
    )
    public ResponseEntity<OAuthTokenVO> token(
            @RequestParam MultiValueMap<String, String> form,
            HttpServletRequest request
    ) {
        if (form.containsKey("audience")) {
            throw oauth("invalid_request");
        }
        String grantType = single(form, "grant_type");
        IdpRuntimePolicy.Snapshot policy = runtimePolicy.current();
        if ("client_credentials".equals(grantType)) {
            return clientCredentials(form, policy.accessTokenTtl());
        }
        OAuthClient client = activeClient(single(form, "client_id"));
        String refreshCookie = cookieValue(
                request,
                refreshCookieName(client.clientId())
        );
        TokenFacade.TokenPair pair;
        if ("authorization_code".equals(grantType)) {
            String resource = single(form, "resource");
            AuthorizationCode authorizationCode = authorizations.consume(
                    single(form, "code"),
                    single(form, "code_verifier"),
                    single(form, "redirect_uri"),
                    client.clientId(),
                    resource
            );
            pair = tokens.issue(
                    authorizationCode,
                    policy.accessTokenTtl(),
                    policy.refreshTokenTtl()
            );
        } else if ("refresh_token".equals(grantType)) {
            if (form.containsKey("refresh_token")) {
                throw oauth("invalid_request");
            }
            String resource = single(form, "resource");
            pair = tokens.refresh(
                    required(refreshCookie),
                    client.clientId(),
                    resource,
                    policy.accessTokenTtl()
            );
        } else {
            throw oauth("unsupported_grant_type");
        }
        long expiresIn = Duration.between(
                clock.instant(),
                pair.accessExpiresAt()
        ).toSeconds();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Pragma", "no-cache")
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(pair, client.clientId()).toString()
                )
                .body(new OAuthTokenVO(
                        pair.accessToken(),
                        "Bearer",
                        Math.max(0L, expiresIn)
                ));
    }

    /**
     * 认证 Confidential Client 并签发不含 Refresh Token 的 SERVICE Token。
     *
     * <p>Authenticates a Confidential Client and issues a SERVICE token without a refresh token.</p>
     *
     * @param form OAuth 表单；OAuth form
     * @param accessTokenTtl 动态短期 Token 有效期；dynamic short-lived token lifetime
     * @return SERVICE Token 响应；SERVICE token response
     */
    private ResponseEntity<OAuthTokenVO> clientCredentials(
            MultiValueMap<String, String> form,
            Duration accessTokenTtl
    ) {
        String clientId = single(form, "client_id");
        ClientAssertionAuthentication authentication =
                clientAuthenticator.authenticate(
                        single(form, "client_assertion_type"),
                        clientId,
                        single(form, "client_assertion")
                );
        ServiceAccessToken token = clientCredentialsTokens.issue(
                authentication,
                resource(single(form, "resource")),
                single(form, "tenant_id"),
                scopes(single(form, "scope")),
                accessTokenTtl
        );
        long expiresIn = Duration.between(
                clock.instant(),
                token.expiresAt()
        ).toSeconds();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Pragma", "no-cache")
                .body(new OAuthTokenVO(
                        token.accessToken(),
                        token.tokenType(),
                        Math.max(0L, expiresIn)
                ));
    }

    /**
     * 撤销当前 Client 的 Refresh Token，且不泄露 Token 是否曾有效。
     *
     * <p>Revokes the current Client refresh token without revealing whether it was valid.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param request HTTP 请求；HTTP request
     * @return 空成功响应和过期 Cookie；empty success response and expired cookie
     */
    @PostMapping(
            value = "/oauth2/revoke",
            consumes = "application/x-www-form-urlencoded"
    )
    public ResponseEntity<Void> revoke(
            @RequestParam("client_id") String clientId,
            HttpServletRequest request
    ) {
        OAuthClient client = activeClient(clientId);
        String refreshCookie = cookieValue(
                request,
                refreshCookieName(client.clientId())
        );
        if (refreshCookie != null && !refreshCookie.isBlank()) {
            try {
                tokens.revoke(refreshCookie, client.clientId());
            } catch (TokenException ignored) {
                // OAuth 撤销端点不披露 Token 是否有效。
                // OAuth revocation does not reveal whether a token was valid.
            }
        }
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        expiredRefreshCookie(client.clientId()).toString()
                )
                .build();
    }

    /**
     * 撤销当前会话或用户全部会话并清除浏览器 Cookie。
     *
     * <p>Revokes the current or all user sessions and clears browser cookies.</p>
     *
     * @param allSessions 是否撤销用户全部会话；whether to revoke all user sessions
     * @param clientId Client 标识；Client identifier
     * @param request HTTP 请求；HTTP request
     * @param principal 当前身份；current identity
     * @return 无内容响应及过期 Cookie；no-content response with expired cookies
     */
    @PostMapping("/oauth2/logout")
    public ResponseEntity<Void> logout(
            @RequestParam(name = "all_sessions", defaultValue = "false")
            boolean allSessions,
            @RequestParam(name = "client_id") String clientId,
            HttpServletRequest request,
            Principal principal
    ) {
        OAuthClient client = activeClient(clientId);
        String refreshCookie = cookieValue(
                request,
                refreshCookieName(client.clientId())
        );
        String ssoCookie = cookieValue(
                request,
                IdpSsoAuthenticationFilter.COOKIE_NAME
        );
        if (allSessions) {
            if (principal == null || principal.getName() == null) {
                throw oauth("invalid_request");
            }
            tokens.logoutAll(principal.getName());
        } else if (refreshCookie != null && !refreshCookie.isBlank()) {
            tokens.revoke(refreshCookie, client.clientId());
        }
        if (ssoCookie != null && !ssoCookie.isBlank()) {
            ssoSessions.revoke(ssoCookie);
        }
        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        expiredRefreshCookie(client.clientId()).toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        expiredSsoCookie().toString()
                )
                .build();
    }

    /**
     * 将 OAuth 和 Token 异常映射为安全协议错误。
     *
     * <p>Maps OAuth and token exceptions to a safe protocol error.</p>
     *
     * @param exception OAuth 或 Token 异常；OAuth or token exception
     * @return OAuth 错误响应；OAuth error response
     */
    @ExceptionHandler({OAuthException.class, TokenException.class})
    public ResponseEntity<OAuthErrorVO> oauthError(RuntimeException exception) {
        String error = exception instanceof OAuthException oauthException
                ? oauthException.oauthError()
                : ((TokenException) exception).oauthError();
        HttpStatus status = "invalid_client".equals(error)
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(
                new OAuthErrorVO(error, "OAuth request is invalid")
        );
    }

    /**
     * 校验 RFC 8707 Resource URI。
     *
     * <p>Validates an RFC 8707 Resource URI.</p>
     *
     * @param value 原始 Resource；raw Resource
     * @return 已校验 URI；validated URI
     */
    private static URI resource(String value) {
        try {
            URI uri = URI.create(required(value));
            if (!uri.isAbsolute()
                    || uri.getFragment() != null
                    || !uri.equals(uri.normalize())) {
                throw oauth("invalid_target");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw oauth("invalid_target");
        }
    }

    /**
     * 解析并拒绝重复的空格分隔 OAuth Scope。
     *
     * <p>Parses space-delimited OAuth scopes and rejects duplicates.</p>
     *
     * @param value 原始 Scope 文本；raw scope text
     * @return 排序后的不可变 Scope；sorted immutable scopes
     */
    private static Set<String> scopes(String value) {
        String raw = required(value);
        if (raw.contains("  ")) {
            throw oauth("invalid_scope");
        }
        String[] parts = raw.split(" ");
        TreeSet<String> result = new TreeSet<>();
        Collections.addAll(result, parts);
        if (result.isEmpty()
                || result.size() != parts.length
                || result.stream().anyMatch(String::isBlank)) {
            throw oauth("invalid_scope");
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * 查询并校验 ACTIVE OAuth Client。
     *
     * <p>Finds and validates an ACTIVE OAuth Client.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @return ACTIVE OAuth Client；ACTIVE OAuth Client
     */
    private OAuthClient activeClient(String clientId) {
        OAuthClient client = clients.findById(required(clientId))
                .orElseThrow(() -> oauth("unauthorized_client"));
        if (client.status() != OAuthClient.Status.ACTIVE) {
            throw oauth("unauthorized_client");
        }
        return client;
    }

    /**
     * 构造轮换后的 HttpOnly Refresh Cookie。
     *
     * <p>Builds the rotated HttpOnly refresh cookie.</p>
     *
     * @param pair Token 对；token pair
     * @param clientId Client 标识；Client identifier
     * @return Refresh Cookie；refresh cookie
     */
    private ResponseCookie refreshCookie(
            TokenFacade.TokenPair pair,
            String clientId
    ) {
        Duration maxAge = Duration.between(
                clock.instant(),
                pair.refreshExpiresAt()
        );
        return cookie(clientId, pair.refreshToken(), maxAge.isNegative()
                ? Duration.ZERO
                : maxAge);
    }

    /**
     * 构造立即过期的 Refresh Cookie。
     *
     * <p>Builds an immediately expired refresh cookie.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @return 过期 Cookie；expired cookie
     */
    private ResponseCookie expiredRefreshCookie(String clientId) {
        return cookie(clientId, "", Duration.ZERO);
    }

    /**
     * 构造立即过期的 SSO Cookie。
     *
     * <p>Builds an immediately expired SSO cookie.</p>
     *
     * @return 过期 SSO Cookie；expired SSO cookie
     */
    private ResponseCookie expiredSsoCookie() {
        return ResponseCookie.from(IdpSsoAuthenticationFilter.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }

    /**
     * 按 Client 构造 Host-only HttpOnly Cookie。
     *
     * <p>Builds a per-Client host-only HttpOnly cookie.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param value Cookie 值；cookie value
     * @param maxAge 有效期；lifetime
     * @return Cookie；cookie
     */
    private ResponseCookie cookie(
            String clientId,
            String value,
            Duration maxAge
    ) {
        return ResponseCookie.from(refreshCookieName(clientId), value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    /**
     * 生成一个不会跨 Client 冲突的 Refresh Cookie 名称。
     *
     * <p>Generates a refresh-cookie name that does not collide across Clients.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @return Refresh Cookie 名称；refresh-cookie name
     */
    public static String refreshCookieName(String clientId) {
        String value = required(clientId);
        if (!value.matches("[A-Za-z0-9_-]{1,100}")) {
            throw oauth("invalid_request");
        }
        return REFRESH_COOKIE_PREFIX + value;
    }

    /**
     * 读取指定 Cookie 的值。
     *
     * <p>Reads the value of a named cookie.</p>
     *
     * @param request HTTP 请求；HTTP request
     * @param name Cookie 名称；cookie name
     * @return Cookie 值，不存在时为空；cookie value or {@code null} when absent
     */
    private static String cookieValue(
            HttpServletRequest request,
            String name
    ) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 读取且只接受一个安全表单值。
     *
     * <p>Reads and accepts exactly one safe form value.</p>
     *
     * @param form 原始表单；raw form
     * @param name 参数名；parameter name
     * @return 唯一参数值；single parameter value
     */
    private static String single(
            MultiValueMap<String, String> form,
            String name
    ) {
        List<String> values = form.get(name);
        if (values == null || values.size() != 1) {
            throw oauth("invalid_request");
        }
        return required(values.getFirst());
    }

    /**
     * 创建不暴露请求细节的 OAuth 异常。
     *
     * <p>Creates an OAuth exception without exposing request details.</p>
     *
     * @param error OAuth 错误码；OAuth error code
     * @return OAuth 异常；OAuth exception
     */
    private static OAuthException oauth(String error) {
        return new OAuthException(error, "OAuth request is invalid");
    }

    /**
     * 校验必填表单或 Cookie 值。
     *
     * <p>Validates a required form or cookie value.</p>
     *
     * @param value 待校验值；value to validate
     * @return 已校验值；validated value
     */
    private static String required(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw oauth("invalid_request");
        }
        return value;
    }
}
