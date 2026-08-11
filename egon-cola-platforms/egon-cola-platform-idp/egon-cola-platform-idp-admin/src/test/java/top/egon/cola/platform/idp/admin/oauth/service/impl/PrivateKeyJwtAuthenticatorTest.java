package top.egon.cola.platform.idp.admin.oauth.service.impl;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrivateKeyJwtAuthenticatorTest {

    private static final Instant NOW =
            Instant.parse("2026-08-10T00:00:00Z");
    private static final String CLIENT_ID = "idp-service";
    private static final String KEY_ID = "idp-service-key-1";
    private static final URI TOKEN_ENDPOINT =
            URI.create("https://idp.example.test/oauth2/token");

    private final OAuthClientStore clients = mock(OAuthClientStore.class);
    private final ClientCredentialStore credentials =
            mock(ClientCredentialStore.class);
    private final ClientAssertionReplayStore replays =
            mock(ClientAssertionReplayStore.class);

    private RSAKey key;
    private PrivateKeyJwtAuthenticator authenticator;

    @BeforeEach
    void setUp() throws Exception {
        key = new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
        when(clients.findById(CLIENT_ID))
                .thenReturn(Optional.of(confidentialClient()));
        when(credentials.findByClientIdAndKeyId(CLIENT_ID, KEY_ID))
                .thenReturn(Optional.of(credential(key, NOW.minusSeconds(60),
                        NOW.plusSeconds(60))));
        when(replays.markIfAbsent(CLIENT_ID, "assertion-1",
                NOW.plusSeconds(60))).thenReturn(true);
        authenticator = new PrivateKeyJwtAuthenticator(
                clients,
                credentials,
                replays,
                TOKEN_ENDPOINT,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void authenticatesExactRs256AssertionAndStoresReplayKey()
            throws Exception {
        String assertion = assertion(
                key,
                JWSAlgorithm.RS256,
                CLIENT_ID,
                CLIENT_ID,
                List.of(TOKEN_ENDPOINT.toString()),
                NOW,
                NOW.plusSeconds(60),
                "assertion-1"
        );

        ClientAssertionAuthentication result = authenticator.authenticate(
                PrivateKeyJwtAuthenticator.ASSERTION_TYPE,
                CLIENT_ID,
                assertion
        );

        assertThat(result.clientId()).isEqualTo(CLIENT_ID);
        assertThat(result.credentialId()).isEqualTo(KEY_ID);
        assertThat(result.assertionId()).isEqualTo("assertion-1");
        verify(credentials).findByClientIdAndKeyId(CLIENT_ID, KEY_ID);
        verify(replays).markIfAbsent(
                CLIENT_ID,
                "assertion-1",
                NOW.plusSeconds(60)
        );
    }

    @Test
    void rejectsWrongIssuerSubjectAudienceAndOversizedWindow()
            throws Exception {
        assertInvalid(assertion(key, JWSAlgorithm.RS256, "other", CLIENT_ID,
                List.of(TOKEN_ENDPOINT.toString()), NOW,
                NOW.plusSeconds(60), "wrong-iss"));
        assertInvalid(assertion(key, JWSAlgorithm.RS256, CLIENT_ID, "other",
                List.of(TOKEN_ENDPOINT.toString()), NOW,
                NOW.plusSeconds(60), "wrong-sub"));
        assertInvalid(assertion(key, JWSAlgorithm.RS256, CLIENT_ID, CLIENT_ID,
                List.of("https://idp.example.test/oauth2/admission"), NOW,
                NOW.plusSeconds(60), "wrong-aud"));
        assertInvalid(assertion(key, JWSAlgorithm.RS256, CLIENT_ID, CLIENT_ID,
                List.of(TOKEN_ENDPOINT.toString()), NOW,
                NOW.plusSeconds(61), "long-window"));
    }

    @Test
    void rejectsUnknownAlgorithmBeforeCredentialLookup() throws Exception {
        SignedJWT token = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256)
                        .type(JOSEObjectType.JWT)
                        .keyID(KEY_ID)
                        .build(),
                claims(CLIENT_ID, CLIENT_ID, List.of(TOKEN_ENDPOINT.toString()),
                        NOW, NOW.plusSeconds(60), "hs-assertion")
        );
        token.sign(new MACSigner(new byte[32]));

        assertInvalid(token.serialize());

        verify(credentials, never()).findByClientIdAndKeyId(CLIENT_ID, KEY_ID);
    }

    @Test
    void rejectsInactiveClientCredentialAndReplay() throws Exception {
        String assertion = assertion(key, JWSAlgorithm.RS256, CLIENT_ID,
                CLIENT_ID, List.of(TOKEN_ENDPOINT.toString()), NOW,
                NOW.plusSeconds(60), "assertion-1");

        when(clients.findById(CLIENT_ID)).thenReturn(Optional.of(
                confidentialClient().withStatus(OAuthClient.Status.DISABLED)
        ));
        assertInvalid(assertion);

        when(clients.findById(CLIENT_ID))
                .thenReturn(Optional.of(confidentialClient()));
        when(credentials.findByClientIdAndKeyId(CLIENT_ID, KEY_ID))
                .thenReturn(Optional.of(new ClientJwkCredential(
                        CLIENT_ID,
                        KEY_ID,
                        "RS256",
                        key.toPublicJWK().toJSONString(),
                        NOW.minusSeconds(60),
                        NOW.plusSeconds(60),
                        ClientJwkCredential.Status.DISABLED,
                        null,
                        0L
                )));
        assertInvalid(assertion);

        when(credentials.findByClientIdAndKeyId(CLIENT_ID, KEY_ID))
                .thenReturn(Optional.of(credential(
                        key,
                        NOW.plusSeconds(1),
                        NOW.plusSeconds(60)
                )));
        assertInvalid(assertion);

        when(credentials.findByClientIdAndKeyId(CLIENT_ID, KEY_ID))
                .thenReturn(Optional.of(credential(
                        key,
                        NOW.minusSeconds(60),
                        NOW.plusSeconds(60)
                )));
        when(replays.markIfAbsent(CLIENT_ID, "assertion-1",
                NOW.plusSeconds(60))).thenReturn(false);
        assertInvalid(assertion);
    }

    private void assertInvalid(String assertion) {
        assertThatThrownBy(() -> authenticator.authenticate(
                PrivateKeyJwtAuthenticator.ASSERTION_TYPE,
                CLIENT_ID,
                assertion
        )).isInstanceOf(OAuthException.class)
                .hasMessage("invalid_client");
    }

    private static String assertion(
            RSAKey rsaKey,
            JWSAlgorithm algorithm,
            String issuer,
            String subject,
            List<String> audience,
            Instant issuedAt,
            Instant expiresAt,
            String tokenId
    ) throws Exception {
        SignedJWT token = new SignedJWT(
                new JWSHeader.Builder(algorithm)
                        .type(JOSEObjectType.JWT)
                        .keyID(KEY_ID)
                        .build(),
                claims(issuer, subject, audience, issuedAt, expiresAt, tokenId)
        );
        token.sign(new RSASSASigner(rsaKey.toPrivateKey()));
        return token.serialize();
    }

    private static JWTClaimsSet claims(
            String issuer,
            String subject,
            List<String> audience,
            Instant issuedAt,
            Instant expiresAt,
            String tokenId
    ) {
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .audience(audience)
                .jwtID(tokenId)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();
    }

    private static OAuthClient confidentialClient() {
        return new OAuthClient(
                CLIENT_ID,
                OAuthClient.ClientType.CONFIDENTIAL,
                OAuthClient.Status.ACTIVE,
                false,
                List.of(),
                Duration.ofMinutes(5),
                Duration.ofDays(1)
        );
    }

    private static ClientJwkCredential credential(
            RSAKey rsaKey,
            Instant validFrom,
            Instant validUntil
    ) {
        return new ClientJwkCredential(
                CLIENT_ID,
                KEY_ID,
                "RS256",
                rsaKey.toPublicJWK().toJSONString(),
                validFrom,
                validUntil,
                ClientJwkCredential.Status.ACTIVE,
                null,
                0L
        );
    }
}
