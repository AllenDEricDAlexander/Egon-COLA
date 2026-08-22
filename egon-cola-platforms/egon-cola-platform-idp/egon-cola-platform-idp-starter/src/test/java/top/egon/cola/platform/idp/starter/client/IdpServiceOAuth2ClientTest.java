package top.egon.cola.platform.idp.starter.client;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.MultiValueMap;
import top.egon.cola.platform.idp.contract.ServiceTokenContext;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdpServiceOAuth2ClientTest {

    private static final URI RESOURCE = URI.create(
            "https://api.example/prod/orders"
    );

    @Test
    void isolatesEveryAuthorizationDimension() {
        IdpServiceTokenRequest base = request(
                "egon-idp",
                "orders-app",
                ServiceTokenContext.TENANT,
                "tenant-1",
                Set.of("orders:read", "orders:write")
        );
        ServiceAuthorizationKey key = ServiceAuthorizationKey.from(base);

        assertThat(base.scopes()).containsExactly(
                "orders:read",
                "orders:write"
        );
        assertThat(key).isEqualTo(ServiceAuthorizationKey.from(request(
                "egon-idp",
                "orders-app",
                ServiceTokenContext.TENANT,
                "tenant-1",
                Set.of("orders:write", "orders:read")
        )));
        assertThat(key).isNotEqualTo(ServiceAuthorizationKey.from(request(
                "egon-idp",
                "orders-app",
                ServiceTokenContext.TENANT,
                "tenant-2",
                base.scopes()
        )));
        assertThat(key).isNotEqualTo(ServiceAuthorizationKey.from(request(
                "egon-idp",
                "other-app",
                ServiceTokenContext.TENANT,
                "tenant-1",
                base.scopes()
        )));
        assertThat(key).isNotEqualTo(ServiceAuthorizationKey.from(request(
                "egon-idp",
                "orders-app",
                ServiceTokenContext.PLATFORM,
                null,
                base.scopes()
        )));
    }

    @Test
    void coalescesConcurrentRenewalForOneAuthorizationKey() throws Exception {
        OAuth2AuthorizedClientManager manager = mock(
                OAuth2AuthorizedClientManager.class
        );
        Instant now = Instant.parse("2026-08-22T02:00:00Z");
        OAuth2AuthorizedClient authorizedClient = authorizedClient(
                "fresh-token",
                now,
                now.plusSeconds(300)
        );
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(manager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenAnswer(invocation -> {
                    calls.incrementAndGet();
                    entered.countDown();
                    release.await();
                    return authorizedClient;
                });

        IdpServiceOAuth2Client client = new IdpServiceOAuth2Client(
                manager,
                Clock.fixed(now, java.time.ZoneOffset.UTC),
                Duration.ofSeconds(30)
        );
        IdpServiceTokenRequest request = request(
                "egon-idp",
                "orders-app",
                ServiceTokenContext.TENANT,
                "tenant-1",
                Set.of("orders:read")
        );
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<OAuth2AccessToken>> futures = new ArrayList<>();
            Callable<OAuth2AccessToken> call = () -> client.authorize(request);
            for (int i = 0; i < 4; i++) {
                futures.add(executor.submit(call));
            }
            assertThat(entered.await(2, java.util.concurrent.TimeUnit.SECONDS))
                    .isTrue();
            release.countDown();
            for (Future<OAuth2AccessToken> future : futures) {
                assertThat(future.get().getTokenValue())
                        .isEqualTo("fresh-token");
            }
            assertThat(calls).hasValue(1);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void converterUsesBasicHeaderAndOnlyStandardExtensionParameters()
            throws Exception {
        ClientRegistration registration = registration();
        var grant = new org.springframework.security.oauth2.client.endpoint
                .OAuth2ClientCredentialsGrantRequest(registration);
        IdpClientCredentialsRequestEntityConverter converter =
                new IdpClientCredentialsRequestEntityConverter();

        var entity = converter.convert(
                grant,
                RESOURCE,
                ServiceTokenContext.TENANT,
                "tenant-1",
                Set.of("orders:write", "orders:read")
        );

        assertThat(entity.getHeaders().getFirst("Authorization"))
                .startsWith("Basic ");
        @SuppressWarnings("unchecked")
        MultiValueMap<String, String> body =
                (MultiValueMap<String, String>) entity.getBody();
        assertThat(body.getFirst("grant_type"))
                .isEqualTo("client_credentials");
        assertThat(body.getFirst("resource")).isEqualTo(RESOURCE.toString());
        assertThat(body.getFirst("tenant_id")).isEqualTo("tenant-1");
        assertThat(body.getFirst("scope"))
                .isEqualTo("orders:read orders:write");
        assertThat(body).doesNotContainKey("client_secret")
                .doesNotContainKey("client_assertion");
    }

    private static IdpServiceTokenRequest request(
            String registrationId,
            String appId,
            ServiceTokenContext context,
            String tenantId,
            Set<String> scopes
    ) {
        return new IdpServiceTokenRequest(
                registrationId,
                appId,
                RESOURCE,
                context,
                tenantId,
                scopes
        );
    }

    private static ClientRegistration registration() {
        return ClientRegistration.withRegistrationId("egon-idp")
                .clientId("orders-key")
                .clientSecret("orders-secret")
                .scope("orders:read", "orders:write")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("https://idp.example/oauth2/token")
                .build();
    }

    private static OAuth2AuthorizedClient authorizedClient(
            String value,
            Instant issuedAt,
            Instant expiresAt
    ) {
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                value,
                issuedAt,
                expiresAt,
                Set.of("orders:read")
        );
        return new OAuth2AuthorizedClient(
                registration(),
                "orders-app",
                token
        );
    }
}
