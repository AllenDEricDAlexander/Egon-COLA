package top.egon.cola.component.rpc.ddc.mapping;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.management.DdcManagementConfig;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTarget;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTask;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeBinding;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceKey;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.ddc.model.management.DdcResourceAdmissionRevocationRequest;
import top.egon.cola.component.ddc.model.management.DdcResourceAdmissionRevocationResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DdcManagementProtoMapperTest {

    private final DdcManagementProtoMapper mapper =
            new DdcManagementProtoMapper(
                    new DdcCommonProtoMapper(4 * 1024 * 1024),
                    1024 * 1024
            );

    @Test
    void roundTripsConfigQueriesAndWriteRequestsWithOptionalValues() {
        DdcManagementConfigQuery query = new DdcManagementConfigQuery(
                "retail", "prod", "order");
        assertThat(mapper.fromFindRequest(mapper.toFindRequest(query)))
                .isEqualTo(query);

        DdcManagementConfigUpsertRequest upsert =
                new DdcManagementConfigUpsertRequest(
                        "retail", "prod", "order", "application.yml",
                        "feature:\n  enabled: true\n", "YAML", "enable flag",
                        6L, "requested-operator");
        assertThat(mapper.fromUpsertRequest(mapper.toUpsertRequest(upsert)))
                .isEqualTo(upsert);

        DdcManagementConfigDeleteRequest delete =
                new DdcManagementConfigDeleteRequest(
                        "retail", "prod", "order", 7L,
                        "requested-operator", "obsolete");
        assertThat(mapper.fromDeleteRequest(mapper.toDeleteRequest(delete)))
                .isEqualTo(delete);

        DdcManagementPublishRequest publish =
                new DdcManagementPublishRequest(
                        "retail", "prod", "order", "application.yml",
                        "feature:\n  enabled: true\n", "YAML", 7L,
                        "change-1", 5000L, "requested-operator");
        assertThat(mapper.fromPublishRequest(mapper.toPublishRequest(publish)))
                .isEqualTo(publish);

        DdcManagementPublishRequest optionalPublish =
                new DdcManagementPublishRequest(
                        "retail", "prod", "order", "application.yml",
                        "a: b\n", "YAML", null, null, null,
                        "requested-operator");
        assertThat(mapper.fromPublishRequest(
                mapper.toPublishRequest(optionalPublish)))
                .isEqualTo(optionalPublish);
    }

    @Test
    void roundTripsConfigAndAllPublicationLifecycleFields() {
        Instant now = Instant.parse("2026-08-09T08:00:00.123456Z");
        DdcManagementConfig config = new DdcManagementConfig(
                "retail", "prod", "order", "application.yml",
                "a: b\n", "YAML", 7L, true, false, now);
        assertThat(mapper.fromConfig(mapper.toConfig(config)))
                .isEqualTo(config);

        DdcManagementPublishTarget target = new DdcManagementPublishTarget(
                "instance-1", "lease-1", 7L, "SUCCESS", null, now);
        for (DdcManagementPublishStatus status
                : DdcManagementPublishStatus.values()) {
            DdcManagementPublishResult result = new DdcManagementPublishResult(
                    "change-1", status, 7L, "abc", 1, List.of(target), null,
                    now, now.plusSeconds(1), now.plusSeconds(2));
            assertThat(mapper.fromPublishResult(mapper.toPublishResult(result)))
                    .isEqualTo(result);
        }

        DdcManagementPublishTask task = new DdcManagementPublishTask(
                "change-1", DdcManagementPublishStatus.PARTIAL_SUCCESS,
                7L, "abc", 4, 1, 1, 1, 1, 2,
                List.of(target), "partial", now, now.plusSeconds(1),
                now.plusSeconds(2));
        assertThat(mapper.fromPublishTask(mapper.toPublishTask(task)))
                .isEqualTo(task);
    }

    @Test
    void preservesMissingPublicationLifecycleTimestamps() {
        Instant now = Instant.parse("2026-08-09T08:00:00Z");
        DdcManagementPublishResult result = new DdcManagementPublishResult(
                "change-1", DdcManagementPublishStatus.PUBLISHING,
                7L, "abc", 1, List.of(), null,
                now, null, null);
        assertThat(mapper.fromPublishResult(mapper.toPublishResult(result)))
                .isEqualTo(result);

        DdcManagementPublishTask task = new DdcManagementPublishTask(
                "change-1", DdcManagementPublishStatus.PUBLISHING,
                7L, "abc", 1, 0, 0, 0, 0, 1,
                List.of(), null, now, null, null);
        assertThat(mapper.fromPublishTask(mapper.toPublishTask(task)))
                .isEqualTo(task);
    }

    @Test
    void roundTripsManagementViewsQueriesAndSnapshots() {
        Instant now = Instant.parse("2026-08-09T08:00:00Z");
        DdcManagementConfigClientInstance client =
                new DdcManagementConfigClientInstance(
                        "retail", "prod", "order", "instance-1", "lease-1",
                        "10.0.0.1", 8080, "CONFIG_CLIENT", "ONLINE",
                        now, now.plusSeconds(1), now.plusSeconds(30),
                        Map.of("zone", "east"));
        assertThat(mapper.fromConfigClientsResponse(
                mapper.toConfigClientsResponse(List.of(client))))
                .containsExactly(client);

        DdcManagementScopeBinding binding = new DdcManagementScopeBinding(
                "binding-1", "retail", "default", "prod", "app-1",
                "order", "Order", true);
        assertThat(mapper.fromScopeBindingsResponse(
                mapper.toScopeBindingsResponse(List.of(binding))))
                .containsExactly(binding);

        DdcManagementScopeQuery scopeQuery = new DdcManagementScopeQuery(
                "retail", "default", "prod", "order");
        assertThat(mapper.fromScopeBindingsRequest(
                mapper.toScopeBindingsRequest(scopeQuery)))
                .isEqualTo(scopeQuery);

        DdcManagementInstanceQuery instanceQuery =
                new DdcManagementInstanceQuery("retail", "prod", "order");
        assertThat(mapper.fromConfigClientsRequest(
                mapper.toConfigClientsRequest(instanceQuery)))
                .isEqualTo(instanceQuery);

        DdcManagementServiceQuery serviceQuery =
                new DdcManagementServiceQuery(
                        "retail", "default", "prod", "order", "RPC_PROVIDER",
                        "grpc", "OrderRpc", "default", "1.0.0");
        assertThat(mapper.fromServiceKeysRequest(
                mapper.toServiceKeysRequest(serviceQuery)))
                .isEqualTo(serviceQuery);
        assertThat(mapper.fromInstancesRequest(
                mapper.toInstancesRequest(serviceQuery)))
                .isEqualTo(serviceQuery);

        DdcManagementServiceKey key = new DdcManagementServiceKey(
                "retail", "prod", "order", "service-id", "RPC_PROVIDER",
                "OrderRpc", "default", "1.0.0", "grpc");
        DdcManagementServiceCatalog catalog = new DdcManagementServiceCatalog(
                9L, now, List.of(key));
        assertThat(mapper.fromServiceKeysResponse(
                mapper.toServiceKeysResponse(catalog)))
                .isEqualTo(catalog);

        DdcManagementServiceInstance instance =
                new DdcManagementServiceInstance(
                        "instance-1", "lease-1", "10.0.0.1", 19080, true,
                        Map.of("zone", "east"), "ONLINE", now,
                        now.plusSeconds(1), now.plusSeconds(30));
        DdcManagementServiceSnapshot snapshot =
                new DdcManagementServiceSnapshot(
                        key, 10L, now, List.of(instance));
        assertThat(mapper.fromInstancesResponse(
                mapper.toInstancesResponse(snapshot)))
                .isEqualTo(snapshot);
    }

    @Test
    void roundTripsExactResourceAdmissionRevocation() {
        DdcResourceAdmissionRevocationRequest request =
                new DdcResourceAdmissionRevocationRequest(
                        "permission-idp-prod",
                        "permission",
                        "idp",
                        "prod",
                        7L
                );
        assertThat(mapper.fromResourceAdmissionRevocationRequest(
                mapper.toResourceAdmissionRevocationRequest(request)))
                .isEqualTo(request);

        DdcResourceAdmissionRevocationResult result =
                new DdcResourceAdmissionRevocationResult(2, 3, 2);
        assertThat(mapper.fromResourceAdmissionRevocationResponse(
                mapper.toResourceAdmissionRevocationResponse(result)))
                .isEqualTo(result);
    }
}
