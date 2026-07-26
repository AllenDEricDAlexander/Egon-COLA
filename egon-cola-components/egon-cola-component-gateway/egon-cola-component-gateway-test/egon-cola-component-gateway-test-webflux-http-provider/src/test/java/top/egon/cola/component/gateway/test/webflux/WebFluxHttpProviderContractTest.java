package top.egon.cola.component.gateway.test.webflux;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.result.method.annotation
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
import top.egon.cola.component.gateway.starter.discovery
        .WebFluxGatewayDefinitionContributor;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = GatewayWebFluxHttpTestProviderApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(WebFluxHttpProviderContractTest.ProviderTestConfiguration.class)
class WebFluxHttpProviderContractTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Autowired
    private GatewayReportingProperties reportingProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpProviderLeaseRuntime runtime;

    @Autowired
    private RecordingRegistry registry;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void reportsAnnotatedMonoEndpointAndAutoRegisters() {
        assertNotNull(runtime);
        assertEquals(1, registry.registrations.get());
        assertEquals(
                "gateway-test-http-provider",
                registry.registration.serviceKey().serviceName()
        );
        assertEquals(
                reportingProperties.getArtifactVersion(),
                registry.registration.serviceKey().version()
        );
        assertEquals(
                "webflux-http-provider-default",
                registry.registration.instanceId()
        );
        assertEquals(
                "zone-b",
                registry.registration.metadata().get("gateway.zone")
        );
        assertTrue(registry.registration.port() > 0);

        GatewayInterfaceDefinitionReport.Operation operation =
                new WebFluxGatewayDefinitionContributor(
                        mappings,
                        reportingProperties,
                        objectMapper
                ).discover().stream()
                        .flatMap(group -> group.interfaceGroup()
                                .operations().stream())
                        .filter(candidate -> "/test/items/{id}".equals(
                                candidate.attributes().get("path")
                        ))
                        .findFirst()
                        .orElseThrow();

        assertEquals("GET", operation.attributes().get("httpMethod"));
        assertEquals(
                "TRANSPARENT",
                operation.attributes().get("responseMode")
        );
        assertFalse((Boolean) operation.attributes().get("streaming"));
        assertEquals("SUPPORTED", operation.gatewaySupport());
        assertEquals(
                ReactiveInventoryController.InventoryResponse.class
                        .getName(),
                operation.responseSchema().get("javaType")
        );

        webTestClient.get()
                .uri("/test/items/item-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("item-1")
                .jsonPath("$.providerId")
                .isEqualTo("webflux-http-provider-default")
                .jsonPath("$.framework").isEqualTo("webflux");
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
                    "test-webflux-definition-set",
                    properties.getArtifactVersion(),
                    "test-webflux-build"
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
