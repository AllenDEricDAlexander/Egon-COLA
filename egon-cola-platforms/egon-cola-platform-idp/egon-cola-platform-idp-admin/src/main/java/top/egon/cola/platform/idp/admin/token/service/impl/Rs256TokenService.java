package top.egon.cola.platform.idp.admin.token.service.impl;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import top.egon.cola.platform.idp.core.port.TokenSigner;
import top.egon.cola.platform.idp.core.token.AccessTokenClaims;
import top.egon.cola.platform.idp.core.token.RefreshTokenClaims;
import top.egon.cola.platform.idp.core.token.TokenException;

import java.net.URI;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Rs256TokenService implements TokenSigner {

    private static final String REFRESH_TOKEN_USE = "refresh";

    private final RSAKey rsaKey;
    private final String issuer;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

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
                .build();
        jwtDecoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(this.issuer)
        );
        this.decoder = jwtDecoder;
    }

    @Override
    public String signAccess(AccessTokenClaims claims) {
        Objects.requireNonNull(claims, "claims");
        JwtClaimsSet claimSet = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(claims.subject())
                .audience(claims.audience())
                .claim("tid", claims.tenantId())
                .claim("sid", claims.sessionId())
                .claim("client_id", claims.clientId())
                .claim("token_version", claims.tokenVersion())
                .claim("nonce", claims.nonce())
                .id(claims.tokenId())
                .issuedAt(claims.issuedAt())
                .notBefore(claims.notBefore())
                .expiresAt(claims.expiresAt())
                .build();
        return encode(claimSet, "JWT");
    }

    @Override
    public String signRefresh(RefreshTokenClaims claims) {
        Objects.requireNonNull(claims, "claims");
        JwtClaimsSet claimSet = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(claims.subject())
                .audience(List.of(claims.clientId()))
                .claim("token_use", REFRESH_TOKEN_USE)
                .claim("tid", claims.tenantId())
                .claim("sid", claims.sessionId())
                .claim("client_id", claims.clientId())
                .claim("family_id", claims.familyId())
                .claim("generation", claims.generation())
                .claim("token_version", claims.tokenVersion())
                .claim("access_aud", claims.audience())
                .claim("nonce", claims.nonce())
                .claim("token_id", claims.tokenId())
                .id(claims.tokenId())
                .issuedAt(claims.issuedAt())
                .notBefore(claims.issuedAt())
                .expiresAt(claims.expiresAt())
                .build();
        return encode(claimSet, "JWT");
    }

    @Override
    public RefreshTokenClaims verifyRefresh(String rawRefreshToken) {
        try {
            Jwt jwt = decoder.decode(required(
                    rawRefreshToken,
                    "rawRefreshToken"
            ));
            if (!REFRESH_TOKEN_USE.equals(
                    jwt.getClaimAsString("token_use")
            )) {
                throw invalidToken();
            }
            String clientId = jwt.getClaimAsString("client_id");
            String tokenId = jwt.getClaimAsString("token_id");
            if (clientId == null
                    || tokenId == null
                    || !jwt.getAudience().equals(List.of(clientId))
                    || !Objects.equals(tokenId, jwt.getId())) {
                throw invalidToken();
            }
            return new RefreshTokenClaims(
                    jwt.getSubject(),
                    jwt.getClaimAsString("tid"),
                    jwt.getClaimAsString("sid"),
                    clientId,
                    jwt.getClaimAsString("family_id"),
                    tokenId,
                    longClaim(jwt, "generation"),
                    longClaim(jwt, "token_version"),
                    jwt.getClaimAsStringList("access_aud"),
                    jwt.getClaimAsString("nonce"),
                    jwt.getIssuedAt(),
                    jwt.getExpiresAt()
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw invalidToken();
        }
    }

    public Map<String, Object> jwkSet() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }

    public String issuer() {
        return issuer;
    }

    public JwtDecoder jwtDecoder() {
        return decoder;
    }

    private String encode(JwtClaimsSet claims, String type) {
        JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(rsaKey.getKeyID())
                .type(type)
                .build();
        return encoder.encode(JwtEncoderParameters.from(headers, claims))
                .getTokenValue();
    }

    private static long longClaim(Jwt jwt, String name) {
        Object value = jwt.getClaim(name);
        if (!(value instanceof Number number)) {
            throw invalidToken();
        }
        return number.longValue();
    }

    private static TokenException invalidToken() {
        return new TokenException("invalid_grant");
    }

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

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
