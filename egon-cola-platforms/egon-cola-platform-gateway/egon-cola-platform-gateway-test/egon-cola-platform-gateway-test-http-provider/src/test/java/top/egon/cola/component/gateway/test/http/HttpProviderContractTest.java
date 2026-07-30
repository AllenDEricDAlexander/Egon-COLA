package top.egon.cola.component.gateway.test.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.mvc.method.annotation
        .RequestMappingHandlerMapping;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.gateway.contract.definition
        .GatewayDefinitionIdentity;
import top.egon.cola.component.gateway.contract.reporting
        .GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.discovery
        .GatewayDefinitionContributor;
import top.egon.cola.component.gateway.starter.discovery
        .MvcGatewayDefinitionContributor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = GatewayHttpTestProviderApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(HttpProviderContractTest.ProviderTestConfiguration.class)
class HttpProviderContractTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private HttpProviderLeaseRuntime runtime;

    @Autowired
    private RecordingRegistry registry;

    @Autowired
    private GatewayReportingProperties reportingProperties;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMappings;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void consumesProviderAutoConfigurationAndOneVersionSource() {
        assertNotNull(runtime);
        assertFalse(context.containsBean("httpProviderRuntimeConfiguration"));
        assertEquals(1, registry.registrations.get());
        assertEquals(
                reportingProperties.getArtifactVersion(),
                registry.registration.serviceKey().version()
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
    void everyControllerDefinesItsOwnInterfaceGroup() {
        assertNotNull(OrderController.class.getAnnotation(
                GatewayInterfaceGroup.class
        ));
        assertNotNull(InventoryController.class.getAnnotation(
                GatewayInterfaceGroup.class
        ));
        assertNotNull(BehaviorController.class.getAnnotation(
                GatewayInterfaceGroup.class
        ));
        assertNotNull(ProviderIdentityController.class.getAnnotation(
                GatewayInterfaceGroup.class
        ));
    }

    @Test
    void internalInventoryIsNotExternallyAccessible() throws Exception {
        GatewayOperation operation = InventoryController.class
                .getMethod("inventory", String.class)
                .getAnnotation(GatewayOperation.class);

        assertFalse(operation.externalAccessible());
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
    void orderSchemasExposeEveryFieldTypeAndDescription() {
        GatewayReportingProperties properties = new GatewayReportingProperties();
        properties.setApplicationCode("gateway-test-http-provider");
        properties.setEnv("test");
        properties.setNamespace("gateway-test");
        properties.setArtifactVersion("1.0.0-live");
        List<GatewayDefinitionContributor.DiscoveredInterfaceGroup> groups =
                new MvcGatewayDefinitionContributor(
                        handlerMappings,
                        properties,
                        objectMapper
                ).discover();
        Map<String, GatewayInterfaceDefinitionReport.Operation> operations =
                groups.stream()
                        .filter(group -> OrderController.class.getName().equals(
                                group.interfaceGroup().className()
                        ))
                        .flatMap(group -> group.interfaceGroup()
                                .operations().stream())
                        .collect(Collectors.toMap(
                                GatewayInterfaceDefinitionReport.Operation
                                        ::methodIdentity,
                                operation -> operation
                        ));

        Map<String, SchemaExpectation> expected = Map.of(
                "GET /api/orders/{id}", new SchemaExpectation(
                        Set.of("id", "X-Request-Source"),
                        Set.of("id", "status", "source")
                ),
                "POST /api/orders", new SchemaExpectation(
                        Set.of("customerId", "channel"),
                        Set.of("id", "status", "source")
                ),
                "GET /api/orders/search", new SchemaExpectation(
                        Set.of("customerId", "limit"),
                        Set.of("customerId", "limit", "count")
                ),
                "POST /api/orders/{id}/cancel", new SchemaExpectation(
                        Set.of("id", "Idempotency-Key"),
                        Set.of("id", "status", "source")
                )
        );

        assertEquals(expected.keySet(), operations.keySet());
        expected.forEach((method, expectation) -> {
            GatewayInterfaceDefinitionReport.Operation operation =
                    operations.get(method);
            assertSchemaFields(
                    method + " request",
                    operation.requestSchema(),
                    expectation.requestFields()
            );
            assertSchemaFields(
                    method + " response",
                    operation.responseSchema(),
                    expectation.responseFields()
            );
        });
    }

    private void assertSchemaFields(
            String schemaName,
            Map<String, Object> schema,
            Set<String> expectedNames) {
        Object value = schema.get("properties");
        assertTrue(value instanceof Map<?, ?>, schemaName);
        Map<?, ?> fields = (Map<?, ?>) value;
        assertEquals(expectedNames, fields.keySet(), schemaName);
        fields.forEach((name, field) -> {
            assertTrue(field instanceof Map<?, ?>, schemaName + "." + name);
            Map<?, ?> details = (Map<?, ?>) field;
            assertTrue(details.get("type") instanceof String, schemaName
                    + "." + name + " type");
            assertTrue(details.get("description") instanceof String description
                            && !description.isBlank(),
                    schemaName + "." + name + " description");
        });
    }

    private record SchemaExpectation(
            Set<String> requestFields,
            Set<String> responseFields) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GatewayReportingProperties.class)
    static class ProviderTestConfiguration {

        @Bean
        RecordingRegistry recordingRegistry() {
            return new RecordingRegistry();
        }

        @Bean
        GatewayDefinitionIdentity gatewayProviderDefinitionIdentity(
                GatewayReportingProperties properties) {
            return new GatewayDefinitionIdentity(
                    "test-definition-set",
                    properties.getArtifactVersion(),
                    "test-build"
            );
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
                String instanceId,
                String leaseId) {
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
