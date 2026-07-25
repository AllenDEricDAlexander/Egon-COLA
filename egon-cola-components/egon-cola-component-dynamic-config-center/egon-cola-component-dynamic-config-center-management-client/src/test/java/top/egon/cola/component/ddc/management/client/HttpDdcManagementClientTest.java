package top.egon.cola.component.ddc.management.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;
import top.egon.cola.component.ddc.security.DdcCanonicalRequest;
import top.egon.cola.component.ddc.security.DdcRequestSigner;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpDdcManagementClientTest {

    private static final Instant NOW = Instant.parse("2026-07-25T02:00:00Z");

    @Test
    void signedWriteUsesEncodedScopeAndExactTransmittedBody() {
        ClientFixture fixture = fixture("access-key", "secret-value");
        fixture.server().expect(requestTo(
                        "http://ddc.test/api/v1/ddc/openapi/management/configs"
                                + "/gateway/dev/runtime/route%2Fa%20b"
                ))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(request -> {
                    MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
                    byte[] body = mockRequest.getBodyAsBytes();
                    DdcCanonicalRequest canonicalRequest = new DdcCanonicalRequest(
                            "PUT",
                            request.getURI().getRawPath(),
                            Map.of(),
                            NOW.toEpochMilli(),
                            "fixed-nonce",
                            body
                    );
                    assertThat(request.getHeaders().getFirst(DdcRequestSigner.ACCESS_KEY_HEADER))
                            .isEqualTo("access-key");
                    assertThat(request.getHeaders().getFirst(DdcRequestSigner.TIMESTAMP_HEADER))
                            .isEqualTo(Long.toString(NOW.toEpochMilli()));
                    assertThat(request.getHeaders().getFirst(DdcRequestSigner.NONCE_HEADER))
                            .isEqualTo("fixed-nonce");
                    assertThat(request.getHeaders().getFirst(DdcRequestSigner.CONTENT_SHA256_HEADER))
                            .isEqualTo(canonicalRequest.contentSha256());
                    assertThat(request.getHeaders().getFirst(DdcRequestSigner.SIGNATURE_HEADER))
                            .isEqualTo(new DdcRequestSigner().sign(canonicalRequest, "secret-value"));
                })
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 0,
                          "status": "SUCCESS",
                          "message": "success",
                          "data": {
                            "appCode": "gateway",
                            "env": "dev",
                            "namespace": "runtime",
                            "configKey": "route/a b",
                            "configValue": "{\\"enabled\\":true}",
                            "valueType": "JSON",
                            "version": 2,
                            "enabled": true,
                            "deleted": false,
                            "updatedAt": "2026-07-25T02:00:00Z"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        DdcManagementConfig response = fixture.client().upsert(
                new DdcManagementConfigUpsertRequest(
                        "gateway",
                        "dev",
                        "runtime",
                        "route/a b",
                        "{\"enabled\":true}",
                        "JSON",
                        "route",
                        1L,
                        "gateway-admin"
                )
        );

        assertThat(response.version()).isEqualTo(2L);
        assertThat(response.configKey()).isEqualTo("route/a b");
        fixture.server().verify();
    }

    @Test
    void signedQueryUsesTheSameSortedEncodedQueryAsTheRequestUri() {
        ClientFixture fixture = fixture("ak", "sk");
        String expectedUrl = "http://ddc.test/api/v1/ddc/openapi/management/registry/instances"
                + "?env=dev&namespace=rpc%2Finternal&protocol=grpc"
                + "&serviceKind=INTERNAL_GATEWAY&serviceName=order%20service";
        fixture.server().expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> {
                    DdcCanonicalRequest canonicalRequest = new DdcCanonicalRequest(
                            "GET",
                            request.getURI().getRawPath(),
                            Map.of(
                                    "env", List.of("dev"),
                                    "namespace", List.of("rpc/internal"),
                                    "protocol", List.of("grpc"),
                                    "serviceKind", List.of("INTERNAL_GATEWAY"),
                                    "serviceName", List.of("order service")
                            ),
                            NOW.toEpochMilli(),
                            "fixed-nonce",
                            new byte[0]
                    );
                    assertThat(request.getURI().getRawQuery())
                            .isEqualTo(canonicalRequest.canonicalQuery());
                    assertThat(request.getHeaders().getFirst(DdcRequestSigner.SIGNATURE_HEADER))
                            .isEqualTo(new DdcRequestSigner().sign(canonicalRequest, "sk"));
                })
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 0,
                          "status": "SUCCESS",
                          "message": "success",
                          "data": {
                            "serviceKey": {
                              "env": "dev",
                              "namespace": "rpc/internal",
                              "serviceKind": "INTERNAL_GATEWAY",
                              "serviceName": "order service",
                              "group": null,
                              "version": null,
                              "protocol": "grpc"
                            },
                            "generation": 7,
                            "observedAt": "2026-07-25T02:00:00Z",
                            "instances": []
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        DdcManagementServiceSnapshot snapshot = fixture.client().getInstances(
                new DdcManagementServiceQuery(
                        "dev",
                        "rpc/internal",
                        "INTERNAL_GATEWAY",
                        "grpc",
                        "order service",
                        null,
                        null
                )
        );

        assertThat(snapshot.generation()).isEqualTo(7L);
        assertThat(snapshot.instances()).isEmpty();
        fixture.server().verify();
    }

    @Test
    void failureEnvelopeBecomesTypedExceptionWithoutLeakingSecret() {
        ClientFixture fixture = fixture("ak", "do-not-leak");
        fixture.server().expect(requestTo(
                        "http://ddc.test/api/v1/ddc/openapi/management/publish-tasks/change-1"
                ))
                .andRespond(withSuccess("""
                        {
                          "success": false,
                          "code": 56009,
                          "status": "DDC_CONFLICT",
                          "message": "version conflict",
                          "data": null
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.client().getPublishTask("change-1"))
                .isInstanceOfSatisfying(DdcManagementClientException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(56009);
                    assertThat(exception.status()).isEqualTo("DDC_CONFLICT");
                    assertThat(exception.getMessage()).isEqualTo("version conflict");
                    assertThat(exception.toString()).doesNotContain("do-not-leak");
                });
        fixture.server().verify();
    }

    private ClientFixture fixture(String accessKey, String secretKey) {
        DdcManagementClientProperties properties = new DdcManagementClientProperties(
                "http://ddc.test",
                accessKey,
                secretKey,
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)
        );
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.endpoint());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpDdcManagementClient client = new HttpDdcManagementClient(
                properties,
                builder,
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> "fixed-nonce"
        );
        return new ClientFixture(client, server);
    }

    private record ClientFixture(
            HttpDdcManagementClient client,
            MockRestServiceServer server
    ) {
    }
}
