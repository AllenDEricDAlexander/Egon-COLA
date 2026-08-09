package top.egon.cola.component.ddc.admin.security.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.security.rpc.InMemoryDdcNonceStore;
import top.egon.cola.component.ddc.client.http.DdcCanonicalRequest;
import top.egon.cola.component.ddc.client.http.DdcRequestSigner;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DdcOpenApiHmacFilterTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    private static final String PATH =
            "/api/v1/ddc/openapi/management/configs/infra/dev/gateway";

    private static final byte[] BODY = "{\"instanceId\":\"i1\"}".getBytes(StandardCharsets.UTF_8);

    private final DdcRequestSigner signer = new DdcRequestSigner();

    private DdcAdminProperties properties;

    private DdcOpenApiHmacFilter filter;

    @BeforeEach
    void setUp() {
        properties = new DdcAdminProperties();
        properties.getRpc().setSignatureEnabled(true);
        properties.getRpc().setCredentials(List.of(credential()));
        properties.getRpc().setAllowedClockSkewSeconds(300);
        properties.getRpc().setNonceCacheMaxSize(10);
        filter = filter();
    }

    @Test
    void validRequestReachesControllerWithIdenticalBodyBytes() throws Exception {
        MockHttpServletRequest request = signedRequest(NOW.toEpochMilli(), "nonce-1", BODY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<byte[]> controllerBody = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(jakarta.servlet.http.HttpServletRequest servletRequest,
                                   jakarta.servlet.http.HttpServletResponse servletResponse)
                    throws java.io.IOException {
                controllerBody.set(servletRequest.getInputStream().readAllBytes());
                servletResponse.setStatus(204);
            }
        });

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(controllerBody.get()).containsExactly(BODY);
    }

    @Test
    void everyManagementApiShapeUsesTheSameSignatureContract() throws Exception {
        List<RequestShape> requests = List.of(
                new RequestShape("PUT", PATH, Map.of(), BODY),
                new RequestShape("DELETE", PATH, Map.of(), BODY),
                new RequestShape("POST", PATH + "/publish", Map.of(), BODY),
                new RequestShape("GET",
                        "/api/v1/ddc/openapi/management/publish-tasks/change-1",
                        Map.of(),
                        new byte[0]),
                new RequestShape("POST",
                        "/api/v1/ddc/openapi/management/publish-tasks/change-1/retry",
                        Map.of(),
                        new byte[0]),
                new RequestShape("GET",
                        "/api/v1/ddc/openapi/management/instances",
                        Map.of("appCode", List.of("gateway"),
                                "env", List.of("dev"),
                                "bizCode", List.of("infra")),
                        new byte[0]),
                new RequestShape("GET",
                        "/api/v1/ddc/openapi/management/scope-bindings",
                        Map.of(),
                        new byte[0]),
                new RequestShape("GET",
                        "/api/v1/ddc/openapi/management/registry/services",
                        Map.of("bizCode", List.of("infra"),
                                "env", List.of("dev"),
                                "appCode", List.of("gateway"),
                                "serviceKind", List.of("PROVIDER"),
                                "protocol", List.of("HTTP")),
                        new byte[0]),
                new RequestShape("GET",
                        "/api/v1/ddc/openapi/management/registry/instances",
                        Map.of("bizCode", List.of("infra"),
                                "env", List.of("dev"),
                                "appCode", List.of("gateway"),
                                "serviceKind", List.of("PROVIDER"),
                                "protocol", List.of("HTTP"),
                                "serviceName", List.of("orders")),
                        new byte[0])
        );

        for (int index = 0; index < requests.size(); index++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(
                    signedRequest(
                            requests.get(index),
                            NOW.toEpochMilli(),
                            "management-api-" + index
                    ),
                    response,
                    chain
            );

            assertThat(chain.getRequest())
                    .as("management request %s", requests.get(index).path())
                    .isNotNull();
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void rejectsWrongSignature() throws Exception {
        MockHttpServletRequest request = signedRequest(NOW.toEpochMilli(), "nonce-1", BODY);
        request.removeHeader(DdcRequestSigner.SIGNATURE_HEADER);
        request.addHeader(DdcRequestSigner.SIGNATURE_HEADER, "0".repeat(64));

        assertRejected(request, "DDC_SIGNATURE_INVALID");
    }

    @Test
    void rejectsWrongContentHashAndBodyTampering() throws Exception {
        MockHttpServletRequest wrongHash = signedRequest(NOW.toEpochMilli(), "nonce-1", BODY);
        wrongHash.removeHeader(DdcRequestSigner.CONTENT_SHA256_HEADER);
        wrongHash.addHeader(DdcRequestSigner.CONTENT_SHA256_HEADER, "0".repeat(64));
        assertRejected(wrongHash, "DDC_SIGNATURE_INVALID");

        MockHttpServletRequest tampered = signedRequest(NOW.toEpochMilli(), "nonce-2", BODY);
        tampered.setContent("{\"instanceId\":\"i2\"}".getBytes(StandardCharsets.UTF_8));
        assertRejected(tampered, "DDC_SIGNATURE_INVALID");
    }

    @Test
    void rejectsPastAndFutureTimestampsOutsideTheWindow() throws Exception {
        assertRejected(
                signedRequest(NOW.minusSeconds(301).toEpochMilli(), "nonce-past", BODY),
                "DDC_SIGNATURE_EXPIRED"
        );
        assertRejected(
                signedRequest(NOW.plusSeconds(301).toEpochMilli(), "nonce-future", BODY),
                "DDC_SIGNATURE_EXPIRED"
        );
    }

    @Test
    void rejectsMissingHeadersAndUnknownAccessKey() throws Exception {
        MockHttpServletRequest missing = request(BODY);
        assertRejected(missing, "DDC_SIGNATURE_REQUIRED");

        MockHttpServletRequest unknown = signedRequest(NOW.toEpochMilli(), "nonce-1", BODY);
        unknown.removeHeader(DdcRequestSigner.ACCESS_KEY_HEADER);
        unknown.addHeader(DdcRequestSigner.ACCESS_KEY_HEADER, "unknown");
        assertRejected(unknown, "DDC_SIGNATURE_INVALID");
    }

    @Test
    void rejectsDuplicateNonceAfterFirstValidRequest() throws Exception {
        MockHttpServletRequest first = signedRequest(NOW.toEpochMilli(), "same-nonce", BODY);
        filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

        assertRejected(
                signedRequest(NOW.toEpochMilli(), "same-nonce", BODY),
                "DDC_SIGNATURE_REPLAY"
        );
    }

    @Test
    void rejectsSignedWriteWhenNonceStoreIsUnavailable() throws Exception {
        filter = new DdcOpenApiHmacFilter(
                properties,
                new ObjectMapper(),
                (credentialId, nonce, ttl) -> {
                    throw new IllegalStateException("redis unavailable");
                },
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(
                signedRequest(NOW.toEpochMilli(), "nonce-outage", BODY),
                response,
                chain
        );

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString())
                .contains("DDC_NONCE_STORE_UNAVAILABLE");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void disabledVerificationBypassesHeadersAndBodyWrapping() throws Exception {
        properties.getRpc().setSignatureEnabled(false);
        filter = filter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request(BODY), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private DdcOpenApiHmacFilter filter() {
        return new DdcOpenApiHmacFilter(
                properties,
                new ObjectMapper(),
                new InMemoryDdcNonceStore(
                        properties.getRpc().getNonceCacheMaxSize()
                ),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private DdcAdminProperties.Credential credential() {
        DdcAdminProperties.Credential credential =
                new DdcAdminProperties.Credential();
        credential.setCredentialId("legacy-http-test");
        credential.setAccessKey("access");
        credential.setSecret("secret");
        credential.setClientType("*");
        credential.setAppCodePatterns(List.of("*"));
        credential.setEnvPatterns(List.of("*"));
        credential.setBizCodePatterns(List.of("*"));
        credential.setAllowedOperations(List.of("*"));
        return credential;
    }

    private MockHttpServletRequest signedRequest(long timestamp, String nonce, byte[] body) {
        return signedRequest(
                new RequestShape("PUT", PATH, Map.of(), body),
                timestamp,
                nonce
        );
    }

    private MockHttpServletRequest signedRequest(
            RequestShape shape,
            long timestamp,
            String nonce
    ) {
        MockHttpServletRequest request =
                request(shape.method(), shape.path(), shape.query(), shape.body());
        DdcCanonicalRequest canonicalRequest =
                new DdcCanonicalRequest(
                        shape.method(),
                        shape.path(),
                        shape.query(),
                        timestamp,
                        nonce,
                        shape.body()
                );
        request.addHeader(DdcRequestSigner.ACCESS_KEY_HEADER, "access");
        request.addHeader(DdcRequestSigner.TIMESTAMP_HEADER, Long.toString(timestamp));
        request.addHeader(DdcRequestSigner.NONCE_HEADER, nonce);
        request.addHeader(DdcRequestSigner.CONTENT_SHA256_HEADER, canonicalRequest.contentSha256());
        request.addHeader(DdcRequestSigner.SIGNATURE_HEADER, signer.sign(canonicalRequest, "secret"));
        return request;
    }

    private MockHttpServletRequest request(byte[] body) {
        return request("POST", PATH, Map.of(), body);
    }

    private MockHttpServletRequest request(
            String method,
            String path,
            Map<String, List<String>> query,
            byte[] body
    ) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        query.forEach((name, values) ->
                request.addParameter(name, values.toArray(String[]::new)));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(body);
        return request;
    }

    private void assertRejected(MockHttpServletRequest request, String status) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"status\":\"" + status + "\"");
        assertThat(chain.getRequest()).isNull();
    }

    private record RequestShape(
            String method,
            String path,
            Map<String, List<String>> query,
            byte[] body
    ) {
    }
}
