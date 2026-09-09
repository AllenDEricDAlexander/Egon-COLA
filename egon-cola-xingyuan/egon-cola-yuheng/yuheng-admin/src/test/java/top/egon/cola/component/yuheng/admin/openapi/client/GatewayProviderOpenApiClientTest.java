package top.egon.cola.component.yuheng.admin.openapi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceKey;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.yuheng.admin.openapi.GatewayOpenApiValidationTestFixture;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiSyncCandidateDTO;
import top.egon.cola.component.yuheng.contract.reporting.openapi.GatewayOpenApiGroupManifestDTO;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GatewayProviderOpenApiClientTest {

    private static final Instant NOW =
            GatewayOpenApiValidationTestFixture.NOW;

    @Test
    void deniedFirstDnsResolutionFailsBeforeTokenOrHttp() {
        GatewayOpenApiTokenSupplier tokenSupplier = mock(
                GatewayOpenApiTokenSupplier.class
        );
        GatewayOpenApiDnsPolicy dnsPolicy = mock(GatewayOpenApiDnsPolicy.class);
        HttpClient httpClient = mock(HttpClient.class);
        when(dnsPolicy.resolveAndValidate("provider.internal"))
                .thenThrow(new GatewayOpenApiFetchException(
                        "GATEWAY_OPENAPI_TARGET_FORBIDDEN",
                        false,
                        "provider target is not allowlisted"
                ));
        GatewayProviderOpenApiClient client = client(
                tokenSupplier,
                dnsPolicy,
                httpClient
        );

        assertThatThrownBy(() -> client.fetch(
                GatewayOpenApiValidationTestFixture.candidate()
        )).isInstanceOf(GatewayOpenApiFetchException.class)
                .satisfies(failure -> assertThat(
                        ((GatewayOpenApiFetchException) failure).errorCode()
                ).isEqualTo("GATEWAY_OPENAPI_TARGET_FORBIDDEN"));
        verifyNoInteractions(tokenSupplier, httpClient);
    }

    @Test
    void secondDnsResolutionIsRequiredImmediatelyBeforeHttp() throws Exception {
        GatewayOpenApiTokenSupplier tokenSupplier = mock(
                GatewayOpenApiTokenSupplier.class
        );
        GatewayOpenApiDnsPolicy dnsPolicy = mock(GatewayOpenApiDnsPolicy.class);
        HttpClient httpClient = mock(HttpClient.class);
        when(dnsPolicy.resolveAndValidate("provider.internal"))
                .thenReturn(List.of(loopbackAddress()))
                .thenThrow(new GatewayOpenApiFetchException(
                        "GATEWAY_OPENAPI_TARGET_REBOUND",
                        false,
                        "provider target changed during resolution"
                ));
        when(tokenSupplier.tokenFor(any(URI.class), anyString()))
                .thenReturn("opaque-token");
        GatewayProviderOpenApiClient client = client(
                tokenSupplier,
                dnsPolicy,
                httpClient
        );

        assertThatThrownBy(() -> client.fetch(
                GatewayOpenApiValidationTestFixture.candidate()
        )).isInstanceOf(GatewayOpenApiFetchException.class)
                .satisfies(failure -> assertThat(
                        ((GatewayOpenApiFetchException) failure).errorCode()
                ).isEqualTo("GATEWAY_OPENAPI_TARGET_REBOUND"));
        verify(tokenSupplier).tokenFor(
                eq(URI.create("https://provider.example/resource")),
                eq("gateway.openapi.read")
        );
        verifyNoInteractions(httpClient);
    }

    @Test
    void tokenHeaderInjectionIsRejectedBeforeHttp() throws Exception {
        GatewayOpenApiTokenSupplier tokenSupplier = mock(
                GatewayOpenApiTokenSupplier.class
        );
        GatewayOpenApiDnsPolicy dnsPolicy = mock(GatewayOpenApiDnsPolicy.class);
        HttpClient httpClient = mock(HttpClient.class);
        when(dnsPolicy.resolveAndValidate("provider.internal"))
                .thenReturn(List.of(loopbackAddress()));
        when(tokenSupplier.tokenFor(any(URI.class), anyString()))
                .thenReturn("token\ninjected");
        GatewayProviderOpenApiClient client = client(
                tokenSupplier,
                dnsPolicy,
                httpClient
        );

        assertThatThrownBy(() -> client.fetch(
                GatewayOpenApiValidationTestFixture.candidate()
        )).isInstanceOf(GatewayOpenApiFetchException.class)
                .satisfies(failure -> assertThat(
                        ((GatewayOpenApiFetchException) failure).errorCode()
                ).isEqualTo("GATEWAY_OPENAPI_OAUTH_FAILED"));
        verifyNoInteractions(httpClient);
    }

    @Test
    void successfulResponseIsBoundedParsedAndHasNoRedirectPolicy() throws Exception {
        GatewayOpenApiTokenSupplier tokenSupplier = mock(
                GatewayOpenApiTokenSupplier.class
        );
        GatewayOpenApiDnsPolicy dnsPolicy = mock(GatewayOpenApiDnsPolicy.class);
        HttpClient httpClient = mock(HttpClient.class);
        when(dnsPolicy.resolveAndValidate("provider.internal"))
                .thenReturn(List.of(loopbackAddress()));
        when(tokenSupplier.tokenFor(any(URI.class), anyString()))
                .thenReturn("opaque-token");
        HttpResponse<InputStream> response = response(
                200,
                "application/json; charset=utf-8",
                "{\"openapi\":\"3.1.0\"}"
        );
        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(response);
        GatewayProviderOpenApiClient client = client(
                tokenSupplier,
                dnsPolicy,
                httpClient
        );

        GatewayOpenApiDocumentDTO document = client.fetch(
                GatewayOpenApiValidationTestFixture.candidate()
        );

        assertThat(document.documentSha256()).isEqualTo(
                GatewayOpenApiValidationTestFixture.sha256(
                        "{\"openapi\":\"3.1.0\"}".getBytes()
                )
        );
        assertThat(document.documentJson().path("openapi").asText())
                .isEqualTo("3.1.0");
        verify(httpClient).send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void redirectAndOversizedResponsesAreRejectedWithoutDocumentHandoff()
            throws Exception {
        GatewayOpenApiTokenSupplier tokenSupplier = mock(
                GatewayOpenApiTokenSupplier.class
        );
        GatewayOpenApiDnsPolicy dnsPolicy = mock(GatewayOpenApiDnsPolicy.class);
        HttpClient httpClient = mock(HttpClient.class);
        when(dnsPolicy.resolveAndValidate(anyString()))
                .thenReturn(List.of(loopbackAddress()));
        when(tokenSupplier.tokenFor(any(URI.class), anyString()))
                .thenReturn("opaque-token");
        HttpResponse<InputStream> redirectResponse = response(
                302,
                "application/json",
                "redirect"
        );
        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(redirectResponse);
        GatewayProviderOpenApiClient redirectClient = client(
                tokenSupplier,
                dnsPolicy,
                httpClient
        );

        assertThatThrownBy(() -> redirectClient.fetch(
                GatewayOpenApiValidationTestFixture.candidate()
        )).isInstanceOf(GatewayOpenApiFetchException.class)
                .satisfies(failure -> assertThat(
                        ((GatewayOpenApiFetchException) failure).errorCode()
                ).isEqualTo("GATEWAY_OPENAPI_HTTP_STATUS"));

        HttpClient oversizedHttpClient = mock(HttpClient.class);
        HttpResponse<InputStream> oversizedResponse = response(
                200,
                "application/json",
                "{\"a\":1}"
        );
        when(oversizedHttpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(oversizedResponse);
        GatewayProviderOpenApiClient oversizedClient = client(
                tokenSupplier,
                dnsPolicy,
                oversizedHttpClient,
                5
        );
        assertThatThrownBy(() -> oversizedClient.fetch(
                GatewayOpenApiValidationTestFixture.candidate()
        )).isInstanceOf(GatewayOpenApiFetchException.class)
                .satisfies(failure -> assertThat(
                        ((GatewayOpenApiFetchException) failure).errorCode()
                ).isEqualTo("GATEWAY_OPENAPI_DOCUMENT_TOO_LARGE"));
    }

    @Test
    void candidateCanOnlyBeDerivedFromHealthyDdcHttpProviderData() {
        DdcManagementServiceKey service = new DdcManagementServiceKey(
                "trade",
                "TEST",
                "orders",
                "orders-service-id",
                "HTTP_PROVIDER",
                "orders-service",
                "default",
                "1.0.0",
                "https"
        );
        DdcManagementServiceSnapshot snapshot =
                new DdcManagementServiceSnapshot(
                        service,
                        7,
                        NOW,
                        List.of(new DdcManagementServiceInstance(
                                "instance-1",
                                "lease-1",
                                "provider.internal",
                                9443,
                                true,
                                java.util.Map.of(),
                                "ONLINE",
                                NOW.minusSeconds(30),
                                NOW,
                                NOW.plusSeconds(60)
                        ))
                );
        GatewayOpenApiGroupManifestDTO manifest =
                new GatewayOpenApiGroupManifestDTO(
                        List.of("orders"),
                        GatewayOpenApiGroupManifestDTO.PATH_TEMPLATE,
                        "https://provider.example/resource",
                        "1.0.0",
                        "build-1"
                );

        GatewayOpenApiSyncCandidateDTO candidate =
                GatewayOpenApiSyncCandidateDTO.from(
                        "app-1",
                        snapshot,
                        snapshot.instances().getFirst(),
                        manifest,
                        "orders"
                );

        assertThat(candidate.applicationId()).isEqualTo("app-1");
        assertThat(candidate.buildId()).isEqualTo("build-1");
        assertThat(candidate.openapiGroup()).isEqualTo("orders");
        assertThat(candidate.providerServiceName())
                .isEqualTo("orders-service");
    }

    @Test
    void developmentHttpCandidateRequiresAnExplicitOptIn() {
        DdcManagementServiceKey service = new DdcManagementServiceKey(
                "trade",
                "TEST",
                "orders",
                "orders-service-id",
                "HTTP_PROVIDER",
                "orders-service",
                "default",
                "1.0.0",
                "http"
        );
        DdcManagementServiceSnapshot snapshot =
                new DdcManagementServiceSnapshot(
                        service,
                        7,
                        NOW,
                        List.of(new DdcManagementServiceInstance(
                                "instance-1",
                                "lease-1",
                                "provider.internal",
                                8080,
                                false,
                                java.util.Map.of(),
                                "ONLINE",
                                NOW.minusSeconds(30),
                                NOW,
                                NOW.plusSeconds(60)
                        ))
                );
        GatewayOpenApiGroupManifestDTO manifest =
                new GatewayOpenApiGroupManifestDTO(
                        List.of("orders"),
                        GatewayOpenApiGroupManifestDTO.PATH_TEMPLATE,
                        "https://provider.example/resource",
                        "1.0.0",
                        "build-1"
                );

        assertThatThrownBy(() -> GatewayOpenApiSyncCandidateDTO.from(
                "app-1",
                snapshot,
                snapshot.instances().getFirst(),
                manifest,
                "orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not an HTTP provider");

        GatewayOpenApiSyncCandidateDTO candidate =
                GatewayOpenApiSyncCandidateDTO.from(
                        "app-1",
                        snapshot,
                        snapshot.instances().getFirst(),
                        manifest,
                        "orders",
                        true
                );

        assertThat(candidate.secure()).isFalse();
        assertThat(candidate.developmentPlaintext()).isTrue();
    }

    @Test
    void candidateRejectsAnInstanceOutsideTheDdcSnapshot() {
        DdcManagementServiceKey service = new DdcManagementServiceKey(
                "trade",
                "TEST",
                "orders",
                "orders-service-id",
                "HTTP_PROVIDER",
                "orders-service",
                "default",
                "1.0.0",
                "https"
        );
        DdcManagementServiceInstance snapshotInstance =
                new DdcManagementServiceInstance(
                        "instance-1",
                        "lease-1",
                        "provider.internal",
                        9443,
                        true,
                        java.util.Map.of(),
                        "ONLINE",
                        NOW.minusSeconds(30),
                        NOW,
                        NOW.plusSeconds(60)
                );
        DdcManagementServiceSnapshot snapshot =
                new DdcManagementServiceSnapshot(
                        service,
                        7,
                        NOW,
                        List.of(snapshotInstance)
                );
        GatewayOpenApiGroupManifestDTO manifest =
                new GatewayOpenApiGroupManifestDTO(
                        List.of("orders"),
                        GatewayOpenApiGroupManifestDTO.PATH_TEMPLATE,
                        "https://provider.example/resource",
                        "1.0.0",
                        "build-1"
                );
        DdcManagementServiceInstance unrelated =
                new DdcManagementServiceInstance(
                        "instance-2",
                        "lease-2",
                        "attacker.internal",
                        9443,
                        true,
                        java.util.Map.of(),
                        "ONLINE",
                        NOW.minusSeconds(30),
                        NOW,
                        NOW.plusSeconds(60)
                );

        assertThatThrownBy(() -> GatewayOpenApiSyncCandidateDTO.from(
                "app-1",
                snapshot,
                unrelated,
                manifest,
                "orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not part of the supplied snapshot");
    }

    @Test
    void dnsPolicyRejectsMixedAllowedAndDeniedAnswers() throws Exception {
        GatewayOpenApiDnsPolicy policy = new GatewayOpenApiDnsPolicy(
                List.of("10.0.0.0/8"),
                ignored -> List.of(
                        address("provider.internal", new byte[]{10, 0, 0, 1}),
                        loopbackAddress()
                )
        );

        assertThatThrownBy(() -> policy.resolveAndValidate(
                "provider.internal"
        )).isInstanceOf(GatewayOpenApiFetchException.class)
                .satisfies(failure -> assertThat(
                        ((GatewayOpenApiFetchException) failure).errorCode()
                ).isEqualTo("GATEWAY_OPENAPI_TARGET_FORBIDDEN"));
    }

    private GatewayProviderOpenApiClient client(
            GatewayOpenApiTokenSupplier tokenSupplier,
            GatewayOpenApiDnsPolicy dnsPolicy,
            HttpClient httpClient) {
        return client(tokenSupplier, dnsPolicy, httpClient, 5 * 1024 * 1024);
    }

    private GatewayProviderOpenApiClient client(
            GatewayOpenApiTokenSupplier tokenSupplier,
            GatewayOpenApiDnsPolicy dnsPolicy,
            HttpClient httpClient,
            int maximumDocumentBytes) {
        return new GatewayProviderOpenApiClient(
                tokenSupplier,
                dnsPolicy,
                httpClient,
                new ObjectMapper(),
                Clock.fixed(NOW, java.time.ZoneOffset.UTC),
                Duration.ofSeconds(10),
                maximumDocumentBytes
        );
    }

    private static HttpResponse<InputStream> response(
            int status,
            String contentType,
            String body) {
        @SuppressWarnings("unchecked")
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.headers()).thenReturn(
                java.net.http.HttpHeaders.of(
                        java.util.Map.of("Content-Type", List.of(contentType)),
                        (name, value) -> true
                )
        );
        when(response.body()).thenReturn(new ByteArrayInputStream(
                body.getBytes(StandardCharsets.UTF_8)
        ));
        return response;
    }

    private static InetAddress loopbackAddress() {
        return address("provider.internal", new byte[]{127, 0, 0, 1});
    }

    private static InetAddress address(String host, byte[] bytes) {
        try {
            return InetAddress.getByAddress(
                    host,
                    bytes
            );
        } catch (java.net.UnknownHostException failure) {
            throw new AssertionError(failure);
        }
    }
}
