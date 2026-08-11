package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.config.DdcPublishMessage;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionTicket;
import top.egon.cola.component.ddc.format.DdcYamlConfigFormatStrategy;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.model.instance.DdcRuntimeState;
import top.egon.cola.component.ddc.test.service.SampleConfigService;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "egon.cola.component.ddc.enabled=true",
        "egon.cola.component.ddc.biz-code=demo-biz",
        "egon.cola.component.ddc.app-code=demo-app",
        "egon.cola.component.ddc.env=dev",
        "egon.cola.component.ddc.namespace=default",
        "egon.cola.component.ddc.rpc.target=dns:///127.0.0.1:19080",
        "egon.cola.component.ddc.rpc.tls.development-plaintext=true",
        "egon.cola.component.ddc.redis.enabled=true",
        "egon.cola.component.ddc.consistency.fail-fast=true",
        "egon.cola.component.ddc.instance.lease-seconds=7200",
        "egon.cola.component.ddc.instance.heartbeat-interval-seconds=3600",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@Import(DdcStarterRuntimeFlowTest.RuntimeTestConfiguration.class)
@ContextConfiguration(initializers =
        DdcStarterRuntimeFlowTest.DdcPropertySourceInitializer.class)
class DdcStarterRuntimeFlowTest {

    @Autowired
    private DdcRuntimeCoordinator runtimeCoordinator;

    @Autowired
    private RecordingDdcConfigClient adminClient;

    @Autowired
    private SampleConfigService sampleConfigService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void starterRegistersPullsAppliesAndGoesOfflineWithoutAdminBeans() {
        assertThat(runtimeCoordinator.state()).isEqualTo(DdcRuntimeState.READY);
        assertThat(adminClient.events()).containsExactly("register", "pull");
        assertThat(sampleConfigService.getRateLimit()).isEqualTo(250);
        assertThat(applicationContext.containsBean("ddcConfigService")).isFalse();

        runtimeCoordinator.stop();

        assertThat(adminClient.events())
                .containsExactly("register", "pull", "offline");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RuntimeTestConfiguration {

        @Bean
        RecordingDdcConfigClient recordingDdcConfigClient() {
            return new RecordingDdcConfigClient();
        }

        @Bean
        DdcAdmissionTicketSupplier admissionTicketSupplier() {
            return (bizCode, appCode, env, instanceId) ->
                    new DdcAdmissionTicket(
                            "test-admission-ticket",
                            Instant.now().plusSeconds(300),
                            "demo-resource",
                            URI.create("https://api.egon.internal/dev/demo-biz/demo-app"),
                            1L,
                            bizCode,
                            appCode,
                            env,
                            instanceId,
                            "test-kid"
                    );
        }

        @Bean(name = "ddcRedissonClient", destroyMethod = "")
        RedissonClient ddcRedissonClient() {
            RedissonClient client = mock(RedissonClient.class);
            RTopic topic = mock(RTopic.class);
            when(client.getTopic(anyString())).thenReturn(topic);
            when(topic.addListener(eq(DdcPublishMessage.class), any()))
                    .thenReturn(1);
            return client;
        }
    }

    static class DdcPropertySourceInitializer implements
            ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            context.getEnvironment().getPropertySources().addFirst(
                    new DdcYamlConfigFormatStrategy()
                            .empty("application.yml")
            );
        }
    }

    static final class RecordingDdcConfigClient implements DdcConfigClient {

        private final List<String> events = new ArrayList<>();

        @Override
        public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
            assertThat(request.getAdmissionTicket())
                    .isEqualTo("test-admission-ticket");
            events.add("register");
            Instant registeredAt = Instant.now();
            return new DdcLeaseSession(
                    request.getInstanceId(),
                    "lease-1",
                    DdcLeaseRole.CONFIG_CLIENT,
                    request.getLeaseSeconds(),
                    request.getHeartbeatIntervalSeconds(),
                    registeredAt,
                    registeredAt.plusSeconds(request.getLeaseSeconds())
            );
        }

        @Override
        public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.RENEWED,
                    Instant.now().plusSeconds(7200)
            );
        }

        @Override
        public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
            events.add("offline");
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.DELETED, null);
        }

        @Override
        public List<DdcConfigValue> pull() {
            events.add("pull");
            DdcConfigValue value = new DdcConfigValue();
            value.setResourceName("application.yml");
            value.setContent("order:\n  rate-limit:\n    permits-per-second: 250\n");
            value.setFormat("YAML");
            value.setVersion(1L);
            return List.of(value);
        }

        @Override
        public void ack(DdcAckRequest request) {
        }

        List<String> events() {
            return List.copyOf(events);
        }
    }
}
