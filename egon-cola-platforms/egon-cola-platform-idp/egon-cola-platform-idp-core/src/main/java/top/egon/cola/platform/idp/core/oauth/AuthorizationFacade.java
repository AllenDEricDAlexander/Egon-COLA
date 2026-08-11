package top.egon.cola.platform.idp.core.oauth;

import top.egon.cola.platform.idp.core.port.AuthorizationCodeStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;
import top.egon.cola.platform.idp.core.port.UserResourceAccessAuthorizationPort;
import top.egon.cola.platform.idp.core.resource.ResourceAuthorizationException;
import top.egon.cola.platform.idp.core.resource.UserResourceAccessPolicy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 编排 OAuth 用户授权请求、一次性授权码和单 Resource 入口策略。
 * Orchestrates OAuth user authorization requests, one-time codes, and single-Resource entry policy.
 */
public final class AuthorizationFacade {

    /** 默认授权码有效期。 / Default authorization-code lifetime. */
    private static final Duration DEFAULT_CODE_TTL = Duration.ofSeconds(60);
    /** 最小授权码有效期。 / Minimum authorization-code lifetime. */
    private static final Duration MINIMUM_CODE_TTL = Duration.ofSeconds(30);
    /** 最大授权码有效期。 / Maximum authorization-code lifetime. */
    private static final Duration MAXIMUM_CODE_TTL = Duration.ofMinutes(5);
    /** RFC 7636 Code Verifier 格式。 / RFC 7636 code-verifier format. */
    private static final Pattern CODE_VERIFIER =
            Pattern.compile("[A-Za-z0-9\\-._~]{43,128}");
    /** S256 Code Challenge 格式。 / S256 code-challenge format. */
    private static final Pattern CODE_CHALLENGE = Pattern.compile("[A-Za-z0-9_-]{43}");
    /** 浏览器参数最大长度。 / Maximum browser-parameter length. */
    private static final int MAXIMUM_BROWSER_VALUE_LENGTH = 512;
    /** 在初始授权阶段映射为 invalid_target 的 Resource 错误。 / Resource errors mapped to invalid_target during initial authorization. */
    private static final Set<String> INVALID_TARGET_CODES = Set.of(
            "IDP_RESOURCE_SERVER_NOT_FOUND",
            "IDP_RESOURCE_SERVER_DISABLED",
            "IDP_USER_RESOURCE_GRANT_NOT_FOUND");

    /** OAuth Client 查询端口。 / OAuth Client lookup port. */
    private final OAuthClientStore clients;
    /** 一次性授权码存储。 / One-time authorization-code store. */
    private final AuthorizationCodeStore codes;
    /** 统一用户 Resource 入口策略。 / Shared user Resource-entry policy. */
    private final UserResourceAccessPolicy resourceAccess;
    /** 业务时钟。 / Business clock. */
    private final Clock clock;
    /** 高熵授权码生成器。 / High-entropy authorization-code generator. */
    private final Supplier<String> codeGenerator;
    /** 动态授权码有效期来源。 / Dynamic authorization-code lifetime source. */
    private final Supplier<Duration> codeTtl;

    /**
     * 使用安全默认值创建授权门面。
     * Creates the authorization facade with secure defaults.
     *
     * @param clients OAuth Client 查询端口 / OAuth Client lookup port
     * @param codes 授权码存储 / authorization-code store
     * @param resourceAccess USER Resource 入口策略 / USER Resource entry policy
     * @param clock 业务时钟 / business clock
     */
    public AuthorizationFacade(
            OAuthClientStore clients,
            AuthorizationCodeStore codes,
            UserResourceAccessPolicy resourceAccess,
            Clock clock) {
        this(clients, codes, resourceAccess, clock, secureCodeGenerator(),
                () -> DEFAULT_CODE_TTL);
    }

