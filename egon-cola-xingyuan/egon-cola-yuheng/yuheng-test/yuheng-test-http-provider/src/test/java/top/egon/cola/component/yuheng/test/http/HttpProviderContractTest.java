package top.egon.cola.component.yuheng.test.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseRole;
import top.egon.cola.component.tianshu.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;
import top.egon.cola.component.tianshu.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.tianshu.model.registry.DdcServiceQuery;
import top.egon.cola.component.tianshu.model.registry.DdcServiceRegistration;
import top.egon.cola.component.tianshu.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseSession;
import top.egon.cola.component.tianshu.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.tianshu.service.registry.DdcServiceKeyFactory;
import top.egon.cola.component.tianshu.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.tianshu.http.registration
        .DdcHttpRegistrationContributor;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationRuntime;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceTokenRequest;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.HashSet;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = GatewayHttpTestProviderApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@Import(HttpProviderContractTest.ProviderTestConfiguration.class)
class HttpProviderContractTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DdcHttpRegistrationRuntime runtime;

    @Autowired
    private RecordingRegistry registry;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void consumesProviderAutoConfigurationAndOneVersionSource() {
        assertNotNull(runtime);
        assertFalse(context.containsBean("httpProviderRuntimeConfiguration"));
        assertEquals(1, registry.registrations.get());
        assertEquals(
                "1.0.0-live", registry.registration.serviceKey().version()
        );
        assertEquals(
                "gateway-test-http-provider",
                registry.registration.serviceKey().serviceName()
        );
        assertEquals("default", registry.registration.serviceKey().group());
        assertTrue(registry.registration.port() > 0);
        assertEquals(
                "test-definition-set",
                registry.registration.metadata().get(
                        "gateway.definition-set-id"
                )
        );
    }

    @Test
    void everyControllerDeclaresItsPublishedApiCatalog() {
        assertEquals("orders", OrderController.class.getAnnotation(
                EgonApiCatalog.class).interfaceGroupCode());
        assertEquals("inventory", InventoryController.class.getAnnotation(
                EgonApiCatalog.class).interfaceGroupCode());
        assertEquals("orders", BehaviorController.class.getAnnotation(
                EgonApiCatalog.class).interfaceGroupCode());
        assertEquals("orders", ProviderIdentityController.class
                .getAnnotation(EgonApiCatalog.class).interfaceGroupCode());
    }

    @Test
    void internalInventoryIsNotExternallyAccessible() throws Exception {
        EgonGatewayPolicy operation = InventoryController.class
                .getMethod("inventory", String.class)
                .getAnnotation(EgonGatewayPolicy.class);

        assertEquals(EgonGatewayPolicy.Exposure.INTERNAL,
                operation.exposure());
    }

    @Test
    void bodyEndpointEchoesBinaryPayload() {
        byte[] body = {0, 1, 2, 127};

        assertArrayEquals(body, new BehaviorController().echo(body));
    }

    @Test
    void sharedProviderEndpointIdentifiesMvcRuntime() {
        var response = new ProviderIdentityController("mvc-provider")
                .identity("request-1");

        assertEquals("request-1", response.requestId());
        assertEquals("mvc-provider", response.providerId());
        assertEquals("mvc", response.framework());
    }

    @Test
    void groupedOpenApiDocumentsAreScopedAndDisjoint() throws Exception {
        JsonNode orders = readDocument("orders");
        JsonNode inventory = readDocument("inventory");

        assertGolden(orders, "orders");
        assertGolden(inventory, "inventory");

        assertEquals("3.1.0", orders.path("openapi").asText());
        assertEquals("orders", orders.path("x-egon-service")
                .path("openapiGroup").asText());
        assertEquals("inventory", inventory.path("x-egon-service")
                .path("openapiGroup").asText());
        assertTrue(orders.path("paths").has("/api/orders/{id}"));
        assertTrue(orders.path("paths").has("/api/slow/{millis}"));
        assertTrue(inventory.path("paths")
                .has("/api/internal/inventory/{sku}"));
        assertFalse(orders.path("paths")
                .has("/api/internal/inventory/{sku}"));
        assertTrue(operationIds(orders).stream()
                .noneMatch(operationIds(inventory)::contains));
    }

    @Test
    void groupedDocumentsRequireTheGatewayOpenApiScope() throws Exception {
        mockMvc.perform(get("/v3/api-docs/orders"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs/orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                "SCOPE_gateway.other"))))
                .andExpect(status().isForbidden());
    }

    private JsonNode readDocument(String group) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get(
                        "/v3/api-docs/" + group)
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                "SCOPE_gateway.openapi.read"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsByteArray());
    }

    private void assertGolden(JsonNode document, String group)
            throws Exception {
        JsonNode golden;
        try (InputStream stream = getClass().getResourceAsStream(
                "/openapi/" + group + "-golden.json")) {
            assertNotNull(stream);
            golden = objectMapper.readTree(stream);
        }
        assertEquals(golden.path("group").asText(),
                document.path("x-egon-service").path("openapiGroup")
                        .asText());
        assertEquals(golden.path("openapi").asText(),
                document.path("openapi").asText());
        assertEquals(golden.path("x-egon-service"),
                document.path("x-egon-service"));
        assertEquals(values(golden.path("paths")), pathNames(document));
        assertEquals(values(golden.path("operationIds")),
                operationIds(document));
    }

    private Set<String> values(JsonNode nodes) {
        Set<String> values = new HashSet<>();
        nodes.elements().forEachRemaining(node -> values.add(node.asText()));
        return values;
    }

    private Set<String> pathNames(JsonNode document) {
        Set<String> paths = new HashSet<>();
        document.path("paths").fieldNames().forEachRemaining(paths::add);
        return paths;
    }

    private Set<String> operationIds(JsonNode document) {
        Set<String> result = new HashSet<>();
        Iterator<JsonNode> paths = document.path("paths").elements();
        while (paths.hasNext()) {
            Iterator<JsonNode> methods = paths.next().elements();
            while (methods.hasNext()) {
                JsonNode operation = methods.next();
                if (operation.has("operationId")) {
                    result.add(operation.path("operationId").asText());
                }
            }
        }
        return result;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            DdcProperties.class
    })
    static class ProviderTestConfiguration {

        @Bean
        RecordingRegistry recordingRegistry() {
            return new RecordingRegistry();
        }

        @Bean
        DdcServiceKeyFactory ddcServiceKeyFactory(DdcProperties properties) {
            return new DdcServiceKeyFactory(properties);
        }

        @Bean
        IdpServiceOAuth2Client idpServiceOAuth2Client() {
            IdpServiceOAuth2Client client = mock(IdpServiceOAuth2Client.class);
            Instant issuedAt = Instant.now();
            when(client.authorize(any(IdpServiceTokenRequest.class)))
                    .thenReturn(new OAuth2AccessToken(
                            OAuth2AccessToken.TokenType.BEARER,
                            "test-ddc-token",
                            issuedAt,
                            issuedAt.plusSeconds(300)
                    ));
            return client;
        }

        @Bean
        DdcHttpRegistrationContributor httpRegistrationContributor() {
            return new DdcHttpRegistrationContributor() {
                @Override
                public String serviceVersion() {
                    return "1.0.0-live";
                }

                @Override
                public Map<String, String> metadata() {
                    return Map.of(
                            "gateway.definition-set-id",
                            "test-definition-set"
                    );
                }
            };
        }
    }

    static final class RecordingRegistry
            implements DdcServiceRegistryClient {

        private final AtomicInteger registrations = new AtomicInteger();

        private volatile DdcServiceRegistration registration;

        @Override
        public DdcLeaseSession register(DdcServiceRegistration registration) {
            this.registration = registration;
            int sequence = registrations.incrementAndGet();
            Instant now = Instant.now();
            return new DdcLeaseSession(
                    registration.instanceId(),
                    "lease-" + sequence,
                    DdcLeaseRole.HTTP_PROVIDER,
                    registration.leaseSeconds(),
                    registration.heartbeatIntervalSeconds(),
                    now,
                    now.plusSeconds(registration.leaseSeconds())
            );
        }

        @Override
        public DdcLeaseOperationResult heartbeat(
                DdcServiceLeaseRequest request) {
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.RENEWED,
                    Instant.now().plusSeconds(30)
            );
        }

        @Override
        public DdcLeaseOperationResult deregister(
                String instanceId,
                String leaseId) {
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.DELETED,
                    Instant.now()
            );
        }

        @Override
        public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcRegistrySubscription subscribe(
                DdcServiceKey serviceKey,
                Consumer<DdcServiceSnapshot> listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcServiceCatalogSnapshot getServiceKeys(
                DdcServiceQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcRegistrySubscription subscribeServices(
                DdcServiceQuery query,
                Consumer<DdcServiceCatalogSnapshot> listener) {
            throw new UnsupportedOperationException();
        }
    }
}
