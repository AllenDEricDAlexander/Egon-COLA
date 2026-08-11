package top.egon.cola.platform.idp.admin.oauth.service.impl;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import top.egon.cola.platform.idp.core.oauth.ClientAssertionAuthentication;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.oauth.OAuthException;
import top.egon.cola.platform.idp.core.port.ClientAssertionReplayStore;
import top.egon.cola.platform.idp.core.port.ClientCredentialStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.resource.ClientJwkCredential;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 直接验证 OAuth {@code private_key_jwt} Client Assertion。
 *
 * <p>Directly authenticates OAuth {@code private_key_jwt} Client Assertions.</p>
 *
 * <p>认证器在读取 Claims 前先限制 {@code alg=RS256}，随后按 {@code client_id + kid}
 * 选择已登记公钥，并在完整验签成功后原子记录 {@code client_id + jti}。该流程没有可替换
 * 算法，直接编排比额外 Strategy/Factory 层更清晰。</p>
 *
 * <p>The authenticator allowlists {@code alg=RS256} before reading claims, then selects the
 * registered key by {@code client_id + kid}, and atomically stores {@code client_id + jti} only
 * after complete verification. The flow has no interchangeable algorithm, so direct orchestration
 * is clearer than an additional Strategy or Factory layer.</p>
 */
public final class PrivateKeyJwtAuthenticator {

    /** RFC 7523 Client Assertion 类型；RFC 7523 Client Assertion type. */
    public static final String ASSERTION_TYPE =
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    /** Assertion 最大有效期；maximum assertion lifetime. */
    private static final Duration MAXIMUM_LIFETIME = Duration.ofSeconds(60);

    /** 允许的签发时钟漂移；allowed issuance clock skew. */
    private static final Duration ISSUED_AT_SKEW = Duration.ofSeconds(5);

    /** OAuth Client 查询端口；OAuth Client lookup port. */
    private final OAuthClientStore clients;

    /** Client JWK 查询端口；Client JWK lookup port. */
    private final ClientCredentialStore credentials;

    /** Assertion 防重放端口；assertion replay-prevention port. */
    private final ClientAssertionReplayStore replays;

    /** 当前端点的精确 Audience；exact audience of the current endpoint. */
    private final String endpointAudience;

    /** UTC 业务时钟；UTC business clock. */
    private final Clock clock;

