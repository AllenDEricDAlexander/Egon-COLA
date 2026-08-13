package top.egon.cola.component.gateway.admin.runtime.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.model.management.DdcManagementConfig;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTask;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeBinding;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceKey;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.gateway.admin.release.service.GatewayReleaseService;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleaseRepository;
import top.egon.cola.component.gateway.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.group.domain.po.GatewayGroupPO;
import top.egon.cola.component.gateway.admin.group.repository.GatewayGroupRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayProjectionServiceTest {

    @Test
    void preservesOptionalFiltersWhenListingProviderServices() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        DdcManagementClient client = mock(DdcManagementClient.class);
        when(client.getServiceKeys(any())).thenReturn(
                new DdcManagementServiceCatalog(0, now, List.of())
        );
        GatewayProjectionService service = new GatewayProjectionService(
                mock(GatewayGroupRepository.class),
                mock(GatewayReleaseService.class),
                client,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        service.services(new top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO(
                "test-biz",
                "orders",
                "test",
                "gateway",
                null,
                null,
                null,
                null,
                null
        ));

        verify(client).getServiceKeys(new DdcManagementServiceQuery(
                "test-biz",
                "gateway",
                "test",
                "orders",
                null,
                null,
                null,
                null,
                null
        ));
    }

    @Test
    void adaptsAdminProtocolToDdcProviderQuery() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        DdcManagementClient client = mock(DdcManagementClient.class);
        when(client.getInstances(any())).thenReturn(
                new DdcManagementServiceSnapshot(null, 0, now, List.of())
        );
        GatewayProjectionService service = new GatewayProjectionService(
                mock(GatewayGroupRepository.class),
                mock(GatewayReleaseService.class),
                client,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        service.instances(new top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO(
                "test-biz",
                "orders",
                "test",
                "gateway",
                null,
                "RPC",
                "orders-rpc",
                null,
                null
        ));

        verify(client).getInstances(new DdcManagementServiceQuery(
                "test-biz",
                "gateway",
                "test",
                "orders",
                "RPC_PROVIDER",
                "grpc",
                "orders-rpc",
                null,
                null
        ));
    }

    @Test
    void flattensHttpAndRpcRegistryInstancesForAdminProjection() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        DdcManagementServiceKey http = new DdcManagementServiceKey(
                "test-biz",
                "test",
                "orders",
                "http-service-id",
                "HTTP_PROVIDER",
                "orders",
                "default",
                "1.0.0",
                "http"
        );
        DdcManagementServiceKey rpc = new DdcManagementServiceKey(
                "test-biz",
                "test",
                "orders",
                "rpc-service-id",
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
        var groups = mock(top.egon.cola.component.gateway.admin.group.repository
                .GatewayGroupRepository.class);
        when(groups
                .findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc(
                        "test",
                        "gateway"
                )).thenReturn(List.of());
        GatewayProjectionService service = new GatewayProjectionService(
                groups,
                mock(top.egon.cola.component.gateway.admin.release.service
                        .GatewayReleaseService.class),
                client,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        var projection = service.instances(
                "test-biz", "orders", "test", "gateway"
        );

        assertThat(projection.stale()).isFalse();
        assertThat(projection.value()).extracting(
                top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO::protocol
        ).containsExactly("http", "grpc");
        assertThat(projection.value()).extracting(
                top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO::status
        ).containsOnly("ONLINE");
        assertThat(projection.value().getFirst().weight()).isEqualTo(80);
        assertThat(projection.value().getFirst().definitionSetId())
                .isEqualTo("definition-http");
        assertThat(service.scopeCounts(
                "test-biz", "orders", "test", "gateway"
        ))
                .extracting(
                        top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts
                                ::activeProviders,
                        top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts
                                ::abnormalProviders
                )
                .containsExactly(2L, 0L);
    }

    @Test
    void reportsConsistencyOnlyWhenOnlineEngineMetadataMatchesReleaseAck() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        GatewayGroupRepository groups = mock(GatewayGroupRepository.class);
        GatewayReleaseService releases = mock(GatewayReleaseService.class);
        GatewayGroupPO group = new GatewayGroupPO(
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
        top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO target =
                new top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO(
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
                        "infra",
                        "test",
                        "ge",
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
                        top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::status,
                        top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::reason
                )
                .containsExactly("CONSISTENT", null);
    }

    @Test
    void acceptsCurrentMetadataFromEnginesRegisteredAfterTheRelease() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        GatewayGroupRepository groups = mock(GatewayGroupRepository.class);
        GatewayReleaseService releases = mock(GatewayReleaseService.class);
        GatewayGroupPO group = new GatewayGroupPO(
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
        top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO historicalTarget =
                new top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO(
                        "engine-1",
                        "lease-1",
                        "SUCCESS",
                        12L,
                        "artifact-sha",
                        null,
                        now.minusSeconds(5)
                );
        when(releases.history("group-1")).thenReturn(List.of(
                release("release-1", historicalTarget, now)
        ));
        Map<String, String> currentMetadata = Map.of(
                "activeReleaseId", "release-1",
                "activeRuleVersion", "12",
                "activeRuleChecksum", "artifact-sha",
                "lastApplyStatus", "ACK_SUCCESS",
                "lastAckAt", now.minusSeconds(1).toString()
        );
        DdcManagementConfigClientInstance renewedLease =
                new DdcManagementConfigClientInstance(
                        "infra",
                        "test",
                        "ge",
                        "engine-1",
                        "lease-2",
                        "127.0.0.1",
                        18080,
                        "CONFIG_CLIENT",
                        "ONLINE",
                        now.minusSeconds(3),
                        now.minusSeconds(1),
                        now.plusSeconds(30),
                        currentMetadata
                );
        DdcManagementConfigClientInstance scaledNode =
                new DdcManagementConfigClientInstance(
                        "infra",
                        "test",
                        "ge",
                        "engine-2",
                        "lease-1",
                        "127.0.0.2",
                        18080,
                        "CONFIG_CLIENT",
                        "ONLINE",
                        now.minusSeconds(3),
                        now.minusSeconds(1),
                        now.plusSeconds(30),
                        currentMetadata
                );
        GatewayProjectionService service = new GatewayProjectionService(
                groups,
                releases,
                new StubClient(
                        now,
                        null,
                        null,
                        List.of(renewedLease, scaledNode)
                ),
                Clock.fixed(now, ZoneOffset.UTC)
        );

        var consistency = service.runtimeConsistency("group-1");

        assertThat(consistency.consistent()).isTrue();
        assertThat(consistency.readyEngineNodeCount()).isEqualTo(2);
        assertThat(consistency.nodes()).extracting(
                top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::status,
                top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::reason
        ).containsExactly(
                org.assertj.core.groups.Tuple.tuple("CONSISTENT", null),
                org.assertj.core.groups.Tuple.tuple("CONSISTENT", null)
        );
    }

    @Test
    void ignoresExpiredHistoricalEnginesWhenCheckingRuntimeConsistency() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        GatewayGroupRepository groups = mock(GatewayGroupRepository.class);
        GatewayReleaseService releases = mock(GatewayReleaseService.class);
        GatewayGroupPO group = new GatewayGroupPO(
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
        top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO target =
                new top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO(
                        "engine-current",
                        "lease-current",
                        "SUCCESS",
                        12L,
                        "artifact-sha",
                        null,
                        now.minusSeconds(5)
                );
        when(releases.history("group-1")).thenReturn(List.of(
                release("release-1", target, now)
        ));
        Map<String, String> currentMetadata = Map.of(
                "activeReleaseId", "release-1",
                "activeRuleVersion", "12",
                "activeRuleChecksum", "artifact-sha",
                "lastApplyStatus", "ACK_SUCCESS",
                "lastAckAt", now.minusSeconds(1).toString()
        );
        DdcManagementConfigClientInstance expired =
                new DdcManagementConfigClientInstance(
                        "infra", "test", "ge", "engine-expired",
                        "lease-expired", "127.0.0.2", 18080,
                        "CONFIG_CLIENT", "ONLINE",
                        now.minusSeconds(90), now.minusSeconds(60),
                        now.minusSeconds(30), Map.of()
                );
        DdcManagementConfigClientInstance current =
                new DdcManagementConfigClientInstance(
                        "infra", "test", "ge", "engine-current",
                        "lease-current", "127.0.0.1", 18080,
                        "CONFIG_CLIENT", "ONLINE",
                        now.minusSeconds(30), now.minusSeconds(2),
                        now.plusSeconds(30), currentMetadata
                );
        GatewayProjectionService service = new GatewayProjectionService(
                groups,
                releases,
                new StubClient(
                        now,
                        null,
                        null,
                        List.of(expired, current)
                ),
                Clock.fixed(now, ZoneOffset.UTC)
        );

        var consistency = service.runtimeConsistency("group-1");

        assertThat(consistency.engineNodeCount()).isEqualTo(1);
        assertThat(consistency.readyEngineNodeCount()).isEqualTo(1);
        assertThat(consistency.consistent()).isTrue();
        assertThat(consistency.nodes()).singleElement()
                .extracting(
                        top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO
                                ::instanceId,
                        top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::status
                )
                .containsExactly("engine-current", "CONSISTENT");
    }

    @Test
    void identifiesOnlineEngineWithStaleRelease() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        GatewayGroupRepository groups = mock(GatewayGroupRepository.class);
        GatewayReleaseService releases = mock(GatewayReleaseService.class);
        GatewayGroupPO group = new GatewayGroupPO(
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
        top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO target =
                new top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO(
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
                        "infra",
                        "test",
                        "ge",
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
                        top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::status,
                        top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::reason
                )
                .containsExactly("INCONSISTENT", "RELEASE_MISMATCH");
    }

    private top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO release(
            String releaseId,
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO target,
            Instant now) {
        return new top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO(
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
                List.of(new top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO(
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
        public Optional<DdcManagementConfig> findConfig(
                DdcManagementConfigQuery query) {
            return Optional.empty();
        }

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
                            "UP",
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

        @Override
        public List<DdcManagementScopeBinding> getScopeBindings(
                DdcManagementScopeQuery query) {
            return List.of();
        }
    }
}
