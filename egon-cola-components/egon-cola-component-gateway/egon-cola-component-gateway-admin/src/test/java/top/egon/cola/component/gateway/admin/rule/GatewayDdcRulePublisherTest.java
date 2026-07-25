package top.egon.cola.component.gateway.admin.rule;

import org.junit.jupiter.api.Test;
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
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayDdcRulePublisherTest {

    @Test
    void publishesChunksBeforeTheOnlyActivationKey() {
        GatewayRuleCompiler compiler = new GatewayRuleCompiler(
                new GatewayRuleCanonicalizer()
        );
        CompiledGatewayRelease release = compiler.compile(
                "release-large",
                Instant.parse("2026-07-25T00:00:00Z"),
                content("x".repeat(GatewayRuleCompiler.INLINE_LIMIT_BYTES + 10))
        );
        RecordingClient client = new RecordingClient();
        GatewayDdcRulePublisher publisher = new GatewayDdcRulePublisher(
                client,
                Duration.ofSeconds(10)
        );

        GatewayRulePublishResult result = publisher.publish(
                release,
                3L,
                "change-1",
                "tester"
        );

        assertEquals(
                release.activation().chunks().size(),
                result.chunkResults().size()
        );
        assertEquals(
                GatewayDdcRulePublisher.ACTIVE_CONFIG_KEY,
                client.requests.getLast().configKey()
        );
        assertEquals(3L, client.requests.getLast().expectedVersion());
        assertEquals(
                "gateway-engine-orders",
                client.requests.getLast().appCode()
        );
    }

    private GatewayRuleContent content(String schema) {
        GatewayProviderServiceRef service = new GatewayProviderServiceRef(
                "local",
                "default",
                GatewayProtocol.HTTP,
                "orders",
                "default",
                "v1",
                "http"
        );
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "orders",
                "orders",
                GatewayProtocol.HTTP,
                "GET /orders",
                schema,
                "{}",
                true,
                service,
                "TRANSPARENT",
                Set.of(),
                Map.of(),
                false
        );
        GatewayRuntimeRoute route = new GatewayRuntimeRoute(
                "orders",
                "orders",
                "api.example.com",
                "GET",
                "/orders",
                Set.of(AccessZone.PUBLIC),
                0,
                true
        );
        return new GatewayRuleContent(
                "group-1",
                "orders",
                "local",
                "default",
                List.of(operation),
                List.of(route),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private static final class RecordingClient
            implements DdcManagementClient {

        private final List<DdcManagementPublishRequest> requests =
                new ArrayList<>();

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
                    1L,
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
            return List.of(new DdcManagementConfigClientInstance(
                    query.appCode(),
                    query.env(),
                    query.namespace(),
                    "engine-1",
                    "lease-1",
                    "127.0.0.1",
                    18080,
                    "CONFIG_CLIENT",
                    "REGISTERED",
                    Instant.now(),
                    Instant.now(),
                    Instant.now().plusSeconds(30)
            ));
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
