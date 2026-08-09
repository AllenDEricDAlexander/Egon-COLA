package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.configuration.client.DdcConfigClient;
import top.egon.cola.component.ddc.autoconfigure.DdcProperties;
import top.egon.cola.component.ddc.configuration.model.DdcAckRequest;
import top.egon.cola.component.ddc.configuration.model.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.configuration.model.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.configuration.model.DdcPublishMessage;
import top.egon.cola.component.ddc.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.configuration.model.DdcConfigValue;
import top.egon.cola.component.ddc.configuration.runtime.DdcInstanceIdentity;
import top.egon.cola.component.ddc.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.configuration.runtime.DdcInstanceService;
import top.egon.cola.component.ddc.configuration.runtime.DdcLeaseSessionHolder;
import top.egon.cola.component.ddc.configuration.refresh.DdcRefreshService;
import top.egon.cola.component.ddc.configuration.runtime.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.configuration.runtime.DdcRuntimeState;
import top.egon.cola.component.ddc.transport.redis.DdcRedisTopicSubscription;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcLeaseLifecycleTest {

    @Test
    void startsInRegisterDefaultsSnapshotOrderAndRejectsStaleLeaseOperations() {
        DdcProperties properties = properties();
        RecordingAdminClient adminClient = new RecordingAdminClient();
        DdcLeaseSessionHolder sessionHolder = new DdcLeaseSessionHolder();
        DdcInstanceService instanceService = new DdcInstanceService(
                properties,
                adminClient,
                new DdcInstanceIdentity(
                        "config-1",
                        "retail",
                        "demo",
                        "dev",
                        "127.0.0.1",
                        8080,
                        "100",
                        "5.2.3"
                ),
                sessionHolder
        );
        DdcRedisTopicSubscription<DdcPublishMessage> subscription =
                mock(DdcRedisTopicSubscription.class);
        when(subscription.isActive()).thenReturn(true);
        DdcRuntimeCoordinator coordinator = new DdcRuntimeCoordinator(
                properties,
                instanceService,
                adminClient,
                mock(DdcRefreshService.class),
                subscription,
                sessionHolder
        );

        coordinator.start();

        assertThat(coordinator.state()).isEqualTo(DdcRuntimeState.READY);
        assertThat(adminClient.events()).containsExactly(
                "register",
                "snapshot"
        );
        DdcLeaseSession first = coordinator.currentSession().orElseThrow();

        DdcLeaseSession replacement = instanceService.register();

        assertThat(replacement.leaseId()).isNotEqualTo(first.leaseId());
        assertThat(instanceService.heartbeat(first).status())
                .isEqualTo(DdcLeaseOperationStatus.LEASE_MISMATCH);
        assertThat(instanceService.offline(first).status())
                .isEqualTo(DdcLeaseOperationStatus.NOT_DELETED);
        assertThat(instanceService.heartbeat(replacement).status())
                .isEqualTo(DdcLeaseOperationStatus.RENEWED);
        assertThat(adminClient.currentLeaseId()).isEqualTo(replacement.leaseId());

        coordinator.stop();

        assertThat(coordinator.state()).isEqualTo(DdcRuntimeState.STOPPED);
        assertThat(coordinator.currentSession()).isEmpty();
        verify(subscription).close();
    }

    private DdcProperties properties() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail");
        properties.setAppCode("demo");
        properties.setEnv("dev");
        properties.setNamespace("default");
        properties.getInstance().setHeartbeatIntervalSeconds(10);
        properties.getInstance().setLeaseSeconds(30);
        return properties;
    }

    private static final class RecordingAdminClient implements DdcConfigClient {

        private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

        private final AtomicInteger sequence = new AtomicInteger();

        private final List<String> events = new ArrayList<>();

        private String currentLeaseId;

        @Override
        public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
            events.add("register");
            currentLeaseId = "lease-" + sequence.incrementAndGet();
            return new DdcLeaseSession(
                    request.getInstanceId(),
                    currentLeaseId,
                    DdcLeaseRole.CONFIG_CLIENT,
                    request.getLeaseSeconds(),
                    request.getHeartbeatIntervalSeconds(),
                    NOW,
                    NOW.plusSeconds(request.getLeaseSeconds())
            );
        }

        @Override
        public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
            if (!request.getLeaseId().equals(currentLeaseId)) {
                return new DdcLeaseOperationResult(
                        DdcLeaseOperationStatus.LEASE_MISMATCH,
                        null
                );
            }
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.RENEWED,
                    NOW.plusSeconds(30)
            );
        }

        @Override
        public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
            if (!request.getLeaseId().equals(currentLeaseId)) {
                return new DdcLeaseOperationResult(
                        DdcLeaseOperationStatus.NOT_DELETED,
                        null
                );
            }
            currentLeaseId = null;
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.DELETED,
                    null
            );
        }

        @Override
        public List<DdcConfigValue> pull() {
            events.add("snapshot");
            return List.of();
        }

        @Override
        public void ack(DdcAckRequest request) {
        }

        List<String> events() {
            return List.copyOf(events);
        }

        String currentLeaseId() {
            return currentLeaseId;
        }
    }
}