    /**
     * 使用动态授权码有效期创建授权门面。
     * Creates an authorization facade with a dynamic code lifetime.
     *
     * @param clients OAuth Client 查询端口 / OAuth Client lookup port
     * @param codes 授权码存储 / authorization-code store
     * @param resourceAccess USER Resource 入口策略 / USER Resource entry policy
     * @param clock 业务时钟 / business clock
     * @param codeTtl 动态授权码有效期来源 / dynamic code-lifetime source
     * @return OAuth 授权门面 / OAuth authorization facade
     */
    public static AuthorizationFacade dynamicTtl(
            OAuthClientStore clients,
            AuthorizationCodeStore codes,
            UserResourceAccessPolicy resourceAccess,
            Clock clock,
            Supplier<Duration> codeTtl) {
        return new AuthorizationFacade(clients, codes, resourceAccess, clock,
                secureCodeGenerator(), codeTtl);
    }

    /**
     * 使用可测试授权码生成器创建授权门面。
     * Creates an authorization facade with a testable code generator.
     *
     * @param clients OAuth Client 查询端口 / OAuth Client lookup port
     * @param codes 授权码存储 / authorization-code store
     * @param resourceAccess USER Resource 入口策略 / USER Resource entry policy
     * @param clock 业务时钟 / business clock
     * @param codeGenerator 授权码生成器 / authorization-code generator
     */
    public AuthorizationFacade(
            OAuthClientStore clients,
            AuthorizationCodeStore codes,
            UserResourceAccessPolicy resourceAccess,
            Clock clock,
            Supplier<String> codeGenerator) {
        this(clients, codes, resourceAccess, clock, codeGenerator,
                () -> DEFAULT_CODE_TTL);
    }

