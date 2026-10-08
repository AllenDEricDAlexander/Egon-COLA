package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthManagementRealmResolverTest {

    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private final TenantIssuerService issuers = mock(TenantIssuerService.class);
    private OAuthManagementRealmResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new OAuthManagementRealmResolver(
                URI.create("https://auth.example"),
                issuers,
                1001L,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(issuers.resolveExactIssuer(URI.create("https://auth.example/tenants/1001")))
                .thenReturn(realm("https://auth.example/tenants/1001", "tenant:1001", RealmTypeEnum.TENANT, 1001L, 1001L));
        when(issuers.resolveExactIssuer(URI.create("https://auth.example/platform")))
                .thenReturn(realm("https://auth.example/platform", "platform", RealmTypeEnum.PLATFORM, 77L, null));
    }

    @Test
    void defaultsToPrincipalTenantAndUsesRegisteredIssuer() {
        TrustedRealmBO trusted = resolver.resolve(principal("1001", "PASSWORD", NOW), null);

        assertThat(trusted.getRealmKey()).isEqualTo("tenant:1001");
        assertThat(trusted.getStoragePartitionId()).isEqualTo(1001L);
    }

    @Test
    void rejectsCrossTenantSelectionBeforeResolvingTargetIssuer() {
        assertThatThrownBy(() -> resolver.resolve(principal("1001", "PASSWORD", NOW), "tenant:1002"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void platformRequiresConfiguredAdministratorTenantAndFreshStrongAuthentication() {
        TrustedRealmBO trusted = resolver.resolve(principal("1001", "STRONG", NOW.minusSeconds(60)), "platform");
        assertThat(trusted.getRealmKey()).isEqualTo("platform");

        assertThatThrownBy(() -> resolver.resolve(principal("1001", "PASSWORD", NOW), "platform"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> resolver.resolve(principal("1001", "STRONG", NOW.minusSeconds(301)), "platform"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> resolver.resolve(principal("2002", "STRONG", NOW), "platform"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsMalformedTenantAndUnknownIssuer() {
        assertThatThrownBy(() -> resolver.resolve(principal("001", "PASSWORD", NOW), null))
                .isInstanceOf(IllegalArgumentException.class);

        when(issuers.resolveExactIssuer(URI.create("https://auth.example/tenants/1001")))
                .thenThrow(new OAuth2AuthenticationException("invalid_request"));
        assertThatThrownBy(() -> resolver.resolve(principal("1001", "PASSWORD", NOW), null))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    private static IdentityPrincipal principal(String tenantId, String acr, Instant authTime) {
        return new IdentityPrincipal(
                "subject-1",
                tenantId,
                "token-1",
                java.util.Set.of("https://api.example/orders"),
                NOW.minusSeconds(60),
                NOW.plusSeconds(300),
                AuthenticationContext.of(acr, authTime));
    }

    private static TrustedRealmBO realm(
            String issuer,
            String realmKey,
            RealmTypeEnum realmType,
            long storagePartitionId,
            Long businessTenantId) {
        return TrustedRealmBO.builder()
                .issuer(URI.create(issuer))
                .realmKey(realmKey)
                .realmType(realmType)
                .storagePartitionId(storagePartitionId)
                .businessTenantId(businessTenantId)
                .trustVersion(0L)
                .build();
    }
}
