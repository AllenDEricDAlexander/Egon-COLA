package top.egon.cola.component.ddc.admin.rpc.provider;

import org.junit.jupiter.api.Test;
import io.grpc.Context;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.security.rpc.DdcServicePrincipal;
import top.egon.cola.component.ddc.admin.service.management.DdcManagementFacade;
import top.egon.cola.component.ddc.admin.service.lease.DdcResourceAdmissionRevocationService;
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
import top.egon.cola.component.ddc.model.management.DdcManagementApp;
import top.egon.cola.component.ddc.model.management.DdcManagementAppQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementBiz;
import top.egon.cola.component.ddc.model.management.DdcManagementBizLookup;
import top.egon.cola.component.ddc.model.management.DdcManagementBizQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceKey;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.ddc.model.management.DdcResourceAdmissionRevocationRequest;
import top.egon.cola.component.ddc.model.management.DdcResourceAdmissionRevocationResult;
import top.egon.cola.component.ddc.error.management.DdcManagementErrorCode;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetPublishTaskRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RetryPublishTaskRequest;
import top.egon.cola.component.rpc.ddc.mapping.DdcCommonProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcManagementProtoMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcManagementRpcProviderTest {

    @Test
    void mapsMissingConfigToAnEmptyFindResponse() {
        DdcManagementFacade facade = mock(DdcManagementFacade.class);
        DdcResourceAdmissionRevocationService revocations =
                mock(DdcResourceAdmissionRevocationService.class);
        DdcManagementProtoMapper mapper = new DdcManagementProtoMapper(
                new DdcCommonProtoMapper(4 * 1024 * 1024), 1024 * 1024);
        DdcManagementRpcProvider provider = new DdcManagementRpcProvider(
                facade, revocations, mapper);
        DdcManagementConfigQuery query = new DdcManagementConfigQuery(
                "biz", "test", "app");
        when(facade.findConfig(query)).thenThrow(
                new DdcAdminException(DdcManagementErrorCode.CONFIG_NOT_FOUND));

        principal().bind(Context.current()).run(() -> {
            var response = provider.findConfig(mapper.toFindRequest(query));

            assertThat(response.getFound()).isFalse();
            assertThat(response.hasConfig()).isFalse();
        });

        verify(facade).findConfig(query);
    }

    @Test
    void mapsAndDelegatesAllElevenManagementMethods() {
        DdcManagementFacade facade = mock(DdcManagementFacade.class);
        DdcResourceAdmissionRevocationService revocations =
                mock(DdcResourceAdmissionRevocationService.class);
        DdcManagementProtoMapper mapper = new DdcManagementProtoMapper(
                new DdcCommonProtoMapper(4 * 1024 * 1024), 1024 * 1024);
        DdcManagementRpcProvider provider = new DdcManagementRpcProvider(
                facade, revocations, mapper);
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
        String trustedOperator =
                "service:management-a [requested=requested-operator]";
        DdcManagementConfigUpsertRequest trustedUpsert =
                new DdcManagementConfigUpsertRequest(
                        upsert.bizCode(), upsert.env(), upsert.appCode(),
                        upsert.resourceName(), upsert.content(), upsert.format(),
                        upsert.description(), upsert.expectedVersion(),
                        trustedOperator);
        DdcManagementConfigDeleteRequest trustedDelete =
                new DdcManagementConfigDeleteRequest(
                        delete.bizCode(), delete.env(), delete.appCode(),
                        delete.expectedVersion(), trustedOperator,
                        delete.reason());
        DdcManagementPublishRequest trustedPublish =
                new DdcManagementPublishRequest(
                        publish.bizCode(), publish.env(), publish.appCode(),
                        publish.resourceName(), publish.content(),
                        publish.format(), publish.expectedVersion(),
                        publish.changeId(), publish.timeoutMs(),
                        trustedOperator);
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
        DdcResourceAdmissionRevocationRequest revocation =
                new DdcResourceAdmissionRevocationRequest(
                        "permission-idp-prod",
                        "permission",
                        "idp",
                        "prod",
                        7L
                );
        DdcResourceAdmissionRevocationResult revocationResult =
                new DdcResourceAdmissionRevocationResult(2, 3, 2);
        when(facade.findConfig(find)).thenReturn(config);
        when(facade.upsert(trustedUpsert)).thenReturn(config);
        when(facade.publish(trustedPublish)).thenReturn(publishResult);
        when(facade.getPublishTask("change-1")).thenReturn(task);
        when(facade.retry(
                "change-1",
                "service:management-a [requested=retry-operator]"))
                .thenReturn(publishResult);
        when(facade.getConfigClients(clients)).thenReturn(List.of());
        when(facade.getScopeBindings(scopes)).thenReturn(List.of());
        when(facade.getServiceKeys(services)).thenReturn(catalog);
        when(facade.getInstances(services)).thenReturn(snapshot);
        when(revocations.revoke(revocation)).thenReturn(revocationResult);

        principal().bind(Context.current()).run(() -> {
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
                    .setChangeId("change-1")
                    .setRequestedOperator("retry-operator")
                    .build()).getResult())
                    .isEqualTo(mapper.toPublishResult(publishResult));
            assertThat(provider.getConfigClients(mapper.toConfigClientsRequest(clients))
                    .getClientsCount()).isZero();
            assertThat(provider.revokeResourceAdmission(
                    mapper.toResourceAdmissionRevocationRequest(revocation)))
                    .isEqualTo(mapper.toResourceAdmissionRevocationResponse(
                            revocationResult));
            assertThat(provider.getScopeBindings(mapper.toScopeBindingsRequest(scopes))
                    .getBindingsCount()).isZero();
            assertThat(provider.getServiceKeys(mapper.toServiceKeysRequest(services)))
                    .isEqualTo(mapper.toServiceKeysResponse(catalog));
            assertThat(provider.getInstances(mapper.toInstancesRequest(services)))
                    .isEqualTo(mapper.toInstancesResponse(snapshot));
        });

        verify(facade).findConfig(find);
        verify(facade).upsert(trustedUpsert);
        verify(facade).delete(trustedDelete);
        verify(facade).publish(trustedPublish);
        verify(facade).getPublishTask("change-1");
        verify(facade).retry(
                "change-1",
                "service:management-a [requested=retry-operator]");
        verify(facade).getConfigClients(clients);
        verify(facade).getScopeBindings(scopes);
        verify(facade).getServiceKeys(services);
        verify(facade).getInstances(services);
        verify(revocations).revoke(revocation);
    }

    @Test
    void mapsAndDelegatesReadOnlyBusinessCatalogMethods() {
        DdcManagementFacade facade = mock(DdcManagementFacade.class);
        DdcResourceAdmissionRevocationService revocations =
                mock(DdcResourceAdmissionRevocationService.class);
        DdcManagementProtoMapper mapper = new DdcManagementProtoMapper(
                new DdcCommonProtoMapper(4 * 1024 * 1024), 1024 * 1024);
        DdcManagementRpcProvider provider = new DdcManagementRpcProvider(
                facade, revocations, mapper);
        DdcManagementBiz biz = new DdcManagementBiz(
                "business-1", "retail", "Retail", true);
        DdcManagementApp app = new DdcManagementApp(
                "app-1", "business-1", "retail", "order", "Order", true,
                true);
        DdcManagementBizLookup bizLookup = new DdcManagementBizLookup(
                "business-1", null);
        DdcManagementBizQuery bizQuery = new DdcManagementBizQuery("retail", true);
        DdcManagementAppQuery appQuery = new DdcManagementAppQuery(
                "business-1", null, "order", true);
        when(facade.getBiz(bizLookup)).thenReturn(Optional.of(biz));
        when(facade.listBizs(bizQuery)).thenReturn(List.of(biz));
        when(facade.getApp("app-1")).thenReturn(Optional.of(app));
        when(facade.listApps(appQuery)).thenReturn(List.of(app));

        principal().bind(Context.current()).run(() -> {
            assertThat(provider.getBiz(mapper.toGetBizRequest(bizLookup)))
                    .isEqualTo(mapper.toBizResponse(Optional.of(biz)));
            assertThat(provider.listBizs(mapper.toListBizsRequest(bizQuery)))
                    .isEqualTo(mapper.toBizsResponse(List.of(biz)));
            assertThat(provider.getApp(mapper.toGetAppRequest("app-1")))
                    .isEqualTo(mapper.toAppResponse(Optional.of(app)));
            assertThat(provider.listApps(mapper.toListAppsRequest(appQuery)))
                    .isEqualTo(mapper.toAppsResponse(List.of(app)));
        });

        verify(facade).getBiz(bizLookup);
        verify(facade).listBizs(bizQuery);
        verify(facade).getApp("app-1");
        verify(facade).listApps(appQuery);
    }

    private DdcServicePrincipal principal() {
        return new DdcServicePrincipal(
                "management-a", "MANAGEMENT", Set.of("*"), Set.of("*"),
                Set.of("*"), Set.of("*"), "app", "test", "biz");
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
