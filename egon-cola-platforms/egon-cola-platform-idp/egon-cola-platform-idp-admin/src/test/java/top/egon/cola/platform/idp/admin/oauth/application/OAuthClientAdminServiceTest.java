package top.egon.cola.platform.idp.admin.oauth.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.admin.oauth.domain.IdentityClientAudienceEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.IdentityClientRedirectUriEntity;
import top.egon.cola.platform.idp.admin.oauth.infrastructure.IdentityClientAudienceRepository;
import top.egon.cola.platform.idp.admin.oauth.infrastructure.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.infrastructure.IdentityClientRepository;

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

class OAuthClientAdminServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-02T00:00:00Z");

    private final IdentityClientRepository clients =
            mock(IdentityClientRepository.class);
    private final IdentityClientRedirectUriRepository redirects =
            mock(IdentityClientRedirectUriRepository.class);
    private final IdentityClientAudienceRepository audiences =
            mock(IdentityClientAudienceRepository.class);
    private final AtomicLong ids = new AtomicLong(2000L);

    private OAuthClientAdminService service;

    @BeforeEach
    void setUp() {
        service = new OAuthClientAdminService(
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

        OAuthClientAdminService.ClientView created = service.create(
                new OAuthClientAdminService.CreateClientCommand(
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

        OAuthClientAdminService.ClientView updated = service.update(
                "gateway-admin-web",
                new OAuthClientAdminService.UpdateClientCommand(
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
