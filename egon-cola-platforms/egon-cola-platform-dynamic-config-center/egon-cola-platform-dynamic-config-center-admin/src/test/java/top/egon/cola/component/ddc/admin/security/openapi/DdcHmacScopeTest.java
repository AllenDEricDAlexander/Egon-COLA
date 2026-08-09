package top.egon.cola.component.ddc.admin.security.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.controller.DdcManagementOpenApiController;
import top.egon.cola.component.ddc.admin.service.management.DdcManagementFacade;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.client.http.DdcCanonicalRequest;
import top.egon.cola.component.ddc.client.http.DdcRequestSigner;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DdcHmacScopeTest {

    private static final Instant NOW = Instant.parse("2026-07-26T08:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DdcRequestSigner signer = new DdcRequestSigner();

    private DdcOpenApiHmacFilter filter;

    @BeforeEach
    void setUp() {
        DdcAdminProperties properties = new DdcAdminProperties();
        properties.getOpenapi().setSignatureEnabled(true);
        properties.getOpenapi().setCredentials(List.of(
                credential(
                        "sdk-a",
                        "sdk-access",
                        "sdk-secret",
                        "SDK",
                        List.of("app-a"),
                        List.of("dev"),
                        List.of("biz-*"),
                        List.of(
                                "CONFIG_PULL",
                                "PUBLISH_ACK"
                        )
                ),
                credential(
                        "registry-a",
                        "registry-access",
                        "registry-secret",
                        "REGISTRY",
                        List.of("*"),
                        List.of("dev"),
                        List.of("biz-a"),
                        List.of(
                                "REGISTRY_REGISTER",
                                "REGISTRY_HEARTBEAT"
                        )
                ),
                credential(
                        "sdk-readonly",
                        "sdk-readonly-access",
                        "sdk-readonly-secret",
                        "SDK",
                        List.of("app-a"),
                        List.of("dev"),
                        List.of("biz-a"),
                        List.of("CONFIG_PULL")
                ),
                credential(
                        "management-a",
                        "management-access",
                        "management-secret",
                        "MANAGEMENT",
                        List.of("app-a"),
                        List.of("dev"),
                        List.of("biz-a"),
                        List.of(
                                "MANAGEMENT_CONFIG_READ",
                                "MANAGEMENT_CONFIG_WRITE",
                                "MANAGEMENT_PUBLISH"
                        )
                )
        ));
        filter = new DdcOpenApiHmacFilter(
                properties,
                objectMapper,
                new InMemoryDdcNonceStore(100),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void permitsSdkPullAndAckInsideCredentialScope() throws Exception {
        assertAllowed(signed(
                "GET",
                "/api/v1/ddc/openapi/configs/pull",
                Map.of(
                        "appCode", List.of("app-a"),
                        "env", List.of("dev"),
                        "bizCode", List.of("biz-a")
                ),
                new byte[0],
                "sdk-access",
                "sdk-secret",
                "sdk-pull"
        ), "sdk-a");
        assertAllowed(signed(
                "POST",
                "/api/v1/ddc/openapi/publish/ack",
                Map.of(),
                json("""
                        {"appCode":"app-a","env":"dev",
                         "bizCode":"biz-b"}
                        """),
                "sdk-access",
                "sdk-secret",
                "sdk-ack"
        ), "sdk-a");
    }

    @Test
    void permitsRegistryRegisterAndHeartbeatInsideCredentialScope()
            throws Exception {
        byte[] body = json("""
                {"serviceKey":{"bizCode":"biz-a","env":"dev",
                 "appCode":"registry-app"}}
                """);
        assertAllowed(signed(
                "POST",
                "/api/v1/ddc/openapi/registry/instances/register",
                Map.of(),
                body,
                "registry-access",
                "registry-secret",
                "registry-register"
        ), "registry-a");
        assertAllowed(signed(
                "POST",
                "/api/v1/ddc/openapi/registry/instances/heartbeat",
                Map.of(),
                body,
                "registry-access",
                "registry-secret",
                "registry-heartbeat"
        ), "registry-a");
    }

    @Test
    void permitsExactManagementReadUpsertAndPublish() throws Exception {
        String configPath = "/api/v1/ddc/openapi/management/configs/"
                + "biz-a/dev/app-a";
        assertAllowed(signed(
                "GET",
                configPath,
                Map.of(),
                new byte[0],
                "management-access",
                "management-secret",
                "management-read"
        ), "management-a");
        assertAllowed(signed(
                "PUT",
                configPath,
                Map.of(),
                json("{}"),
                "management-access",
                "management-secret",
                "management-write"
        ), "management-a");
        assertAllowed(signed(
                "POST",
                configPath + "/publish",
                Map.of(),
                json("{}"),
                "management-access",
                "management-secret",
                "management-publish"
        ), "management-a");
    }

    @Test
    void rejectsCrossScopeWrongClientTypeAndDisallowedOperation()
            throws Exception {
        assertDenied(signed(
                "GET",
                "/api/v1/ddc/openapi/configs/pull",
                Map.of(
                        "appCode", List.of("app-b"),
                        "env", List.of("dev"),
                        "bizCode", List.of("biz-a")
                ),
                new byte[0],
                "sdk-access",
                "sdk-secret",
                "cross-app"
        ));
        assertDenied(signed(
                "GET",
                "/api/v1/ddc/openapi/configs/pull",
                Map.of(
                        "appCode", List.of("app-a"),
                        "env", List.of("dev"),
                        "bizCode", List.of("biz-a")
                ),
                new byte[0],
                "management-access",
                "management-secret",
                "wrong-client"
        ));
        assertDenied(signed(
                "POST",
                "/api/v1/ddc/openapi/publish/ack",
                Map.of(),
                json("""
                        {"appCode":"app-a","env":"dev",
                         "bizCode":"biz-a"}
                        """),
                "sdk-readonly-access",
                "sdk-readonly-secret",
                "wrong-operation"
        ));
    }

    @Test
    void rejectsSignedRequestWhenRequiredScopeIsMissing()
            throws Exception {
        assertDenied(signed(
                "GET",
                "/api/v1/ddc/openapi/configs/pull",
                Map.of(
                        "appCode", List.of("app-a"),
                        "env", List.of("dev")
                ),
                new byte[0],
                "sdk-access",
                "sdk-secret",
                "missing-biz-code"
        ));
    }

    @Test
    void managementWriteUsesTrustedServiceOperator() {
        DdcManagementFacade facade = mock(DdcManagementFacade.class);
        DdcManagementOpenApiController controller =
                new DdcManagementOpenApiController(facade);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(
                DdcServicePrincipal.REQUEST_ATTRIBUTE,
                new DdcServicePrincipal(
                        "management-a",
                        "MANAGEMENT",
                        Set.of("app-a"),
                        Set.of("dev"),
                        Set.of("biz-a"),
                        Set.of("MANAGEMENT_CONFIG_WRITE"),
                        "app-a",
                        "dev",
                        "biz-a"
                )
        );

        controller.upsert(
                "biz-a",
                "dev",
                "app-a",
                new DdcManagementConfigUpsertRequest(
                        null,
                        null,
                        null,
                        "application.yml",
                        "feature:\n  enabled: true\n",
                        "YAML",
                        "routes",
                        1L,
                        "claimed-user"
                ),
                servletRequest
        );

        ArgumentCaptor<DdcManagementConfigUpsertRequest> command =
                ArgumentCaptor.forClass(
                        DdcManagementConfigUpsertRequest.class
                );
        verify(facade).upsert(command.capture());
        assertThat(command.getValue().operator())
                .isEqualTo(
                        "service:management-a [requested=claimed-user]"
                );
    }

    private DdcAdminProperties.Credential credential(
            String credentialId,
            String accessKey,
            String secret,
            String clientType,
            List<String> appCodes,
            List<String> envs,
            List<String> bizCodes,
            List<String> operations) {
        DdcAdminProperties.Credential credential =
                new DdcAdminProperties.Credential();
        credential.setCredentialId(credentialId);
        credential.setAccessKey(accessKey);
        credential.setSecret(secret);
        credential.setClientType(clientType);
        credential.setAppCodePatterns(appCodes);
        credential.setEnvPatterns(envs);
        credential.setBizCodePatterns(bizCodes);
        credential.setAllowedOperations(operations);
        return credential;
    }

    private MockHttpServletRequest signed(
            String method,
            String path,
            Map<String, List<String>> query,
            byte[] body,
            String accessKey,
            String secret,
            String nonce) {
        MockHttpServletRequest request = new MockHttpServletRequest(
                method,
                path
        );
        request.setRequestURI(path);
        query.forEach((name, values) -> request.addParameter(
                name,
                values.toArray(String[]::new)
        ));
        request.setContent(body);
        DdcCanonicalRequest canonical = new DdcCanonicalRequest(
                method,
                path,
                query,
                NOW.toEpochMilli(),
                nonce,
                body
        );
        request.addHeader(DdcRequestSigner.ACCESS_KEY_HEADER, accessKey);
        request.addHeader(
                DdcRequestSigner.TIMESTAMP_HEADER,
                Long.toString(NOW.toEpochMilli())
        );
        request.addHeader(DdcRequestSigner.NONCE_HEADER, nonce);
        request.addHeader(
                DdcRequestSigner.CONTENT_SHA256_HEADER,
                canonical.contentSha256()
        );
        request.addHeader(
                DdcRequestSigner.SIGNATURE_HEADER,
                signer.sign(canonical, secret)
        );
        return request;
    }

    private void assertAllowed(
            MockHttpServletRequest request,
            String credentialId) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(chain.getRequest().getAttribute(
                DdcServicePrincipal.REQUEST_ATTRIBUTE
        )).isInstanceOfSatisfying(
                DdcServicePrincipal.class,
                principal -> assertThat(principal.credentialId())
                        .isEqualTo(credentialId)
        );
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
    }

    private void assertDenied(MockHttpServletRequest request)
            throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
                .contains("DDC_HMAC_SCOPE_DENIED");
        assertThat(chain.getRequest()).isNull();
    }

    private byte[] json(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
