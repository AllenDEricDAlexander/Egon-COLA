package top.egon.cola.component.ddc.http.registration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionTicket;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DdcHttpRegistrationAutoConfigurationTest {

    private static final String SERVICE_VERSION = "1.0.0";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            DdcHttpRegistrationAutoConfiguration.class
                    ))
                    .withBean(
                            DdcHttpRegistrationContributor.class,
                            this::registrationContributor
                    )
                    .withBean(
                            DdcServiceKeyFactory.class,
                            this::serviceKeyFactory
                    )
                    .withBean(
                            DdcInstanceIdentity.class,
                            this::ddcInstanceIdentity
                    )
                    .withBean(
                            DdcAdmissionTicketSupplier.class,
                            this::admissionTickets
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
                    DdcHttpRegistrationRuntime.class
            );

            context.publishEvent(webServerEvent(18101, null));

            assertThat(registry.registration.port()).isEqualTo(18101);
            assertThat(registry.registration.serviceKey().env())
                    .isEqualTo("test");
            assertThat(registry.registration.instanceId())
                    .isEqualTo("ddc-runtime-1");
            assertThat(registry.registration.serviceKey().serviceName())
                    .isEqualTo("orders");
            assertThat(registry.registration.serviceKey().version())
                    .isEqualTo(SERVICE_VERSION);
            assertThat(registry.registration.metadata())
                    .containsEntry(
                            "gateway.definition-set-id",
                            "definition-set-a"
                    );

            DdcHttpRegistrationHealthIndicator healthIndicator =
                    context.getBean(
                            DdcHttpRegistrationHealthIndicator.class
                    );
            assertThat(healthIndicator.health().getStatus())
                    .isEqualTo(Status.UP);
            assertThat(healthIndicator.health().getDetails())
                    .containsEntry("state", "REGISTERED")
                    .containsEntry("instanceId", "ddc-runtime-1")
                    .containsEntry("leaseId", "lease-1")
                    .containsKey("leaseExpireAt");
        });

        assertThat(registry.deregistrations).hasValue(1);
    }

    @Test
    void registersWithoutDefinitionReportingWhenVersionIsExplicit() {
        FakeRegistry registry = new FakeRegistry();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DdcHttpRegistrationAutoConfiguration.class
                ))
                .withPropertyValues(requiredProperties())
                .withPropertyValues(
                        "egon.cola.component.ddc.registry.http.version=1.0.0"
                )
                .withBean(
                        DdcServiceKeyFactory.class,
                        this::serviceKeyFactory
                )
                .withBean(
                        DdcInstanceIdentity.class,
                        this::ddcInstanceIdentity
                )
                .withBean(
                        DdcAdmissionTicketSupplier.class,
                        this::admissionTickets
                )
                .withBean(DdcServiceRegistryClient.class, () -> registry)
                .run(context -> {
                    assertThat(context).hasSingleBean(
                            DdcHttpRegistrationRuntime.class
                    );

                    context.publishEvent(webServerEvent(18101, null));

                    assertThat(registry.registration.serviceKey().version())
                            .isEqualTo("1.0.0");
                    assertThat(registry.registration.metadata())
                            .doesNotContainKeys(
                                    "gateway.definition-set-id",
                                    "gateway.build-id"
                            );
                });
    }

    @Test
    void acceptsExplicitInstanceIdWithoutDdcIdentityBean() {
        FakeRegistry registry = new FakeRegistry();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DdcHttpRegistrationAutoConfiguration.class
                ))
                .withPropertyValues(requiredProperties())
                .withPropertyValues(
                        "egon.cola.component.ddc.registry.http.instance-id=explicit-provider",
                        "egon.cola.component.ddc.registry.http.version=1.0.0"
                )
                .withBean(
                        DdcServiceKeyFactory.class,
                        this::serviceKeyFactory
                )
                .withBean(
                        DdcAdmissionTicketSupplier.class,
                        this::admissionTickets
                )
                .withBean(DdcServiceRegistryClient.class, () -> registry)
                .run(context -> {
                    assertThat(context).hasSingleBean(
                            DdcHttpRegistrationRuntime.class
                    );
                    context.publishEvent(webServerEvent(18101, null));
                    assertThat(registry.registration.instanceId())
                            .isEqualTo("explicit-provider");
                });
    }

    @Test
    void backsOffWhenDisabledOrRegistryIsMissing() {
        contextRunner.withPropertyValues(
                "egon.cola.component.ddc.registry.http.enabled=false"
        ).run(context -> assertThat(context).doesNotHaveBean(
                DdcHttpRegistrationRuntime.class
        ));

        contextRunner.run(context -> assertThat(context).doesNotHaveBean(
                DdcHttpRegistrationRuntime.class
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
                "egon.cola.component.ddc.registry.http.fail-fast=false"
        ).run(context -> {
            DdcHttpRegistrationRuntime runtime = context.getBean(
                    DdcHttpRegistrationRuntime.class
            );

            context.publishEvent(webServerEvent(18101, null));
            assertThat(runtime.state())
                    .isEqualTo(DdcHttpRegistrationState.RECOVERING);

            await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
                assertThat(runtime.state())
                        .isEqualTo(DdcHttpRegistrationState.REGISTERED);
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
                    DdcHttpRegistrationRuntime.class
            );
            context.publishEvent(webServerEvent(18101, null));
            assertThat(registry.registration.port()).isEqualTo(18101);
        });
    }

    private String[] requiredProperties() {
        return new String[]{
                "egon.cola.component.ddc.registry.http.enabled=true",
                "egon.cola.component.ddc.registry.http.advertised-host=127.0.0.1",
                "egon.cola.component.ddc.registry.http.port=0",
                "egon.cola.component.ddc.registry.http.lease-seconds=3",
                "egon.cola.component.ddc.registry.http.heartbeat-interval-seconds=1",
                "egon.cola.component.ddc.env=test",
                "egon.cola.component.ddc.namespace=gateway-test",
                "spring.application.name=orders"
        };
    }

    private DdcHttpRegistrationContributor registrationContributor() {
        return new DdcHttpRegistrationContributor() {
            @Override
            public String serviceVersion() {
                return SERVICE_VERSION;
            }

            @Override
            public Map<String, String> metadata() {
                return Map.of(
                        "gateway.definition-set-id",
                        "definition-set-a",
                        "gateway.build-id",
                        "build-a"
                );
            }
        };
    }

    private DdcServiceKeyFactory serviceKeyFactory() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("test-biz");
        properties.setAppCode("orders");
        properties.setEnv("test");
        properties.setNamespace("gateway-test");
        return new DdcServiceKeyFactory(properties);
    }

    private DdcAdmissionTicketSupplier admissionTickets() {
        return (bizCode, appCode, environment, instanceId) ->
                new DdcAdmissionTicket(
                        "test-admission-ticket",
                        Instant.parse("2099-01-01T00:00:00Z"),
                        "resource-test",
                        URI.create("urn:egon:resource:test"),
                        1L,
                        bizCode,
                        appCode,
                        environment,
                        instanceId,
                        "kid-test"
                );
    }

    private DdcInstanceIdentity ddcInstanceIdentity() {
        return new DdcInstanceIdentity(
                "ddc-runtime-1",
                "test-biz",
                "orders",
                "test",
                "127.0.0.1",
                null,
                "100",
                "5.3.2"
        );
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
                DdcServiceLeaseRequest request) {
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
