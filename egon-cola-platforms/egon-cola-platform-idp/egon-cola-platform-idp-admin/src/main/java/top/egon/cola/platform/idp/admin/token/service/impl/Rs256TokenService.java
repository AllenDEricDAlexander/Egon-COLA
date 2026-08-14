package top.egon.cola.platform.idp.admin.token.service.impl;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import top.egon.cola.platform.idp.core.port.TokenSigner;
import top.egon.cola.platform.idp.core.resource.AdmissionTicketClaims;
import top.egon.cola.platform.idp.core.token.AccessTokenClaims;
import top.egon.cola.platform.idp.core.token.RefreshTokenClaims;
import top.egon.cola.platform.idp.core.token.ServiceAccessTokenClaims;
import top.egon.cola.platform.idp.core.token.TokenException;

import java.net.URI;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 使用 RS256 签发 IdP Access Token 与内部 Refresh Token 的服务。
 *
 * <p>Service that issues IdP access tokens and internal refresh tokens with RS256.</p>
 */
public final class Rs256TokenService implements TokenSigner {

    /** Refresh Token 用途声明；refresh-token use claim. */
    private static final String REFRESH_TOKEN_USE = "refresh";

    /** Resource Server 准入票据用途；Resource Server admission-ticket use. */
    private static final String ADMISSION_TOKEN_USE =
            "resource_server_admission";

    /** DDC Registry 固定准入 Audience；fixed DDC Registry admission audience. */
    private static final String ADMISSION_AUDIENCE = "ddc-registry";

    /** 含私钥的当前 RSA JWK；current RSA JWK including private key. */
    private final RSAKey rsaKey;

    /** 规范化 IdP Issuer；normalized IdP issuer. */
    private final String issuer;

    /** JWT 编码器；JWT encoder. */
    private final JwtEncoder encoder;

    /** Refresh Token 验签器；refresh-token decoder. */
    private final JwtDecoder refreshDecoder;

    /** Access Token 验签器；access-token decoder. */
    private final JwtDecoder accessDecoder;

