package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.RotateClientSecretDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.pojo.IdentityClientSecretEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.pojo.IdentityClientRedirectUriEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.RotatedClientSecretVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.IdentityClientSecretRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.tianquan.shoubing.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.tianquan.shoubing.core.port.PasswordHashPort;

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
        when(clients.existsById("yuheng-admin-web")).thenReturn(false);
        when(clients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        IdentityResourceServerEntity resource = resource();
        when(resources.findByResourceUri(resource.getResourceUri()))
                .thenReturn(Optional.of(resource));

        CreatedOAuthClientVO created = service.create(
                new CreateOAuthClientDTO(
                        "yuheng-admin-web",
                        "Yuheng Admin Web",
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
        when(clients.existsById("tianquan-shoubing-service")).thenReturn(false);
        when(clients.save(any())).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        CreatedOAuthClientVO created = service.create(new CreateOAuthClientDTO(
                "tianquan-shoubing-service-app",
                "tianquan-shoubing-service",
                "Tianquan-Shoubing Service",
                IdentityClientEntity.ClientType.CONFIDENTIAL,
                300,
                86_400,
                List.of(),
                List.of()
        ));

        assertThat(created.clientType()).isEqualTo("CONFIDENTIAL");
        assertThat(created.appId()).isEqualTo("tianquan-shoubing-service-app");
        assertThat(created.clientSecret()).isNotBlank();
        assertThat(created.secretHint()).hasSize(4);
        verify(secrets).save(any(IdentityClientSecretEntity.class));
        verify(securityEvents).append(any());
    }

    @Test
    void rotatesConfidentialSecretAndRevokesPreviousCredential() {
        IdentityClientEntity client = IdentityClientEntity.createConfidential(
                "tianquan-shoubing-service-app",
                "tianquan-shoubing-service",
                "Tianquan-Shoubing Service",
                300,
                86_400,
                NOW
        );
        IdentityClientSecretEntity active = IdentityClientSecretEntity.create(
                "secret-old",
                "tianquan-shoubing-service",
                "{argon2}old-hash",
                "old1",
                NOW
        );
        when(clients.findByClientIdForUpdate("tianquan-shoubing-service"))
                .thenReturn(Optional.of(client));
        when(secrets.findActiveByClientIdForUpdate("tianquan-shoubing-service"))
                .thenReturn(Optional.of(active));
        when(clients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(secrets.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RotatedClientSecretVO rotated = service.rotateSecret(
                "tianquan-shoubing-service",
                new RotateClientSecretDTO(0L)
        );

        assertThat(rotated.clientId()).isEqualTo("tianquan-shoubing-service");
        assertThat(rotated.appId()).isEqualTo("tianquan-shoubing-service-app");
        assertThat(rotated.clientSecret()).isNotBlank();
        assertThat(active.getStatus())
                .isEqualTo(IdentityClientSecretEntity.Status.REVOKED);
        verify(secrets, times(2)).save(any(IdentityClientSecretEntity.class));
        verify(securityEvents).append(any());
    }

    @Test
    void provisionsInitialSecretForMigratedConfidentialClient() {
        IdentityClientEntity client = IdentityClientEntity.createConfidential(
                "tianquan-shoubing-service-app",
                "tianquan-shoubing-service",
                "Tianquan-Shoubing Service",
                300,
                86_400,
                NOW
        );
        when(clients.findByClientIdForUpdate("tianquan-shoubing-service"))
                .thenReturn(Optional.of(client));
        when(secrets.findActiveByClientIdForUpdate("tianquan-shoubing-service"))
                .thenReturn(Optional.empty());
        when(clients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(secrets.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RotatedClientSecretVO provisioned = service.rotateSecret(
                "tianquan-shoubing-service",
                new RotateClientSecretDTO(0L)
        );

        assertThat(provisioned.clientSecret()).isNotBlank();
        verify(secrets).save(any(IdentityClientSecretEntity.class));
        verify(secrets).flush();
    }

    @Test
    void rollsBackWhenSecretPersistenceFailsWithoutReturningPlaintext() {
        when(clients.existsById("tianquan-shoubing-service")).thenReturn(false);
        when(clients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("secret write failed"))
                .when(secrets).save(any(IdentityClientSecretEntity.class));

        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() ->
                service.create(new CreateOAuthClientDTO(
                        "tianquan-shoubing-service-app",
                        "tianquan-shoubing-service",
                        "Tianquan-Shoubing Service",
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
                "tianquan-shoubing-service-app",
                "tianquan-shoubing-service",
                "Tianquan-Shoubing Service",
                300,
                86_400,
                NOW
        );
        when(clients.findByClientIdForUpdate("tianquan-shoubing-service"))
                .thenReturn(Optional.of(client));

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                service.rotateSecret(
                        "tianquan-shoubing-service",
                        new RotateClientSecretDTO(1L)
                )
        )).hasMessage("stale OAuth client version");
    }

    @Test
    void updatesClientUsingOptimisticVersionAndManagesExactValues() {
        IdentityClientEntity client = IdentityClientEntity.createPublic(
                "yuheng-admin-web",
                "Yuheng Admin Web",
                900,
                604800,
                NOW
        );
        when(clients.findById("yuheng-admin-web"))
                .thenReturn(Optional.of(client));
        when(clients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(redirects.findByClientId("yuheng-admin-web"))
                .thenReturn(List.of());
        when(grants.findByClientIdAndGrantTypeAndStatus(
                "yuheng-admin-web",
                IdentityClientResourceGrantEntity.GrantType.USER_DELEGATION,
                IdentityClientResourceGrantEntity.Status.ACTIVE
        ))
                .thenReturn(List.of());
        IdentityResourceServerEntity resource = resource();
        when(resources.findByResourceUri(resource.getResourceUri()))
                .thenReturn(Optional.of(resource));

        OAuthClientVO updated = service.update(
                "yuheng-admin-web",
                new UpdateOAuthClientDTO(
                        "Gateway Web Disabled",
                        IdentityClientEntity.Status.DISABLED,
                        1200,
                        172800,
                        0L
                )
        );
        service.putRedirectUri(
                "yuheng-admin-web",
                "http://127.0.0.1:5173/oauth/callback"
        );
        service.deleteResourceUri(
                "yuheng-admin-web",
                resource.getResourceUri()
        );

        assertThat(updated.status()).isEqualTo("DISABLED");
        assertThat(updated.version()).isEqualTo(1L);
        verify(redirects).save(any(IdentityClientRedirectUriEntity.class));
        verify(grants).deleteByClientIdAndResourceServerIdAndGrantType(
                "yuheng-admin-web",
                resource.getResourceServerId(),
                IdentityClientResourceGrantEntity.GrantType.USER_DELEGATION
        );
    }

    private static IdentityResourceServerEntity resource() {
        return IdentityResourceServerEntity.create(
                "resource-row-1",
                "xingyuan-yuheng-local",
                "https://api.egon.internal/local/platform/yuheng",
                "xingyuan",
                "yuheng",
                "local",
                "Yuheng Local",
                "yuheng-admin-web",
                "yuheng",
                "yuheng:access",
                300,
                IdentityResourceServerEntity.Status.ACTIVE,
                NOW
        );
    }
}
