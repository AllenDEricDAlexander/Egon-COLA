package top.egon.cola.platform.idp.starter.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.contract.IdpClaimNames;
import top.egon.cola.platform.idp.starter.state.IdentityUserStateReader;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 校验 IdP 访问令牌及用户当前全局状态，并构造统一身份主体。
 * 除 JWT 签名、签发方和时间窗口外，本类还强制检查算法、密钥标识、受众、客户端、
 * 必需身份声明和 Redis 状态版本，从而支持禁用用户或提升令牌版本后的即时失效。
 *
 * <p>Validates an IdP access token together with current global user state and creates the unified
 * identity principal. In addition to JWT signature, issuer, and time-window validation, it enforces
 * the algorithm, key identifier, audience, client, required identity claims, and Redis state
 * version so disabling a user or advancing the token version invalidates existing tokens
 * immediately.</p>
 */
public final class IdpJwtVerifier {

    /**
     * 负责验签及标准 JWT 校验的解码器。
     *
     * <p>Decoder responsible for signature and standard JWT validation.</p>
     */
    private final JwtDecoder decoder;

    /**
     * 读取用户当前状态与令牌版本的端口。
     *
     * <p>Port that reads the user's current status and token version.</p>
     */
    private final IdentityUserStateReader stateReader;

    /**
     * 当前资源服务器接受的受众集合。
     *
     * <p>Audiences accepted by the current resource server.</p>
     */
    private final Set<String> audiences;

    /**
     * 当前资源服务器接受的 OAuth 客户端集合。
     *
     * <p>OAuth clients accepted by the current resource server.</p>
     */
    private final Set<String> clientIds;

    /**
     * 创建访问令牌与实时身份状态验证器。
     *
     * <p>Creates the access-token and current-identity-state verifier.</p>
     *
     * @param decoder JWT 解码器；JWT decoder
     * @param stateReader 用户实时状态读取器；current user-state reader
     * @param audiences 允许的受众集合；accepted audiences
     * @param clientIds 允许的 OAuth 客户端集合；accepted OAuth clients
     */
    public IdpJwtVerifier(
            JwtDecoder decoder,
            IdentityUserStateReader stateReader,
            Set<String> audiences,
            Set<String> clientIds
    ) {
        this.decoder = Objects.requireNonNull(decoder, "decoder");
        this.stateReader = Objects.requireNonNull(stateReader, "stateReader");
        this.audiences = requiredValues(audiences, "audiences");
        this.clientIds = requiredValues(clientIds, "clientIds");
    }

    /**
     * 完整验证访问令牌，并返回下游只读使用的身份主体。
     * 返回内容仅包含身份定位和令牌审计字段，不包含姓名、手机号、头像等用户资料。
     *
     * <p>Fully validates an access token and returns the identity principal consumed downstream.
     * The result contains identity-location and token-audit fields only; it does not include user
     * profile data such as name, mobile number, or avatar.</p>
     *
     * @param token 原始 Bearer 访问令牌；raw Bearer access token
     * @return 已验证的统一身份主体；validated unified identity principal
     * @throws InvalidTokenException 当 JWT、客户端范围或用户实时状态不合法或不可用时；when
     *                               the JWT, client scope, or current user state is invalid or
     *                               unavailable
     */
    public IdentityPrincipal verify(String token) {
        try {
            Jwt jwt = decoder.decode(required(token, "token"));
            if (!"RS256".equals(jwt.getHeaders().get("alg"))) {
                throw new InvalidTokenException("JWT_ALGORITHM_INVALID");
            }
            text(jwt.getHeaders().get("kid"), "kid");
            if (jwt.hasClaim("token_use")) {
                throw new InvalidTokenException("JWT_TOKEN_USE_INVALID");
            }
            Set<String> tokenAudience = audience(jwt);
            if (tokenAudience.stream().noneMatch(audiences::contains)) {
                throw new InvalidTokenException("JWT_AUDIENCE_INVALID");
            }
            String clientId = claim(jwt, IdpClaimNames.CLIENT_ID);
            if (!clientIds.contains(clientId)) {
                throw new InvalidTokenException("JWT_CLIENT_INVALID");
            }
            String subject = claim(jwt, "sub");
            long tokenVersion = number(jwt, IdpClaimNames.TOKEN_VERSION);
            instant(jwt.getNotBefore(), "nbf");
            IdentityUserState state = stateReader.read(subject)
                    .orElseThrow(() -> new InvalidTokenException(
                            "IDENTITY_STATE_MISSING"));
            if (state.status() != IdentityUserState.Status.ACTIVE) {
                throw new InvalidTokenException("IDENTITY_NOT_ACTIVE");
            }
            if (state.tokenVersion() != tokenVersion) {
                throw new InvalidTokenException(
                        "IDENTITY_TOKEN_VERSION_STALE");
            }
            return new IdentityPrincipal(
                    subject,
                    claim(jwt, IdpClaimNames.TENANT_ID),
                    claim(jwt, IdpClaimNames.SESSION_ID),
                    clientId,
                    claim(jwt, "jti"),
                    tokenVersion,
                    tokenAudience,
                    instant(jwt.getIssuedAt(), "iat"),
                    instant(jwt.getExpiresAt(), "exp"));
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException
                 | NullPointerException exception) {
            throw new InvalidTokenException("JWT_INVALID", exception);
        } catch (RuntimeException exception) {
            throw new InvalidTokenException(
                    "IDENTITY_STATE_UNAVAILABLE", exception);
        }
    }

