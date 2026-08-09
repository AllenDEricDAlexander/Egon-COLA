package top.egon.cola.component.ddc.admin.rpc.provider;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.service.management.DdcManagementFacade;
import top.egon.cola.component.ddc.model.management.DdcManagementConfig;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTask;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceKey;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetPublishTaskRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RetryPublishTaskRequest;
import top.egon.cola.component.rpc.ddc.mapping.DdcCommonProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcManagementProtoMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcManagementRpcProviderTest {

    @Test
    void mapsAndDelegatesAllTenManagementMethods() {
        DdcManagementFacade facade = mock(DdcManagementFacade.class);
        DdcManagementProtoMapper mapper = new DdcManagementProtoMapper(
                new DdcCommonProtoMapper(4 * 1024 * 1024), 1024 * 1024);
        DdcManagementRpcProvider provider = new DdcManagementRpcProvider(
                facade, mapper);
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        DdcManagementConfigQuery find = new DdcManagementConfigQuery(
                "biz", "test", "app");
        DdcManagementConfig config = new DdcManagementConfig(
                "biz", "test", "app", "application.yml", "feature: true\n",
                "YAML", 2L, true, false, now);
        DdcManagementConfigUpsertRequest upsert =
                new DdcManagementConfigUpsertRequest(
                        "biz", "test", "app", "application.yml",
                        "feature: true\n", "YAML", "description", 1L,
                        "requested-operator");
        DdcManagementConfigDeleteRequest delete =
                new DdcManagementConfigDeleteRequest(
                        "biz", "test", "app", 2L,
                        "requested-operator", "cleanup");
        DdcManagementPublishRequest publish = new DdcManagementPublishRequest(
                "biz", "test", "app", "application.yml", "feature: true\n",
                "YAML", 2L, "change-1", 1000L, "requested-operator");
        DdcManagementPublishResult publishResult = new DdcManagementPublishResult(
                "change-1", DdcManagementPublishStatus.SUCCESS, 3L,
                "checksum", 0, List.of(), null, now, now, now);
        DdcManagementPublishTask task = new DdcManagementPublishTask(
                "change-1", DdcManagementPublishStatus.SUCCESS, 3L,
                "checksum", 0, 0, 0, 0, 0, 1,
                List.of(), null, now, now, now);
        DdcManagementInstanceQuery clients = new DdcManagementInstanceQuery(
                "biz", "test", "app");
        DdcManagementScopeQuery scopes = new DdcManagementScopeQuery(
                "biz", "namespace", "test", "app");
        DdcManagementServiceQuery services = new DdcManagementServiceQuery(
                "biz", null, "test", "app", "RPC_PROVIDER", "grpc",
                "OrderQueryService", "default", "1.0.0");
        DdcManagementServiceKey serviceKey = new DdcManagementServiceKey(
                "biz", "test", "app", "service-id", "RPC_PROVIDER",
                "OrderQueryService", "default", "1.0.0", "grpc");
        DdcManagementServiceCatalog catalog = new DdcManagementServiceCatalog(
                3L, now, List.of(serviceKey));
        DdcManagementServiceSnapshot snapshot = new DdcManagementServiceSnapshot(
                serviceKey, 4L, now, List.of());
        when(facade.findConfig(find)).thenReturn(config);
        when(facade.upsert(upsert)).thenReturn(config);
        when(facade.publish(publish)).thenReturn(publishResult);
        when(facade.getPublishTask("change-1")).thenReturn(task);
        when(facade.retry("change-1")).thenReturn(publishResult);
        when(facade.getConfigClients(clients)).thenReturn(List.of());
        when(facade.getScopeBindings(scopes)).thenReturn(List.of());
        when(facade.getServiceKeys(services)).thenReturn(catalog);
        when(facade.getInstances(services)).thenReturn(snapshot);

        assertThat(provider.findConfig(mapper.toFindRequest(find)).getConfig())
                .isEqualTo(mapper.toConfig(config));
        assertThat(provider.upsertConfig(mapper.toUpsertRequest(upsert)).getConfig())
                .isEqualTo(mapper.toConfig(config));
        assertThat(provider.deleteConfig(mapper.toDeleteRequest(delete))).isNotNull();
        assertThat(provider.publishConfig(mapper.toPublishRequest(publish)).getResult())
                .isEqualTo(mapper.toPublishResult(publishResult));
        assertThat(provider.getPublishTask(GetPublishTaskRequest.newBuilder()
                .setChangeId("change-1").build()).getTask())
                .isEqualTo(mapper.toPublishTask(task));
        assertThat(provider.retryPublishTask(RetryPublishTaskRequest.newBuilder()
                .setChangeId("change-1").setRequestedOperator("ignored-until-task-8")
                .build()).getResult()).isEqualTo(mapper.toPublishResult(publishResult));
        assertThat(provider.getConfigClients(mapper.toConfigClientsRequest(clients))
                .getClientsCount()).isZero();
        assertThat(provider.getScopeBindings(mapper.toScopeBindingsRequest(scopes))
                .getBindingsCount()).isZero();
        assertThat(provider.getServiceKeys(mapper.toServiceKeysRequest(services)))
                .isEqualTo(mapper.toServiceKeysResponse(catalog));
        assertThat(provider.getInstances(mapper.toInstancesRequest(services)))
                .isEqualTo(mapper.toInstancesResponse(snapshot));

        verify(facade).findConfig(find);
        verify(facade).upsert(upsert);
        verify(facade).delete(delete);
        verify(facade).publish(publish);
        verify(facade).getPublishTask("change-1");
        verify(facade).retry("change-1");
        verify(facade).getConfigClients(clients);
        verify(facade).getScopeBindings(scopes);
        verify(facade).getServiceKeys(services);
        verify(facade).getInstances(services);
    }

    @Test
    void providerBoundaryDoesNotDependOnPersistenceOrRedissonTypes() {
        assertThat(List.of(
                DdcConfigRpcProvider.class,
                DdcRegistryRpcProvider.class,
                DdcManagementRpcProvider.class
        )).allSatisfy(type -> {
            assertThat(type.getDeclaredFields()).allSatisfy(field ->
                    assertThat(field.getType().getName())
                            .doesNotContain(".repository.", "redisson"));
            assertThat(type.getDeclaredConstructors()).allSatisfy(constructor ->
                    assertThat(constructor.getParameterTypes()).allSatisfy(parameter ->
                            assertThat(parameter.getName())
                                    .doesNotContain(".repository.", "redisson")));
        });
    }
}
