package top.egon.cola.component.ddc.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcException;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DdcRuntimeCoordinatorTest {

    @Test
    void startsInContractOrderAndStopsInReverseInfrastructureOrder() {
        List<String> events = new ArrayList<>();
        RecordingAdminClient adminClient = new RecordingAdminClient(events);
        DdcRefreshService refreshService = mock(DdcRefreshService.class);
        doAnswer(invocation -> {
            events.add("snapshot");
            return null;
        }).when(refreshService).applySnapshot(adminClient.snapshot);
        DdcRedisChangeSubscription subscription = subscription(events);
        DdcRuntimeCoordinator coordinator = coordinator(adminClient, refreshService, subscription, true);

        coordinator.start();

        assertThat(coordinator.state()).isEqualTo(DdcRuntimeState.READY);
        assertThat(events).containsExactly("register", "defaults", "pull", "snapshot");

        coordinator.stop();

        assertThat(events).containsExactly(
                "register", "defaults", "pull", "snapshot", "offline", "unsubscribe"
        );
        assertThat(coordinator.state()).isEqualTo(DdcRuntimeState.STOPPED);
    }

    @Test
    void failFastPropagatesInitialRegistrationFailure() {
        RecordingAdminClient adminClient = new RecordingAdminClient(new ArrayList<>());
        adminClient.registrationFailures = 1;
        DdcRuntimeCoordinator coordinator = coordinator(
                adminClient,
                mock(DdcRefreshService.class),
                subscription(new ArrayList<>()),
                true
        );

        assertThatThrownBy(coordinator::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("register failed");
        assertThat(coordinator.state()).isEqualTo(DdcRuntimeState.FAILED);
    }

    @Test
    void invalidScopeDoesNotLeaveCoordinatorRunning() {
        RecordingAdminClient adminClient = new RecordingAdminClient(new ArrayList<>());
        DdcProperties properties = properties(true);
        properties.setAppCode(" ");
        DdcRuntimeCoordinator coordinator = coordinator(
                adminClient,
                mock(DdcRefreshService.class),
                subscription(new ArrayList<>()),
                properties
        );

        assertThatThrownBy(coordinator::start)
                .isInstanceOf(DdcException.class)
                .hasMessageContaining("appCode");
        assertThat(coordinator.isRunning()).isFalse();
        assertThat(coordinator.state()).isEqualTo(DdcRuntimeState.NEW);
    }

    @Test
    void nonFailFastRetriesTheCompleteInitializationSequence() {
        List<String> events = new ArrayList<>();
        RecordingAdminClient adminClient = new RecordingAdminClient(events);
        adminClient.registrationFailures = 1;
        DdcRuntimeCoordinator coordinator = coordinator(
                adminClient,
                mock(DdcRefreshService.class),
                subscription(events),
                false
        );

        coordinator.start();
        assertThat(coordinator.state()).isEqualTo(DdcRuntimeState.RECOVERING);

        coordinator.heartbeatOnce();

        assertThat(coordinator.state()).isEqualTo(DdcRuntimeState.READY);
        assertThat(events).containsExactly("register", "register", "defaults", "pull");
        coordinator.stop();
    }

    @Test
    void missingOrMismatchedHeartbeatReRegistersWithANewLease() {
        for (DdcLeaseOperationStatus status : List.of(
                DdcLeaseOperationStatus.NOT_FOUND,
                DdcLeaseOperationStatus.LEASE_MISMATCH
        )) {
            RecordingAdminClient adminClient = new RecordingAdminClient(new ArrayList<>());
            adminClient.heartbeatStatus = status;
            DdcRuntimeCoordinator coordinator = coordinator(
                    adminClient,
                    mock(DdcRefreshService.class),
                    subscription(new ArrayList<>()),
                    true
            );
            coordinator.start();
            String firstLeaseId = coordinator.currentSession().orElseThrow().leaseId();

            coordinator.heartbeatOnce();

            assertThat(coordinator.currentSession().orElseThrow().leaseId())
                    .isNotEqualTo(firstLeaseId);
            assertThat(adminClient.registerCount).isEqualTo(2);
            coordinator.stop();
        }
    }

    private DdcRuntimeCoordinator coordinator(RecordingAdminClient adminClient,
                                              DdcRefreshService refreshService,
                                              DdcRedisChangeSubscription subscription,
                                              boolean failFast) {
        return coordinator(adminClient, refreshService, subscription, properties(failFast));
    }

    private DdcProperties properties(boolean failFast) {
        DdcProperties properties = new DdcProperties();
        properties.setAppCode("demo");
        properties.setEnv("dev");
        properties.setNamespace("default");
        properties.getConsistency().setFailFast(failFast);
        properties.getInstance().setLeaseSeconds(30);
        properties.getInstance().setHeartbeatIntervalSeconds(10);
        return properties;
    }

    private DdcRuntimeCoordinator coordinator(RecordingAdminClient adminClient,
                                              DdcRefreshService refreshService,
                                              DdcRedisChangeSubscription subscription,
                                              DdcProperties properties) {
        DdcLeaseSessionHolder holder = new DdcLeaseSessionHolder();
        DdcInstanceIdentity identity = new DdcInstanceIdentity(
                "instance-1",
                "demo",
                "dev",
                "default",
                "127.0.0.1",
                null,
                "100",
                "5.2.3"
        );
        DdcInstanceService instanceService =
                new DdcInstanceService(properties, adminClient, identity, holder);
        return new DdcRuntimeCoordinator(
                properties,
                instanceService,
                adminClient,
                new DdcLocalConfigRepository(),
                refreshService,
                subscription,
                holder
        );
    }

    private DdcRedisChangeSubscription subscription(List<String> events) {
        DdcRedisChangeSubscription subscription = mock(DdcRedisChangeSubscription.class);
        when(subscription.isActive()).thenReturn(true);
        doAnswer(invocation -> {
            events.add("unsubscribe");
            return null;
        }).when(subscription).close();
        return subscription;
    }

    private static class RecordingAdminClient implements DdcAdminClient {

        private final List<String> events;

        private final DdcConfigValue snapshot;

        private int registrationFailures;

        private int registerCount;

        private DdcLeaseOperationStatus heartbeatStatus = DdcLeaseOperationStatus.RENEWED;

        private RecordingAdminClient(List<String> events) {
            this.events = events;
            this.snapshot = new DdcConfigValue();
            snapshot.setConfigKey("switch");
            snapshot.setConfigValue("true");
            snapshot.setVersion(1L);
        }

        @Override
        public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
            events.add("register");
            if (registrationFailures-- > 0) {
                throw new IllegalStateException("register failed");
            }
            registerCount++;
            Instant now = Instant.parse("2026-07-24T12:00:00Z").plusSeconds(registerCount);
            return new DdcLeaseSession(
                    request.getInstanceId(),
                    "lease-" + registerCount,
                    DdcLeaseRole.CONFIG_CLIENT,
                    request.getLeaseSeconds(),
                    request.getHeartbeatIntervalSeconds(),
                    now,
                    now.plusSeconds(request.getLeaseSeconds())
            );
        }

        @Override
        public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
            return new DdcLeaseOperationResult(heartbeatStatus, Instant.parse("2026-07-24T12:01:00Z"));
        }

        @Override
        public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
            events.add("offline");
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.DELETED, null);
        }

        @Override
        public List<DdcConfigValue> pull() {
            events.add("pull");
            return List.of(snapshot);
        }

        @Override
        public void reportDefaults(DdcDefaultReportRequest request) {
            events.add("defaults");
        }

        @Override
        public void ack(DdcAckRequest request) {
        }
    }
}
