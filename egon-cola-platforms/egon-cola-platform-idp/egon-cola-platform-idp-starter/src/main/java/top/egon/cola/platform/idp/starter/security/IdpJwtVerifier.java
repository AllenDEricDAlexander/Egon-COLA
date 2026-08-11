package top.egon.cola.platform.idp.starter.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.contract.IdpClaimNames;
import top.egon.cola.platform.idp.contract.IdpPrincipal;
import top.egon.cola.platform.idp.contract.PrincipalType;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;
import top.egon.cola.platform.idp.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityUserStateReader;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 校验绑定当前唯一 Resource 的 IdP USER 或 SERVICE Access Token。
 * 两类主体共享签名、类型、Issuer、时间、精确 Audience 和 Resource 投影校验；USER 额外校验
 * 用户实时状态，SERVICE 额外校验当前 Confidential Client 状态并提取 IdP 授权 Scope。
 *
 * <p>Validates an IdP USER or SERVICE access token bound to the current sole Resource. Both
 * principal types share signature, type, issuer, time, exact-audience, and Resource-projection
 * checks. USER additionally checks current user state, while SERVICE checks current confidential
 * Client state and extracts scopes authorized by IdP.</p>
 */
public final class IdpJwtVerifier {

    /** JWT 解码器；JWT decoder. */
    private final JwtDecoder decoder;

    /** 用户实时状态读取端口；current user-state reader. */
    private final IdentityUserStateReader userStates;

    /** Resource Server 状态读取端口；Resource Server state reader. */
    private final IdentityResourceServerStateReader resourceStates;

    /** OAuth Client 状态读取端口；OAuth Client state reader. */
    private final IdentityOAuthClientStateReader clientStates;

    /** 当前应用唯一的 Resource Server 标识；sole current Resource Server identifier. */
    private final String resourceServerId;

    /** 当前应用唯一的 Resource URI；sole current Resource URI. */
    private final URI resourceUri;

    /**
     * 创建精确 Resource 的 USER/SERVICE Token 验证器。
     *
     * <p>Creates the exact-Resource USER/SERVICE token verifier.</p>
     *
     * @param decoder JWT 解码器；JWT decoder
     * @param userStates 用户实时状态读取器；current user-state reader
     * @param resourceStates Resource Server 状态读取器；Resource Server state reader
     * @param clientStates OAuth Client 状态读取器；OAuth Client state reader
     * @param resourceServerId 当前 Resource Server 标识；current Resource Server identifier
     * @param resourceUri 当前 Resource URI；current Resource URI
     */
    public IdpJwtVerifier(
            JwtDecoder decoder,
            IdentityUserStateReader userStates,
            IdentityResourceServerStateReader resourceStates,
            IdentityOAuthClientStateReader clientStates,
            String resourceServerId,
            URI resourceUri
    ) {
        this.decoder = Objects.requireNonNull(decoder, "decoder");
        this.userStates = Objects.requireNonNull(userStates, "userStates");
        this.resourceStates = Objects.requireNonNull(
                resourceStates,
                "resourceStates"
        );
        this.clientStates = Objects.requireNonNull(
                clientStates,
                "clientStates"
        );
        this.resourceServerId = required(
                resourceServerId,
                "resourceServerId"
        );
        this.resourceUri = validResource(resourceUri);
    }

