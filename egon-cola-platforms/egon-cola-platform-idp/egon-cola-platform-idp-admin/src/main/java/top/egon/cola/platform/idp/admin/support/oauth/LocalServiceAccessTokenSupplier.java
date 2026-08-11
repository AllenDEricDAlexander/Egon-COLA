package top.egon.cola.platform.idp.admin.support.oauth;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import top.egon.cola.platform.idp.admin.oauth.service.impl.PrivateKeyJwtAuthenticator;
import top.egon.cola.platform.idp.admin.token.service.impl.ClientCredentialsTokenService;
import top.egon.cola.platform.idp.core.oauth.ClientAssertionAuthentication;
import top.egon.cola.platform.idp.core.token.ServiceAccessToken;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 为 IdP 调用 RBAC3 的本地 HTTP 适配器提供短期 SERVICE Bearer Token。
 *
 * <p>Supplies short-lived SERVICE bearer tokens to IdP's local RBAC3 HTTP adapters.</p>
 *
 * <p>该提供器生成端点绑定的 {@code private_key_jwt}，复用生产认证、IdP Service Grant
 * 策略和 RS256 签名链路，并且只缓存到 {@code exp - renewalSkew}。私钥文件必须是绝对路径、
 * 非符号链接的普通文件且仅所有者可访问。</p>
 *
 * <p>The supplier creates endpoint-bound {@code private_key_jwt} assertions, reuses the production
 * authentication, IdP Service Grant policy, and RS256 signing path, and caches only until
 * {@code exp - renewalSkew}. The private-key file must be absolute, non-symlink, regular, and
 * owner-only.</p>
 */
