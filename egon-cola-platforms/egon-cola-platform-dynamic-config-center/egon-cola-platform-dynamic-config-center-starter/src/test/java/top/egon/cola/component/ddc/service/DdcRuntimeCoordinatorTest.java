package top.egon.cola.component.ddc.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;
import top.egon.cola.component.ddc.transport.redis.DdcRedisTopicSubscription;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRuntimeCoordinatorTest {

    @Test
    void consistencyReconciliationDefaultsToEnabledEveryThirtySeconds() {
        DdcProperties properties = new DdcProperties();

        assertThat(properties.getConsistency().isReconcileEnabled()).isTrue();
        assertThat(properties.getConsistency().getReconcileIntervalSeconds()).isEqualTo(30);
    }

    @Test
    void startsInContractOrderAndStopsInReverseInfrastructureOrder() {
        List<String> events = new ArrayList<>();
        RecordingAdminClient adminClient = new RecordingAdminClient(events);
        DdcRefreshService refreshService = mock(DdcRefreshService.class);
        doAnswer(invocation -> {
            events.add("snapshot");
            return null;
        }).when(refreshService).applySnapshots(List.of(adminClient.snapshot));
        DdcRedisTopicSubscription<DdcPublishMessage> subscription = subscription(events);
        DdcRuntimeCoordinator coordinator = coordinator(adminClient, refreshService, subscription, true);

        coordinator.start();

        assertThat(coordinator.state()).isEqualTo(DdcRuntimeState.READY);
        assertThat(events).containsExactly("register", "pull", "snapshot");

        coordinator.stop();

        assertThat(events).containsExactly(
                "register", "pull", "snapshot", "offline", "unsubscribe"
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
        assertThat(events).containsExactly("register", "register", "pull");
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

    @Test
    void disabledReconciliationDoesNotPullAnotherSnapshot() {
        RecordingAdminClient adminClient = new RecordingAdminClient(new ArrayList<>());
        DdcProperties properties = properties(true);
        properties.getConsistency().setReconcileEnabled(false);
        DdcRuntimeCoordinator coordinator = coordinator(
                adminClient,
                mock(DdcRefreshService.class),
                subscription(new ArrayList<>()),
                properties
        );
        coordinator.start();

        coordinator.reconcileOnce();

        assertThat(adminClient.pullCount).isEqualTo(1);
        coordinator.stop();
    }

    @Test
    void reconciliationPullsAndDelegatesCurrentSnapshots() {
        RecordingAdminClient adminClient = new RecordingAdminClient(new ArrayList<>());
        DdcRefreshService refreshService = mock(DdcRefreshService.class);
        DdcRuntimeCoordinator coordinator = coordinator(
                adminClient,
                refreshService,
                subscription(new ArrayList<>()),
                true
        );
        coordinator.start();
        clearInvocations(refreshService);
        adminClient.snapshot.setVersion(2L);

        coordinator.reconcileOnce();

        assertThat(adminClient.pullCount).isEqualTo(2);
        verify(refreshService).applySnapshots(List.of(adminClient.snapshot));
        coordinator.stop();
    }

    @Test
    void reconciliationFailureKeepsReadyStateAndLocalMetadata() {
        RecordingAdminClient adminClient = new RecordingAdminClient(new ArrayList<>());
        DdcRefreshService refreshService = mock(DdcRefreshService.class);
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        repository.updateVersion("switch", 1L);
        repository.updateChecksum("switch", "last-known-good");
        DdcRuntimeCoordinator coordinator = coordinator(
                adminClient,
                refreshService,
                subscription(new ArrayList<>()),
                properties(true),
                repository
        );
        coordinator.start();
        clearInvocations(refreshService);
        adminClient.pullFailures = 1;

        coordinator.reconcileOnce();

        assertThat(coordinator.state()).isEqualTo(DdcRuntimeState.READY);
        assertThat(repository.version("switch")).isEqualTo(1L);
        assertThat(repository.checksum("switch")).isEqualTo("last-known-good");
        verify(refreshService, never()).applySnapshots(List.of(adminClient.snapshot));
        coordinator.stop();
    }

    @Test
    void invalidReconciliationIntervalFailsBeforeStartup() {
        RecordingAdminClient adminClient = new RecordingAdminClient(new ArrayList<>());
        DdcProperties properties = properties(true);
        properties.getConsistency().setReconcileIntervalSeconds(0);
        DdcRuntimeCoordinator coordinator = coordinator(
                adminClient,
                mock(DdcRefreshService.class),
                subscription(new ArrayList<>()),
                properties
        );

        assertThatThrownBy(coordinator::start)
                .isInstanceOf(DdcException.class)
                .hasMessageContaining("reconcileIntervalSeconds");
        assertThat(coordinator.isRunning()).isFalse();
        assertThat(adminClient.registerCount).isZero();
    }

    @Test
    void invalidHeartbeatIntervalFailsBeforeRegistration() {
        RecordingAdminClient adminClient = new RecordingAdminClient(new ArrayList<>());
        DdcProperties properties = properties(true);
        properties.getInstance().setHeartbeatIntervalSeconds(30);
        DdcRuntimeCoordinator coordinator = coordinator(
                adminClient,
                mock(DdcRefreshService.class),
                subscription(new ArrayList<>()),
                properties
        );

        assertThatThrownBy(coordinator::start)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "egon.cola.component.ddc.instance.heartbeat-interval-seconds "
                                + "must be positive and less than lease-seconds"
                );
        assertThat(coordinator.isRunning()).isFalse();
        assertThat(adminClient.registerCount).isZero();
    }

    private DdcRuntimeCoordinator coordinator(RecordingAdminClient adminClient,
                                              DdcRefreshService refreshService,
                                              DdcRedisTopicSubscription<DdcPublishMessage> subscription,
                                              boolean failFast) {
        return coordinator(adminClient, refreshService, subscription, properties(failFast));
    }

    private DdcProperties properties(boolean failFast) {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail");
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
                                              DdcRedisTopicSubscription<DdcPublishMessage> subscription,
                                              DdcProperties properties) {
        return coordinator(
                adminClient,
                refreshService,
                subscription,
                properties,
                new DdcLocalConfigRepository()
        );
    }

    private DdcRuntimeCoordinator coordinator(RecordingAdminClient adminClient,
                                              DdcRefreshService refreshService,
                                              DdcRedisTopicSubscription<DdcPublishMessage> subscription,
                                              DdcProperties properties,
                                              DdcLocalConfigRepository repository) {
        DdcLeaseSessionHolder holder = new DdcLeaseSessionHolder();
        DdcInstanceIdentity identity = new DdcInstanceIdentity(
                "instance-1",
                "retail",
                "demo",
                "dev",
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
                refreshService,
                subscription,
                holder
        );
    }

    private DdcRedisTopicSubscription<DdcPublishMessage> subscription(List<String> events) {
        DdcRedisTopicSubscription<DdcPublishMessage> subscription = mock(
                DdcRedisTopicSubscription.class
        );
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

        private int pullCount;

        private int pullFailures;

        private DdcLeaseOperationStatus heartbeatStatus = DdcLeaseOperationStatus.RENEWED;

        private RecordingAdminClient(List<String> events) {
            this.events = events;
            this.snapshot = new DdcConfigValue();
            snapshot.setResourceName("switch");
            snapshot.setContent("true");
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
            pullCount++;
            if (pullFailures-- > 0) {
                throw new IllegalStateException("pull failed");
            }
            return List.of(snapshot);
        }

        @Override
        public void ack(DdcAckRequest request) {
        }
    }
}
