package top.egon.cola.component.ddc.service.lifecycle;

import top.egon.cola.component.ddc.state.DdcLeaseSessionHolder;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.component.ddc.service.refresh.DdcRefreshService;
import top.egon.cola.component.ddc.error.DdcException;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.config.DdcPublishMessage;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.instance.DdcRuntimeState;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionTicket;
import top.egon.cola.component.ddc.state.DdcLocalConfigState;
import top.egon.cola.component.ddc.redis.DdcRedisTopicSubscription;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    void admissionFailureBeforeInitialRegistrationPreventsReady() {
        RecordingAdminClient adminClient = new RecordingAdminClient(
                new ArrayList<>()
        );
        RecordingAdmissionTickets admissionTickets =
                new RecordingAdmissionTickets();
        admissionTickets.failures.set(1);
        DdcRuntimeCoordinator coordinator = coordinator(
                adminClient,
                mock(DdcRefreshService.class),
                subscription(new ArrayList<>()),
                properties(true),
                new DdcLocalConfigState(),
                admissionTickets
        );

        assertThatThrownBy(coordinator::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IdP admission unavailable");
        assertThat(coordinator.state()).isEqualTo(DdcRuntimeState.FAILED);
        assertThat(adminClient.registerCount).isZero();
    }

    @Test
    void renewalFailureLeavesReadyWithoutExtendingOrLosingShutdownLease() {
        List<String> events = new ArrayList<>();
        RecordingAdminClient adminClient = new RecordingAdminClient(events);
        RecordingAdmissionTickets admissionTickets =
                new RecordingAdmissionTickets();
        DdcRuntimeCoordinator coordinator = coordinator(
                adminClient,
                mock(DdcRefreshService.class),
                subscription(events),
                properties(true),
                new DdcLocalConfigState(),
                admissionTickets
        );
        coordinator.start();
        DdcLeaseSession established = coordinator.currentSession()
                .orElseThrow();
        admissionTickets.failures.set(1);

        coordinator.heartbeatOnce();

        assertThat(coordinator.state())
                .isEqualTo(DdcRuntimeState.RECOVERING);
        assertThat(coordinator.currentSession()).contains(established);
        assertThat(adminClient.heartbeatCount).isZero();
        coordinator.stop();
        assertThat(events).endsWith("offline", "unsubscribe");
        assertThat(admissionTickets.calls.get()).isEqualTo(2);
    }

    @Test
    void everyRenewalUsesCurrentTicketAndUpdatesLocalLeaseExpiry() {
        RecordingAdminClient adminClient = new RecordingAdminClient(
                new ArrayList<>()
        );
        RecordingAdmissionTickets admissionTickets =
                new RecordingAdmissionTickets();
        DdcRuntimeCoordinator coordinator = coordinator(
                adminClient,
                mock(DdcRefreshService.class),
                subscription(new ArrayList<>()),
                properties(true),
                new DdcLocalConfigState(),
                admissionTickets
        );
        coordinator.start();
        adminClient.heartbeatExpireAt = Instant.parse(
                "2026-07-24T12:02:00Z"
        );

        coordinator.heartbeatOnce();

        assertThat(adminClient.registrationAdmissionTicket)
                .isEqualTo("admission-ticket-1");
        assertThat(adminClient.heartbeatAdmissionTicket)
                .isEqualTo("admission-ticket-2");
        assertThat(coordinator.currentSession().orElseThrow()
                .leaseExpireAt()).isEqualTo(adminClient.heartbeatExpireAt);
        coordinator.stop();
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
        DdcLocalConfigState repository = new DdcLocalConfigState();
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
                new DdcLocalConfigState()
        );
    }

    private DdcRuntimeCoordinator coordinator(RecordingAdminClient adminClient,
                                              DdcRefreshService refreshService,
                                              DdcRedisTopicSubscription<DdcPublishMessage> subscription,
                                              DdcProperties properties,
                                              DdcLocalConfigState repository) {
        return coordinator(
                adminClient,
                refreshService,
                subscription,
                properties,
                repository,
                new RecordingAdmissionTickets()
        );
    }

    private DdcRuntimeCoordinator coordinator(
            RecordingAdminClient adminClient,
            DdcRefreshService refreshService,
            DdcRedisTopicSubscription<DdcPublishMessage> subscription,
            DdcProperties properties,
            DdcLocalConfigState repository,
            DdcAdmissionTicketSupplier admissionTickets) {
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
                new DdcInstanceService(
                        properties,
                        adminClient,
                        identity,
                        holder,
                        List.of(),
                        admissionTickets
                );
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

    private static class RecordingAdminClient implements DdcConfigClient {

        private final List<String> events;

        private final DdcConfigValue snapshot;

        private int registrationFailures;

        private int registerCount;

        private int pullCount;

        private int pullFailures;

        private DdcLeaseOperationStatus heartbeatStatus = DdcLeaseOperationStatus.RENEWED;

        private int heartbeatCount;

        private Instant heartbeatExpireAt = Instant.parse(
                "2026-07-24T12:01:00Z"
        );

        private String registrationAdmissionTicket;

        private String heartbeatAdmissionTicket;

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
            registrationAdmissionTicket = request.getAdmissionTicket();
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
            heartbeatCount++;
            heartbeatAdmissionTicket = request.getAdmissionTicket();
            return new DdcLeaseOperationResult(
                    heartbeatStatus,
                    heartbeatExpireAt
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

    private static final class RecordingAdmissionTickets
            implements DdcAdmissionTicketSupplier {

        private final AtomicInteger calls = new AtomicInteger();

        private final AtomicInteger failures = new AtomicInteger();

        @Override
        public DdcAdmissionTicket getTicket(
                String bizCode,
                String appCode,
                String environment,
                String instanceId) {
            int sequence = calls.incrementAndGet();
            if (failures.getAndUpdate(value -> Math.max(0, value - 1))
                    > 0) {
                throw new IllegalStateException(
                        "IdP admission unavailable"
                );
            }
            return new DdcAdmissionTicket(
                    "admission-ticket-" + sequence,
                    Instant.parse("2026-08-10T00:05:00Z"),
                    "resource-demo",
                    URI.create("https://api.example/demo"),
                    1L,
                    bizCode,
                    appCode,
                    environment,
                    instanceId,
                    "kid-test"
            );
        }
    }
}