    /**
     * 创建端点绑定的 {@code private_key_jwt} 认证器。
     *
     * <p>Creates an endpoint-bound {@code private_key_jwt} authenticator.</p>
     *
     * @param clients OAuth Client 查询端口；OAuth Client lookup port
     * @param credentials Client JWK 查询端口；Client JWK lookup port
     * @param replays Assertion 防重放端口；assertion replay-prevention port
     * @param endpointAudience 当前端点绝对 URI；absolute URI of the current endpoint
     * @param clock UTC 业务时钟；UTC business clock
     */
    public PrivateKeyJwtAuthenticator(
            OAuthClientStore clients,
            ClientCredentialStore credentials,
            ClientAssertionReplayStore replays,
            URI endpointAudience,
            Clock clock
    ) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.credentials = Objects.requireNonNull(
                credentials,
                "credentials"
        );
        this.replays = Objects.requireNonNull(replays, "replays");
        this.endpointAudience = endpoint(endpointAudience);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 验证 Client Assertion 并返回最小认证上下文。
     *
     * <p>Authenticates a Client Assertion and returns the minimal authentication context.</p>
     *
     * @param assertionType Client Assertion 类型；Client Assertion type
     * @param clientId 请求中的 Client 标识；Client identifier from the request
     * @param assertion 紧凑 JWT Assertion；compact JWT assertion
     * @return 已认证 Client；authenticated Client
     * @throws OAuthException Assertion 无效、过期或重放时抛出；when the assertion is invalid,
     * expired, or replayed
     */
    public ClientAssertionAuthentication authenticate(
            String assertionType,
            String clientId,
            String assertion
    ) {
        try {
            if (!ASSERTION_TYPE.equals(assertionType)) {
                throw invalidClient();
            }
            String safeClientId = required(clientId, "clientId");
            String rawAssertion = required(assertion, "clientAssertion");
            if (rawAssertion.length() > 8_192) {
                throw invalidClient();
            }
            SignedJWT jwt = SignedJWT.parse(rawAssertion);
            if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())
                    || !JOSEObjectType.JWT.equals(jwt.getHeader().getType())) {
                throw invalidClient();
            }
            String keyId = required(jwt.getHeader().getKeyID(), "kid");
            OAuthClient client = clients.findById(safeClientId)
                    .filter(value -> value.status()
                            == OAuthClient.Status.ACTIVE)
                    .orElseThrow(PrivateKeyJwtAuthenticator::invalidClient);
            ClientJwkCredential credential = credentials
                    .findByClientIdAndKeyId(client.clientId(), keyId)
                    .filter(value -> value.activeAt(clock.instant()))
                    .orElseThrow(PrivateKeyJwtAuthenticator::invalidClient);
            RSAKey publicKey = RSAKey.parse(credential.publicJwk());
            if (publicKey.isPrivate()
                    || publicKey.getKeyID() != null
                    && !keyId.equals(publicKey.getKeyID())
                    || publicKey.getAlgorithm() != null
                    && !JWSAlgorithm.RS256.equals(publicKey.getAlgorithm())) {
                throw invalidClient();
            }
            if (!jwt.verify(new RSASSAVerifier(publicKey.toRSAPublicKey()))) {
                throw invalidClient();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Instant issuedAt = instant(claims.getIssueTime());
            Instant expiresAt = instant(claims.getExpirationTime());
            String tokenId = required(claims.getJWTID(), "jti");
            Instant now = clock.instant();
            if (!safeClientId.equals(claims.getIssuer())
                    || !safeClientId.equals(claims.getSubject())
                    || !claims.getAudience().equals(
                            List.of(endpointAudience)
                    )
                    || issuedAt.isAfter(now.plus(ISSUED_AT_SKEW))
                    || !expiresAt.isAfter(now)
                    || !expiresAt.isAfter(issuedAt)
                    || Duration.between(issuedAt, expiresAt)
                    .compareTo(MAXIMUM_LIFETIME) > 0) {
                throw invalidClient();
            }
            if (!replays.markIfAbsent(
                    safeClientId,
                    tokenId,
                    expiresAt
            )) {
                throw invalidClient();
            }
            return new ClientAssertionAuthentication(
                    safeClientId,
                    credential.keyId(),
                    tokenId,
                    issuedAt,
                    expiresAt
            );
        } catch (OAuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidClient();
        }
    }

    /**
     * 校验端点 Audience URI。
     *
     * <p>Validates the endpoint audience URI.</p>
     *
     * @param value 端点 URI；endpoint URI
     * @return 精确 URI 文本；exact URI text
     */
    private static String endpoint(URI value) {
        Objects.requireNonNull(value, "endpointAudience");
        if (!value.isAbsolute()
                || value.getFragment() != null
                || value.getQuery() != null
                || !value.equals(value.normalize())) {
            throw new IllegalArgumentException(
                    "endpointAudience must be an absolute endpoint URI"
            );
        }
        return value.toString();
    }

    /**
     * 将必填日期转换为时间点。
     *
     * <p>Converts a required date to an instant.</p>
     *
     * @param value JWT 日期；JWT date
     * @return 时间点；instant
     */
    private static Instant instant(java.util.Date value) {
        if (value == null) {
            throw invalidClient();
        }
        return value.toInstant();
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
            throw invalidClient();
        }
        return value;
    }

    /**
     * 创建不泄露认证细节的 OAuth 错误。
     *
     * <p>Creates an OAuth error that does not disclose authentication details.</p>
     *
     * @return {@code invalid_client}；{@code invalid_client}
     */
    private static OAuthException invalidClient() {
        return new OAuthException("invalid_client", "invalid_client");
    }
}
