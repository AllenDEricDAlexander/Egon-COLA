package top.egon.cola.platform.tianquan.jianshen.starter.client;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpTenantServiceTokenSupplierTest {

    private static final Instant NOW = Instant.parse("2026-08-11T10:00:00Z");

    @Test
    void issuesAndCachesServiceTokensIndependentlyForExactTenants() {
        List<HttpTenantServiceTokenSupplier.TokenRequest> requests =
                new ArrayList<>();
        AtomicInteger assertionSequence = new AtomicInteger();
        HttpTenantServiceTokenSupplier supplier = supplier(
                () -> "assertion-" + assertionSequence.incrementAndGet(),
                request -> {
                    requests.add(request);
                    return new HttpTenantServiceTokenSupplier.TokenResponse(
                            "token-for-" + request.tenantId(),
                            "Bearer",
                            300L
                    );
                }
        );

        assertThat(supplier.apply("tenant-a")).isEqualTo("token-for-tenant-a");
        assertThat(supplier.apply("tenant-b")).isEqualTo("token-for-tenant-b");
        assertThat(supplier.apply("tenant-a")).isEqualTo("token-for-tenant-a");

        assertThat(requests).extracting(
                HttpTenantServiceTokenSupplier.TokenRequest::tenantId
        ).containsExactly("tenant-a", "tenant-b");
        assertThat(requests.getFirst().clientId()).isEqualTo("tianquan-jianshen-service");
        assertThat(requests.getFirst().assertion()).isEqualTo("assertion-1");
        assertThat(requests.getFirst().resourceUri()).isEqualTo(
                URI.create("https://api.egon.internal/permission/tianquan-jianshen")
        );
        assertThat(requests.getFirst().scopes()).containsExactlyInAnyOrder(
                "service:authorization:snapshot",
                "service:authorization:decide"
        );
    }

    @Test
    void rejectsAResponseThatIsNotABearerToken() {
        HttpTenantServiceTokenSupplier supplier = supplier(
                () -> "assertion",
                request -> new HttpTenantServiceTokenSupplier.TokenResponse(
                        "do-not-use", "MAC", 300L
                )
        );

        assertThatThrownBy(() -> supplier.apply("tenant-a"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TIANQUAN_SHOUBING_SERVICE_TOKEN_RESPONSE_INVALID")
                .hasMessageNotContaining("do-not-use");
    }

    @Test
    void productionCallerSelectsTheExplicitRegistrationForEachTenantAndRejectsMissingMapping() {
        var properties = new top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties();
        var client = properties.getServiceClient();
        client.setAppId("stable-service-app");
        client.setRegistrationId("legacy-single");
        client.setCredentialVersions(java.util.Map.of("tenant-a-registration", 1L, "tenant-b-registration", 2L));
        var binding = new org.springframework.beans.BeanWrapperImpl(client);
        assertThat(binding.isWritableProperty("tenantRegistrationIds"))
                .as("production caller requires explicit tenant registration binding").isTrue();
        binding.setPropertyValue("tenantRegistrationIds", java.util.Map.of(
                "1001", "tenant-a-registration", "1002", "tenant-b-registration"));
        var facade = org.mockito.Mockito.mock(
                top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client.class);
        org.mockito.Mockito.when(facade.authorize(org.mockito.ArgumentMatchers.any())).thenReturn(
                new org.springframework.security.oauth2.core.OAuth2AccessToken(
                        org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,
                        "fixture-token", NOW, NOW.plusSeconds(300), Set.of("service:authorization:decide")));
        var supplier = new HttpTenantServiceTokenSupplier(facade, properties,
                URI.create("https://api.example/authorization"), Set.of("service:authorization:decide"));
        assertThat(supplier.apply("1001")).isEqualTo("fixture-token");
        assertThat(supplier.apply("1002")).isEqualTo("fixture-token");
        var requests = org.mockito.ArgumentCaptor.forClass(
                top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceTokenRequest.class);
        org.mockito.Mockito.verify(facade, org.mockito.Mockito.times(2)).authorize(requests.capture());
        assertThat(requests.getAllValues()).extracting(
                top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceTokenRequest::registrationId)
                .containsExactly("tenant-a-registration", "tenant-b-registration");
        assertThat(requests.getAllValues()).extracting(
                top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceTokenRequest::tenantId)
                .containsExactly("1001", "1002");
        assertThatThrownBy(() -> supplier.apply("1003")).isInstanceOf(IllegalStateException.class);
        org.mockito.Mockito.verifyNoMoreInteractions(facade);
    }

    @Test
    void tenantRegistrationBindingRejectsMalformedOverflowAndUnversionedMappings() {
        var client = new top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure
                .IdpStarterProperties.ServiceClient();
        client.setAppId("stable-app");
        client.setRegistrationId("single");
        client.setCredentialVersions(java.util.Map.of("tenant-registration", 1L));
        var binding = new org.springframework.beans.BeanWrapperImpl(client);
        assertThat(binding.isWritableProperty("tenantRegistrationIds")).isTrue();
        for (String invalid : new String[]{"tenant-a", "0", "01", "9223372036854775808"}) {
            binding.setPropertyValue("tenantRegistrationIds", java.util.Map.of(invalid, "tenant-registration"));
            assertThatThrownBy(client::validate).isInstanceOf(IllegalStateException.class);
        }
        binding.setPropertyValue("tenantRegistrationIds", java.util.Map.of("1001", "unversioned"));
        assertThatThrownBy(client::validate).isInstanceOf(IllegalStateException.class);
        binding.setPropertyValue("tenantRegistrationIds", java.util.Map.of());
        client.validate();
    }

    @Test
    void emptyTenantMappingPreservesTheSingleRegistrationAndStillUsesTheTypedFacade() {
        var properties = new top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties();
        properties.getServiceClient().setAppId("stable-app");
        properties.getServiceClient().setRegistrationId("single-registration");
        properties.getServiceClient().setCredentialVersions(java.util.Map.of("single-registration", 3L));
        var facade = org.mockito.Mockito.mock(
                top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client.class);
        org.mockito.Mockito.when(facade.authorize(org.mockito.ArgumentMatchers.any())).thenReturn(
                new org.springframework.security.oauth2.core.OAuth2AccessToken(
                        org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,
                        "fixture-token", NOW, NOW.plusSeconds(300), Set.of("service:authorization:decide")));
        var supplier = new HttpTenantServiceTokenSupplier(facade, properties,
                URI.create("https://api.example/authorization"), Set.of("service:authorization:decide"));
        assertThat(supplier.apply("1001")).isEqualTo("fixture-token");
        org.mockito.Mockito.verify(facade).authorize(org.mockito.ArgumentMatchers.argThat(request ->
                request.registrationId().equals("single-registration") && request.tenantId().equals("1001")));
        org.mockito.Mockito.verifyNoMoreInteractions(facade);
    }

    private HttpTenantServiceTokenSupplier supplier(
            java.util.function.Supplier<String> assertions,
            HttpTenantServiceTokenSupplier.TokenEndpoint endpoint
    ) {
        return new HttpTenantServiceTokenSupplier(
                "tianquan-jianshen-service",
                assertions,
                URI.create("https://api.egon.internal/permission/tianquan-jianshen"),
                Set.of(
                        "service:authorization:snapshot",
                        "service:authorization:decide"
                ),
                Duration.ofSeconds(30),
                Clock.fixed(NOW, ZoneOffset.UTC),
                endpoint
        );
    }
}
