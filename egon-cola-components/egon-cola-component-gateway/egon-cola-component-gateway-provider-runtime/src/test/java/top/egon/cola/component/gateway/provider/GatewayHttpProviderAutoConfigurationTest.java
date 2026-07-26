package top.egon.cola.component.gateway.provider;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayHttpProviderAutoConfigurationTest {

    private final GatewayDefinitionIdentity identity =
            new GatewayDefinitionIdentity(
                    "definition-set-a",
                    "1.0.0",
                    "build-a"
            );

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            GatewayHttpProviderAutoConfiguration.class
                    ))
                    .withBean(
                            GatewayDefinitionIdentity.class,
                            () -> identity
                    )
                    .withPropertyValues(requiredProperties());

    @Test
    void registersActualPortExposesHealthAndDeregistersOnClose() {
        FakeRegistry registry = new FakeRegistry();

        contextRunner.withBean(
                DdcServiceRegistryClient.class,
                () -> registry
        ).run(context -> {
            assertThat(context).hasSingleBean(
                    HttpProviderLeaseRuntime.class
            );

            context.publishEvent(webServerEvent(18101, null));

            assertThat(registry.registration.port()).isEqualTo(18101);
            assertThat(registry.registration.serviceKey().env())
                    .isEqualTo("test");
            assertThat(registry.registration.serviceKey().namespace())
                    .isEqualTo("gateway-test");
            assertThat(registry.registration.serviceKey().serviceName())
                    .isEqualTo("orders");
            assertThat(registry.registration.serviceKey().version())
                    .isEqualTo(identity.artifactVersion());

            GatewayHttpProviderHealthIndicator healthIndicator =
                    context.getBean(
                            GatewayHttpProviderHealthIndicator.class
                    );
            assertThat(healthIndicator.health().getStatus())
                    .isEqualTo(Status.UP);
            assertThat(healthIndicator.health().getDetails())
                    .containsEntry("state", "REGISTERED")
                    .containsEntry("instanceId", "orders-1")
                    .containsEntry("leaseId", "lease-1")
                    .containsKey("leaseExpireAt");
        });

        assertThat(registry.deregistrations).hasValue(1);
    }

    @Test
    void backsOffWhenDisabledOrRegistryIsMissing() {
        contextRunner.withPropertyValues(
                "egon.cola.component.gateway.provider.http.enabled=false"
        ).run(context -> assertThat(context).doesNotHaveBean(
                HttpProviderLeaseRuntime.class
        ));

        contextRunner.run(context -> assertThat(context).doesNotHaveBean(
                HttpProviderLeaseRuntime.class
        ));
    }

    @Test
    void propagatesInitialRegistrationFailureWhenFailFast() {
        FakeRegistry registry = new FakeRegistry();
        registry.registrationFailures.set(1);

        contextRunner.withBean(
                DdcServiceRegistryClient.class,
                () -> registry
        ).run(context -> assertThatThrownBy(
                () -> context.publishEvent(webServerEvent(18101, null))
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("registry unavailable"));
    }

    @Test
    void retriesInitialRegistrationUntilNetworkRecovers() {
        FakeRegistry registry = new FakeRegistry();
        registry.registrationFailures.set(1);

        contextRunner.withBean(
                DdcServiceRegistryClient.class,
                () -> registry
        ).withPropertyValues(
                "egon.cola.component.gateway.provider.http.fail-fast=false"
        ).run(context -> {
            HttpProviderLeaseRuntime runtime = context.getBean(
                    HttpProviderLeaseRuntime.class
            );

            context.publishEvent(webServerEvent(18101, null));
            assertThat(runtime.state())
                    .isEqualTo(HttpProviderRuntimeState.RECOVERING);

            await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
                assertThat(runtime.state())
                        .isEqualTo(HttpProviderRuntimeState.REGISTERED);
                assertThat(registry.registrations).hasValue(2);
            });
        });
    }

    @Test
    void ignoresManagementWebServerEvent() {
        FakeRegistry registry = new FakeRegistry();

        contextRunner.withBean(
                DdcServiceRegistryClient.class,
                () -> registry
        ).run(context -> {
            context.publishEvent(webServerEvent(18102, "management"));
            assertThat(registry.registrations).hasValue(0);

            context.publishEvent(webServerEvent(18101, null));
            assertThat(registry.registration.port()).isEqualTo(18101);
        });
    }

    @Test
    void operatesWithoutActuatorOnTheApplicationClasspath() {
        FakeRegistry registry = new FakeRegistry();

        contextRunner.withClassLoader(new FilteredClassLoader(
                "org.springframework.boot.actuate.health"
        )).withBean(
                DdcServiceRegistryClient.class,
                () -> registry
        ).run(context -> {
            assertThat(context).hasSingleBean(
                    HttpProviderLeaseRuntime.class
            );
            context.publishEvent(webServerEvent(18101, null));
            assertThat(registry.registration.port()).isEqualTo(18101);
        });
    }

    private String[] requiredProperties() {
        return new String[]{
                "egon.cola.component.gateway.provider.http.enabled=true",
                "egon.cola.component.gateway.provider.http.instance-id=orders-1",
                "egon.cola.component.gateway.provider.http.advertised-host=127.0.0.1",
                "egon.cola.component.gateway.provider.http.port=0",
                "egon.cola.component.gateway.provider.http.lease-seconds=3",
                "egon.cola.component.gateway.provider.http.heartbeat-interval-seconds=1",
                "egon.cola.component.ddc.env=test",
                "egon.cola.component.ddc.namespace=gateway-test",
                "egon.cola.component.gateway.reporting.application-code=orders"
        };
    }

    private WebServerInitializedEvent webServerEvent(
            int port,
            String namespace) {
        WebServer server = new WebServer() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public int getPort() {
                return port;
            }
        };
        WebServerApplicationContext applicationContext = mock(
                WebServerApplicationContext.class
        );
        when(applicationContext.getServerNamespace()).thenReturn(namespace);
        return new WebServerInitializedEvent(server) {
            @Override
            public WebServerApplicationContext getApplicationContext() {
                return applicationContext;
            }
        };
    }

    private static final class FakeRegistry
            implements DdcServiceRegistryClient {

        private final AtomicInteger registrations = new AtomicInteger();

        private final AtomicInteger registrationFailures = new AtomicInteger();

        private final AtomicInteger deregistrations = new AtomicInteger();

        private volatile DdcServiceRegistration registration;

        @Override
        public DdcLeaseSession register(DdcServiceRegistration registration) {
            this.registration = registration;
            int attempt = registrations.incrementAndGet();
            if (registrationFailures.getAndUpdate(value ->
                    Math.max(0, value - 1)) > 0) {
                throw new IllegalStateException("registry unavailable");
            }
            Instant now = Instant.now();
            return new DdcLeaseSession(
                    registration.instanceId(),
                    "lease-" + attempt,
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
                    Instant.now().plusSeconds(3)
            );
        }

        @Override
        public DdcLeaseOperationResult deregister(
                String instanceId,
                String leaseId) {
            deregistrations.incrementAndGet();
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
