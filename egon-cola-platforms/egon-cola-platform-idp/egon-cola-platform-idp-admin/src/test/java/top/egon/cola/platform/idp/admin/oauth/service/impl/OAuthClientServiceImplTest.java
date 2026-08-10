package top.egon.cola.platform.idp.admin.oauth.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientAudienceEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientRedirectUriEntity;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientAudienceRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;

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
    private final IdentityClientAudienceRepository audiences =
            mock(IdentityClientAudienceRepository.class);
    private final AtomicLong ids = new AtomicLong(2000L);

    private OAuthClientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OAuthClientServiceImpl(
                clients,
                redirects,
                audiences,
                ids::incrementAndGet,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsPublicPkceClientWithExactRedirectsAndAudiences() {
        when(clients.existsById("gateway-admin-web")).thenReturn(false);
        when(clients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OAuthClientVO created = service.create(
                new CreateOAuthClientDTO(
                        "gateway-admin-web",
                        "Gateway Admin Web",
                        900,
                        604800,
                        List.of("http://127.0.0.1:5173/oauth/callback"),
                        List.of("gateway-admin")
                )
        );

        assertThat(created.clientType()).isEqualTo("PUBLIC");
        assertThat(created.pkceRequired()).isTrue();
        assertThat(created.redirectUris())
                .containsExactly("http://127.0.0.1:5173/oauth/callback");
        assertThat(created.audiences()).containsExactly("gateway-admin");
        verify(redirects).save(any(IdentityClientRedirectUriEntity.class));
        verify(audiences).save(any(IdentityClientAudienceEntity.class));
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
        when(audiences.findByClientId("gateway-admin-web"))
                .thenReturn(List.of());

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
        service.deleteAudience("gateway-admin-web", "gateway-admin");

        assertThat(updated.status()).isEqualTo("DISABLED");
        assertThat(updated.version()).isEqualTo(1L);
        verify(redirects).save(any(IdentityClientRedirectUriEntity.class));
        verify(audiences).deleteByClientIdAndAudience(
                "gateway-admin-web",
                "gateway-admin"
        );
    }
}
