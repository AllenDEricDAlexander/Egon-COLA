package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.listener.DdcRedisChangeSubscription;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;
import top.egon.cola.component.ddc.service.DdcInstanceService;
import top.egon.cola.component.ddc.service.DdcLeaseSessionHolder;
import top.egon.cola.component.ddc.service.DdcRefreshService;
import top.egon.cola.component.ddc.service.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.service.DdcRuntimeState;

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
                        "demo",
                        "dev",
                        "default",
                        "127.0.0.1",
                        8080,
                        "100",
                        "5.2.3"
                ),
                sessionHolder
        );
        DdcRedisChangeSubscription subscription =
                mock(DdcRedisChangeSubscription.class);
        when(subscription.isActive()).thenReturn(true);
        DdcRuntimeCoordinator coordinator = new DdcRuntimeCoordinator(
                properties,
                instanceService,
                adminClient,
                new DdcLocalConfigRepository(),
                mock(DdcRefreshService.class),
                subscription,
                sessionHolder
        );

        coordinator.start();

        assertThat(coordinator.state()).isEqualTo(DdcRuntimeState.READY);
        assertThat(adminClient.events()).containsExactly(
                "register",
                "defaults",
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
        properties.setAppCode("demo");
        properties.setEnv("dev");
        properties.setNamespace("default");
        properties.getInstance().setHeartbeatIntervalSeconds(10);
        properties.getInstance().setLeaseSeconds(30);
        return properties;
    }

    private static final class RecordingAdminClient implements DdcAdminClient {

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
        public void reportDefaults(DdcDefaultReportRequest request) {
            events.add("defaults");
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
