package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.config.DdcPublishMessage;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.service.lifecycle.DdcInstanceService;
import top.egon.cola.component.ddc.state.DdcLeaseSessionHolder;
import top.egon.cola.component.ddc.service.refresh.DdcRefreshService;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.model.instance.DdcRuntimeState;
import top.egon.cola.component.ddc.redis.DdcRedisTopicSubscription;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import top.egon.cola.platform.idp.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.idp.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.idp.starter.client.IdpServiceTokenRequest;

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
                sessionHolder,
                List.of(),
                serviceClient(),
                idpProperties()
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

    private IdpStarterProperties idpProperties() {
        IdpStarterProperties properties = new IdpStarterProperties();
        properties.setResourceUri(java.net.URI.create("https://api.example/ddc"));
        IdpStarterProperties.ServiceClient client =
                new IdpStarterProperties.ServiceClient();
        client.setAppId("ddc-app");
        client.setRegistrationId("ddc-registration");
        properties.setServiceClient(client);
        return properties;
    }

    private IdpServiceOAuth2Client serviceClient() {
        IdpServiceOAuth2Client client = mock(IdpServiceOAuth2Client.class);
        Instant issuedAt = Instant.now();
        when(client.authorize(any(IdpServiceTokenRequest.class)))
                .thenReturn(new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "service-token",
                        issuedAt,
                        issuedAt.plusSeconds(300)
                ));
        return client;
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
