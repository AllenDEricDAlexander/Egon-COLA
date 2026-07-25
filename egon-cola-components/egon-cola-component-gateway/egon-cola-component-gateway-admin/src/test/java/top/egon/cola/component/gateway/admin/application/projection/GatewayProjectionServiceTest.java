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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
                "HTTP"
        );
        DdcManagementServiceKey rpc = new DdcManagementServiceKey(
                "test",
                "gateway",
                "RPC_PROVIDER",
                "orders-rpc",
                "default",
                "1.0.0",
                "RPC"
        );
        DdcManagementClient client = new StubClient(now, http, rpc);
        GatewayProjectionService service = new GatewayProjectionService(
                mock(top.egon.cola.component.gateway.admin.infrastructure
                        .persistence.GatewayGroupRepository.class),
                mock(top.egon.cola.component.gateway.admin.application
                        .release.GatewayReleaseService.class),
                client,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        var projection = service.instances("test", "gateway");

        assertThat(projection.stale()).isFalse();
        assertThat(projection.value()).extracting(
                GatewayProjectionService.ProviderInstanceProjection::protocol
        ).containsExactly("HTTP", "RPC");
        assertThat(projection.value().getFirst().weight()).isEqualTo(80);
        assertThat(projection.value().getFirst().definitionSetId())
                .isEqualTo("definition-http");
    }

    private record StubClient(
            Instant now,
            DdcManagementServiceKey http,
            DdcManagementServiceKey rpc
    ) implements DdcManagementClient {

        @Override
        public DdcManagementServiceCatalog getServiceKeys(
                DdcManagementServiceQuery query) {
            DdcManagementServiceKey key =
                    "HTTP".equals(query.protocol()) ? http : rpc;
            return new DdcManagementServiceCatalog(1, now, List.of(key));
        }

        @Override
        public DdcManagementServiceSnapshot getInstances(
                DdcManagementServiceQuery query) {
            DdcManagementServiceKey key =
                    "HTTP".equals(query.protocol()) ? http : rpc;
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
                                    "gateway.definition-set",
                                    "definition-" + query.protocol()
                                            .toLowerCase()
                            ),
                            "REGISTERED",
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
            throw new UnsupportedOperationException();
        }
    }
}