    /**
     * 使用完整依赖创建授权门面。
     * Creates the authorization facade with all dependencies.
     *
     * @param clients OAuth Client 查询端口 / OAuth Client lookup port
     * @param codes 授权码存储 / authorization-code store
     * @param resourceAccess USER Resource 入口策略 / USER Resource entry policy
     * @param clock 业务时钟 / business clock
     * @param codeGenerator 授权码生成器 / authorization-code generator
     * @param codeTtl 动态授权码有效期来源 / dynamic code-lifetime source
     */
    public AuthorizationFacade(
            OAuthClientStore clients,
            AuthorizationCodeStore codes,
            UserResourceAccessPolicy resourceAccess,
            Clock clock,
            Supplier<String> codeGenerator,
            Supplier<Duration> codeTtl) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.codes = Objects.requireNonNull(codes, "codes");
        this.resourceAccess = Objects.requireNonNull(resourceAccess, "resourceAccess");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.codeGenerator = Objects.requireNonNull(codeGenerator, "codeGenerator");
        this.codeTtl = Objects.requireNonNull(codeTtl, "codeTtl");
    }

    /**
     * 校验浏览器请求和用户 Resource 入口资格并签发一次性授权码。
     * Validates the browser request and user Resource entry before issuing a one-time code.
     *
     * @param request Authorization Endpoint 请求 / Authorization Endpoint request
     * @param identitySub 用户身份标识 / user identity subject
     * @param sessionId IdP 会话标识 / IdP session identifier
     * @return 授权成功结果 / successful authorization result
     */
    public AuthorizationResult authorize(
            AuthorizationRequest request, String identitySub, String sessionId) {
        Objects.requireNonNull(request, "request");
        String subject = required(identitySub, "identitySub");
        String stableSessionId = required(sessionId, "sessionId");
        validateBrowserRequest(request);
        OAuthClient client = client(request.clientId());
        validateClient(client, request);
        URI target = resource(request.resource(), false);
        UserResourceAccessPolicy.UserResourceAccess access = authorizeResource(
                client, target, subject, request.tenantId(), stableSessionId, false);
        Instant issuedAt = clock.instant();
        Duration currentCodeTtl = currentCodeTtl();
        Instant expiresAt = issuedAt.plus(currentCodeTtl);
        AuthorizationCode authorizationCode = new AuthorizationCode(
                subject, request.tenantId(), access.rbac3UserId(), stableSessionId,
                request.clientId(), access.resourceUri(), access.resourceServerId(),
                access.resourceVersion(), request.redirectUri(), request.nonce(),
                request.codeChallenge(), issuedAt, expiresAt);
        String rawCode = required(codeGenerator.get(), "authorizationCode");
        if (rawCode.length() < 32) {
            throw new IllegalStateException(
                    "authorization code generator returned a short value");
        }
        codes.put(digest(rawCode), authorizationCode, currentCodeTtl);
        return new AuthorizationResult(rawCode, request.state(),
                request.redirectUri(), expiresAt);
    }

    /**
     * 原子消费授权码，校验 PKCE、Client、回调和精确 Resource，并重新执行入口策略。
     * Atomically consumes a code, validates PKCE, Client, redirect, and exact Resource, then reruns entry policy.
     *
     * @param rawCode 原始授权码 / raw authorization code
     * @param codeVerifier PKCE Verifier / PKCE verifier
     * @param redirectUri 精确回调地址 / exact redirect URI
     * @param clientId Client 标识 / Client identifier
     * @param resource RFC 8707 Resource Identifier / RFC 8707 Resource Identifier
     * @return 重新校验后的授权码载荷 / revalidated authorization-code payload
     */
    public AuthorizationCode consume(
            String rawCode,
            String codeVerifier,
            String redirectUri,
            String clientId,
            String resource) {
        String code = grantValue(rawCode);
        String verifier = grantValue(codeVerifier);
        if (!CODE_VERIFIER.matcher(verifier).matches()) {
            throw invalidGrant();
        }
        String redirect = grantValue(redirectUri);
        String clientIdValue = grantValue(clientId);
        URI target = resource(resource, true);
        AuthorizationCode authorizationCode = codes.consume(digest(code));
        if (authorizationCode == null
                || !authorizationCode.expiresAt().isAfter(clock.instant())
                || !constantTimeEquals(authorizationCode.codeChallenge(), s256(verifier))
                || !authorizationCode.redirectUri().equals(redirect)
                || !authorizationCode.clientId().equals(clientIdValue)
                || !authorizationCode.resourceUri().equals(target)) {
            throw invalidGrant();
        }
        OAuthClient client = clients.findById(clientIdValue)
                .orElseThrow(AuthorizationFacade::invalidGrant);
        UserResourceAccessPolicy.UserResourceAccess access = authorizeResource(
                client, target, authorizationCode.identitySub(),
                authorizationCode.tenantId(), authorizationCode.sessionId(), true);
        return authorizationCode.withResourceAccess(access);
    }

    /**
     * 校验 Authorization Endpoint 浏览器参数。
     * Validates Authorization Endpoint browser parameters.
     *
     * @param request 浏览器授权请求 / browser authorization request
     */
    private void validateBrowserRequest(AuthorizationRequest request) {
        if (!"code".equals(request.responseType())) {
            throw oauth("invalid_request", "response_type must be code");
        }
        requiredRequestValue(request.clientId(), "client_id");
        requiredRequestValue(request.redirectUri(), "redirect_uri");
        requiredRequestValue(request.resource(), "resource");
        requiredRequestValue(request.tenantId(), "tenant_id");
        browserValue(request.state(), "state");
        browserValue(request.nonce(), "nonce");
        if (!"S256".equals(request.codeChallengeMethod())) {
            throw oauth("invalid_request", "code_challenge_method must be S256");
        }
        if (request.codeChallenge() == null
                || !CODE_CHALLENGE.matcher(request.codeChallenge()).matches()) {
            throw oauth("invalid_request", "invalid code_challenge");
        }
    }

    /**
     * 校验 Client 状态、精确回调和 PKCE 策略。
     * Validates Client status, exact redirect, and PKCE policy.
     *
     * @param client OAuth Client / OAuth Client
     * @param request 浏览器授权请求 / browser authorization request
     */
    private void validateClient(OAuthClient client, AuthorizationRequest request) {
        if (client.status() != OAuthClient.Status.ACTIVE) {
            throw oauth("unauthorized_client", "client is not authorized");
        }
        if (!client.pkceRequired() || !client.acceptsRedirectUri(request.redirectUri())) {
            throw oauth("invalid_request", "redirect URI or PKCE policy is invalid");
        }
    }

    /**
     * 执行共享用户 Resource 策略并映射到 OAuth 协议错误。
     * Executes the shared user Resource policy and maps failures to OAuth protocol errors.
     *
     * @param client OAuth Client / OAuth Client
     * @param resource 目标 Resource URI / target Resource URI
     * @param identitySub 用户身份标识 / user identity subject
     * @param tenantId 租户标识 / tenant identifier
     * @param sessionId IdP 会话标识 / IdP session identifier
     * @param exchange 是否处于换码阶段 / whether this is the exchange phase
     * @return 已授权 Resource 上下文 / authorized Resource context
     */
    private UserResourceAccessPolicy.UserResourceAccess authorizeResource(
            OAuthClient client,
            URI resource,
            String identitySub,
            String tenantId,
            String sessionId,
            boolean exchange) {
        try {
            return resourceAccess.authorize(
                    client, resource, identitySub, tenantId, sessionId);
        } catch (UserResourceAccessAuthorizationPort.AccessUnavailableException exception) {
            throw oauth("temporarily_unavailable", "authorization service is unavailable");
        } catch (TenantMembershipPort.TenantMembershipException exception) {
            throw oauth("access_denied", "tenant membership is not active");
        } catch (ResourceAuthorizationException exception) {
            if (exchange) {
                throw oauth("access_denied", "Resource access is denied");
            }
            if ("IDP_CLIENT_DISABLED".equals(exception.code())) {
                throw oauth("unauthorized_client", "client is not authorized");
            }
            if (INVALID_TARGET_CODES.contains(exception.code())) {
                throw oauth("invalid_target", "requested Resource is not authorized");
            }
            throw oauth("access_denied", "Resource access is denied");
        }
    }

    /**
     * 按标识读取已登记 Client。
     * Loads a registered Client by identifier.
     *
     * @param clientId Client 标识 / Client identifier
     * @return 已登记 Client / registered Client
     */
    private OAuthClient client(String clientId) {
        return clients.findById(clientId).orElseThrow(() ->
                oauth("unauthorized_client", "client is not authorized"));
    }

    /**
     * 解析并严格校验一个 RFC 8707 Resource Identifier。
     * Parses and strictly validates one RFC 8707 Resource Identifier.
     *
     * @param value 原始 Resource 参数 / raw Resource parameter
     * @param grantError 是否映射为 invalid_grant / whether to map to invalid_grant
     * @return 已校验 Resource URI / validated Resource URI
     */
    private URI resource(String value, boolean grantError) {
        try {
            String candidate = grantError ? grantValue(value)
                    : requiredRequestValue(value, "resource");
            URI uri = URI.create(candidate);
            if (!uri.isAbsolute() || uri.getScheme() == null
                    || uri.getScheme().isBlank() || uri.getFragment() != null
                    || !uri.equals(uri.normalize())) {
                throw new IllegalArgumentException("invalid Resource URI");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            if (grantError) {
                throw invalidGrant();
            }
            throw oauth("invalid_target", "resource must be an absolute URI without a fragment");
        }
    }

    /**
     * 校验浏览器值长度。
     * Validates a browser value and its length.
     *
     * @param value 待校验值 / value to validate
     * @param field 字段名 / field name
     */
    private void browserValue(String value, String field) {
        requiredRequestValue(value, field);
        if (value.length() > MAXIMUM_BROWSER_VALUE_LENGTH) {
            throw oauth("invalid_request", field + " is too long");
        }
    }

    /**
     * 校验请求必填值。
     * Validates a required request value.
     *
     * @param value 待校验值 / value to validate
     * @param field 字段名 / field name
     * @return 已校验值 / validated value
     */
    private String requiredRequestValue(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw oauth("invalid_request", field + " is required");
        }
        return value;
    }

    /**
     * 校验 Grant 值且不泄露具体失败字段。
     * Validates a grant value without revealing the failed field.
     *
     * @param value 待校验 Grant 值 / grant value to validate
     * @return 已校验值 / validated value
     */
    private String grantValue(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw invalidGrant();
        }
        return value;
    }

    /**
     * 校验内部必填值。
     * Validates an internal required value.
     *
     * @param value 待校验值 / value to validate
     * @param field 字段名 / field name
     * @return 已校验值 / validated value
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 计算授权码摘要。
     * Computes an authorization-code digest.
     *
     * @param value 原始授权码 / raw authorization code
     * @return URL-safe 摘要 / URL-safe digest
     */
    private static String digest(String value) {
        return base64Url(sha256(value));
    }

    /**
     * 计算 PKCE S256。
     * Computes PKCE S256.
     *
     * @param verifier PKCE Verifier / PKCE verifier
     * @return S256 Challenge / S256 challenge
     */
    private static String s256(String verifier) {
        return base64Url(sha256(verifier));
    }

    /**
     * 计算 SHA-256。
     * Computes SHA-256.
     *
     * @param value ASCII 文本 / ASCII text
     * @return SHA-256 字节 / SHA-256 bytes
     */
    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * 执行无 Padding 的 URL-safe Base64 编码。
     * Encodes URL-safe Base64 without padding.
     *
     * @param value 原始字节 / raw bytes
     * @return URL-safe Base64 文本 / URL-safe Base64 text
     */
    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    /**
     * 常量时间比较安全值。
     * Compares security values in constant time.
     *
     * @param left 左值 / left value
     * @param right 右值 / right value
     * @return 相等时为 {@code true} / {@code true} when equal
     */
    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.US_ASCII),
                right.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * 创建 invalid_grant。
     * Creates an invalid_grant error.
     *
     * @return OAuth 异常 / OAuth exception
     */
    private static OAuthException invalidGrant() {
        return oauth("invalid_grant", "authorization grant is invalid");
    }

    /**
     * 创建 OAuth 协议异常。
     * Creates an OAuth protocol exception.
     *
     * @param error OAuth 错误码 / OAuth error code
     * @param message 安全错误描述 / safe error description
     * @return OAuth 异常 / OAuth exception
     */
    private static OAuthException oauth(String error, String message) {
        return new OAuthException(error, message);
    }

    /**
     * 读取并校验当前授权码有效期。
     * Loads and validates the current code lifetime.
     *
     * @return 当前授权码有效期 / current code lifetime
     */
    private Duration currentCodeTtl() {
        Duration value = Objects.requireNonNull(codeTtl.get(),
                "authorization code TTL");
        if (value.compareTo(MINIMUM_CODE_TTL) < 0
                || value.compareTo(MAXIMUM_CODE_TTL) > 0) {
            throw new IllegalStateException("authorization code TTL is out of range");
        }
        return value;
    }

    /**
     * 创建 256-bit 安全授权码生成器。
     * Creates a secure 256-bit authorization-code generator.
     *
     * @return 授权码生成器 / authorization-code generator
     */
    private static Supplier<String> secureCodeGenerator() {
        SecureRandom random = new SecureRandom();
        return () -> {
            byte[] value = new byte[32];
            random.nextBytes(value);
            return base64Url(value);
        };
    }

    /**
     * Authorization Endpoint 成功结果。
     * Successful Authorization Endpoint result.
     *
     * @param code 原始一次性授权码 / raw one-time authorization code
     * @param state 原样返回的 State / returned state
     * @param redirectUri 已校验回调地址 / validated redirect URI
     * @param expiresAt 授权码过期时间 / code expiration time
     */
    public record AuthorizationResult(
            String code, String state, String redirectUri, Instant expiresAt) {
    }
}
