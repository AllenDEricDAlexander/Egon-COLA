package top.egon.cola.platform.idp.admin.oauth.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientRedirectUriEntity;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthClientServiceImplTest {

    private static final Instant NOW =
            Instant.parse("2026-08-02T00:00:00Z");

    private final IdentityClientRepository clients =
            mock(IdentityClientRepository.class);
    private final IdentityClientRedirectUriRepository redirects =
            mock(IdentityClientRedirectUriRepository.class);
    private final IdentityResourceServerRepository resources =
            mock(IdentityResourceServerRepository.class);
    private final IdentityClientResourceGrantRepository grants =
            mock(IdentityClientResourceGrantRepository.class);
    private final AtomicLong ids = new AtomicLong(2000L);

    private OAuthClientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OAuthClientServiceImpl(
                clients,
                redirects,
                resources,
                grants,
                ids::incrementAndGet,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsPublicPkceClientWithExactRedirectsAndAudiences() {
        when(clients.existsById("gateway-admin-web")).thenReturn(false);
        when(clients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        IdentityResourceServerEntity resource = resource();
        when(resources.findByResourceUri(resource.getResourceUri()))
                .thenReturn(Optional.of(resource));

        OAuthClientVO created = service.create(
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
        assertThat(created.pkceRequired()).isTrue();
        assertThat(created.redirectUris())
                .containsExactly("http://127.0.0.1:5173/oauth/callback");
        assertThat(created.audiences())
                .containsExactly(resource.getResourceUri());
        verify(redirects).save(any(IdentityClientRedirectUriEntity.class));
        verify(grants).save(any(IdentityClientResourceGrantEntity.class));
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
        service.deleteAudience(
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
