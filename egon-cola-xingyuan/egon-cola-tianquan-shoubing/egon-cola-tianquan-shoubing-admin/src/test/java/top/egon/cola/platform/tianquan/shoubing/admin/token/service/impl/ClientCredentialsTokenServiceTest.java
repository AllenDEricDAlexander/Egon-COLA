package top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.ClientSecretAuthentication;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthException;
import top.egon.cola.platform.tianquan.shoubing.core.port.OAuthClientStore;
import top.egon.cola.platform.tianquan.shoubing.core.port.ResourceServerStore;
import top.egon.cola.platform.tianquan.shoubing.core.port.TokenSigner;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientCredentialsAccessPolicy;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServer;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.core.token.ServiceAccessToken;
import top.egon.cola.platform.tianquan.shoubing.core.token.ServiceAccessTokenClaims;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientCredentialsTokenServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-10T00:00:00Z");
    private static final URI TARGET_URI = URI.create(
            "https://api.egon.internal/prod/permission/tianquan-jianshen"
    );

    private final OAuthClientStore clients = mock(OAuthClientStore.class);
    private final ResourceServerStore resources =
            mock(ResourceServerStore.class);
    private final TokenSigner signer = mock(TokenSigner.class);

    private ClientCredentialsTokenService service;

    @BeforeEach
    void setUp() {
        when(clients.findById("tianquan-shoubing-service"))
                .thenReturn(Optional.of(confidentialClient()));
        when(resources.findByUri(TARGET_URI))
                .thenReturn(Optional.of(targetResource()));
        when(resources.findByManagementClientId("tianquan-shoubing-service"))
                .thenReturn(Optional.of(sourceResource()));
        when(resources.findGrant(
                "tianquan-shoubing-service",
                "permission-tianquan-jianshen-prod",
                ResourceGrantType.CLIENT_CREDENTIALS,
                "tenant-001"
        )).thenReturn(Optional.of(grant()));
        when(signer.signServiceAccess(any())).thenReturn("signed-service-jwt");
        service = new ClientCredentialsTokenService(
                clients,
                resources,
                new ClientCredentialsAccessPolicy(resources),
                signer,
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> "service-token-1"
        );
    }

    @Test
    void issuesSingleResourceTenantScopedServiceTokenWithoutRefresh() {
        ServiceAccessToken token = service.issue(
                authentication(),
                TARGET_URI,
                "tenant-001",
                Set.of("tianquan-jianshen:policy:read"),
                Duration.ofMinutes(5)
        );

        assertThat(token.accessToken()).isEqualTo("signed-service-jwt");
        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.scopes()).containsExactly("tianquan-jianshen:policy:read");
        assertThat(token.expiresAt()).isEqualTo(NOW.plusSeconds(300));

        ArgumentCaptor<ServiceAccessTokenClaims> claims =
                ArgumentCaptor.forClass(ServiceAccessTokenClaims.class);
        org.mockito.Mockito.verify(signer).signServiceAccess(claims.capture());
        assertThat(claims.getValue().principalType().name())
                .isEqualTo("SERVICE");
        assertThat(claims.getValue().audience()).isEqualTo(TARGET_URI);
        assertThat(claims.getValue().tenantId()).isEqualTo("tenant-001");
        assertThat(claims.getValue().appId()).isEqualTo("tianquan-shoubing-service-app");
        assertThat(claims.getValue().scopeContext())
                .isEqualTo(ServiceTokenContext.TENANT);
        assertThat(claims.getValue().sourceBizCode()).isEqualTo("permission");
        assertThat(claims.getValue().sourceAppCode()).isEqualTo("tianquan-shoubing");
        assertThat(claims.getValue().sourceEnvironment()).isEqualTo("prod");
        assertThat(claims.getValue().credentialId())
                .isEqualTo("tianquan-shoubing-service-key-1");
        assertThat(claims.getValue().scopes())
                .containsExactly("tianquan-jianshen:policy:read");
    }

    @Test
    void issuesPlatformServiceTokenWithoutTenantClaim() {
        when(resources.findGrant(
                "tianquan-shoubing-service",
                "permission-tianquan-jianshen-prod",
                ResourceGrantType.CLIENT_CREDENTIALS,
                null
        )).thenReturn(Optional.of(new ClientResourceGrant(
                "tianquan-shoubing-service",
                "permission-tianquan-jianshen-prod",
                ResourceGrantType.CLIENT_CREDENTIALS,
                null,
                Set.of("tianquan-jianshen:policy:read"),
                ClientResourceGrant.Status.ACTIVE,
                4L
        )));

        ServiceAccessToken token = service.issue(
                authentication(),
                TARGET_URI,
                null,
                Set.of("tianquan-jianshen:policy:read"),
                Duration.ofMinutes(5)
        );

        assertThat(token).isNotNull();
        ArgumentCaptor<ServiceAccessTokenClaims> claims =
                ArgumentCaptor.forClass(ServiceAccessTokenClaims.class);
        org.mockito.Mockito.verify(signer).signServiceAccess(claims.capture());
        assertThat(claims.getValue().tenantId()).isNull();
        assertThat(claims.getValue().scopeContext())
                .isEqualTo(ServiceTokenContext.PLATFORM);
    }

    @Test
    void rejectsUnknownTargetExactTenantAndScopeEscalation() {
        when(resources.findByUri(TARGET_URI)).thenReturn(Optional.empty());
        assertOAuthError("invalid_target", () -> service.issue(
                authentication(), TARGET_URI, "tenant-001",
                Set.of("tianquan-jianshen:policy:read"), Duration.ofMinutes(5)));

        when(resources.findByUri(TARGET_URI))
                .thenReturn(Optional.of(targetResource()));
        assertOAuthError("invalid_target", () -> service.issue(
                authentication(), TARGET_URI, "tenant-002",
                Set.of("tianquan-jianshen:policy:read"), Duration.ofMinutes(5)));
        assertOAuthError("invalid_scope", () -> service.issue(
                authentication(), TARGET_URI, "tenant-001",
                Set.of("tianquan-jianshen:policy:write"), Duration.ofMinutes(5)));
    }

    @Test
    void rejectsDisabledTargetAndDisabledServiceGrant() {
        when(resources.findByUri(TARGET_URI)).thenReturn(Optional.of(
                targetResource(ResourceServerStatus.DISABLED)
        ));
        assertOAuthError("invalid_target", () -> service.issue(
                authentication(), TARGET_URI, "tenant-001",
                Set.of("tianquan-jianshen:policy:read"), Duration.ofMinutes(5)));

        when(resources.findByUri(TARGET_URI))
                .thenReturn(Optional.of(targetResource()));
        when(resources.findGrant(
                "tianquan-shoubing-service",
                "permission-tianquan-jianshen-prod",
                ResourceGrantType.CLIENT_CREDENTIALS,
                "tenant-001"
        )).thenReturn(Optional.of(new ClientResourceGrant(
                "tianquan-shoubing-service",
                "permission-tianquan-jianshen-prod",
                ResourceGrantType.CLIENT_CREDENTIALS,
                "tenant-001",
                Set.of("tianquan-jianshen:policy:read"),
                ClientResourceGrant.Status.DISABLED,
                4L
        )));
        assertOAuthError("invalid_target", () -> service.issue(
                authentication(), TARGET_URI, "tenant-001",
                Set.of("tianquan-jianshen:policy:read"), Duration.ofMinutes(5)));
    }

    @Test
    void rejectsClientThatIsNoLongerConfidentialAndActive() {
        when(clients.findById("tianquan-shoubing-service")).thenReturn(Optional.of(
                confidentialClient().withStatus(OAuthClient.Status.DISABLED)
        ));

        assertOAuthError("unauthorized_client", () -> service.issue(
                authentication(), TARGET_URI, "tenant-001",
                Set.of("tianquan-jianshen:policy:read"), Duration.ofMinutes(5)));
    }

    @Test
    void rs256ServiceJwtContainsOnlyIdpServiceAuthorizationClaims()
            throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        Rs256TokenService tokens = new Rs256TokenService(
                (RSAPublicKey) pair.getPublic(),
                (RSAPrivateKey) pair.getPrivate(),
                "tianquan-shoubing-key-1",
                "https://tianquan-shoubing.example.test"
        );
        Instant now = Instant.now().minusSeconds(1);
        String encoded = tokens.signServiceAccess(
                new ServiceAccessTokenClaims(
                        "tianquan-shoubing-service",
                        "tianquan-shoubing-service",
                        TARGET_URI,
                        "tenant-001",
                        "permission",
                        "tianquan-shoubing",
                        "prod",
                        "tianquan-shoubing-service-key-1",
                        9L,
                        Set.of("tianquan-jianshen:policy:read"),
                        "service-jti-1",
                        now,
                        now,
                        now.plusSeconds(300),
                        "tianquan-shoubing-service-app",
                        ServiceTokenContext.TENANT
                )
        );

        Jwt claims = tokens.jwtDecoder().decode(encoded);

        assertThat(claims.getHeaders().get("alg")).isEqualTo("RS256");
        assertThat(claims.getHeaders().get("typ")).isEqualTo("at+jwt");
        assertThat(claims.getClaimAsString("principal_type"))
                .isEqualTo("SERVICE");
        assertThat(claims.getClaimAsString("app_id"))
                .isEqualTo("tianquan-shoubing-service-app");
        assertThat(claims.getClaimAsString("scope_context"))
                .isEqualTo("TENANT");
        assertThat(claims.getAudience()).containsExactly(TARGET_URI.toString());
        assertThat(claims.getClaimAsString("tid")).isEqualTo("tenant-001");
        assertThat(claims.getClaimAsStringList("scope"))
                .containsExactly("tianquan-jianshen:policy:read");
        assertThat(claims.getClaimAsString("source_biz"))
                .isEqualTo("permission");
        assertThat(claims.getClaimAsString("source_app")).isEqualTo("tianquan-shoubing");
        assertThat(claims.getClaimAsString("source_env")).isEqualTo("prod");
        assertThat((Object) claims.getClaim("roles")).isNull();
        assertThat((Object) claims.getClaim("permissions")).isNull();
        assertThat((Object) claims.getClaim("refresh_token")).isNull();
    }

    private static void assertOAuthError(
            String error,
            org.assertj.core.api.ThrowableAssert.ThrowingCallable operation
    ) {
        assertThatThrownBy(operation)
                .isInstanceOf(OAuthException.class)
                .hasMessage(error);
    }

    private static ClientSecretAuthentication authentication() {
        return new ClientSecretAuthentication(
                "tianquan-shoubing-service",
                "tianquan-shoubing-service-key-1"
        );
    }

    private static OAuthClient confidentialClient() {
        return new OAuthClient(
                "tianquan-shoubing-service",
                "tianquan-shoubing-service-app",
                OAuthClient.ClientType.CONFIDENTIAL,
                OAuthClient.Status.ACTIVE,
                false,
                List.of(),
                Duration.ofMinutes(5),
                Duration.ofDays(1)
        );
    }

    private static ResourceServer sourceResource() {
        return new ResourceServer(
                "permission-tianquan-shoubing-prod",
                URI.create("https://api.egon.internal/prod/permission/tianquan-shoubing"),
                "permission",
                "tianquan-shoubing",
                "prod",
                "tianquan-shoubing-service",
                "tianquan-shoubing",
                "tianquan-shoubing:access",
                Duration.ofMinutes(5),
                ResourceServerStatus.ACTIVE,
                4L
        );
    }

    private static ResourceServer targetResource() {
        return targetResource(ResourceServerStatus.ACTIVE);
    }

    private static ResourceServer targetResource(
            ResourceServerStatus status
    ) {
        return new ResourceServer(
                "permission-tianquan-jianshen-prod",
                TARGET_URI,
                "permission",
                "tianquan-jianshen",
                "prod",
                "tianquan-jianshen-service",
                "tianquan-jianshen",
                "tianquan-jianshen:access",
                Duration.ofMinutes(5),
                status,
                9L
        );
    }

    private static ClientResourceGrant grant() {
        return new ClientResourceGrant(
                "tianquan-shoubing-service",
                "permission-tianquan-jianshen-prod",
                ResourceGrantType.CLIENT_CREDENTIALS,
                "tenant-001",
                Set.of("tianquan-jianshen:policy:read", "tianquan-jianshen:identity:resolve"),
                ClientResourceGrant.Status.ACTIVE,
                3L
        );
    }
}
