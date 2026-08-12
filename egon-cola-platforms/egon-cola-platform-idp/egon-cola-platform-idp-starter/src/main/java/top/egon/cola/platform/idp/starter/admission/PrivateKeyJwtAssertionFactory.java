package top.egon.cola.platform.idp.starter.admission;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.net.URI;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

/**
 * 为单个 IdP Admission RPC 能力创建最长 60 秒的 RS256 Client Assertion。
 *
 * <p>Creates RS256 Client Assertions lasting at most 60 seconds for one IdP Admission
 * RPC capability.</p>
 */
public final class PrivateKeyJwtAssertionFactory {

    /** Assertion 固定有效秒数；fixed assertion lifetime in seconds. */
    private static final long ASSERTION_TTL_SECONDS = 60L;

    /** Management Client 标识；Management Client identifier. */
    private final String clientId;

    /** Client JWK kid；Client JWK kid. */
    private final String keyId;

    /** Admission RPC 能力精确 Audience；exact Admission RPC capability audience. */
    private final String audience;

    /** Client 私钥；Client private key. */
    private final RSAPrivateKey privateKey;

    /** UTC 业务时钟；UTC business clock. */
    private final Clock clock;

    /** 密码学安全随机源；cryptographically secure random source. */
    private final SecureRandom random;

    /**
     * 创建 RPC 能力 Audience 绑定的 Client Assertion 工厂。
     *
     * <p>Creates an RPC-capability-audience-bound Client Assertion factory.</p>
     *
     * @param clientId Management Client 标识；Management Client identifier
     * @param keyId JWK kid；JWK kid
     * @param audience Admission RPC 能力 URI；Admission RPC capability URI
     * @param privateKey owner-only 文件装载的 RSA 私钥；RSA private key loaded from an owner-only
     * file
     * @param clock UTC 业务时钟；UTC business clock
     * @param random 密码学安全随机源；cryptographically secure random source
     */
    public PrivateKeyJwtAssertionFactory(
            String clientId,
            String keyId,
            URI audience,
            RSAPrivateKey privateKey,
            Clock clock,
            SecureRandom random
    ) {
        this.clientId = required(clientId, "clientId");
        this.keyId = required(keyId, "keyId");
        this.audience = audience(audience);
        this.privateKey = Objects.requireNonNull(privateKey, "privateKey");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * 创建新的不可重放 Admission Client Assertion。
     *
     * <p>Creates a new non-replayable Admission Client Assertion.</p>
     *
     * @return 紧凑 RS256 Client Assertion；compact RS256 Client Assertion
     */
    public String create() {
        try {
            Instant issuedAt = clock.instant();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(clientId)
                    .subject(clientId)
                    .audience(audience)
                    .jwtID(tokenId())
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(
                            issuedAt.plusSeconds(ASSERTION_TTL_SECONDS)))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .type(JOSEObjectType.JWT)
                            .keyID(keyId)
                            .build(),
                    claims
            );
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Admission Client Assertion could not be created",
                    exception
            );
        }
    }

    /**
     * 返回 Management Client 标识。
     *
     * <p>Returns the Management Client identifier.</p>
     *
     * @return Client 标识；Client identifier
     */
    public String clientId() {
        return clientId;
    }

    /**
     * 返回 Client JWK kid。
     *
     * <p>Returns the Client JWK kid.</p>
     *
     * @return JWK kid；JWK kid
     */
    public String keyId() {
        return keyId;
    }

    /**
     * 生成随机 JWT ID。
     *
     * <p>Generates a random JWT identifier.</p>
     *
     * @return Base64URL JWT ID；Base64URL JWT identifier
     */
    private String tokenId() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 校验精确 Audience URI。
     *
     * <p>Validates an exact audience URI.</p>
     *
     * @param value Audience URI；audience URI
     * @return 精确 URI 文本；exact URI text
     */
    private static String audience(URI value) {
        Objects.requireNonNull(value, "audience");
        if (!value.isAbsolute()
                || value.getFragment() != null
                || value.getQuery() != null
                || !value.equals(value.normalize())) {
            throw new IllegalArgumentException(
                    "audience must be an absolute normalized URI"
            );
        }
        return value.toString();
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验文本；validated text
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
