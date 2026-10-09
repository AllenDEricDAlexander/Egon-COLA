package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthClientViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.RotateClientSecretDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.RotatedClientSecretVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthResourceRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.OAuthClientRevokeCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService;
import top.egon.cola.platform.tianquan.shoubing.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

class OAuthClientServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private final OAuthClientRepository clients = mock(OAuthClientRepository.class);
    private final OAuthRealmRepository realms = mock(OAuthRealmRepository.class);
    private final OAuthResourceRepository resources = mock(OAuthResourceRepository.class);
    private final OAuthGrantRepository grants = mock(OAuthGrantRepository.class);
    private final OAuthClientViewConverter views = mock(OAuthClientViewConverter.class);
    private final IdentitySecurityEventPort events = mock(IdentitySecurityEventPort.class);
    private final TokenRevocationService revocations = mock(TokenRevocationService.class);
    private final TrustedRealmBO tenantA = tenant(1001L);
    private final TrustedRealmBO tenantB = tenant(1002L);
    private final TrustedRealmBO platform = platform();
    private OAuthClientServiceImpl service;

    @BeforeEach
    void setUp() {
        when(realms.findByRealmKeyForUpdate(tenantA)).thenReturn(Optional.of(realmRow(tenantA)));
        when(realms.findByRealmKeyForUpdate(tenantB)).thenReturn(Optional.of(realmRow(tenantB)));
        when(realms.findByRealmKeyForUpdate(platform)).thenReturn(Optional.of(realmRow(platform)));
        service = new OAuthClientServiceImpl(clients, realms, resources, grants, views, events, revocations,
                new SecureRandom(), Clock.fixed(NOW, ZoneOffset.UTC), "test");
        when(views.toCreatedTarget(any(OAuthClientBO.class), any(URI.class), nullable(String.class)))
                .thenAnswer(invocation -> {
                    OAuthClientBO client = invocation.getArgument(0);
                    URI issuer = invocation.getArgument(1);
                    String secret = invocation.getArgument(2);
                    return CreatedOAuthClientVO.builder().clientId(client.getClientId())
                            .appId(client.getAppId()).clientName(client.getClientName())
                            .clientType(client.getClientType().name()).purpose(client.getPurpose().name())
                            .status(client.getStatus().name()).clientSecret(secret).secretHint(client.getSecretHint())
                            .version(client.getVersion()).createdAt(client.getCreatedAt())
                            .issuer(issuer.toASCIIString()).build();
                });
        when(views.toRotatedTarget(any(OAuthClientBO.class), any(URI.class), anyString(), any(Instant.class)))
                .thenAnswer(invocation -> {
                    OAuthClientBO client = invocation.getArgument(0);
                    URI issuer = invocation.getArgument(1);
                    String secret = invocation.getArgument(2);
                    Instant rotatedAt = invocation.getArgument(3);
                    return RotatedClientSecretVO.builder().clientId(client.getClientId()).appId(client.getAppId())
                            .clientSecret(secret).secretHint(client.getSecretHint()).version(client.getVersion())
                            .rotatedAt(rotatedAt).issuer(issuer.toASCIIString()).build();
                });
    }

    @Test
    void createsPublicPkceClientAndSameRealmUserDelegationGrants() {
        String resourceUri = "https://api.example/orders";
        when(clients.findByClientId(tenantA, "orders-web")).thenReturn(Optional.empty());
        when(clients.findAllByRealm(tenantA)).thenReturn(List.of());
        when(resources.findByResourceUri(platform, URI.create(resourceUri))).thenReturn(Optional.of(resource()));
        when(clients.save(eq(tenantA), any(OAuthClientBO.class), eq(0L))).thenAnswer(invocation -> {
            OAuthClientBO value = invocation.getArgument(1);
            value.setId(501L).setCreatedAt(NOW).setUpdatedAt(NOW);
            return value;
        });
        when(grants.findGrantForUpdate(eq(tenantA), eq(501L), eq("orders-resource"),
                eq(ResourceGrantType.USER_DELEGATION))).thenReturn(Optional.empty());

        var created = service.create(tenantA, platform, userClient(resourceUri), "admin-1");

        assertThat(created.getClientSecret()).isNull();
        assertThat(created.getIssuer()).isEqualTo(tenantA.getIssuer().toASCIIString());
        verify(clients).save(eq(tenantA), any(OAuthClientBO.class), eq(0L));
        verify(revocations).revokeClient(eq(tenantA), argThat(command -> command.getClientPk() == 501L
                && command.getReasonCode().equals("CLIENT_CREATED")));
        var initialGrant = org.mockito.ArgumentCaptor.forClass(OAuthGrantBO.class);
        verify(grants).save(eq(tenantA), initialGrant.capture(), eq(0L));
        assertThat(initialGrant.getValue().getAllowedScopes()).isEmpty();
        assertThat(initialGrant.getValue().getGrantType()).isEqualTo(ResourceGrantType.USER_DELEGATION);
        verify(clients, never()).findByClientId(tenantB, "orders-web");
    }

    @Test
    void createsConfidentialServiceClientWithBcryptSecretReturnedOnlyOnce() {
        when(clients.findByClientId(platform, "orders-service")).thenReturn(Optional.empty());
        when(clients.findAllByRealm(platform)).thenReturn(List.of());
        when(clients.save(eq(platform), any(OAuthClientBO.class), eq(0L))).thenAnswer(invocation -> {
            OAuthClientBO value = invocation.getArgument(1);
            value.setId(601L).setCreatedAt(NOW).setUpdatedAt(NOW);
            return value;
        });

        var created = service.create(platform, platform, serviceClient(), "bootstrap-1");

        assertThat(created.getClientSecret()).hasSize(43);
        assertThat(created.toString()).doesNotContain(created.getClientSecret());
        var captor = org.mockito.ArgumentCaptor.forClass(OAuthClientBO.class);
        verify(clients).save(eq(platform), captor.capture(), eq(0L));
        OAuthClientBO stored = captor.getValue();
        assertThat(stored.getSecretHash()).startsWith("$2");
        assertThat(new BCryptPasswordEncoder(12).matches(created.getClientSecret(), stored.getSecretHash())).isTrue();
        assertThat(stored.getSecretHint()).isEqualTo(created.getClientSecret().substring(39));
        verify(revocations).revokeClient(eq(platform), argThat(command -> command.getClientPk() == 601L
                && command.getReasonCode().equals("CLIENT_CREATED")));
    }

    @Test
    void rotatesSecretOnlyWithinSelectedRealmAndIncrementsCredentialVersion() {
        OAuthClientBO current = confidential("orders-service", 40L, 2L, 3L);
        when(clients.findByClientIdForUpdate(tenantA, "orders-service")).thenReturn(Optional.of(current));
        when(clients.save(eq(tenantA), any(OAuthClientBO.class), eq(2L))).thenAnswer(invocation -> invocation.getArgument(1));

        var rotated = service.rotateSecret(tenantA, "orders-service", new RotateClientSecretDTO(2L), "admin-1");

        assertThat(rotated.getIssuer()).isEqualTo(tenantA.getIssuer().toASCIIString());
        var captor = org.mockito.ArgumentCaptor.forClass(OAuthClientBO.class);
        verify(clients).save(eq(tenantA), captor.capture(), eq(2L));
        assertThat(captor.getValue().getVersion()).isEqualTo(3L);
        assertThat(captor.getValue().getCredentialVersion()).isEqualTo(4L);
        assertThat(new BCryptPasswordEncoder(12).matches(rotated.getClientSecret(), captor.getValue().getSecretHash()))
                .isTrue();
        verify(revocations).revokeClient(eq(tenantA), argThat(command -> command.getClientPk() == 40L
                && command.getReasonCode().equals("CLIENT_SECRET_ROTATED")));
        InOrder lockOrder = inOrder(realms, clients);
        lockOrder.verify(realms).findByRealmKeyForUpdate(tenantA);
        lockOrder.verify(clients).findByClientIdForUpdate(tenantA, "orders-service");
        verify(clients, never()).findByClientIdForUpdate(eq(tenantB), eq("orders-service"));
    }

    @Test
    void rejectsStaleRotationBeforeWritingASecret() {
        OAuthClientBO current = confidential("orders-service", 40L, 3L, 3L);
        when(clients.findByClientIdForUpdate(tenantA, "orders-service")).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.rotateSecret(tenantA, "orders-service",
                new RotateClientSecretDTO(2L), "admin-1"))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
        verify(clients, never()).save(eq(tenantA), any(), anyLong());
    }

    @Test
    void rejectsRefreshTokensForServiceClientsAtTheDirectServiceBoundary() {
        OAuthClientBO current = confidential("orders-service", 40L, 2L, 3L);
        when(clients.findByClientIdForUpdate(tenantA, "orders-service")).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.update(tenantA, "orders-service",
                new UpdateOAuthClientDTO("Orders Service", OAuthClient.Status.ACTIVE, 600, 3600, 2L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAUTH_CLIENT_REFRESH_TTL_INVALID");
        verify(clients, never()).save(eq(tenantA), any(OAuthClientBO.class), anyLong());
    }

    @Test
    void updatePreservesRegisteredScopesWhenOptionalScopesAreOmitted() {
        OAuthClientBO current = userBO("orders-web", 2L, List.of("openid", "profile"));
        when(clients.findByClientIdForUpdate(tenantA, "orders-web")).thenReturn(Optional.of(current));
        when(clients.save(eq(tenantA), any(OAuthClientBO.class), eq(2L))).thenAnswer(invocation -> invocation.getArgument(1));
        OAuthClientVO expected = OAuthClientVO.builder().clientId("orders-web").issuer(tenantA.getIssuer().toASCIIString())
                .allowedScopes(List.of("openid", "profile")).build();
        when(views.toTarget(any(OAuthClientBO.class), eq(tenantA.getIssuer()))).thenReturn(expected);

        OAuthClientVO updated = service.update(tenantA, "orders-web",
                new UpdateOAuthClientDTO("Orders Web", OAuthClient.Status.ACTIVE, 600, 0, 2L, null));

        assertThat(updated.getAllowedScopes()).containsExactly("openid", "profile");
        var captor = org.mockito.ArgumentCaptor.forClass(OAuthClientBO.class);
        verify(clients).save(eq(tenantA), captor.capture(), eq(2L));
        assertThat(captor.getValue().getAllowedScopes()).containsExactly("openid", "profile");
        verify(revocations, never()).revokeClient(eq(tenantA), any(OAuthClientRevokeCommand.class));
    }

    @Test
    void updateRejectsClearingScopesWithoutWritingTheClient() {
        OAuthClientBO current = userBO("orders-web", 2L, List.of("openid", "profile"));
        when(clients.findByClientIdForUpdate(tenantA, "orders-web")).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.update(tenantA, "orders-web",
                new UpdateOAuthClientDTO("Orders Web", OAuthClient.Status.ACTIVE, 600, 0, 2L, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAUTH_CLIENT_SCOPES_EMPTY");
        verify(clients, never()).save(eq(tenantA), any(OAuthClientBO.class), anyLong());
    }

    @Test
    void permitsExactLoopbackRedirectOnlyUnderTheLocalProfile() {
        OAuthClientBO current = userBO("orders-web", 2L, List.of("openid"));
        when(clients.findByClientIdForUpdate(tenantA, "orders-web")).thenReturn(Optional.of(current));
        when(clients.save(eq(tenantA), any(OAuthClientBO.class), eq(2L)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(views.toTarget(any(OAuthClientBO.class), eq(tenantA.getIssuer())))
                .thenReturn(OAuthClientVO.builder().clientId("orders-web").issuer(tenantA.getIssuer().toASCIIString())
                        .build());

        OAuthClientServiceImpl localService = new OAuthClientServiceImpl(
                clients, realms, resources, grants, views, events, revocations,
                new SecureRandom(), Clock.fixed(NOW, ZoneOffset.UTC), "local");
        OAuthClientVO updated = localService.putRedirectUri(
                tenantA, "orders-web", "http://127.0.0.1:18121/oauth/callback");

        assertThat(updated).isNotNull();
        var saved = org.mockito.ArgumentCaptor.forClass(OAuthClientBO.class);
        verify(clients).save(eq(tenantA), saved.capture(), eq(2L));
        assertThat(saved.getValue().getRedirectUris()).contains("http://127.0.0.1:18121/oauth/callback");
    }

    @Test
    void acceptsCanonicalHttpsRedirectOutsideTheLocalProfile() {
        OAuthClientBO current = userBO("orders-web", 2L, List.of("openid"));
        when(clients.findByClientIdForUpdate(tenantA, "orders-web")).thenReturn(Optional.of(current));
        when(clients.save(eq(tenantA), any(OAuthClientBO.class), eq(2L)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(views.toTarget(any(OAuthClientBO.class), eq(tenantA.getIssuer())))
                .thenReturn(OAuthClientVO.builder().clientId("orders-web").issuer(tenantA.getIssuer().toASCIIString())
                        .build());

        service.putRedirectUri(tenantA, "orders-web", "https://client.example/callback");

        var saved = org.mockito.ArgumentCaptor.forClass(OAuthClientBO.class);
        verify(clients).save(eq(tenantA), saved.capture(), eq(2L));
        assertThat(saved.getValue().getRedirectUris()).contains("https://client.example/callback");
    }

    @Test
    void rejectsLoopbackHttpRedirectOutsideLocalAndRejectsMalformedServiceScopes() {
        OAuthClientBO current = userBO("orders-web", 2L, List.of("openid"));
        when(clients.findByClientIdForUpdate(tenantA, "orders-web")).thenReturn(Optional.of(current));
        assertThatThrownBy(() -> service.putRedirectUri(
                tenantA, "orders-web", "http://127.0.0.1:18121/oauth/callback"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAUTH_REDIRECT_URI_INVALID");
        verify(clients, never()).save(eq(tenantA), any(OAuthClientBO.class), anyLong());

        CreateOAuthClientDTO invalidScopes = serviceClient().setAllowedScopes(List.of("scope with space"));
        assertThatThrownBy(() -> service.create(platform, platform, invalidScopes, "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAUTH_CLIENT_SCOPE_INVALID");
        verify(clients, never()).save(eq(platform), any(OAuthClientBO.class), anyLong());
    }

    @Test
    void rejectsMalformedRedirectTargetsAndInvalidTtlOnDirectServiceCalls() {
        OAuthClientBO current = userBO("orders-web", 2L, List.of("openid"));
        when(clients.findByClientIdForUpdate(tenantA, "orders-web")).thenReturn(Optional.of(current));
        for (String redirect : List.of(
                "http://localhost:18121/oauth/callback",
                "http://127.0.0.1/oauth/callback",
                "http://127.0.0.1:0/oauth/callback",
                "http://example.test:18121/oauth/callback",
                "https://client.example/callback?next=/",
                "https://client.example/callback#fragment",
                "https://user@client.example/callback",
                "https://client.example/*",
                "https://client.example/callback/../redirect")) {
            assertThatThrownBy(() -> service.putRedirectUri(tenantA, "orders-web", redirect))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("OAUTH_REDIRECT_URI_INVALID");
        }
        CreateOAuthClientDTO invalidTtl = serviceClient().setAccessTokenTtlSeconds(601);
        assertThatThrownBy(() -> service.create(platform, platform, invalidTtl, "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAUTH_CLIENT_TTL_INVALID");
        CreateOAuthClientDTO duplicateRedirects = userClient("https://api.example/orders")
                .setRedirectUris(List.of("https://client.example/callback", "https://client.example/callback"));
        assertThatThrownBy(() -> service.create(tenantA, platform, duplicateRedirects, "admin-1"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(clients, never()).save(eq(tenantA), any(OAuthClientBO.class), anyLong());
        verify(clients, never()).save(eq(platform), any(OAuthClientBO.class), anyLong());
    }

    private static CreateOAuthClientDTO userClient(String resourceUri) {
        return CreateOAuthClientDTO.builder()
                .clientId("orders-web")
                .clientName("Orders Web")
                .clientType(OAuthClient.ClientType.PUBLIC)
                .purpose(OAuthClientPurposeEnum.USER)
                .accessTokenTtlSeconds(600)
                .refreshTokenTtlSeconds(0)
                .redirectUris(List.of("https://client.example/callback"))
                .resourceUris(List.of(resourceUri))
                .allowedScopes(List.of("openid"))
                .build();
    }

    private static CreateOAuthClientDTO serviceClient() {
        return CreateOAuthClientDTO.builder()
                .appId("orders-service")
                .clientId("orders-service")
                .clientName("Orders Service")
                .clientType(OAuthClient.ClientType.CONFIDENTIAL)
                .purpose(OAuthClientPurposeEnum.SERVICE)
                .accessTokenTtlSeconds(600)
                .refreshTokenTtlSeconds(0)
                .redirectUris(List.of())
                .resourceUris(List.of())
                .allowedScopes(List.of("orders:read"))
                .build();
    }

    private static OAuthClientBO confidential(String clientId, long id, long version, long credentialVersion) {
        return OAuthClientBO.builder().id(id).version(version).clientId(clientId).appId("orders-service")
                .clientName("Orders Service").clientType(OAuthClient.ClientType.CONFIDENTIAL)
                .purpose(OAuthClientPurposeEnum.SERVICE).status(OAuthClient.Status.ACTIVE)
                .secretHash("$2a$12$encoded").secretHint("s123").credentialVersion(credentialVersion)
                .redirectUris(List.of()).resourceUris(List.of()).allowedScopes(List.of("orders:read"))
                .accessTtlSeconds(600).refreshTtlSeconds(0).createdAt(NOW).updatedAt(NOW).build();
    }

    private static OAuthClientBO userBO(String clientId, long version, List<String> scopes) {
        return OAuthClientBO.builder().id(44L).version(version).clientId(clientId).clientName("Orders Web")
                .clientType(OAuthClient.ClientType.PUBLIC).purpose(OAuthClientPurposeEnum.USER)
                .status(OAuthClient.Status.ACTIVE).credentialVersion(0L)
                .redirectUris(List.of("https://client.example/callback"))
                .resourceUris(List.of("https://api.example/orders")).allowedScopes(scopes)
                .accessTtlSeconds(600).refreshTtlSeconds(0).createdAt(NOW).updatedAt(NOW).build();
    }

    private static OAuthResourceBO resource() {
        return OAuthResourceBO.builder().id(81L).version(2L).resourceServerId("orders-resource")
                .resourceUri(URI.create("https://api.example/orders")).bizCode("orders").appCode("orders")
                .environment("prod").displayName("Orders").managementClientId("orders-service")
                .managementClientPk(601L).rbacApplicationCode("orders").entryPermissionCode("orders:entry")
                .status(ResourceServerStatus.ACTIVE).admissionTicketTtl(Duration.ofMinutes(5)).build();
    }

    private static TrustedRealmBO tenant(long tenantId) {
        return TrustedRealmBO.builder().issuer(URI.create("https://auth.example/tenants/" + tenantId))
                .realmKey("tenant:" + tenantId).realmType(RealmTypeEnum.TENANT)
                .storagePartitionId(tenantId).businessTenantId(tenantId).trustVersion(0L).build();
    }

    private static TrustedRealmBO platform() {
        return TrustedRealmBO.builder().issuer(URI.create("https://auth.example/platform"))
                .realmKey("platform").realmType(RealmTypeEnum.PLATFORM)
                .storagePartitionId(9001L).businessTenantId(null).trustVersion(0L).build();
    }

    private static OAuthRealmBO realmRow(TrustedRealmBO realm) {
        return OAuthRealmBO.builder()
                .id(realm.getStoragePartitionId())
                .version(0L)
                .issuer(realm.getIssuer())
                .realmKey(realm.getRealmKey())
                .realmType(realm.getRealmType())
                .businessTenantId(realm.getBusinessTenantId())
                .storagePartitionId(realm.getStoragePartitionId())
                .status("ACTIVE")
                .trustVersion(realm.getTrustVersion())
                .build();
    }
}
