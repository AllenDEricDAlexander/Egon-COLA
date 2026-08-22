package top.egon.cola.platform.idp.admin.oauth.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.RotateClientSecretDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientSecretEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientRedirectUriEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.RotatedClientSecretVO;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientSecretRepository;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthClientServiceImplTest {

    private static final Instant NOW =
            Instant.parse("2026-08-02T00:00:00Z");

    private final IdentityClientRepository clients =
            mock(IdentityClientRepository.class);
    private final IdentityClientRedirectUriRepository redirects =
            mock(IdentityClientRedirectUriRepository.class);
    private final IdentityClientSecretRepository secrets =
            mock(IdentityClientSecretRepository.class);
    private final IdentityResourceServerRepository resources =
            mock(IdentityResourceServerRepository.class);
    private final IdentityClientResourceGrantRepository grants =
            mock(IdentityClientResourceGrantRepository.class);
    private final AtomicLong ids = new AtomicLong(2000L);
    private final PasswordHashPort passwordHashes = mock(PasswordHashPort.class);
    private final IdentitySecurityEventPort securityEvents =
            mock(IdentitySecurityEventPort.class);
    private final SecureRandom secureRandom = mock(SecureRandom.class);

    private OAuthClientServiceImpl service;

    @BeforeEach
    void setUp() {
        when(passwordHashes.encode(any(char[].class)))
                .thenReturn("{argon2}encoded-hash");
        service = new OAuthClientServiceImpl(
                clients,
                redirects,
                resources,
                grants,
                secrets,
                ids::incrementAndGet,
                Clock.fixed(NOW, ZoneOffset.UTC),
                passwordHashes,
                securityEvents,
                secureRandom
        );
    }

    @Test
    void createsPublicPkceClientWithExactRedirectsAndResources() {
        when(clients.existsById("gateway-admin-web")).thenReturn(false);
        when(clients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        IdentityResourceServerEntity resource = resource();
        when(resources.findByResourceUri(resource.getResourceUri()))
                .thenReturn(Optional.of(resource));

        CreatedOAuthClientVO created = service.create(
                new CreateOAuthClientDTO(
                        "gateway-admin-web",
                        "Gateway Admin Web",
                        900,
                        604800,
                        List.of("http://127.0.0.1:5173/oauth/callback"),
                        List.of(resource.getResourceUri())
                )
        );

        assertThat(created.clientType()).isEqualTo("PUBLIC");
        assertThat(created.clientSecret()).isNull();
        verify(redirects).save(any(IdentityClientRedirectUriEntity.class));
        verify(grants).save(any(IdentityClientResourceGrantEntity.class));
    }

    @Test
    void createsMachineConfidentialClientWithoutBrowserValues() {
        when(clients.existsById("idp-service")).thenReturn(false);
        when(clients.save(any())).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        CreatedOAuthClientVO created = service.create(new CreateOAuthClientDTO(
                "idp-service-app",
                "idp-service",
                "IdP Service",
                IdentityClientEntity.ClientType.CONFIDENTIAL,
                300,
                86_400,
                List.of(),
                List.of()
        ));

        assertThat(created.clientType()).isEqualTo("CONFIDENTIAL");
        assertThat(created.appId()).isEqualTo("idp-service-app");
        assertThat(created.clientSecret()).isNotBlank();
        assertThat(created.secretHint()).hasSize(4);
        verify(secrets).save(any(IdentityClientSecretEntity.class));
        verify(securityEvents).append(any());
    }

    @Test
    void rotatesConfidentialSecretAndRevokesPreviousCredential() {
        IdentityClientEntity client = IdentityClientEntity.createConfidential(
                "idp-service-app",
                "idp-service",
                "IdP Service",
                300,
                86_400,
                NOW
        );
        IdentityClientSecretEntity active = IdentityClientSecretEntity.create(
                "secret-old",
                "idp-service",
                "{argon2}old-hash",
                "old1",
                NOW
        );
        when(clients.findByClientIdForUpdate("idp-service"))
                .thenReturn(Optional.of(client));
        when(secrets.findActiveByClientIdForUpdate("idp-service"))
                .thenReturn(Optional.of(active));
        when(clients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(secrets.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RotatedClientSecretVO rotated = service.rotateSecret(
                "idp-service",
                new RotateClientSecretDTO(0L)
        );

        assertThat(rotated.clientId()).isEqualTo("idp-service");
        assertThat(rotated.appId()).isEqualTo("idp-service-app");
        assertThat(rotated.clientSecret()).isNotBlank();
        assertThat(active.getStatus())
                .isEqualTo(IdentityClientSecretEntity.Status.REVOKED);
        verify(secrets, times(2)).save(any(IdentityClientSecretEntity.class));
        verify(securityEvents).append(any());
    }

    @Test
    void rollsBackWhenSecretPersistenceFailsWithoutReturningPlaintext() {
        when(clients.existsById("idp-service")).thenReturn(false);
        when(clients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("secret write failed"))
                .when(secrets).save(any(IdentityClientSecretEntity.class));

        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() ->
                service.create(new CreateOAuthClientDTO(
                        "idp-service-app",
                        "idp-service",
                        "IdP Service",
                        IdentityClientEntity.ClientType.CONFIDENTIAL,
                        300,
                        86_400,
                        List.of(),
                        List.of()
                ))
        );

        assertThat(failure).isInstanceOf(IllegalStateException.class);
        assertThat(failure).hasMessage("secret write failed");
    }

    @Test
    void rejectsSecretRotationOnStaleClientVersion() {
        IdentityClientEntity client = IdentityClientEntity.createConfidential(
                "idp-service-app",
                "idp-service",
                "IdP Service",
                300,
                86_400,
                NOW
        );
        when(clients.findByClientIdForUpdate("idp-service"))
                .thenReturn(Optional.of(client));

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                service.rotateSecret(
                        "idp-service",
                        new RotateClientSecretDTO(1L)
                )
        )).hasMessage("stale OAuth client version");
    }

    @Test
    void updatesClientUsingOptimisticVersionAndManagesExactValues() {
        IdentityClientEntity client = IdentityClientEntity.createPublic(
                "gateway-admin-web",
                "Gateway Admin Web",
                900,
                604800,
                NOW
        );
        when(clients.findById("gateway-admin-web"))
                .thenReturn(Optional.of(client));
        when(clients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(redirects.findByClientId("gateway-admin-web"))
                .thenReturn(List.of());
        when(grants.findByClientIdAndGrantTypeAndStatus(
                "gateway-admin-web",
                IdentityClientResourceGrantEntity.GrantType.USER_DELEGATION,
                IdentityClientResourceGrantEntity.Status.ACTIVE
        ))
                .thenReturn(List.of());
        IdentityResourceServerEntity resource = resource();
        when(resources.findByResourceUri(resource.getResourceUri()))
                .thenReturn(Optional.of(resource));

        OAuthClientVO updated = service.update(
                "gateway-admin-web",
                new UpdateOAuthClientDTO(
                        "Gateway Web Disabled",
                        IdentityClientEntity.Status.DISABLED,
                        1200,
                        172800,
                        0L
                )
        );
        service.putRedirectUri(
                "gateway-admin-web",
                "http://127.0.0.1:5173/oauth/callback"
        );
        service.deleteResourceUri(
                "gateway-admin-web",
                resource.getResourceUri()
        );

        assertThat(updated.status()).isEqualTo("DISABLED");
        assertThat(updated.version()).isEqualTo(1L);
        verify(redirects).save(any(IdentityClientRedirectUriEntity.class));
        verify(grants).deleteByClientIdAndResourceServerIdAndGrantType(
                "gateway-admin-web",
                resource.getResourceServerId(),
                IdentityClientResourceGrantEntity.GrantType.USER_DELEGATION
        );
    }

    private static IdentityResourceServerEntity resource() {
        return IdentityResourceServerEntity.create(
                "resource-row-1",
                "platform-gateway-local",
                "https://api.egon.internal/local/platform/gateway",
                "platform",
                "gateway",
                "local",
                "Gateway Local",
                "gateway-admin-web",
                "gateway",
                "gateway:access",
                300,
                IdentityResourceServerEntity.Status.ACTIVE,
                NOW
        );
    }
}
