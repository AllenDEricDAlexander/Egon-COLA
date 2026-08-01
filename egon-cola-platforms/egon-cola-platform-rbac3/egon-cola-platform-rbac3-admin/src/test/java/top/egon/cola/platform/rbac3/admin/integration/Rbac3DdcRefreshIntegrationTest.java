package top.egon.cola.platform.rbac3.admin.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.dto.DdcPublishTarget;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;
import top.egon.cola.component.ddc.service.DdcAckDelivery;
import top.egon.cola.component.ddc.service.DdcAckDeliveryProperties;
import top.egon.cola.component.ddc.service.DdcLeaseSessionHolder;
import top.egon.cola.component.ddc.service.DdcRefreshService;
import top.egon.cola.component.ddc.service.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.service.DdcRuntimeState;
import top.egon.cola.component.ddc.service.DefaultDdcConfigApplierRegistry;
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;
import top.egon.cola.component.gateway.provider.HttpProviderRuntimeState;
import top.egon.cola.platform.rbac3.admin.config.Rbac3AdminProperties;
import top.egon.cola.platform.rbac3.admin.integration.ddc.AtomicRbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.integration.ddc.DdcConfigClientStatusService;
import top.egon.cola.platform.rbac3.admin.integration.ddc.DdcProviderLeaseStatusService;
import top.egon.cola.platform.rbac3.admin.integration.ddc.Rbac3DdcPolicyConfiguration;
import top.egon.cola.platform.rbac3.admin.integration.ddc.Rbac3IntegrationMetrics;
import top.egon.cola.platform.rbac3.admin.integration.runtime.GatewayDdcRuntimeStatusService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Rbac3DdcRefreshIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");
    private static final String CONFIG_LEASE_ID = "config-lease-sensitive";
    private static final String PROVIDER_LEASE_ID = "provider-lease-sensitive";
    private static final String INVALID_RAW_VALUE = "7200";

    private final AtomicInteger messageSequence = new AtomicInteger();

    @Test
    void realRefreshRegistryAndAckDeliveryPreserveLkgAndRecover() throws Exception {
        AtomicRbac3RuntimePolicy policy = policy();
        DefaultDdcConfigApplierRegistry registry = registry(policy);
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        DdcLeaseSession configSession = configSession();
        DdcLeaseSessionHolder holder = new DdcLeaseSessionHolder();
        holder.replace(configSession);
        RecordingAdminClient adminClient = new RecordingAdminClient();
        DdcAckDeliveryProperties deliveryProperties = new DdcAckDeliveryProperties();
        deliveryProperties.setJitter(0.0d);
        deliveryProperties.setMaxAttempts(1);

        try (DdcAckDelivery delivery = new DdcAckDelivery(
                adminClient, deliveryProperties)) {
            delivery.start();
            DdcRefreshService refresh = new DdcRefreshService(
                    repository, registry, delivery, holder);

            refresh.applySnapshots(List.of(
                    config(AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY,
                            "28800", 4L),
                    config(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY,
                            "1200", 1L),
                    config(AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY,
                            "8", 5L),
                    config(AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY,
                            "43200", 3L),
                    config(AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY,
                            "172800", 2L)));

            assertThat(delivery.submittedCount()).isZero();
            assertThat(adminClient.acks()).isEmpty();
            assertThat(policy.current().accessTokenTtl()).isEqualTo(Duration.ofMinutes(20));
            assertThat(policy.current().refreshTokenTtl()).isEqualTo(Duration.ofDays(2));
            assertThat(policy.current().sessionAbsoluteTimeout()).isEqualTo(Duration.ofHours(12));
            assertThat(policy.current().sessionIdleTimeout()).isEqualTo(Duration.ofHours(8));
            assertThat(policy.current().maximumActiveRoots()).isEqualTo(8);
            assertThat(policy.current().configVersions()).containsExactlyInAnyOrderEntriesOf(
                    Map.of(
                            AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, 1L,
                            AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY, 2L,
                            AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY, 3L,
                            AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY, 4L,
                            AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY, 5L));

            refresh.refresh(message(
                    AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "1500", 6L));
            assertAck(adminClient.nextAck(), DdcAckStatus.SUCCESS, 6L);
            var afterValidAccess = policy.current();

            refresh.refresh(message(
                    AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "1500", 6L));
            assertAck(adminClient.nextAck(), DdcAckStatus.SUCCESS, 6L);
            assertThat(policy.current()).isSameAs(afterValidAccess);

            refresh.refresh(message(
                    AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "1400", 5L));
            assertAck(adminClient.nextAck(), DdcAckStatus.IGNORED, 6L);
            assertThat(policy.current()).isSameAs(afterValidAccess);

            refresh.refresh(message(
                    AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "1400", 6L));
            assertAck(adminClient.nextAck(), DdcAckStatus.FAILED, 6L);
            assertThat(policy.current()).isSameAs(afterValidAccess);

            Long previousVersion = repository.version(
                    AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY);
            String previousChecksum = repository.checksum(
                    AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY);
            refresh.refresh(message(
                    AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY,
                    INVALID_RAW_VALUE, 7L));
            DdcAckRequest failed = adminClient.nextAck();
            assertAck(failed, DdcAckStatus.FAILED, previousVersion);
            assertThat(failed.getErrorMessage())
                    .contains("IDLE_EXCEEDS_ABSOLUTE")
                    .doesNotContain(INVALID_RAW_VALUE);
            assertThat(repository.version(
                    AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY))
                    .isEqualTo(previousVersion);
            assertThat(repository.checksum(
                    AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY))
                    .isEqualTo(previousChecksum);
            assertThat(policy.current().sessionAbsoluteTimeout())
                    .isEqualTo(Duration.ofHours(12));
            assertThat(policy.lastApplyFailure()).hasValueSatisfying(failure -> {
                assertThat(failure.key()).isEqualTo(
                        AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY);
                assertThat(failure.targetVersion()).isEqualTo(7L);
                assertThat(failure.errorCode()).isEqualTo("IDLE_EXCEEDS_ABSOLUTE");
            });

            refresh.refresh(message(
                    AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY,
                    "86400", 8L));
            assertAck(adminClient.nextAck(), DdcAckStatus.SUCCESS, 8L);
            assertThat(policy.current().sessionAbsoluteTimeout()).isEqualTo(Duration.ofDays(1));
            assertThat(policy.lastApplyFailure()).isEmpty();
            assertThat(delivery.submittedCount()).isEqualTo(6L);
            assertThat(adminClient.acks()).hasSize(6);

            String ackJson = new ObjectMapper().writeValueAsString(adminClient.acks());
            assertThat(ackJson).doesNotContain(INVALID_RAW_VALUE);
        }

        assertIndependentLeaseStatus(policy, configSession);
        String rawSecretLikeValue = "raw-policy-value-never-log";
        assertThatThrownBy(() -> policy().apply(
                AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY,
                rawSecretLikeValue, 9L))
                .hasMessageNotContaining(rawSecretLikeValue);
    }

    private AtomicRbac3RuntimePolicy policy() {
        Rbac3AdminProperties properties = new Rbac3AdminProperties();
        properties.setRefreshTokenTtl(Duration.ofDays(1));
        properties.setSessionIdleTimeout(Duration.ofMinutes(30));
        properties.setSessionAbsoluteTimeout(Duration.ofHours(1));
        return new AtomicRbac3RuntimePolicy(properties);
    }

    private DefaultDdcConfigApplierRegistry registry(
            AtomicRbac3RuntimePolicy policy) throws Exception {
        DefaultDdcConfigApplierRegistry registry = new DefaultDdcConfigApplierRegistry(
                (key, value, version) -> {
                    throw new AssertionError("RBAC3 keys must use exact appliers");
                });
        StaticListableBeanFactory emptyBeans = new StaticListableBeanFactory();
        new Rbac3DdcPolicyConfiguration().rbac3DdcPolicyRegistrar(
                registry,
                policy,
                emptyBeans.getBeanProvider(Rbac3IntegrationMetrics.class))
                .afterPropertiesSet();
        registry.freeze();
        return registry;
    }

    private void assertIndependentLeaseStatus(
            AtomicRbac3RuntimePolicy policy,
            DdcLeaseSession configSession) {
        DdcRuntimeCoordinator coordinator = mock(DdcRuntimeCoordinator.class);
        when(coordinator.state()).thenReturn(DdcRuntimeState.READY);
        when(coordinator.currentSession()).thenReturn(java.util.Optional.of(configSession));
        var configStatus = new DdcConfigClientStatusService(coordinator, policy).status();

        DdcLeaseSession providerSession = new DdcLeaseSession(
                "rbac3-1", PROVIDER_LEASE_ID, DdcLeaseRole.HTTP_PROVIDER,
                30, 10, NOW, NOW.plusSeconds(30));
        HttpProviderLeaseRuntime providerRuntime = mock(HttpProviderLeaseRuntime.class);
        when(providerRuntime.state()).thenReturn(HttpProviderRuntimeState.REGISTERED);
        when(providerRuntime.instanceId()).thenReturn("rbac3-1");
        when(providerRuntime.lease()).thenReturn(java.util.Optional.of(providerSession));
        var providerStatus = new DdcProviderLeaseStatusService(
                providerRuntime, serviceIdentity()).status();

        assertThat(configSession.leaseId()).isNotEqualTo(providerSession.leaseId());
        assertThat(configStatus.state()).isEqualTo("READY");
        assertThat(configStatus.leaseIdFingerprint())
                .isNotBlank()
                .doesNotContain(CONFIG_LEASE_ID);
        assertThat(configStatus.toString())
                .doesNotContain(CONFIG_LEASE_ID, INVALID_RAW_VALUE);
        assertThat(providerStatus.state()).isEqualTo("REGISTERED");
        assertThat(providerStatus.instanceId()).isEqualTo("rbac3-1");
    }

    private void assertAck(
            DdcAckRequest ack,
            DdcAckStatus status,
            long currentVersion) {
        assertThat(ack).isNotNull();
        assertThat(ack.getStatus()).isEqualTo(status);
        assertThat(ack.getCurrentVersion()).isEqualTo(currentVersion);
        assertThat(ack.getInstanceId()).isEqualTo("rbac3-1");
        assertThat(ack.getLeaseId()).isEqualTo(CONFIG_LEASE_ID);
    }

    private DdcLeaseSession configSession() {
        return new DdcLeaseSession(
                "rbac3-1", CONFIG_LEASE_ID, DdcLeaseRole.CONFIG_CLIENT,
                30, 10, NOW, NOW.plusSeconds(30));
    }

    private GatewayDdcRuntimeStatusService.ServiceIdentity serviceIdentity() {
        return new GatewayDdcRuntimeStatusService.ServiceIdentity(
                "rbac3", "rbac3-admin", "prod", "default",
                "HTTP_PROVIDER", "http", "rbac3-admin", "default", "1.0.0");
    }

    private DdcPublishMessage message(String key, String value, long version) {
        DdcPublishMessage message = new DdcPublishMessage();
        message.setChangeId("change-" + messageSequence.incrementAndGet());
        message.setBizCode("rbac3");
        message.setAppCode("rbac3-admin");
        message.setEnv("prod");
        message.setConfigKey(key);
        message.setConfigValue(value);
        message.setTargetVersion(version);
        message.setContentChecksum(DdcChecksum.content(value));
        message.setTargets(List.of(new DdcPublishTarget("rbac3-1", CONFIG_LEASE_ID)));
        return message;
    }

    private DdcConfigValue config(String key, String value, long version) {
        DdcConfigValue config = new DdcConfigValue();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setVersion(version);
        return config;
    }

    private static final class RecordingAdminClient implements DdcAdminClient {

        private final BlockingQueue<DdcAckRequest> pending = new LinkedBlockingQueue<>();
        private final List<DdcAckRequest> delivered = new ArrayList<>();

        @Override
        public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
            throw new UnsupportedOperationException("not used by refresh integration test");
        }

        @Override
        public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.RENEWED, NOW);
        }

        @Override
        public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.DELETED, NOW);
        }

        @Override
        public List<DdcConfigValue> pull() {
            return List.of();
        }

        @Override
        public void reportDefaults(DdcDefaultReportRequest request) {
        }

        @Override
        public void ack(DdcAckRequest request) {
            synchronized (delivered) {
                delivered.add(request);
            }
            pending.add(request);
        }

        DdcAckRequest nextAck() throws InterruptedException {
            return pending.poll(2, TimeUnit.SECONDS);
        }

        List<DdcAckRequest> acks() {
            synchronized (delivered) {
                return List.copyOf(delivered);
            }
        }
    }
}
