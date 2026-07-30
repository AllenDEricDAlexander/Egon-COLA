package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.service.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.service.DdcRuntimeState;
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
class DdcStarterRuntimeFlowTest {

    @Autowired
    private DdcRuntimeCoordinator runtimeCoordinator;

    @Autowired
    private RecordingDdcAdminClient adminClient;

    @Autowired
    private SampleConfigService sampleConfigService;

    @Test
    void starterRegistersPullsAppliesAndGoesOfflineWithoutAdminClasses() {
        assertThat(runtimeCoordinator.state()).isEqualTo(DdcRuntimeState.READY);
        assertThat(adminClient.events()).containsExactly("register", "defaults", "pull");
        assertThat(sampleConfigService.getRateLimit()).isEqualTo(250);
        assertThatThrownBy(() -> Class.forName(
                "top.egon.cola.component.ddc.admin.service.DdcConfigService"
        )).isInstanceOf(ClassNotFoundException.class);

        runtimeCoordinator.stop();

        assertThat(adminClient.events())
                .containsExactly("register", "defaults", "pull", "offline");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RuntimeTestConfiguration {

        @Bean
        RecordingDdcAdminClient recordingDdcAdminClient() {
            return new RecordingDdcAdminClient();
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

    static final class RecordingDdcAdminClient implements DdcAdminClient {

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
            value.setConfigKey("rateLimit");
            value.setConfigValue("250");
            value.setValueType(Integer.class.getName());
            value.setVersion(1L);
            return List.of(value);
        }

        @Override
        public void reportDefaults(DdcDefaultReportRequest request) {
            events.add("defaults");
        }

        @Override
        public void ack(DdcAckRequest request) {
        }

        List<String> events() {
            return List.copyOf(events);
        }
    }
}
