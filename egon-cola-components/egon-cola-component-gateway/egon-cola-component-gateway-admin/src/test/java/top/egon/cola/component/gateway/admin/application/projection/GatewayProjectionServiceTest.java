package top.egon.cola.component.gateway.admin.application.projection;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceKey;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleaseService;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleaseStore;
import top.egon.cola.component.gateway.admin.domain.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayProjectionServiceTest {

    @Test
    void flattensHttpAndRpcRegistryInstancesForAdminProjection() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        DdcManagementServiceKey http = new DdcManagementServiceKey(
                "test",
                "gateway",
                "HTTP_PROVIDER",
                "orders",
                "default",
                "1.0.0",
                "http"
        );
        DdcManagementServiceKey rpc = new DdcManagementServiceKey(
                "test",
                "gateway",
                "RPC_PROVIDER",
                "orders-rpc",
                "default",
                "1.0.0",
                "grpc"
        );
        DdcManagementClient client = new StubClient(
                now,
                http,
                rpc,
                List.of()
        );
        var groups = mock(top.egon.cola.component.gateway.admin.infrastructure
                .persistence.GatewayGroupRepository.class);
        when(groups
                .findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc(
                        "test",
                        "gateway"
                )).thenReturn(List.of());
        GatewayProjectionService service = new GatewayProjectionService(
                groups,
                mock(top.egon.cola.component.gateway.admin.application
                        .release.GatewayReleaseService.class),
                client,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        var projection = service.instances("test", "gateway");

        assertThat(projection.stale()).isFalse();
        assertThat(projection.value()).extracting(
                GatewayProjectionService.ProviderInstanceProjection::protocol
        ).containsExactly("http", "grpc");
        assertThat(projection.value().getFirst().weight()).isEqualTo(80);
        assertThat(projection.value().getFirst().definitionSetId())
                .isEqualTo("definition-http");
        assertThat(service.scopeCounts("test", "gateway"))
                .extracting(
                        GatewayProjectionService.ProjectionCounts
                                ::activeProviders,
                        GatewayProjectionService.ProjectionCounts
                                ::abnormalProviders
                )
                .containsExactly(2L, 0L);
    }

    @Test
    void reportsConsistencyOnlyWhenOnlineEngineMetadataMatchesReleaseAck() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        GatewayGroupRepository groups = mock(GatewayGroupRepository.class);
        GatewayReleaseService releases = mock(GatewayReleaseService.class);
        GatewayGroupEntity group = new GatewayGroupEntity(
                "group-1",
                "edge",
                "Edge",
                "test",
                "gateway",
                null,
                "admin",
                now
        );
        when(groups.findByIdAndDeletedFalse("group-1"))
                .thenReturn(java.util.Optional.of(group));
        GatewayReleaseStore.TargetRecord target =
                new GatewayReleaseStore.TargetRecord(
                        "engine-1",
                        "lease-1",
                        "SUCCESS",
                        12L,
                        "artifact-sha",
                        null,
                        now.minusSeconds(5)
                );
        when(releases.history("group-1")).thenReturn(List.of(
                release("release-1", target, now)
        ));
        DdcManagementConfigClientInstance engine =
                new DdcManagementConfigClientInstance(
                        "gateway-engine-edge",
                        "test",
                        "gateway",
                        "engine-1",
                        "lease-1",
                        "127.0.0.1",
                        18080,
                        "CONFIG_CLIENT",
                        "ONLINE",
                        now.minusSeconds(30),
                        now.minusSeconds(2),
                        now.plusSeconds(30),
                        Map.of(
                                "activeReleaseId", "release-1",
                                "activeRuleVersion", "12",
                                "activeRuleChecksum", "artifact-sha",
                                "lastApplyStatus", "ACK_SUCCESS",
                                "lastAckAt", now.minusSeconds(4).toString()
                        )
                );
        GatewayProjectionService service = new GatewayProjectionService(
                groups,
                releases,
                new StubClient(now, null, null, List.of(engine)),
                Clock.fixed(now, ZoneOffset.UTC)
        );

        var consistency = service.runtimeConsistency("group-1");

        assertThat(consistency.consistent()).isTrue();
        assertThat(consistency.readyEngineNodeCount()).isEqualTo(1);
        assertThat(consistency.nodes()).singleElement()
                .extracting(
                        GatewayProjectionService.EngineNodeConsistency::status,
                        GatewayProjectionService.EngineNodeConsistency::reason
                )
                .containsExactly("CONSISTENT", null);
    }

    @Test
    void identifiesOnlineEngineWithStaleRelease() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        GatewayGroupRepository groups = mock(GatewayGroupRepository.class);
        GatewayReleaseService releases = mock(GatewayReleaseService.class);
        GatewayGroupEntity group = new GatewayGroupEntity(
                "group-1",
                "edge",
                "Edge",
                "test",
                "gateway",
                null,
                "admin",
                now
        );
        when(groups.findByIdAndDeletedFalse("group-1"))
                .thenReturn(java.util.Optional.of(group));
        GatewayReleaseStore.TargetRecord target =
                new GatewayReleaseStore.TargetRecord(
                        "engine-1",
                        "lease-1",
                        "SUCCESS",
                        12L,
                        "artifact-sha",
                        null,
                        now.minusSeconds(5)
                );
        when(releases.history("group-1")).thenReturn(List.of(
                release("release-1", target, now)
        ));
        DdcManagementConfigClientInstance engine =
                new DdcManagementConfigClientInstance(
                        "gateway-engine-edge",
                        "test",
                        "gateway",
                        "engine-1",
                        "lease-1",
                        "127.0.0.1",
                        18080,
                        "CONFIG_CLIENT",
                        "ONLINE",
                        now.minusSeconds(30),
                        now.minusSeconds(2),
                        now.plusSeconds(30),
                        Map.of(
                                "activeReleaseId", "release-0",
                                "activeRuleVersion", "11",
                                "activeRuleChecksum", "old-sha",
                                "lastApplyStatus", "ACK_SUCCESS"
                        )
                );
        GatewayProjectionService service = new GatewayProjectionService(
                groups,
                releases,
                new StubClient(now, null, null, List.of(engine)),
                Clock.fixed(now, ZoneOffset.UTC)
        );

        var consistency = service.runtimeConsistency("group-1");

        assertThat(consistency.consistent()).isFalse();
        assertThat(consistency.readyEngineNodeCount()).isZero();
        assertThat(consistency.nodes()).singleElement()
                .extracting(
                        GatewayProjectionService.EngineNodeConsistency::status,
                        GatewayProjectionService.EngineNodeConsistency::reason
                )
                .containsExactly("INCONSISTENT", "RELEASE_MISMATCH");
    }

    private GatewayReleaseService.ReleaseView release(
            String releaseId,
            GatewayReleaseStore.TargetRecord target,
            Instant now) {
        return new GatewayReleaseService.ReleaseView(
                releaseId,
                "group-1",
                1,
                null,
                null,
                GatewayReleaseStatus.SUCCESS,
                false,
                "change-1",
                Map.of(),
                Map.of(),
                "test",
                now.minusSeconds(10),
                now.minusSeconds(5),
                List.of(new GatewayReleaseStore.AttemptRecord(
                        1,
                        "SUCCESS",
                        "change-1",
                        now.minusSeconds(10),
                        now.minusSeconds(5),
                        null,
                        null,
                        List.of(target)
                ))
        );
    }

    private record StubClient(
            Instant now,
            DdcManagementServiceKey http,
            DdcManagementServiceKey rpc,
            List<DdcManagementConfigClientInstance> engines
    ) implements DdcManagementClient {

        @Override
        public DdcManagementServiceCatalog getServiceKeys(
                DdcManagementServiceQuery query) {
            if ("https".equals(query.protocol())) {
                return new DdcManagementServiceCatalog(
                        1,
                        now,
                        List.of()
                );
            }
            DdcManagementServiceKey key =
                    "http".equals(query.protocol()) ? http : rpc;
            return new DdcManagementServiceCatalog(1, now, List.of(key));
        }

        @Override
        public DdcManagementServiceSnapshot getInstances(
                DdcManagementServiceQuery query) {
            DdcManagementServiceKey key =
                    "http".equals(query.protocol()) ? http : rpc;
            return new DdcManagementServiceSnapshot(
                    key,
                    1,
                    now,
                    List.of(new DdcManagementServiceInstance(
                            key.serviceName() + "-1",
                            "lease-" + key.serviceName(),
                            "127.0.0.1",
                            18090,
                            false,
                            Map.of(
                                    "gateway.weight", "80",
                                    "gateway.definition-set-id",
                                    "definition-" + query.protocol()
                            ),
                            "ONLINE",
                            now.minusSeconds(10),
                            now.minusSeconds(1),
                            now.plusSeconds(30)
                    ))
            );
        }

        @Override
        public DdcManagementConfig upsert(
                DdcManagementConfigUpsertRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(DdcManagementConfigDeleteRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcManagementPublishResult publish(
                DdcManagementPublishRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcManagementPublishTask getPublishTask(String changeId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcManagementPublishResult retry(String changeId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DdcManagementConfigClientInstance> getConfigClients(
                DdcManagementInstanceQuery query) {
            return engines;
        }
    }
}
