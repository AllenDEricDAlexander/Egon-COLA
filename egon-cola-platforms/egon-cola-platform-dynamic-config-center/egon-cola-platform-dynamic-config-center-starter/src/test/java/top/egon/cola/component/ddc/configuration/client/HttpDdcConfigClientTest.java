package top.egon.cola.component.ddc.configuration.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.ddc.error.DdcException;
import top.egon.cola.component.ddc.autoconfigure.DdcProperties;
import top.egon.cola.component.ddc.configuration.model.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.configuration.model.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.transport.http.DdcCanonicalRequest;
import top.egon.cola.component.ddc.transport.http.DdcRequestSigner;
import top.egon.cola.component.ddc.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.lease.DdcLeaseSession;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpDdcConfigClientTest {

    @Test
    void rejectsMissingEndpointBeforeCreatingTransport() {
        DdcProperties properties = new DdcProperties();

        assertThatThrownBy(() -> new HttpDdcConfigClient(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("egon.cola.component.ddc.admin.endpoint is required");
    }

    @Test
    void signedPostHashesAndSignsTheExactTransmittedJsonBytes() {
        DdcProperties properties = new DdcProperties();
        properties.getAdmin().setEndpoint("http://ddc.test");
        properties.getAdmin().setSignatureEnabled(true);
        properties.getAdmin().setAccessKey("ak");
        properties.getAdmin().setSecretKey("sk");
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getAdmin().getEndpoint());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpDdcConfigClient client = new HttpDdcConfigClient(properties, builder);
        DdcRequestSigner signer = new DdcRequestSigner();
        server.expect(requestTo("http://ddc.test/api/v1/ddc/openapi/instances/register"))
                .andExpect(request -> {
                    MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
                    byte[] body = mockRequest.getBodyAsBytes();
                    long timestamp = Long.parseLong(
                            request.getHeaders().getFirst(DdcRequestSigner.TIMESTAMP_HEADER)
                    );
                    String nonce = request.getHeaders().getFirst(DdcRequestSigner.NONCE_HEADER);
                    DdcCanonicalRequest canonicalRequest = new DdcCanonicalRequest(
                            "POST",
                            request.getURI().getPath(),
                            Map.of(),
                            timestamp,
                            nonce,
                            body
                    );
                    assertThat(request.getHeaders().getFirst(DdcRequestSigner.ACCESS_KEY_HEADER))
                            .isEqualTo("ak");
                    assertThat(request.getHeaders().getFirst(DdcRequestSigner.CONTENT_SHA256_HEADER))
                            .isEqualTo(canonicalRequest.contentSha256());
                    assertThat(request.getHeaders().getFirst(DdcRequestSigner.SIGNATURE_HEADER))
                            .isEqualTo(signer.sign(canonicalRequest, "sk"));
                })
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 0,
                          "status": "SUCCESS",
                          "message": "success",
                          "data": {
                            "instanceId": "instance-1",
                            "leaseId": "lease-1",
                            "role": "CONFIG_CLIENT",
                            "leaseSeconds": 30,
                            "heartbeatIntervalSeconds": 10,
                            "registeredAt": "2026-07-24T12:00:00Z",
                            "leaseExpireAt": "2026-07-24T12:00:30Z"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        DdcInstanceRegisterRequest registerRequest = new DdcInstanceRegisterRequest();
        registerRequest.setInstanceId("instance-1");
        client.register(registerRequest);

        server.verify();
    }

    @Test
    void propagatesTraceparentAndRequestIdWithoutEgonTraceId() {
        DdcProperties properties = new DdcProperties();
        properties.getAdmin().setEndpoint("http://ddc.test");
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getAdmin().getEndpoint());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpDdcConfigClient client = new HttpDdcConfigClient(properties, builder);
        TraceContext parent = TraceContext.root("request-1");
        server.expect(requestTo("http://ddc.test/api/v1/ddc/openapi/instances/register"))
                .andExpect(request -> {
                    assertThat(request.getHeaders().getFirst(TraceContext.TRACEPARENT_HEADER))
                            .startsWith("00-" + parent.traceId() + "-");
                    assertThat(request.getHeaders().getFirst(TraceContext.REQUEST_ID_HEADER))
                            .isEqualTo("request-1");
                    assertThat(request.getHeaders().getFirst("x-egon-trace-id"))
                            .isNull();
                })
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 0,
                          "status": "SUCCESS",
                          "message": "success",
                          "data": {
                            "instanceId": "instance-1",
                            "leaseId": "lease-1",
                            "role": "CONFIG_CLIENT",
                            "leaseSeconds": 30,
                            "heartbeatIntervalSeconds": 10,
                            "registeredAt": "2026-07-24T12:00:00Z",
                            "leaseExpireAt": "2026-07-24T12:00:30Z"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        try (TraceContext.Scope ignored = parent.open()) {
            DdcInstanceRegisterRequest registerRequest = new DdcInstanceRegisterRequest();
            registerRequest.setInstanceId("instance-1");
            client.register(registerRequest);
        }

        server.verify();
    }

    @Test
    void signedGetUsesTheSameCanonicalEncodedQueryAsTheRequestUri() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail");
        properties.setAppCode("demo app");
        properties.setEnv("dev");
        properties.setNamespace("a/b");
        properties.getAdmin().setEndpoint("http://ddc.test");
        properties.getAdmin().setSignatureEnabled(true);
        properties.getAdmin().setAccessKey("ak");
        properties.getAdmin().setSecretKey("sk");
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getAdmin().getEndpoint());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpDdcConfigClient client = new HttpDdcConfigClient(properties, builder);
        DdcRequestSigner signer = new DdcRequestSigner();
        server.expect(requestTo(
                        "http://ddc.test/api/v1/ddc/openapi/configs/pull"
                                + "?appCode=demo%20app&bizCode=retail&env=dev"
                ))
                .andExpect(request -> {
                    long timestamp = Long.parseLong(
                            request.getHeaders().getFirst(DdcRequestSigner.TIMESTAMP_HEADER)
                    );
                    String nonce = request.getHeaders().getFirst(DdcRequestSigner.NONCE_HEADER);
                    DdcCanonicalRequest canonicalRequest = new DdcCanonicalRequest(
                            "GET",
                            request.getURI().getPath(),
                            Map.of(
                                    "appCode", List.of("demo app"),
                                    "bizCode", List.of("retail"),
                                    "env", List.of("dev")
                            ),
                            timestamp,
                            nonce,
                            new byte[0]
                    );
                    assertThat(request.getURI().getRawQuery())
                            .isEqualTo(canonicalRequest.canonicalQuery());
                    assertThat(request.getHeaders().getFirst(DdcRequestSigner.SIGNATURE_HEADER))
                            .isEqualTo(signer.sign(canonicalRequest, "sk"));
                })
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 0,
                          "status": "SUCCESS",
                          "message": "success",
                          "data": []
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.pull()).isEmpty();

        server.verify();
    }

    @Test
    void parsesLeaseRegisterHeartbeatAndOfflineEnvelopes() {
        ClientFixture fixture = fixture();
        fixture.server().expect(requestTo("http://ddc.test/api/v1/ddc/openapi/instances/register"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 0,
                          "status": "SUCCESS",
                          "message": "success",
                          "data": {
                            "instanceId": "instance-1",
                            "leaseId": "lease-1",
                            "role": "CONFIG_CLIENT",
                            "leaseSeconds": 30,
                            "heartbeatIntervalSeconds": 10,
                            "registeredAt": "2026-07-24T12:00:00Z",
                            "leaseExpireAt": "2026-07-24T12:00:30Z"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo("http://ddc.test/api/v1/ddc/openapi/instances/heartbeat"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 0,
                          "status": "SUCCESS",
                          "message": "success",
                          "data": {
                            "status": "RENEWED",
                            "leaseExpireAt": "2026-07-24T12:01:00Z"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo("http://ddc.test/api/v1/ddc/openapi/instances/offline"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 0,
                          "status": "SUCCESS",
                          "message": "success",
                          "data": {
                            "status": "DELETED",
                            "leaseExpireAt": null
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        DdcLeaseSession session = fixture.client().register(new DdcInstanceRegisterRequest());
        DdcLeaseOperationResult heartbeat = fixture.client().heartbeat(new DdcHeartbeatRequest());
        DdcLeaseOperationResult offline = fixture.client().offline(new DdcHeartbeatRequest());

        assertThat(session.instanceId()).isEqualTo("instance-1");
        assertThat(session.leaseId()).isEqualTo("lease-1");
        assertThat(session.role()).isEqualTo(DdcLeaseRole.CONFIG_CLIENT);
        assertThat(heartbeat.status()).isEqualTo(DdcLeaseOperationStatus.RENEWED);
        assertThat(heartbeat.renewed()).isTrue();
        assertThat(offline.status()).isEqualTo(DdcLeaseOperationStatus.DELETED);
        assertThat(offline.deleted()).isTrue();
        fixture.server().verify();
    }

    @Test
    void failureEnvelopeBecomesTypedDdcException() {
        ClientFixture fixture = fixture();
        fixture.server().expect(requestTo("http://ddc.test/api/v1/ddc/openapi/instances/register"))
                .andRespond(withSuccess("""
                        {
                          "success": false,
                          "code": 56002,
                          "status": "DDC_LEASE_MISMATCH",
                          "message": "lease mismatch",
                          "data": null
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.client().register(new DdcInstanceRegisterRequest()))
                .isInstanceOfSatisfying(DdcException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(56002);
                    assertThat(exception.getStatus()).isEqualTo("DDC_LEASE_MISMATCH");
                    assertThat(exception.getMessage()).isEqualTo("lease mismatch");
                });
        fixture.server().verify();
    }

    private ClientFixture fixture() {
        DdcProperties properties = new DdcProperties();
        properties.getAdmin().setEndpoint("http://ddc.test");
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getAdmin().getEndpoint());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new ClientFixture(new HttpDdcConfigClient(properties, builder), server);
    }

    private record ClientFixture(
            HttpDdcConfigClient client,
            MockRestServiceServer server
    ) {
    }
}
