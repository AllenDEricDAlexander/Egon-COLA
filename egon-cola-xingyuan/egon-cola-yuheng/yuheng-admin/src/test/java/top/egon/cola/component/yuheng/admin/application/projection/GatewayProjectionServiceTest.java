package top.egon.cola.component.yuheng.admin.runtime.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;
import top.egon.cola.component.yuheng.admin.release.repository.GatewayReleasePublicationRepository;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleasePublicationPO;
import top.egon.cola.component.yuheng.admin.release.domain.dto.GatewayPublicationScopeDTO;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayPublicationPhaseEnum;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayPublicationStatusEnum;
import top.egon.cola.component.tianshu.api.client.DdcManagementClient;
import top.egon.cola.component.tianshu.model.management.DdcManagementConfig;
import top.egon.cola.component.tianshu.model.management.DdcManagementConfigClientInstance;
import top.egon.cola.component.tianshu.model.management.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.tianshu.model.management.DdcManagementConfigQuery;
import top.egon.cola.component.tianshu.model.management.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.tianshu.model.management.DdcManagementInstanceQuery;
import top.egon.cola.component.tianshu.model.management.DdcManagementPublishRequest;
import top.egon.cola.component.tianshu.model.management.DdcManagementPublishResult;
import top.egon.cola.component.tianshu.model.management.DdcManagementPublishTask;
import top.egon.cola.component.tianshu.model.management.DdcManagementScopeBinding;
import top.egon.cola.component.tianshu.model.management.DdcManagementScopeQuery;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceKey;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.yuheng.admin.release.service.GatewayReleaseService;
import top.egon.cola.component.yuheng.admin.release.repository.GatewayReleaseRepository;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.yuheng.admin.group.domain.po.GatewayGroupPO;
import top.egon.cola.component.yuheng.admin.group.repository.GatewayGroupRepository;

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
    void requiresBothRolesEvenWhenEveryExistingNodeAcknowledgedTheRelease() {
        var api = roleNode("api-1", "API_RPC", Map.of());
        assertThat(roleProjection(List.of(api)).consistent()).isFalse();
        var mcp = roleNode("mcp-1", "MCP", Map.of());
        assertThat(roleProjection(List.of(api, mcp)).consistent()).isTrue();
        assertThat(roleProjection(List.of(api, mcp, roleNode("mcp-2", "MCP", Map.of()))))
                .extracting(value -> value.engineNodeCount(), value -> value.readyEngineNodeCount(),
                        value -> value.consistent()).containsExactly(3, 3L, true);
        assertThat(roleProjection(List.of()).consistent()).isFalse();
    }

    @Test
    void classifiesMissingAndUnknownRolesBeforeReleaseSkew() {
        for (String role : new String[]{null, "", " ", "COMBINED", "api_rpc"}) {
            var projection = roleProjection(List.of(roleNode("node", role, Map.of("activeReleaseId", "old"))));
            assertThat(projection.consistent()).isFalse();
            assertThat(projection.readyEngineNodeCount()).isZero();
            assertThat(projection.nodes()).singleElement().extracting(value -> value.reason())
                    .isEqualTo(role == null || role.isBlank() ? "ROLE_MISSING" : "ROLE_UNKNOWN");
        }
    }

    @Test
    void ignoresOfflineUnknownNodesWhenBothOnlineRolesAreComplete() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        var offline = new DdcManagementConfigClientInstance("infra", "test", "ge", "old-node",
                "old-lease", "127.0.0.1", 18080, "CONFIG_CLIENT", "OFFLINE",
                now.minusSeconds(60), now.minusSeconds(30), now.plusSeconds(30), Map.of());
        var projection = roleProjection(List.of(roleNode("api", "API_RPC", Map.of()),
                roleNode("mcp", "MCP", Map.of()), offline));
        assertThat(projection.consistent()).isTrue();
        assertThat(projection.engineNodeCount()).isEqualTo(2);
    }

    @Test
    void roleCompletenessNeverHidesReleaseVersionChecksumOrAckSkew() {
        for (var mismatch : List.of(
                Map.entry("activeReleaseId", "RELEASE_MISMATCH"),
                Map.entry("activeRuleVersion", "VERSION_MISMATCH"),
                Map.entry("activeRuleChecksum", "CHECKSUM_MISMATCH"),
                Map.entry("lastApplyStatus", "APPLY_NOT_ACKED"),
                Map.entry("lastAckAt", "APPLY_NOT_ACKED"))) {
            var projection = roleProjection(List.of(roleNode("api", "API_RPC", Map.of()),
                    roleNode("mcp", "MCP", Map.of(mismatch.getKey(), "invalid"))));
            assertThat(projection.consistent()).isFalse();
            assertThat(projection.readyEngineNodeCount()).isEqualTo(1);
            assertThat(projection.nodes().getLast().reason()).isEqualTo(mismatch.getValue());
        }
    }

    private top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayRuntimeConsistencyVO roleProjection(
            List<DdcManagementConfigClientInstance> nodes) {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        var groups = mock(GatewayGroupRepository.class);
        var releases = mock(GatewayReleaseService.class);
        when(groups.findByIdAndDeletedFalse("group-1")).thenReturn(Optional.of(new GatewayGroupPO(
                "group-1", "edge", "Edge", "test", "yuheng", null, "admin", now)));
        var target = new top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetPO(
                "old-node", "old-lease", "SUCCESS", 12L, "artifact-sha", null, now.minusSeconds(5), GatewayEngineRoleEnum.API_RPC);
        when(releases.history("group-1")).thenReturn(List.of(release("release-1", target, now)));
        return projectionService(groups, releases, new StubClient(now, null, null, nodes),
                Clock.fixed(now, ZoneOffset.UTC)).runtimeConsistency("group-1");
    }

    private DdcManagementConfigClientInstance roleNode(String id, String role, Map<String, String> overrides) {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        var metadata = new java.util.LinkedHashMap<>(Map.of(
                "activeReleaseId", "release-1", "activeRuleVersion", "12",
                "activeRuleChecksum", "artifact-sha", "lastApplyStatus", "ACK_SUCCESS",
                "lastAckAt", now.minusSeconds(4).toString()));
        if (role != null) {
            metadata.put("yuheng.engine.role", role);
        }
        metadata.putAll(overrides);
        return new DdcManagementConfigClientInstance("infra", "test", "MCP".equals(role) ? "gme" : "ge", id, "lease-" + id,
                "127.0.0.1", 18080, "CONFIG_CLIENT", "ONLINE",
                now.minusSeconds(30), now.minusSeconds(2), now.plusSeconds(30), metadata);
    }

    @Test
    void preservesOptionalFiltersWhenListingProviderServices() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        DdcManagementClient client = mock(DdcManagementClient.class);
        when(client.getServiceKeys(any())).thenReturn(
                new DdcManagementServiceCatalog(0, now, List.of())
        );
        GatewayProjectionService service = projectionService(
                mock(GatewayGroupRepository.class),
                mock(GatewayReleaseService.class),
                client,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        service.services(new top.egon.cola.component.yuheng.admin.runtime.domain.dto.GatewayProviderQueryDTO(
                "test-biz",
                "orders",
                "test",
                "yuheng",
                null,
                null,
                null,
                null,
                null
        ));

        verify(client).getServiceKeys(new DdcManagementServiceQuery(
                "test-biz",
                "yuheng",
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
        GatewayProjectionService service = projectionService(
                mock(GatewayGroupRepository.class),
                mock(GatewayReleaseService.class),
                client,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        var projection = service.instances(new top.egon.cola.component.yuheng.admin.runtime.domain.dto.GatewayProviderQueryDTO(
                "test-biz",
                "orders",
                "test",
                "yuheng",
                null,
                "RPC",
                "orders-rpc",
                null,
                null
        ));

        assertThat(projection.value()).isEmpty();
        verify(client).getInstances(new DdcManagementServiceQuery(
                "test-biz",
                "yuheng",
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
        var groups = mock(top.egon.cola.component.yuheng.admin.group.repository
                .GatewayGroupRepository.class);
        when(groups
                .findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc(
                        "test",
                        "yuheng"
                )).thenReturn(List.of());
        GatewayProjectionService service = projectionService(
                groups,
                mock(top.egon.cola.component.yuheng.admin.release.service
                        .GatewayReleaseService.class),
                client,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        var projection = service.instances(
                "test-biz", "orders", "test", "yuheng"
        );

        assertThat(projection.stale()).isFalse();
        assertThat(projection.value()).extracting(
                top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayProviderInstanceVO::protocol
        ).containsExactly("http", "grpc");
        assertThat(projection.value()).extracting(
                top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayProviderInstanceVO::status
        ).containsOnly("ONLINE");
        assertThat(projection.value().getFirst().weight()).isEqualTo(80);
        assertThat(projection.value().getFirst().definitionSetId())
                .isEqualTo("definition-http");
        assertThat(service.scopeCounts(
                "test-biz", "orders", "test", "yuheng"
        ))
                .extracting(
                        top.egon.cola.component.yuheng.admin.runtime.service.GatewayProjectionCounts
                                ::activeProviders,
                        top.egon.cola.component.yuheng.admin.runtime.service.GatewayProjectionCounts
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
                "yuheng",
                null,
                "admin",
                now
        );
        when(groups.findByIdAndDeletedFalse("group-1"))
                .thenReturn(java.util.Optional.of(group));
        top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetPO target =
                new top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetPO(
                        "engine-1",
                        "lease-1",
                        "SUCCESS",
                        12L,
                        "artifact-sha",
                        null,
                        now.minusSeconds(5), GatewayEngineRoleEnum.API_RPC
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
                                "yuheng.engine.role", "API_RPC",
                                "activeReleaseId", "release-1",
                                "activeRuleVersion", "12",
                                "activeRuleChecksum", "artifact-sha",
                                "lastApplyStatus", "ACK_SUCCESS",
                                "lastAckAt", now.minusSeconds(4).toString()
                        )
                );
        GatewayProjectionService service = projectionService(
                groups,
                releases,
                new StubClient(now, null, null, List.of(engine)),
                Clock.fixed(now, ZoneOffset.UTC)
        );

        var consistency = service.runtimeConsistency("group-1");

        assertThat(consistency.consistent()).isFalse();
        assertThat(consistency.readyEngineNodeCount()).isEqualTo(1);
        assertThat(consistency.nodes()).singleElement()
                .extracting(
                        top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::status,
                        top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::reason
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
                "yuheng",
                null,
                "admin",
                now
        );
        when(groups.findByIdAndDeletedFalse("group-1"))
                .thenReturn(java.util.Optional.of(group));
        top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetPO historicalTarget =
                new top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetPO(
                        "engine-1",
                        "lease-1",
                        "SUCCESS",
                        12L,
                        "artifact-sha",
                        null,
                        now.minusSeconds(5), GatewayEngineRoleEnum.API_RPC
                );
        when(releases.history("group-1")).thenReturn(List.of(
                release("release-1", historicalTarget, now)
        ));
        Map<String, String> currentMetadata = Map.of(
                "yuheng.engine.role", "API_RPC",
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
        GatewayProjectionService service = projectionService(
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

        assertThat(consistency.consistent()).isFalse();
        assertThat(consistency.readyEngineNodeCount()).isEqualTo(2);
        assertThat(consistency.nodes()).extracting(
                top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::status,
                top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::reason
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
                "yuheng",
                null,
                "admin",
                now
        );
        when(groups.findByIdAndDeletedFalse("group-1"))
                .thenReturn(java.util.Optional.of(group));
        top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetPO target =
                new top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetPO(
                        "engine-current",
                        "lease-current",
                        "SUCCESS",
                        12L,
                        "artifact-sha",
                        null,
                        now.minusSeconds(5), GatewayEngineRoleEnum.API_RPC
                );
        when(releases.history("group-1")).thenReturn(List.of(
                release("release-1", target, now)
        ));
        Map<String, String> currentMetadata = Map.of(
                "yuheng.engine.role", "API_RPC",
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
        GatewayProjectionService service = projectionService(
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
        assertThat(consistency.consistent()).isFalse();
        assertThat(consistency.nodes()).singleElement()
                .extracting(
                        top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO
                                ::instanceId,
                        top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::status
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
                "yuheng",
                null,
                "admin",
                now
        );
        when(groups.findByIdAndDeletedFalse("group-1"))
                .thenReturn(java.util.Optional.of(group));
        top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetPO target =
                new top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetPO(
                        "engine-1",
                        "lease-1",
                        "SUCCESS",
                        12L,
                        "artifact-sha",
                        null,
                        now.minusSeconds(5), GatewayEngineRoleEnum.API_RPC
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
                                "yuheng.engine.role", "API_RPC",
                                "activeReleaseId", "release-0",
                                "activeRuleVersion", "11",
                                "activeRuleChecksum", "old-sha",
                                "lastApplyStatus", "ACK_SUCCESS"
                        )
                );
        GatewayProjectionService service = projectionService(
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
                        top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::status,
                        top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO::reason
                )
                .containsExactly("INCONSISTENT", "RELEASE_MISMATCH");
    }

    private top.egon.cola.component.yuheng.admin.release.domain.vo.GatewayReleaseVO release(
            String releaseId,
            top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetPO target,
            Instant now) {
        return new top.egon.cola.component.yuheng.admin.release.domain.vo.GatewayReleaseVO(
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
                List.of(new top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseAttemptPO(
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


    private GatewayProjectionService projectionService(GatewayGroupRepository groups, GatewayReleaseService releases,
                                                        DdcManagementClient client, Clock clock) {
        var beans = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        if (client != null) {
            beans.registerSingleton("ddcManagementClient", client);
        }
        when(releases.artifactSha256("release-1")).thenReturn(Optional.of("artifact-sha"));
        var publications = mock(GatewayReleasePublicationRepository.class);
        when(publications.findAttemptMetadata("release-1", 1)).thenReturn(List.of(
                activation(GatewayEngineRoleEnum.API_RPC, 12L, clock.instant()),
                activation(GatewayEngineRoleEnum.MCP, 12L, clock.instant())));
        var service = new GatewayProjectionService(groups, releases,
                beans.getBeanProvider(DdcManagementClient.class), clock,
                new top.egon.cola.component.yuheng.admin.config.GatewayAdminProperties(),
                new GatewayEngineRoleConsistencyStrategy(), publications);
        service.validateBootstrap();
        return service;
    }

    private GatewayReleasePublicationPO activation(GatewayEngineRoleEnum role, long version, Instant now) {
        return new GatewayReleasePublicationPO("release-1", 1, role.ordinal(),
                GatewayPublicationPhaseEnum.ACTIVATION, "yuheng.rules.active", null, "sha",
                version - 1, "change-" + role, version, GatewayPublicationStatusEnum.SUCCESS,
                null, null, now.minusSeconds(10), now.minusSeconds(5),
                new GatewayPublicationScopeDTO("infra", "test", role == GatewayEngineRoleEnum.API_RPC ? "ge" : "gme", role));
    }

    @Test
    void acceptsIndependentDdcVersionsAndLaterYamlReapplyForTheSameArtifact() {
        var projection = roleProjection(List.of(roleNode("api", "API_RPC", Map.of()),
                roleNode("mcp", "MCP", Map.of("activeRuleVersion", "27"))));
        assertThat(projection.consistent()).isTrue();
        assertThat(projection.readyEngineNodeCount()).isEqualTo(2);
    }

    @Test
    void rejectsAValidRoleReportedFromTheOtherRolesScope() {
        var wrongScope = roleNode("mcp", "API_RPC", Map.of("yuheng.engine.role", "MCP"));
        var projection = roleProjection(List.of(roleNode("api", "API_RPC", Map.of()), wrongScope));
        assertThat(projection.consistent()).isFalse();
        assertThat(projection.nodes().getLast().reason()).isEqualTo("TARGET_SCOPE_MISMATCH");
    }

    @Test
    void rejectsVersionBelowTheRolesActivationEvenWithMatchingReleaseAndChecksum() {
        var projection = roleProjection(List.of(roleNode("api", "API_RPC", Map.of()),
                roleNode("mcp", "MCP", Map.of("activeRuleVersion", "11"))));
        assertThat(projection.consistent()).isFalse();
        assertThat(projection.nodes().getLast().reason()).isEqualTo("VERSION_MISMATCH");
    }

    private record StubClient(
            Instant now,
            DdcManagementServiceKey http,
            DdcManagementServiceKey rpc,
            List<DdcManagementConfigClientInstance> engines
    ) implements DdcManagementClient {

        @Override
        public Optional<top.egon.cola.component.tianshu.model.management.DdcManagementBiz>
                getBiz(top.egon.cola.component.tianshu.model.management.DdcManagementBizLookup lookup) {
            return Optional.empty();
        }

        @Override
        public List<top.egon.cola.component.tianshu.model.management.DdcManagementBiz>
                listBizs(top.egon.cola.component.tianshu.model.management.DdcManagementBizQuery query) {
            return List.of();
        }

        @Override
        public Optional<top.egon.cola.component.tianshu.model.management.DdcManagementApp>
                getApp(String ddcApplicationId) {
            return Optional.empty();
        }

        @Override
        public List<top.egon.cola.component.tianshu.model.management.DdcManagementApp>
                listApps(top.egon.cola.component.tianshu.model.management.DdcManagementAppQuery query) {
            return List.of();
        }

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
                                    "yuheng.weight", "80",
                                    "yuheng.definition-set-id",
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
            return engines.stream().filter(engine -> engine.bizCode().equals(query.bizCode())
                    && engine.env().equals(query.env()) && engine.appCode().equals(query.appCode())).toList();
        }

        @Override
        public List<DdcManagementScopeBinding> getScopeBindings(
                DdcManagementScopeQuery query) {
            return List.of();
        }
    }
}