    /**
     * 完整验证 Access Token，并按 {@code principal_type} 返回 USER 或 SERVICE 身份。
     * 返回对象不保存原始 Token；USER 不包含用户资料，SERVICE 不加载 RBAC3 权限。
     *
     * <p>Fully validates an access token and returns a USER or SERVICE identity according to
     * {@code principal_type}. The result never retains the raw token; USER contains no profile
     * data and SERVICE loads no RBAC3 permissions.</p>
     *
     * @param token 原始 Bearer Access Token；raw Bearer access token
     * @return 已验证的 IdP 主体；validated IdP principal
     * @throws InvalidTokenException 当 Token 或当前投影不可信时；when the token or a current
     *                               projection is untrusted
     */
    public IdpPrincipal verify(String token) {
        try {
            Jwt jwt = decoder.decode(required(token, "token"));
            commonHeaders(jwt);
            Set<String> audience = exactAudience(jwt);
            instant(jwt, "iat");
            instant(jwt, "nbf");
            instant(jwt, "exp");
            long resourceVersion = number(
                    jwt,
                    IdpClaimNames.RESOURCE_VERSION
            );
            verifyResource(resourceVersion);
            PrincipalType principalType = principalType(jwt);
            return switch (principalType) {
                case USER -> verifyUser(jwt, audience);
                case SERVICE -> verifyService(jwt, resourceVersion);
            };
        } catch (InvalidTokenException invalid) {
            throw invalid;
        } catch (JwtException | IllegalArgumentException
                 | NullPointerException invalid) {
            throw new InvalidTokenException("JWT_INVALID", invalid);
        }
    }

    /**
     * 校验 Access Token 专用头字段并拒绝 Refresh Token 或 Admission Ticket。
     *
     * <p>Validates access-token-specific headers and rejects refresh tokens or Admission
     * Tickets.</p>
     *
     * @param jwt 已解码 JWT；decoded JWT
     */
    private void commonHeaders(Jwt jwt) {
        if (!"RS256".equals(jwt.getHeaders().get("alg"))) {
            throw new InvalidTokenException("JWT_ALGORITHM_INVALID");
        }
        text(jwt.getHeaders().get("kid"), "kid");
        if (!"at+jwt".equals(jwt.getHeaders().get("typ"))) {
            throw new InvalidTokenException("JWT_TYPE_INVALID");
        }
        if (jwt.hasClaim(IdpClaimNames.TOKEN_USE)) {
            throw new InvalidTokenException("JWT_TOKEN_USE_INVALID");
        }
    }

    /**
     * 校验 Token 只有当前 Resource URI 这一个 Audience。
     *
     * <p>Validates that the token has the current Resource URI as its only audience.</p>
     *
     * @param jwt 已解码 JWT；decoded JWT
     * @return 唯一 Audience 的不可变集合；immutable set containing the sole audience
     */
    private Set<String> exactAudience(Jwt jwt) {
        List<String> values = jwt.getAudience();
        if (values == null
                || values.size() != 1
                || !resourceUri.toString().equals(values.getFirst())) {
            throw new InvalidTokenException("JWT_AUDIENCE_INVALID");
        }
        return Set.of(values.getFirst());
    }

    /**
     * 校验当前 Resource 仍为 ACTIVE、URI 未漂移且版本与 Token 一致。
     *
     * <p>Validates that the current Resource remains ACTIVE, its URI has not drifted, and its
     * version matches the token.</p>
     *
     * @param tokenResourceVersion Token 中的 Resource 版本；Resource version in the token
     */
    private void verifyResource(long tokenResourceVersion) {
        IdentityResourceServerState state;
        try {
            state = resourceStates.read(resourceServerId).orElseThrow(
                    () -> new InvalidTokenException(
                            "RESOURCE_STATE_MISSING"
                    )
            );
        } catch (InvalidTokenException invalid) {
            throw invalid;
        } catch (RuntimeException unavailable) {
            throw new InvalidTokenException(
                    "RESOURCE_STATE_UNAVAILABLE",
                    unavailable
            );
        }
        if (!resourceServerId.equals(state.resourceServerId())) {
            throw new InvalidTokenException("RESOURCE_ID_MISMATCH");
        }
        if (state.status() != ResourceServerStatus.ACTIVE) {
            throw new InvalidTokenException("RESOURCE_NOT_ACTIVE");
        }
        if (!resourceUri.equals(state.resourceUri())) {
            throw new InvalidTokenException("RESOURCE_URI_MISMATCH");
        }
        if (state.version() != tokenResourceVersion) {
            throw new InvalidTokenException("RESOURCE_VERSION_STALE");
        }
    }

