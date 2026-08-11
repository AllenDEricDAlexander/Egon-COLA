package top.egon.cola.platform.idp.starter.admission;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class PrivateKeyJwtAdmissionAssertionFactoryTest {

    @Test
    void signsAUniqueSixtySecondAssertionForOnlyTheAdmissionEndpoint()
            throws Exception {
        Instant now = Instant.parse("2026-08-10T08:00:00Z");
        KeyPair pair = rsaKeyPair();
        PrivateKeyJwtAssertionFactory factory =
                new PrivateKeyJwtAssertionFactory(
                        "idp-service",
                        "idp-service-2026-08",
                        URI.create(
                                "https://idp.example/oauth2/resource-server-admission"),
                        (RSAPrivateKey) pair.getPrivate(),
                        Clock.fixed(now, ZoneOffset.UTC),
                        new SecureRandom()
                );

        SignedJWT first = SignedJWT.parse(factory.create());
        SignedJWT second = SignedJWT.parse(factory.create());

        assertThat(first.verify(new RSASSAVerifier(
                (RSAPublicKey) pair.getPublic()))).isTrue();
        assertThat(first.getHeader().getAlgorithm())
                .isEqualTo(JWSAlgorithm.RS256);
        assertThat(first.getHeader().getType()).isEqualTo(JOSEObjectType.JWT);
        assertThat(first.getHeader().getKeyID())
                .isEqualTo("idp-service-2026-08");
        JWTClaimsSet claims = first.getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("idp-service");
        assertThat(claims.getSubject()).isEqualTo("idp-service");
        assertThat(claims.getAudience()).containsExactly(
                "https://idp.example/oauth2/resource-server-admission");
        assertThat(claims.getIssueTime().toInstant()).isEqualTo(now);
        assertThat(claims.getExpirationTime().toInstant())
                .isEqualTo(now.plus(Duration.ofSeconds(60)));
        assertThat(claims.getJWTID()).isNotBlank();
        assertThat(second.getJWTClaimsSet().getJWTID())
                .isNotEqualTo(claims.getJWTID());
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
