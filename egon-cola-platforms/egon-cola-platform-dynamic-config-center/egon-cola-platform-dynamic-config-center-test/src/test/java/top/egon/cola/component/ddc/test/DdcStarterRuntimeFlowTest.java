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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import top.egon.cola.component.ddc.configuration.client.DdcConfigClient;
import top.egon.cola.component.ddc.configuration.model.DdcAckRequest;
import top.egon.cola.component.ddc.configuration.model.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.configuration.model.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.configuration.model.DdcPublishMessage;
import top.egon.cola.component.ddc.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.configuration.model.DdcConfigValue;
import top.egon.cola.component.ddc.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.configuration.format.DdcYamlConfigFormatStrategy;
import top.egon.cola.component.ddc.configuration.runtime.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.configuration.runtime.DdcRuntimeState;
import top.egon.cola.component.ddc.test.service.SampleConfigService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        "egon.cola.component.ddc.admin.endpoint=http://ddc.test",
        "egon.cola.component.ddc.admin.tls.development-plaintext=true",
        "egon.cola.component.ddc.redis.enabled=true",
        "egon.cola.component.ddc.consistency.fail-fast=true",
        "egon.cola.component.ddc.instance.lease-seconds=7200",
        "egon.cola.component.ddc.instance.heartbeat-interval-seconds=3600"
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

    @Test
    void starterRegistersPullsAppliesAndGoesOfflineWithoutAdminClasses() {
        assertThat(runtimeCoordinator.state()).isEqualTo(DdcRuntimeState.READY);
        assertThat(adminClient.events()).containsExactly("register", "pull");
        assertThat(sampleConfigService.getRateLimit()).isEqualTo(250);
        assertThatThrownBy(() -> Class.forName(
                "top.egon.cola.component.ddc.admin.service.config.DdcConfigService"
        )).isInstanceOf(ClassNotFoundException.class);

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