    /**
     * 读取并规范化 JWT 受众声明。
     *
     * <p>Reads and normalizes the JWT audience claim.</p>
     *
     * @param jwt 已解码的 JWT；decoded JWT
     * @return 非空受众集合；non-empty audience set
     * @throws InvalidTokenException 当受众声明缺失或包含空白值时；when the audience claim is
     *                               missing or contains a blank value
     */
    private Set<String> audience(Jwt jwt) {
        List<String> values = jwt.getAudience();
        if (values == null || values.isEmpty()
                || values.stream().anyMatch(
                        value -> value == null || value.isBlank())) {
            throw new InvalidTokenException("JWT_AUDIENCE_MISSING");
        }
        return Set.copyOf(values);
    }

    /**
     * 读取必需的文本声明。
     *
     * <p>Reads a required textual claim.</p>
     *
     * @param jwt 已解码的 JWT；decoded JWT
     * @param name 声明名称；claim name
     * @return 去除首尾空白的声明值；trimmed claim value
     * @throws InvalidTokenException 当声明不是有效的非空文本时；when the claim is not valid
     *                               non-blank text
     */
    private String claim(Jwt jwt, String name) {
        return text(jwt.getClaims().get(name), name);
    }

    /**
     * 读取必需的非负整数声明。
     *
     * <p>Reads a required non-negative numeric claim.</p>
     *
     * @param jwt 已解码的 JWT；decoded JWT
     * @param name 声明名称；claim name
     * @return 声明的长整型值；claim value as a long
     * @throws InvalidTokenException 当声明不是非负数值时；when the claim is not a non-negative
     *                               number
     */
    private long number(Jwt jwt, String name) {
        Object value = jwt.getClaims().get(name);
        if (!(value instanceof Number number) || number.longValue() < 0L) {
            throw new InvalidTokenException(
                    "JWT_CLAIM_INVALID_" + name.toUpperCase());
        }
        return number.longValue();
    }

    /**
     * 校验对象为非空文本并进行规范化。
     *
     * <p>Validates an object as non-blank text and normalizes it.</p>
     *
     * @param value 待校验值；value to validate
     * @param name 头字段或声明名称；header or claim name
     * @return 去除首尾空白的文本；trimmed text
     * @throws InvalidTokenException 当值不是有效文本时；when the value is not valid text
     */
    private String text(Object value, String name) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new InvalidTokenException(
                    "JWT_CLAIM_INVALID_" + name.toUpperCase());
        }
        return text.trim();
    }

    /**
     * 校验必需的 JWT 时间声明存在。
     *
     * <p>Validates that a required JWT timestamp is present.</p>
     *
     * @param value 时间声明值；timestamp claim value
     * @param name 声明名称；claim name
     * @return 原时间值；the original instant
     * @throws InvalidTokenException 当时间声明缺失时；when the timestamp is missing
     */
    private Instant instant(Instant value, String name) {
        if (value == null) {
            throw new InvalidTokenException(
                    "JWT_CLAIM_INVALID_" + name.toUpperCase());
        }
        return value;
    }

    /**
     * 校验调用方提供的必需文本参数。
     *
     * <p>Validates a required textual method argument.</p>
     *
     * @param value 参数值；argument value
     * @param name 参数名称；argument name
     * @return 去除首尾空白的参数值；trimmed argument value
     * @throws InvalidTokenException 当参数为空时；when the argument is blank
     */
    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new InvalidTokenException(name + " is required");
        }
        return value.trim();
    }

    /**
     * 校验并复制受信任配置值集合。
     *
     * <p>Validates and defensively copies a configured set of trusted values.</p>
     *
     * @param values 配置值集合；configured values
     * @param name 配置名称；configuration name
     * @return 规范化后的不可变集合；normalized immutable set
     * @throws NullPointerException 当集合本身为空时；when the set itself is {@code null}
     * @throws IllegalArgumentException 当集合为空或包含空白元素时；when the set is empty or
     *                                  contains a blank element
     */
    private Set<String> requiredValues(Set<String> values, String name) {
        Objects.requireNonNull(values, name);
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        name + " must contain only non-blank values");
            }
            normalized.add(value.trim());
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return Set.copyOf(normalized);
    }

    /**
     * 表示访问令牌或关联实时身份状态未通过校验。
     * 消息保存可供上层稳定返回的原因码。
     *
     * <p>Signals that an access token or its associated current identity state failed validation.
     * The message carries the stable reason code returned by upper layers.</p>
     */
    public static final class InvalidTokenException extends RuntimeException {

        /**
         * 使用失败原因创建异常。
         *
         * <p>Creates an exception with a failure reason.</p>
         *
         * @param message 失败原因码或消息；failure reason code or message
         */
        public InvalidTokenException(String message) {
            super(message);
        }

        /**
         * 使用失败原因与底层异常创建异常。
         *
         * <p>Creates an exception with a failure reason and underlying cause.</p>
         *
         * @param message 失败原因码或消息；failure reason code or message
         * @param cause 底层异常；underlying cause
         */
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