public final class LocalServiceAccessTokenSupplier
        implements Supplier<String> {

    /** Client Assertion 固定有效期；fixed Client Assertion lifetime. */
    private static final Duration ASSERTION_TTL = Duration.ofSeconds(60);

    /** {@code private_key_jwt} 认证器；{@code private_key_jwt} authenticator. */
    private final PrivateKeyJwtAuthenticator authenticator;

    /** SERVICE Token 签发服务；SERVICE token issuance service. */
    private final ClientCredentialsTokenService tokens;

    /** Source Client 标识；Source Client identifier. */
    private final String clientId;

    /** 目标 Resource URI；target Resource URI. */
    private final URI resourceUri;

    /** 精确目标租户；exact target tenant. */
    private final String tenantId;

    /** 请求 Scope；requested scopes. */
    private final Set<String> scopes;

    /** SERVICE Token 有效期；SERVICE token lifetime. */
    private final Duration accessTokenTtl;

    /** 提前续签窗口；renewal skew. */
    private final Duration renewalSkew;

    /** UTC 业务时钟；UTC business clock. */
    private final Clock clock;

    /** Client Assertion 生成器；Client Assertion supplier. */
    private final Supplier<String> assertions;

    /** 最近签发并仍可安全使用的 Token；most recently issued token that remains safe to use. */
    private volatile ServiceAccessToken cached;

    /**
     * 使用 owner-only PKCS#8 RSA 私钥创建生产 Token 提供器。
     *
     * <p>Creates the production token supplier with an owner-only PKCS#8 RSA private key.</p>
     *
     * @param authenticator {@code private_key_jwt} 认证器；authenticator
     * @param tokens SERVICE Token 签发服务；SERVICE token issuance service
     * @param clientId Source Client 标识；Source Client identifier
     * @param keyId Client JWK kid；Client JWK kid
     * @param privateKeyFile owner-only PKCS#8 私钥文件；owner-only PKCS#8 private-key file
     * @param tokenEndpoint Token Endpoint 精确 URI；exact Token Endpoint URI
     * @param resourceUri 目标 Resource URI；target Resource URI
     * @param tenantId 精确目标租户；exact target tenant
     * @param scopes 请求 Scope；requested scopes
     * @param accessTokenTtl SERVICE Token 有效期；SERVICE token lifetime
     * @param renewalSkew 提前续签窗口；renewal skew
     * @param clock UTC 业务时钟；UTC business clock
     * @param secureRandom 密码学随机源；cryptographic random source
     */
    public LocalServiceAccessTokenSupplier(
            PrivateKeyJwtAuthenticator authenticator,
            ClientCredentialsTokenService tokens,
            String clientId,
            String keyId,
            Path privateKeyFile,
            URI tokenEndpoint,
            URI resourceUri,
            String tenantId,
            Set<String> scopes,
            Duration accessTokenTtl,
            Duration renewalSkew,
            Clock clock,
            SecureRandom secureRandom
    ) {
        this(
                authenticator,
                tokens,
                clientId,
                resourceUri,
                tenantId,
                scopes,
                accessTokenTtl,
                renewalSkew,
                clock,
                assertionSupplier(
                        clientId,
                        keyId,
                        privateKeyFile,
                        tokenEndpoint,
                        clock,
                        secureRandom
                )
        );
    }

    /**
     * 创建可注入 Assertion 生成器的 Token 提供器测试接缝。
     *
     * <p>Creates the token supplier test seam with an injectable assertion supplier.</p>
     *
     * @param authenticator {@code private_key_jwt} 认证器；authenticator
     * @param tokens SERVICE Token 签发服务；SERVICE token issuance service
     * @param clientId Source Client 标识；Source Client identifier
     * @param resourceUri 目标 Resource URI；target Resource URI
     * @param tenantId 精确租户；exact tenant
     * @param scopes 请求 Scope；requested scopes
     * @param accessTokenTtl SERVICE Token 有效期；SERVICE token lifetime
     * @param renewalSkew 提前续签窗口；renewal skew
     * @param clock UTC 业务时钟；UTC business clock
     * @param assertions Assertion 生成器；assertion supplier
     */
    LocalServiceAccessTokenSupplier(
            PrivateKeyJwtAuthenticator authenticator,
            ClientCredentialsTokenService tokens,
            String clientId,
            URI resourceUri,
            String tenantId,
            Set<String> scopes,
            Duration accessTokenTtl,
            Duration renewalSkew,
            Clock clock,
            Supplier<String> assertions
    ) {
        this.authenticator = Objects.requireNonNull(
                authenticator,
                "authenticator"
        );
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.clientId = required(clientId, "clientId");
        this.resourceUri = Objects.requireNonNull(
                resourceUri,
                "resourceUri"
        );
        this.tenantId = required(tenantId, "tenantId");
        this.scopes = Set.copyOf(Objects.requireNonNull(scopes, "scopes"));
        if (this.scopes.isEmpty()) {
            throw new IllegalArgumentException("scopes must not be empty");
        }
        this.accessTokenTtl = positive(accessTokenTtl, "accessTokenTtl");
        this.renewalSkew = positive(renewalSkew, "renewalSkew");
        if (this.renewalSkew.compareTo(this.accessTokenTtl) >= 0) {
            throw new IllegalArgumentException(
                    "renewalSkew must be shorter than accessTokenTtl"
            );
        }
        this.clock = Objects.requireNonNull(clock, "clock");
        this.assertions = Objects.requireNonNull(assertions, "assertions");
    }

    /**
     * 返回仍在安全缓存窗口内的 Bearer Header，必要时同步续签。
     *
     * <p>Returns a bearer header within the safe cache window, renewing synchronously when
     * necessary.</p>
     *
     * @return HTTP Authorization Header 值；HTTP Authorization header value
     */
    @Override
    public String get() {
        ServiceAccessToken current = cached;
        if (usable(current)) {
            return current.authorizationHeader();
        }
        synchronized (this) {
            current = cached;
            if (usable(current)) {
                return current.authorizationHeader();
            }
            ClientAssertionAuthentication authentication =
                    authenticator.authenticate(
                            PrivateKeyJwtAuthenticator.ASSERTION_TYPE,
                            clientId,
                            assertions.get()
                    );
            current = tokens.issue(
                    authentication,
                    resourceUri,
                    tenantId,
                    scopes,
                    accessTokenTtl
            );
            cached = current;
            return current.authorizationHeader();
        }
    }

    /**
     * 判断缓存 Token 是否仍在提前续签边界之前。
     *
     * <p>Determines whether the cached token remains before the renewal boundary.</p>
     *
     * @param value 缓存 Token；cached token
     * @return 可复用时为 {@code true}；{@code true} when reusable
     */
    private boolean usable(ServiceAccessToken value) {
        return value != null
                && clock.instant().isBefore(
                        value.expiresAt().minus(renewalSkew)
                );
    }

    /**
     * 创建读取私钥并生成端点绑定 Assertion 的提供器。
     *
     * <p>Creates a supplier that reads the private key and generates endpoint-bound assertions.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param keyId JWK kid；JWK kid
     * @param privateKeyFile 私钥文件；private-key file
     * @param tokenEndpoint Token Endpoint；Token Endpoint
     * @param clock UTC 时钟；UTC clock
     * @param secureRandom 密码学随机源；cryptographic random source
     * @return Assertion 提供器；assertion supplier
     */
    private static Supplier<String> assertionSupplier(
            String clientId,
            String keyId,
            Path privateKeyFile,
            URI tokenEndpoint,
            Clock clock,
            SecureRandom secureRandom
    ) {
        String safeClientId = required(clientId, "clientId");
        String safeKeyId = required(keyId, "keyId");
        Path safePrivateKeyFile = privateKeyPath(privateKeyFile);
        String audience = endpoint(tokenEndpoint);
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(secureRandom, "secureRandom");
        return () -> createAssertion(
                safeClientId,
                safeKeyId,
                safePrivateKeyFile,
                audience,
                clock,
                secureRandom
        );
    }

    /**
     * 生成一个最长 60 秒且带随机 jti 的 RS256 Client Assertion。
     *
     * <p>Creates an RS256 Client Assertion with a maximum 60-second lifetime and random jti.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param keyId JWK kid；JWK kid
     * @param privateKeyFile 私钥文件；private-key file
     * @param audience 精确端点 Audience；exact endpoint audience
     * @param clock UTC 时钟；UTC clock
     * @param secureRandom 密码学随机源；cryptographic random source
     * @return 紧凑 JWT；compact JWT
     */
    private static String createAssertion(
            String clientId,
            String keyId,
            Path privateKeyFile,
            String audience,
            Clock clock,
            SecureRandom secureRandom
    ) {
        try {
            Instant now = clock.instant();
            SignedJWT assertion = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .type(JOSEObjectType.JWT)
                            .keyID(keyId)
                            .build(),
                    new JWTClaimsSet.Builder()
                            .issuer(clientId)
                            .subject(clientId)
                            .audience(audience)
                            .jwtID(randomId(secureRandom))
                            .issueTime(Date.from(now))
                            .expirationTime(Date.from(
                                    now.plus(ASSERTION_TTL)
                            ))
                            .build()
            );
            assertion.sign(new RSASSASigner(privateKey(privateKeyFile)));
            return assertion.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "cannot create local service Client Assertion",
                    exception
            );
        }
    }

    /**
     * 从 owner-only PKCS#8 PEM 文件读取 RSA 私钥。
     *
     * <p>Reads an RSA private key from an owner-only PKCS#8 PEM file.</p>
     *
     * @param path 私钥路径；private-key path
     * @return RSA 私钥；RSA private key
     */
    private static RSAPrivateKey privateKey(Path path) {
        verifyOwnerOnly(path);
        try {
            String pem = Files.readString(path, StandardCharsets.US_ASCII)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] encoded = Base64.getDecoder().decode(pem);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "cannot load local service RSA private key",
                    exception
            );
        }
    }

    /**
     * 校验私钥路径为绝对且规范化路径。
     *
     * <p>Validates the private-key path as absolute and normalized.</p>
     *
     * @param value 私钥路径；private-key path
     * @return 规范化路径；normalized path
     */
    private static Path privateKeyPath(Path value) {
        Objects.requireNonNull(value, "privateKeyFile");
        if (!value.isAbsolute()) {
            throw new IllegalArgumentException(
                    "privateKeyFile must be absolute"
            );
        }
        return value.normalize();
    }

    /**
     * 校验文件为非符号链接且仅所有者可访问。
     *
     * <p>Validates that the file is non-symlink, regular, and owner-only.</p>
     *
     * @param path 私钥文件；private-key file
     */
    private static void verifyOwnerOnly(Path path) {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException(
                    "privateKeyFile must be a non-symlink regular file"
            );
        }
        try {
            Set<PosixFilePermission> permissions =
                    Files.getPosixFilePermissions(path);
            if (permissions.stream().anyMatch(permission -> switch (permission) {
                case GROUP_READ, GROUP_WRITE, GROUP_EXECUTE,
                        OTHERS_READ, OTHERS_WRITE, OTHERS_EXECUTE -> true;
                default -> false;
            })) {
                throw new IllegalStateException(
                        "privateKeyFile must be owner-only"
                );
            }
        } catch (UnsupportedOperationException exception) {
            if (!Files.isReadable(path)) {
                throw new IllegalStateException(
                        "privateKeyFile is not readable",
                        exception
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "cannot inspect privateKeyFile permissions",
                    exception
            );
        }
    }

    /**
     * 生成 URL-safe 随机 jti。
     *
     * <p>Generates a URL-safe random jti.</p>
     *
     * @param secureRandom 密码学随机源；cryptographic random source
     * @return 随机标识；random identifier
     */
    private static String randomId(SecureRandom secureRandom) {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 校验 Token Endpoint URI。
     *
     * <p>Validates the Token Endpoint URI.</p>
     *
     * @param value Token Endpoint；Token Endpoint
     * @return 精确 URI 文本；exact URI text
     */
    private static String endpoint(URI value) {
        Objects.requireNonNull(value, "tokenEndpoint");
        if (!value.isAbsolute()
                || value.getQuery() != null
                || value.getFragment() != null
                || !value.equals(value.normalize())) {
            throw new IllegalArgumentException(
                    "tokenEndpoint must be an absolute endpoint URI"
            );
        }
        return value.toString();
    }

    /**
     * 校验正时长。
     *
     * <p>Validates a positive duration.</p>
     *
     * @param value 待校验时长；duration to validate
     * @param field 字段名；field name
     * @return 已校验时长；validated duration
     */
    private static Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验值；validated value
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