    /**
     * 创建固定 RS256 密钥的 Token 服务。
     *
     * <p>Creates a token service backed by a fixed RS256 key pair.</p>
     *
     * @param publicKey RSA 公钥；RSA public key
     * @param privateKey RSA 私钥；RSA private key
     * @param kid JWK 密钥标识；JWK key identifier
     * @param issuer IdP Issuer；IdP issuer
     */
    public Rs256TokenService(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey,
            String kid,
            String issuer
    ) {
        Objects.requireNonNull(publicKey, "publicKey");
        Objects.requireNonNull(privateKey, "privateKey");
        this.issuer = normalizedIssuer(issuer);
        this.rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(required(kid, "kid"))
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                .build();
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(
                new JWKSet(rsaKey)
        );
        this.encoder = new NimbusJwtEncoder(source);
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .jwtProcessorCustomizer(processor ->
                        processor.setJWSTypeVerifier(
                                new DefaultJOSEObjectTypeVerifier<>(
                                        new JOSEObjectType("at+jwt")
                                )
                        ))
                .build();
        jwtDecoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(this.issuer)
        );
        this.accessDecoder = jwtDecoder;
        NimbusJwtDecoder internalDecoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        internalDecoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(this.issuer)
        );
        this.refreshDecoder = internalDecoder;
    }

    /**
     * Signs a platform USER access token with only trusted identity claims.
     */
    @Override
    public String signAccess(AccessTokenClaims claims) {
        Objects.requireNonNull(claims, "claims");
        JwtClaimsSet claimSet = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(claims.subject())
                .audience(List.of(claims.audience()))
                .claim("principal_type", claims.principalType().name())
                .claim("tid", claims.tenantId())
                .claim("acr", claims.authenticationContext().acr())
                .claim("auth_time", claims.authenticationContext().authTime())
                .id(claims.tokenId())
                .issuedAt(claims.issuedAt())
                .notBefore(claims.notBefore())
                .expiresAt(claims.expiresAt())
                .build();
        return encode(claimSet, "at+jwt");
    }

    /**
     * 签发 {@code typ=at+jwt} 的单 Resource SERVICE Access Token。
     *
     * <p>Signs a single-Resource SERVICE access token with {@code typ=at+jwt}.</p>
     *
     * @param claims 可信 SERVICE Token 声明；trusted SERVICE token claims
     * @return 紧凑 JWT；compact JWT
     */
    @Override
    public String signServiceAccess(ServiceAccessTokenClaims claims) {
        Objects.requireNonNull(claims, "claims");
        JwtClaimsSet claimSet = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(claims.subject())
                .audience(List.of(claims.audience().toString()))
                .claim("principal_type", claims.principalType().name())
                .claim("client_id", claims.clientId())
                .claim("tid", claims.tenantId())
                .claim("scope", List.copyOf(claims.scopes()))
                .claim("source_biz", claims.sourceBizCode())
                .claim("source_app", claims.sourceAppCode())
                .claim("source_env", claims.sourceEnvironment())
                .claim("credential_id", claims.credentialId())
                .claim("resource_version", claims.resourceVersion())
                .id(claims.tokenId())
                .issuedAt(claims.issuedAt())
                .notBefore(claims.notBefore())
                .expiresAt(claims.expiresAt())
                .build();
        return encode(claimSet, "at+jwt");
    }

    /**
     * 签发与 OAuth Access Token 隔离的 Resource Server Admission Ticket。
     *
     * <p>Signs a Resource Server Admission Ticket isolated from OAuth access tokens.</p>
     *
     * @param claims 已通过准入策略的可信声明；trusted claims authorized by the admission policy
     * @return {@code typ=rs-admission+jwt} 的紧凑 JWT；compact JWT with
     * {@code typ=rs-admission+jwt}
     */
    public String signAdmission(AdmissionTicketClaims claims) {
        Objects.requireNonNull(claims, "claims");
        JwtClaimsSet claimSet = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(claims.resourceServerId())
                .audience(List.of(ADMISSION_AUDIENCE))
                .claim("token_use", ADMISSION_TOKEN_USE)
                .claim("resource", claims.resourceUri().toString())
                .claim("resource_version", claims.resourceVersion())
                .claim("biz", claims.bizCode())
                .claim("app", claims.appCode())
                .claim("env", claims.environment())
                .claim("instance_id", claims.instanceId())
                .claim("credential_id", claims.credentialId())
                .id(claims.tokenId())
                .issuedAt(claims.issuedAt())
                .notBefore(claims.notBefore())
                .expiresAt(claims.expiresAt())
                .build();
        return encode(claimSet, "rs-admission+jwt");
    }

    /** Signs a stable IdP-only refresh token. */
    @Override
    public String signRefresh(RefreshTokenClaims claims) {
        Objects.requireNonNull(claims, "claims");
        JwtClaimsSet claimSet = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(claims.subject())
                .claim("token_use", REFRESH_TOKEN_USE)
                .claim("tid", claims.tenantId())
                .id(claims.tokenId())
                .issuedAt(claims.issuedAt())
                .notBefore(claims.notBefore())
                .expiresAt(claims.expiresAt())
                .build();
        return encode(claimSet, "JWT");
    }

    /**
     * 验证并解析一个内部 Refresh Token。
     *
     * <p>Verifies and parses an internal refresh token.</p>
     *
     * @param rawRefreshToken 紧凑 Refresh Token；compact refresh token
     * @return 已验证 Refresh Token 声明；verified refresh-token claims
     * @throws TokenException Token 无效时抛出；when the token is invalid
     */
    @Override
    public RefreshTokenClaims verifyRefresh(String rawRefreshToken) {
        try {
            Jwt jwt = refreshDecoder.decode(required(
                    rawRefreshToken,
                    "rawRefreshToken"
            ));
            if (!"JWT".equals(jwt.getHeaders().get("typ"))) {
                throw invalidToken();
            }
            if (!REFRESH_TOKEN_USE.equals(
                    jwt.getClaimAsString("token_use")
            )) {
                throw invalidToken();
            }
            String tokenId = jwt.getId();
            if (tokenId == null
                    || jwt.getAudience() != null && !jwt.getAudience().isEmpty()) {
                throw invalidToken();
            }
            return new RefreshTokenClaims(
                    jwt.getSubject(),
                    jwt.getClaimAsString("tid"),
                    tokenId,
                    jwt.getIssuedAt(),
                    jwt.getNotBefore(),
                    jwt.getExpiresAt()
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw invalidToken();
        }
    }

    /**
     * 导出仅含公钥的 JWK Set。
     *
     * <p>Exports a public-only JWK Set.</p>
     *
     * @return JWK Set JSON 对象；JWK Set JSON object
     */
    public Map<String, Object> jwkSet() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }

    /**
     * 返回规范化 Issuer。
     *
     * <p>Returns the normalized issuer.</p>
     *
     * @return Issuer；issuer
     */
    public String issuer() {
        return issuer;
    }

    /**
     * 返回共享 JWT 验签器。
     *
     * <p>Returns the shared JWT decoder.</p>
     *
     * @return JWT 验签器；JWT decoder
     */
    public JwtDecoder jwtDecoder() {
        return accessDecoder;
    }

    /**
     * 以指定 {@code typ} Header 编码 JWT。
     *
     * <p>Encodes a JWT with the specified {@code typ} header.</p>
     *
     * @param claims JWT 声明；JWT claims
     * @param type JOSE {@code typ}；JOSE {@code typ}
     * @return 紧凑 JWT；compact JWT
     */
    private String encode(JwtClaimsSet claims, String type) {
        JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(rsaKey.getKeyID())
                .type(type)
                .build();
        return encoder.encode(JwtEncoderParameters.from(headers, claims))
                .getTokenValue();
    }

    /**
     * 读取一个必填长整型声明。
     *
     * <p>Reads a required long-valued claim.</p>
     *
     * @param jwt 已验证 JWT；verified JWT
     * @param name 声明名；claim name
     * @return 长整型值；long value
     */
    private static long longClaim(Jwt jwt, String name) {
        Object value = jwt.getClaim(name);
        if (!(value instanceof Number number)) {
            throw invalidToken();
        }
        return number.longValue();
    }

    /**
     * 创建统一的 Refresh Token 无效异常。
     *
     * <p>Creates the uniform invalid-refresh-token exception.</p>
     *
     * @return Token 异常；token exception
     */
    private static TokenException invalidToken() {
        return new TokenException("invalid_grant");
    }

    /**
     * 校验 Issuer 为无查询和 Fragment 的绝对地址。
     *
     * <p>Validates the issuer as an absolute URI without query or fragment.</p>
     *
     * @param value 原始 Issuer；raw issuer
     * @return 规范化 Issuer；normalized issuer
     */
    private static String normalizedIssuer(String value) {
        String issuer = required(value, "issuer");
        URI uri = URI.create(issuer);
        if (!uri.isAbsolute()
                || uri.getHost() == null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("issuer must be an absolute URI");
        }
        return issuer.endsWith("/")
                ? issuer.substring(0, issuer.length() - 1)
                : issuer;
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