    /**
     * 解析严格的 USER 或 SERVICE 主体类型。
     *
     * <p>Parses the strict USER or SERVICE principal type.</p>
     *
     * @param jwt 已解码 JWT；decoded JWT
     * @return 主体类型；principal type
     */
    private PrincipalType principalType(Jwt jwt) {
        try {
            return PrincipalType.valueOf(claim(
                    jwt,
                    IdpClaimNames.PRINCIPAL_TYPE
            ));
        } catch (IllegalArgumentException invalid) {
            throw new InvalidTokenException(
                    "JWT_PRINCIPAL_TYPE_INVALID",
                    invalid
            );
        }
    }

    /**
     * 校验 USER 实时状态并构造用户主体。
     *
     * <p>Validates current USER state and creates the user principal.</p>
     *
     * @param jwt 已解码 JWT；decoded JWT
     * @param audience 唯一 Resource Audience；sole Resource audience
     * @return USER 身份；USER principal
     */
    private IdentityPrincipal verifyUser(Jwt jwt, Set<String> audience) {
        String subject = claim(jwt, "sub");
        long tokenVersion = number(jwt, IdpClaimNames.TOKEN_VERSION);
        IdentityUserState state;
        try {
            state = userStates.read(subject).orElseThrow(
                    () -> new InvalidTokenException(
                            "IDENTITY_STATE_MISSING"
                    )
            );
        } catch (InvalidTokenException invalid) {
            throw invalid;
        } catch (RuntimeException unavailable) {
            throw new InvalidTokenException(
                    "IDENTITY_STATE_UNAVAILABLE",
                    unavailable
            );
        }
        if (state.status() != IdentityUserState.Status.ACTIVE) {
            throw new InvalidTokenException("IDENTITY_NOT_ACTIVE");
        }
        if (state.tokenVersion() != tokenVersion) {
            throw new InvalidTokenException(
                    "IDENTITY_TOKEN_VERSION_STALE"
            );
        }
        return new IdentityPrincipal(
                subject,
                claim(jwt, IdpClaimNames.TENANT_ID),
                claim(jwt, IdpClaimNames.SESSION_ID),
                claim(jwt, IdpClaimNames.CLIENT_ID),
                claim(jwt, "jti"),
                tokenVersion,
                audience,
                instant(jwt, "iat"),
                instant(jwt, "exp")
        );
    }

    /**
     * 校验 SERVICE 的当前 Confidential Client 状态并构造服务主体。
     *
     * <p>Validates current confidential Client state for SERVICE and creates the service
     * principal.</p>
     *
     * @param jwt 已解码 JWT；decoded JWT
     * @param resourceVersion 目标 Resource 版本；target Resource version
     * @return SERVICE 身份；SERVICE principal
     */
    private ServiceIdentityPrincipal verifyService(
            Jwt jwt,
            long resourceVersion
    ) {
        String subject = claim(jwt, "sub");
        String clientId = claim(jwt, IdpClaimNames.CLIENT_ID);
        if (!subject.equals(clientId)) {
            throw new InvalidTokenException("SERVICE_SUBJECT_INVALID");
        }
        String tenantId = claim(jwt, IdpClaimNames.TENANT_ID);
        if ("*".equals(tenantId)) {
            throw new InvalidTokenException("SERVICE_TENANT_INVALID");
        }
        IdentityOAuthClientStateReader.IdentityOAuthClientState state;
        try {
            state = clientStates.read(clientId).orElseThrow(
                    () -> new InvalidTokenException(
                            "OAUTH_CLIENT_STATE_MISSING"
                    )
            );
        } catch (InvalidTokenException invalid) {
            throw invalid;
        } catch (RuntimeException unavailable) {
            throw new InvalidTokenException(
                    "OAUTH_CLIENT_STATE_UNAVAILABLE",
                    unavailable
            );
        }
        if (!clientId.equals(state.clientId())) {
            throw new InvalidTokenException("OAUTH_CLIENT_ID_MISMATCH");
        }
        if (state.status() != OAuthClient.Status.ACTIVE) {
            throw new InvalidTokenException("OAUTH_CLIENT_NOT_ACTIVE");
        }
        if (state.clientType() != OAuthClient.ClientType.CONFIDENTIAL) {
            throw new InvalidTokenException("OAUTH_CLIENT_TYPE_INVALID");
        }
        return new ServiceIdentityPrincipal(
                subject,
                tenantId,
                clientId,
                claim(jwt, "jti"),
                resourceUri,
                resourceVersion,
                scopes(jwt),
                claim(jwt, IdpClaimNames.SOURCE_BIZ),
                claim(jwt, IdpClaimNames.SOURCE_APP),
                claim(jwt, IdpClaimNames.SOURCE_ENV),
                claim(jwt, IdpClaimNames.CREDENTIAL_ID),
                instant(jwt, "iat"),
                instant(jwt, "exp")
        );
    }

