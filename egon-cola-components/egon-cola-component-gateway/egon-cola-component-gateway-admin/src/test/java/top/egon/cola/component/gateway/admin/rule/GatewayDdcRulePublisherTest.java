package top.egon.cola.component.gateway.admin.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayDdcRulePublisherTest {

    @Test
    void publishesExactlyOneFullyResolvedArtifact() {
        RecordingClient client = new RecordingClient();
        GatewayDdcRulePublisher publisher =
                new GatewayDdcRulePublisher(client);
        String changeId = UuidV7.string();
        GatewayDdcPublicationCommand command =
                new GatewayDdcPublicationCommand(
                        "gateway-engine-default",
                        "test",
                        "default",
                        "gateway.rules.chunk.release-1.0",
                        "{\"releaseId\":\"release-1\"}",
                        1L,
                        changeId,
                        "admin",
                        Duration.ofSeconds(30)
                );

        DdcManagementPublishResult result = publisher.publish(command);

        assertThat(result.status())
                .isEqualTo(DdcManagementPublishStatus.SUCCESS);
        assertThat(client.requests).singleElement().satisfies(request -> {
            assertThat(request.appCode())
                    .isEqualTo("gateway-engine-default");
            assertThat(request.configKey())
                    .isEqualTo("gateway.rules.chunk.release-1.0");
            assertThat(request.expectedVersion()).isEqualTo(1L);
            assertThat(request.timeoutMs()).isEqualTo(30_000L);
            assertThat(request.operator()).isEqualTo("admin");
            assertThat(UUID.fromString(request.changeId()).version())
                    .isEqualTo(7);
        });
    }

    @Test
    void rejectsIncompleteOrNonUuidV7Commands() {
        assertThat(command(1L, UuidV7.simpleString()).changeId())
                .hasSize(32);
        assertThatThrownBy(() -> command(null, UuidV7.string()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedVersion");
        assertThatThrownBy(() -> command(1L, UUID.randomUUID().toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUIDv7");
        assertThatThrownBy(() -> new GatewayDdcPublicationCommand(
                " ",
                "test",
                "default",
                "gateway.rules.active",
                "{}",
                1L,
                UuidV7.string(),
                "admin",
                Duration.ofSeconds(1)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("appCode");
    }

    @Test
    void preflightRequiresAnOnlineUnexpiredConfigClient() {
        RecordingClient client = new RecordingClient();
        client.targets = List.of(new DdcManagementConfigClientInstance(
                "gateway-engine-default",
                "test",
                "default",
                "engine-1",
                "lease-1",
                "127.0.0.1",
                18080,
                "CONFIG_CLIENT",
                "OFFLINE",
                Instant.now(),
                Instant.now(),
                Instant.now().plusSeconds(30),
                java.util.Map.of()
        ));
        GatewayDdcRulePublisher publisher =
                new GatewayDdcRulePublisher(client);

        assertThatThrownBy(() -> publisher.ensureReadyTarget(
                "gateway-engine-default",
                "test",
                "default"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("GATEWAY_RELEASE_NO_READY_TARGET");
    }

    private GatewayDdcPublicationCommand command(
            Long expectedVersion,
            String changeId) {
        return new GatewayDdcPublicationCommand(
                "gateway-engine-default",
                "test",
                "default",
                "gateway.rules.active",
                "{}",
                expectedVersion,
                changeId,
                "admin",
                Duration.ofSeconds(1)
        );
    }

    private static final class RecordingClient
            implements DdcManagementClient {

        private final List<DdcManagementPublishRequest> requests =
                new ArrayList<>();

        private List<DdcManagementConfigClientInstance> targets = List.of(
                new DdcManagementConfigClientInstance(
                        "gateway-engine-default",
                        "test",
                        "default",
                        "engine-1",
                        "lease-1",
                        "127.0.0.1",
                        18080,
                        "CONFIG_CLIENT",
                        "ONLINE",
                        Instant.now(),
                        Instant.now(),
                        Instant.now().plusSeconds(30),
                        java.util.Map.of()
                )
        );

        @Override
        public DdcManagementConfig upsert(
                DdcManagementConfigUpsertRequest request) {
            return null;
        }

        @Override
        public void delete(DdcManagementConfigDeleteRequest request) {
        }

        @Override
        public DdcManagementPublishResult publish(
                DdcManagementPublishRequest request) {
            requests.add(request);
            return new DdcManagementPublishResult(
                    request.changeId(),
                    DdcManagementPublishStatus.SUCCESS,
                    request.expectedVersion() + 1,
                    "checksum",
                    1,
                    List.of(),
                    null,
                    Instant.now(),
                    Instant.now(),
                    Instant.now()
            );
        }

        @Override
        public DdcManagementPublishTask getPublishTask(String changeId) {
            return null;
        }

        @Override
        public DdcManagementPublishResult retry(String changeId) {
            return null;
        }

        @Override
        public List<DdcManagementConfigClientInstance> getConfigClients(
                DdcManagementInstanceQuery query) {
            return targets;
        }

        @Override
        public DdcManagementServiceCatalog getServiceKeys(
                DdcManagementServiceQuery query) {
            return null;
        }

        @Override
        public DdcManagementServiceSnapshot getInstances(
                DdcManagementServiceQuery query) {
            return null;
        }
    }
}
