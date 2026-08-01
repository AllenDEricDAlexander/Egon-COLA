package top.egon.cola.platform.rbac3.admin.auth.application;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.Rbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Issues small reference JWTs. Authorization facts remain in the runtime projection.
 */
public final class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtKeyRingService keyRing;
    private final LongIdGenerator idGenerator;
    private final String issuer;
    private final List<String> audiences;
    private final Rbac3RuntimePolicy runtimePolicy;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            JwtKeyRingService keyRing,
            LongIdGenerator idGenerator,
            String issuer,
            List<String> audiences,
            Rbac3RuntimePolicy runtimePolicy) {
        this.jwtEncoder = Objects.requireNonNull(jwtEncoder, "jwtEncoder");
        this.keyRing = Objects.requireNonNull(keyRing, "keyRing");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.issuer = required(issuer, "issuer");
        this.audiences = List.copyOf(audiences);
        if (this.audiences.isEmpty()) {
            throw new IllegalArgumentException("at least one audience is required");
        }
        this.runtimePolicy = Objects.requireNonNull(runtimePolicy, "runtimePolicy");
    }

    public IssuedAccessToken issue(AccessTokenSubject subject, Instant now) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(now, "now");
        Rbac3RuntimePolicy.Snapshot policySnapshot = runtimePolicy.current();
        JwtKeyRingService.KeyDescriptor signingKey = keyRing.signingKey();
        Instant expiresAt = now.plus(policySnapshot.accessTokenTtl());
        Rbac3TokenClaims claims = new Rbac3TokenClaims(
                issuer,
                audiences,
                subject.userId(),
                subject.tenantId(),
                subject.sessionId(),
                subject.authVersion(),
                subject.sessionVersion(),
                subject.policyVersion(),
                idGenerator.nextId(),
                now,
                now,
                expiresAt,
                signingKey.kid());
        JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(signingKey.kid())
                .build();
        JwtClaimsSet claimSet = JwtClaimsSet.builder()
                .issuer(claims.iss())
                .audience(claims.aud())
                .subject(claims.sub())
                .claim("tid", claims.tid())
                .claim("sid", claims.sid())
                .claim("av", claims.av())
                .claim("sv", claims.sv())
                .claim("pv", claims.pv())
                .id(claims.jti())
                .issuedAt(claims.iat())
                .notBefore(claims.nbf())
                .expiresAt(claims.exp())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(headers, claimSet))
                .getTokenValue();
        return new IssuedAccessToken(token, expiresAt, claims);
    }

    public record AccessTokenSubject(
            String tenantId,
            String userId,
            String sessionId,
            long authVersion,
            long sessionVersion,
            long policyVersion
    ) {
    }

    public record IssuedAccessToken(
            String token,
            Instant expiresAt,
            Rbac3TokenClaims claims
    ) {

        @Override
        public String toString() {
            return "IssuedAccessToken[token=<redacted>, expiresAt=" + expiresAt
                    + ", claims=" + claims + ']';
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
