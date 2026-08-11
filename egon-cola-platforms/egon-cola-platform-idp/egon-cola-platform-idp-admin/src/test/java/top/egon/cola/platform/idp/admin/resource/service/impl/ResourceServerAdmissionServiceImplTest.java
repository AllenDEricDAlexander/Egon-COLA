package top.egon.cola.platform.idp.admin.resource.service.impl;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.admin.oauth.service.impl.PrivateKeyJwtAuthenticator;
import top.egon.cola.platform.idp.admin.token.service.impl.Rs256TokenService;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.oauth.OAuthException;
import top.egon.cola.platform.idp.core.port.ClientCredentialStore;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;
import top.egon.cola.platform.idp.core.resource.AdmissionRequest;
import top.egon.cola.platform.idp.core.resource.ClientJwkCredential;
import top.egon.cola.platform.idp.core.resource.ClientResourceGrant;
import top.egon.cola.platform.idp.core.resource.ResourceAuthorizationException;
import top.egon.cola.platform.idp.core.resource.ResourceGrantType;
import top.egon.cola.platform.idp.core.resource.ResourceServer;
import top.egon.cola.platform.idp.core.resource.ResourceServerAdmissionPolicy;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceServerAdmissionServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-10T08:00:00Z");
    private static final URI ENDPOINT = URI.create(
            "https://idp.example/oauth2/resource-server-admission");
    private static final URI RESOURCE = URI.create("https://api.example/idp");
    private static final String CLIENT_ID = "idp-service";
    private static final String KEY_ID = "idp-service-2026-08";

    private KeyPair managementKeyPair;
    private Rs256TokenService signer;
    private ResourceServerAdmissionServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        managementKeyPair = rsaKeyPair();
        KeyPair signingKeyPair = rsaKeyPair();
        signer = new Rs256TokenService(
                (RSAPublicKey) signingKeyPair.getPublic(),
                (RSAPrivateKey) signingKeyPair.getPrivate(),
                "idp-signing-2026-08",
                "https://idp.example"
        );
        ResourceServer resource = new ResourceServer(
                "rs-idp-prod",
                RESOURCE,
                "platform",
                "idp",
                "prod",
                CLIENT_ID,
                "idp",
                "idp:entry",
                Duration.ofMinutes(5),
                ResourceServerStatus.ACTIVE,
                7L
        );
        OAuthClient client = new OAuthClient(
                CLIENT_ID,
                OAuthClient.ClientType.CONFIDENTIAL,
                OAuthClient.Status.ACTIVE,
                false,
                List.of()
        );
        ClientJwkCredential credential = credential(
                managementKeyPair,
                ClientJwkCredential.Status.ACTIVE
        );
        Set<String> replayKeys = ConcurrentHashMap.newKeySet();
        PrivateKeyJwtAuthenticator authenticator =
                new PrivateKeyJwtAuthenticator(
                        requested -> CLIENT_ID.equals(requested)
                                ? Optional.of(client)
                                : Optional.empty(),
                        credentialStore(credential),
                        (clientId, tokenId, expiresAt) -> replayKeys.add(
                                clientId + ":" + tokenId),
                        ENDPOINT,
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );
        service = new ResourceServerAdmissionServiceImpl(
                authenticator,
                requested -> CLIENT_ID.equals(requested)
                        ? Optional.of(client)
                        : Optional.empty(),
                credentialStore(credential),
                resourceStore(resource),
                new ResourceServerAdmissionPolicy(),
                signer,
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> "admission-ticket-jti"
        );
    }

    @Test
    void signsEndpointBoundAdmissionTicketWithExactInstanceClaims()
            throws Exception {
        ResourceServerAdmissionServiceImpl.IssuedAdmissionTicket result =
                service.issue(
                        PrivateKeyJwtAuthenticator.ASSERTION_TYPE,
                        CLIENT_ID,
                        assertion("assertion-jti", ENDPOINT),
                        request("prod")
                );

        SignedJWT ticket = SignedJWT.parse(result.ticket());
        RSAKey publicKey = JWKSet.parse(signer.jwkSet())
                .getKeys().getFirst().toRSAKey();

        assertThat(ticket.verify(new RSASSAVerifier(
                publicKey.toRSAPublicKey()))).isTrue();
        assertThat(ticket.getHeader().getType())
                .isEqualTo(new JOSEObjectType("rs-admission+jwt"));
        JWTClaimsSet claims = ticket.getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("https://idp.example");
        assertThat(claims.getSubject()).isEqualTo("rs-idp-prod");
        assertThat(claims.getAudience()).containsExactly("ddc-registry");
        assertThat(claims.getStringClaim("token_use"))
                .isEqualTo("resource_server_admission");
        assertThat(claims.getStringClaim("resource"))
                .isEqualTo("https://api.example/idp");
        assertThat(claims.getLongClaim("resource_version")).isEqualTo(7L);
        assertThat(claims.getStringClaim("biz")).isEqualTo("platform");
        assertThat(claims.getStringClaim("app")).isEqualTo("idp");
        assertThat(claims.getStringClaim("env")).isEqualTo("prod");
        assertThat(claims.getStringClaim("instance_id"))
                .isEqualTo("idp-10.0.0.8-8080");
        assertThat(claims.getStringClaim("credential_id"))
                .isEqualTo(KEY_ID);
        assertThat(claims.getJWTID()).isEqualTo("admission-ticket-jti");
        assertThat(claims.getIssueTime().toInstant()).isEqualTo(NOW);
        assertThat(claims.getNotBeforeTime().toInstant()).isEqualTo(NOW);
        assertThat(claims.getExpirationTime().toInstant())
                .isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(result.expiresAt())
                .isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void rejectsAReplayAfterTheFirstSuccessfulAssertionUse() throws Exception {
        String assertion = assertion("same-jti", ENDPOINT);
        service.issue(
                PrivateKeyJwtAuthenticator.ASSERTION_TYPE,
                CLIENT_ID,
                assertion,
                request("prod")
        );

        assertThatThrownBy(() -> service.issue(
                PrivateKeyJwtAuthenticator.ASSERTION_TYPE,
                CLIENT_ID,
                assertion,
                request("prod")
        )).isInstanceOf(OAuthException.class)
                .hasMessage("invalid_client")
                .extracting(exception -> ((OAuthException) exception)
                        .internalCode())
                .isEqualTo("IDP_CLIENT_ASSERTION_REPLAYED");
    }

    @Test
    void rejectsAValidSignatureWhenTheRequestedTripleDoesNotMatch()
            throws Exception {
        assertThatThrownBy(() -> service.issue(
                PrivateKeyJwtAuthenticator.ASSERTION_TYPE,
                CLIENT_ID,
                assertion("wrong-env-jti", ENDPOINT),
                request("staging")
        )).isInstanceOf(ResourceAuthorizationException.class)
                .extracting(exception -> ((ResourceAuthorizationException)
                        exception).code())
                .isEqualTo("IDP_RESOURCE_SERVER_ENV_MISMATCH");
    }

    @Test
    void rejectsAnAssertionBoundToTheTokenEndpoint() throws Exception {
        assertThatThrownBy(() -> service.issue(
                PrivateKeyJwtAuthenticator.ASSERTION_TYPE,
                CLIENT_ID,
                assertion(
                        "wrong-endpoint-jti",
                        URI.create("https://idp.example/oauth2/token")
                ),
                request("prod")
        )).isInstanceOf(OAuthException.class)
                .hasMessage("invalid_client")
                .extracting(exception -> ((OAuthException) exception)
                        .internalCode())
                .isEqualTo("IDP_CLIENT_ASSERTION_AUDIENCE_INVALID");
    }

    @Test
    void mapsGenericAssertionFailureToAdmissionCredentialError() {
        PrivateKeyJwtAuthenticator failingAuthenticator = mock(
                PrivateKeyJwtAuthenticator.class);
        when(failingAuthenticator.authenticate(any(), any(), any()))
                .thenThrow(new OAuthException(
                        "invalid_client",
                        "invalid_client",
                        "IDP_CLIENT_ASSERTION_INVALID"
                ));
        ResourceServerAdmissionServiceImpl admissionService =
                new ResourceServerAdmissionServiceImpl(
                        failingAuthenticator,
                        requested -> Optional.empty(),
                        credentialStore(credential(
                                managementKeyPair,
                                ClientJwkCredential.Status.ACTIVE
                        )),
                        resourceStore(new ResourceServer(
                                "rs-idp-prod",
                                RESOURCE,
                                "platform",
                                "idp",
                                "prod",
                                CLIENT_ID,
                                "idp",
                                "idp:entry",
                                Duration.ofMinutes(5),
                                ResourceServerStatus.ACTIVE,
                                7L
                        )),
                        new ResourceServerAdmissionPolicy(),
                        signer,
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        () -> "ticket-jti"
                );

        assertThatThrownBy(() -> admissionService.issue(
                PrivateKeyJwtAuthenticator.ASSERTION_TYPE,
                CLIENT_ID,
                "invalid-assertion",
                request("prod")
        )).isInstanceOf(OAuthException.class)
                .extracting(exception -> ((OAuthException) exception)
                        .internalCode())
                .isEqualTo("IDP_RESOURCE_SERVER_CREDENTIAL_INVALID");
    }

    private AdmissionRequest request(String environment) {
        return new AdmissionRequest(
                "rs-idp-prod",
                RESOURCE,
                "platform",
                "idp",
                environment,
                "idp-10.0.0.8-8080"
        );
    }

    private String assertion(String tokenId, URI audience) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(CLIENT_ID)
                .subject(CLIENT_ID)
                .audience(audience.toString())
                .jwtID(tokenId)
                .issueTime(Date.from(NOW))
                .expirationTime(Date.from(NOW.plusSeconds(60)))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(JOSEObjectType.JWT)
                        .keyID(KEY_ID)
                        .build(),
                claims
        );
        jwt.sign(new RSASSASigner(
                (RSAPrivateKey) managementKeyPair.getPrivate()));
        return jwt.serialize();
    }

    private static ClientCredentialStore credentialStore(
            ClientJwkCredential credential
    ) {
        return new ClientCredentialStore() {
            @Override
            public Optional<ClientJwkCredential> findByClientIdAndKeyId(
                    String clientId,
                    String keyId
            ) {
                return credential.clientId().equals(clientId)
                        && credential.keyId().equals(keyId)
                        ? Optional.of(credential)
                        : Optional.empty();
            }

            @Override
            public List<ClientJwkCredential> findByClientId(String clientId) {
                return credential.clientId().equals(clientId)
                        ? List.of(credential)
                        : List.of();
            }
        };
    }

    private static ResourceServerStore resourceStore(
            ResourceServer resource
    ) {
        return new ResourceServerStore() {
            @Override
            public Optional<ResourceServer> findById(String resourceServerId) {
                return resource.resourceServerId().equals(resourceServerId)
                        ? Optional.of(resource)
                        : Optional.empty();
            }

            @Override
            public Optional<ResourceServer> findByUri(URI resourceUri) {
                return resource.resourceUri().equals(resourceUri)
                        ? Optional.of(resource)
                        : Optional.empty();
            }

            @Override
            public Optional<ResourceServer> findByScope(
                    String bizCode,
                    String appCode,
                    String environment
            ) {
                return resource.matches(bizCode, appCode, environment)
                        ? Optional.of(resource)
                        : Optional.empty();
            }

            @Override
            public Optional<ResourceServer> findByManagementClientId(
                    String clientId
            ) {
                return resource.managementClientId().equals(clientId)
                        ? Optional.of(resource)
                        : Optional.empty();
            }

            @Override
            public Optional<ClientResourceGrant> findGrant(
                    String clientId,
                    String resourceServerId,
                    ResourceGrantType grantType,
                    String tenantId
            ) {
                return Optional.empty();
            }
        };
    }

    private static ClientJwkCredential credential(
            KeyPair keyPair,
            ClientJwkCredential.Status status
    ) {
        RSAKey publicJwk = new RSAKey.Builder(
                (RSAPublicKey) keyPair.getPublic())
                .keyID(KEY_ID)
                .algorithm(JWSAlgorithm.RS256)
                .build();
        return new ClientJwkCredential(
                CLIENT_ID,
                KEY_ID,
                "RS256",
                publicJwk.toJSONString(),
                NOW.minusSeconds(60),
                NOW.plusSeconds(600),
                status,
                null,
                1L
        );
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
