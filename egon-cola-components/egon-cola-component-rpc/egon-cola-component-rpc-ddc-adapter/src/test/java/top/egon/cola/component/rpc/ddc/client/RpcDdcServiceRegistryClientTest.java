package top.egon.cola.component.rpc.ddc.client;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.model.lease.*;
import top.egon.cola.component.ddc.model.registry.*;
import top.egon.cola.component.rpc.ddc.client.registry.RpcDdcServiceRegistryClient;
import top.egon.cola.component.rpc.ddc.contract.DdcServiceRegistryRpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DeregisterServiceResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatServiceResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterServiceResponse;
import top.egon.cola.component.rpc.ddc.mapping.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RpcDdcServiceRegistryClientTest {

    @Test
    void mapsLeaseSnapshotAndSubscriptionMethods() {
        DdcServiceRegistryRpc rpc = mock(DdcServiceRegistryRpc.class);
        DdcCommonProtoMapper common = new DdcCommonProtoMapper(1024 * 1024);
        DdcRegistryProtoMapper mapper = new DdcRegistryProtoMapper(common);
        RpcDdcServiceRegistryClient client = new RpcDdcServiceRegistryClient(
                rpc, mapper, common, new DdcRpcStatusExceptionMapper());
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        top.egon.cola.component.ddc.model.registry.DdcServiceKey key = key();
        DdcLeaseSession session = new DdcLeaseSession(
                "instance-1", "lease-1", DdcLeaseRole.RPC_PROVIDER,
                30, 10, now, now.plusSeconds(30));
        when(rpc.registerService(any())).thenReturn(RegisterServiceResponse
                .newBuilder().setSession(common.toProto(session)).build());
        when(rpc.heartbeatService(any())).thenReturn(HeartbeatServiceResponse
                .newBuilder().setResult(common.toProto(new DdcLeaseOperationResult(
                        DdcLeaseOperationStatus.RENEWED, now.plusSeconds(30)))).build());
        when(rpc.deregisterService(any())).thenReturn(DeregisterServiceResponse
                .newBuilder().setResult(common.toProto(new DdcLeaseOperationResult(
                        DdcLeaseOperationStatus.DELETED, null))).build());
        DdcServiceSnapshot instances = new DdcServiceSnapshot(key, 7, List.of(), now);
        DdcServiceQuery query = new DdcServiceQuery(
                "biz", "test", "app", DdcServiceKind.RPC_PROVIDER,
                "grpc", null, null, null);
        DdcServiceCatalogSnapshot services = new DdcServiceCatalogSnapshot(
                query, 8, List.of(key), now);
        when(rpc.getServiceInstances(any())).thenReturn(mapper.toInstancesResponse(instances));
        when(rpc.getServices(any())).thenReturn(mapper.toServicesResponse(services));
        AtomicInteger subscriptions = new AtomicInteger();
        client.subscriptions(new RpcDdcServiceRegistryClient.RegistrySubscriptions() {
            @Override
            public DdcRegistrySubscription subscribe(
                    top.egon.cola.component.ddc.model.registry.DdcServiceKey ignored,
                    java.util.function.Consumer<DdcServiceSnapshot> listener) {
                subscriptions.incrementAndGet(); listener.accept(instances); return () -> { };
            }

            @Override
            public DdcRegistrySubscription subscribeServices(DdcServiceQuery ignored,
                    java.util.function.Consumer<DdcServiceCatalogSnapshot> listener) {
                subscriptions.incrementAndGet(); listener.accept(services); return () -> { };
            }
        });

        assertThat(client.register(registration(key))).isEqualTo(session);
        assertThat(client.heartbeat(lease(key())).renewed()).isTrue();
        assertThat(client.getInstances(key).revision()).isEqualTo(7);
        assertThat(client.getServiceKeys(query).revision()).isEqualTo(8);
        client.subscribe(key, ignored -> { }).close();
        client.subscribeServices(query, ignored -> { }).close();
        assertThat(client.deregister("instance-1", "lease-1").deleted()).isTrue();
        assertThat(subscriptions).hasValue(2);

        verify(rpc).heartbeatService(argThat(request ->
                request.getServiceKey().getServiceKind()
                        == top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceKind.DDC_SERVICE_KIND_RPC_PROVIDER
                        && request.getAdmissionTicket().equals("service-heartbeat-ticket")));
    }

    @Test
    void keepsLocalLeaseIndexConsistentWithRemoteOperationStatus() {
        DdcServiceRegistryRpc rpc = mock(DdcServiceRegistryRpc.class);
        DdcCommonProtoMapper common = new DdcCommonProtoMapper(1024 * 1024);
        RpcDdcServiceRegistryClient client = new RpcDdcServiceRegistryClient(
                rpc,
                new DdcRegistryProtoMapper(common),
                common,
                new DdcRpcStatusExceptionMapper()
        );
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        when(rpc.registerService(any())).thenReturn(RegisterServiceResponse
                .newBuilder().setSession(common.toProto(new DdcLeaseSession(
                        "instance-1", "lease-1", DdcLeaseRole.RPC_PROVIDER,
                        30, 10, now, now.plusSeconds(30)))).build());
        when(rpc.deregisterService(any())).thenReturn(DeregisterServiceResponse
                .newBuilder().setResult(common.toProto(
                        new DdcLeaseOperationResult(
                                DdcLeaseOperationStatus.NOT_DELETED, null)))
                .build());
        when(rpc.heartbeatService(any())).thenReturn(HeartbeatServiceResponse
                .newBuilder().setResult(common.toProto(
                        new DdcLeaseOperationResult(
                                DdcLeaseOperationStatus.NOT_FOUND, null)))
                .build());

        client.register(registration(key()));
        assertThat(client.deregister("instance-1", "lease-1").status())
                .isEqualTo(DdcLeaseOperationStatus.NOT_DELETED);
        assertThat(client.heartbeat(lease(key())).status())
                .isEqualTo(DdcLeaseOperationStatus.NOT_FOUND);
        assertThatThrownBy(() -> client.heartbeat(lease(key())))
                .isInstanceOf(top.egon.cola.component.ddc.error.DdcException.class);
        verify(rpc, times(1)).heartbeatService(any());
    }

    private top.egon.cola.component.ddc.model.registry.DdcServiceKey key() {
        return new top.egon.cola.component.ddc.model.registry.DdcServiceKey("biz", "test", "app",
                DdcServiceKind.RPC_PROVIDER, "OrderService", "default", "1.0.0", "grpc");
    }

    private DdcServiceRegistration registration(
            top.egon.cola.component.ddc.model.registry.DdcServiceKey key) {
        return new DdcServiceRegistration(
                "instance-1", key, "127.0.0.1", 19090, false,
                Map.of("zone", "a"), 30, 10, "service-register-ticket");
    }

    private DdcServiceLeaseRequest lease(
            top.egon.cola.component.ddc.model.registry.DdcServiceKey key) {
        DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
        request.setServiceKey(key);
        request.setInstanceId("instance-1");
        request.setLeaseId("lease-1");
        request.setAdmissionTicket("service-heartbeat-ticket");
        return request;
    }
}
