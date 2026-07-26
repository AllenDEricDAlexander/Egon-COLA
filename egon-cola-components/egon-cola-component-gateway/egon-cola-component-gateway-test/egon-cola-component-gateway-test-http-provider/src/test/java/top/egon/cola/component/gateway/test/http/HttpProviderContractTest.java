package top.egon.cola.component.gateway.test.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
