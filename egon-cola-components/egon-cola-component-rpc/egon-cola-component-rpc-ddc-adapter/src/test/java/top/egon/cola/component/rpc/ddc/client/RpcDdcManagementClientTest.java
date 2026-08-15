package top.egon.cola.component.rpc.ddc.client;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.management.*;
import top.egon.cola.component.rpc.ddc.client.management.RpcDdcManagementClient;
import top.egon.cola.component.rpc.ddc.contract.DdcManagementRpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DeleteConfigResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.FindConfigResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetConfigClientsResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetPublishTaskResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetScopeBindingsResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetBizResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetAppResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.ListBizsResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.ListAppsResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PublishConfigResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RetryPublishTaskResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.UpsertConfigResponse;
import top.egon.cola.component.rpc.ddc.mapping.*;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RpcDdcManagementClientTest {

    @Test
    void mapsAllTenManagementPortMethods() {
        DdcManagementRpc rpc = mock(DdcManagementRpc.class);
        DdcCommonProtoMapper common = new DdcCommonProtoMapper(1024 * 1024);
        DdcManagementProtoMapper mapper = new DdcManagementProtoMapper(common, 1024 * 1024);
        RpcDdcManagementClient client = new RpcDdcManagementClient(
                rpc, mapper, new DdcRpcStatusExceptionMapper());
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        DdcManagementConfig config = new DdcManagementConfig(
                "biz", "test", "app", "application.yml", "a: 1\n",
                "YAML", 2L, true, false, now);
        DdcManagementPublishResult result = new DdcManagementPublishResult(
                "change-1", DdcManagementPublishStatus.SUCCESS, 2L,
                "checksum", 0, List.of(), null, now, now, now);
        DdcManagementPublishTask task = new DdcManagementPublishTask(
                "change-1", DdcManagementPublishStatus.SUCCESS, 2L,
                "checksum", 0, 0, 0, 0, 0, 1,
                List.of(), null, now, now, now);
        DdcManagementServiceKey serviceKey = new DdcManagementServiceKey(
                "biz", "test", "app", "service-id", "RPC_PROVIDER",
                "OrderService", "default", "1.0.0", "grpc");
        DdcManagementServiceCatalog catalog = new DdcManagementServiceCatalog(
                3, now, List.of(serviceKey));
        DdcManagementServiceSnapshot snapshot = new DdcManagementServiceSnapshot(
                serviceKey, 4, now, List.of());

        when(rpc.findConfig(any())).thenReturn(FindConfigResponse.newBuilder()
                .setFound(true).setConfig(mapper.toConfig(config)).build());
        when(rpc.upsertConfig(any())).thenReturn(UpsertConfigResponse.newBuilder()
                .setConfig(mapper.toConfig(config)).build());
        when(rpc.deleteConfig(any())).thenReturn(DeleteConfigResponse.getDefaultInstance());
        when(rpc.publishConfig(any())).thenReturn(PublishConfigResponse.newBuilder()
                .setResult(mapper.toPublishResult(result)).build());
        when(rpc.getPublishTask(any())).thenReturn(GetPublishTaskResponse.newBuilder()
                .setFound(true).setTask(mapper.toPublishTask(task)).build());
        when(rpc.retryPublishTask(any())).thenReturn(RetryPublishTaskResponse.newBuilder()
                .setResult(mapper.toPublishResult(result)).build());
        when(rpc.getConfigClients(any())).thenReturn(GetConfigClientsResponse.getDefaultInstance());
        when(rpc.getScopeBindings(any())).thenReturn(GetScopeBindingsResponse.getDefaultInstance());
        when(rpc.getServiceKeys(any())).thenReturn(mapper.toServiceKeysResponse(catalog));
        when(rpc.getInstances(any())).thenReturn(mapper.toInstancesResponse(snapshot));

        assertThat(client.findConfig(new DdcManagementConfigQuery("biz", "test", "app")))
                .contains(config);
        assertThat(client.upsert(new DdcManagementConfigUpsertRequest(
                "biz", "test", "app", "application.yml", "a: 1\n",
                "YAML", null, 1L, "operator"))).isEqualTo(config);
        client.delete(new DdcManagementConfigDeleteRequest(
                "biz", "test", "app", 1L, "operator", "cleanup"));
        assertThat(client.publish(new DdcManagementPublishRequest(
                "biz", "test", "app", "application.yml", "a: 1\n",
                "YAML", 1L, "change-1", 1000L, "operator"))).isEqualTo(result);
        assertThat(client.getPublishTask("change-1")).isEqualTo(task);
        assertThat(client.retry("change-1")).isEqualTo(result);
        assertThat(client.getConfigClients(new DdcManagementInstanceQuery(null, null, null))).isEmpty();
        assertThat(client.getScopeBindings(new DdcManagementScopeQuery(null, null, null, null))).isEmpty();
        DdcManagementServiceQuery query = new DdcManagementServiceQuery(
                "biz", null, "test", "app", "RPC_PROVIDER", "grpc",
                "OrderService", "default", "1.0.0");
        assertThat(client.getServiceKeys(query)).isEqualTo(catalog);
        assertThat(client.getInstances(query)).isEqualTo(snapshot);

        verify(rpc).retryPublishTask(argThat(request ->
                request.getChangeId().equals("change-1")
                        && request.getRequestedOperator().equals("rpc-client")));
    }

    @Test
    void readsBusinessAndApplicationCatalogThroughTheTypedClient() {
        DdcManagementRpc rpc = mock(DdcManagementRpc.class);
        DdcManagementProtoMapper mapper = new DdcManagementProtoMapper(
                new DdcCommonProtoMapper(1024 * 1024), 1024 * 1024);
        RpcDdcManagementClient client = new RpcDdcManagementClient(
                rpc, mapper, new DdcRpcStatusExceptionMapper());
        DdcManagementBiz biz = new DdcManagementBiz(
                "business-1", "retail", "Retail", true);
        DdcManagementApp app = new DdcManagementApp(
                "app-1", "business-1", "retail", "order", "Order", true,
                true);

        when(rpc.getBiz(any())).thenReturn(GetBizResponse.newBuilder()
                .setFound(true).setBiz(mapper.toBiz(biz)).build());
        when(rpc.listBizs(any())).thenReturn(ListBizsResponse.newBuilder()
                .addBusinesses(mapper.toBiz(biz)).build());
        when(rpc.getApp(any())).thenReturn(GetAppResponse.newBuilder()
                .setFound(true).setApp(mapper.toApp(app)).build());
        when(rpc.listApps(any())).thenReturn(ListAppsResponse.newBuilder()
                .addApplications(mapper.toApp(app)).build());

        assertThat(client.getBiz(new DdcManagementBizLookup("business-1", null)))
                .contains(biz);
        assertThat(client.listBizs(new DdcManagementBizQuery(null, true)))
                .containsExactly(biz);
        assertThat(client.getApp("app-1")).contains(app);
        assertThat(client.listApps(new DdcManagementAppQuery(
                "business-1", null, null, true))).containsExactly(app);
    }
}