    /**
     * 读取非空且不重复的服务 Scope 集合。
     *
     * <p>Reads a non-empty and distinct service-scope set.</p>
     *
     * @param jwt 已解码 JWT；decoded JWT
     * @return 不可变 Scope 集合；immutable scope set
     */
    private Set<String> scopes(Jwt jwt) {
        Object raw = jwt.getClaims().get(IdpClaimNames.SCOPE);
        Collection<?> values;
        if (raw instanceof String text) {
            values = List.of(text.trim().split("\\s+"));
        } else if (raw instanceof Collection<?> collection) {
            values = collection;
        } else {
            throw new InvalidTokenException("JWT_SCOPE_INVALID");
        }
        LinkedHashSet<String> scopes = new LinkedHashSet<>();
        for (Object value : values) {
            scopes.add(text(value, IdpClaimNames.SCOPE));
        }
        if (scopes.isEmpty() || scopes.size() != values.size()) {
            throw new InvalidTokenException("JWT_SCOPE_INVALID");
        }
        return Set.copyOf(scopes);
    }

    /** 读取必需文本声明；Reads a required textual claim. */
    private String claim(Jwt jwt, String name) {
        return text(jwt.getClaims().get(name), name);
    }

    /** 读取必需非负整数声明；Reads a required non-negative numeric claim. */
    private long number(Jwt jwt, String name) {
        Object value = jwt.getClaims().get(name);
        if (!(value instanceof Number number) || number.longValue() < 0L) {
            throw new InvalidTokenException(
                    "JWT_CLAIM_INVALID_" + name.toUpperCase()
            );
        }
        return number.longValue();
    }

    /** 校验对象为非空文本；Validates an object as non-blank text. */
    private String text(Object value, String name) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new InvalidTokenException(
                    "JWT_CLAIM_INVALID_" + name.toUpperCase()
            );
        }
        return text.trim();
    }

    /** 校验并读取必需时间声明；Validates and reads a required timestamp claim. */
    private Instant instant(Jwt jwt, String name) {
        Object value = jwt.getClaims().get(name);
        if (!(value instanceof Instant instant)) {
            throw new InvalidTokenException(
                    "JWT_CLAIM_INVALID_" + name.toUpperCase()
            );
        }
        return instant;
    }

    /** 校验必填方法参数；Validates a required method argument. */
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new InvalidTokenException(name + " is required");
        }
        return value.trim();
    }

    /** 校验 Resource URI；Validates a Resource URI. */
    private static URI validResource(URI value) {
        Objects.requireNonNull(value, "resourceUri");
        if (!value.isAbsolute()
                || value.getFragment() != null
                || !value.equals(value.normalize())) {
            throw new IllegalArgumentException("resourceUri is invalid");
        }
        return value;
    }

    /**
     * 表示 Access Token 或关联运行态投影未通过校验。
     * 消息保存上层可稳定返回的失败原因码。
     *
     * <p>Signals that an access token or associated runtime projection failed validation. The
     * message carries the stable reason code returned by upper layers.</p>
     */
    public static final class InvalidTokenException extends RuntimeException {

        /**
         * 使用失败原因创建异常。
         *
         * <p>Creates an exception with a failure reason.</p>
         *
         * @param message 失败原因码；failure reason code
         */
        public InvalidTokenException(String message) {
            super(message);
        }

        /**
         * 使用失败原因与底层异常创建异常。
         *
         * <p>Creates an exception with a failure reason and cause.</p>
         *
         * @param message 失败原因码；failure reason code
         * @param cause 底层异常；underlying cause
         */
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
