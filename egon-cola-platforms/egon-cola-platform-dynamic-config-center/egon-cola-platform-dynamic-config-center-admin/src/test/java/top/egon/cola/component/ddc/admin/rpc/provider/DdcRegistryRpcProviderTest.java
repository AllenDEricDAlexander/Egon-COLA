package top.egon.cola.component.ddc.admin.rpc.provider;

import org.junit.jupiter.api.Test;
import io.grpc.Context;
import top.egon.cola.component.ddc.admin.security.rpc.DdcServicePrincipal;
import top.egon.cola.component.ddc.admin.service.registry.DdcRegistryFacade;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServiceInstancesRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServicesRequest;
import top.egon.cola.component.rpc.ddc.mapping.DdcCommonProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcRegistryProtoMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRegistryRpcProviderTest {

    @Test
    void mapsAndDelegatesAllFiveRegistryMethods() {
        DdcRegistryFacade facade = mock(DdcRegistryFacade.class);
        DdcCommonProtoMapper common = new DdcCommonProtoMapper(4 * 1024 * 1024);
        DdcRegistryProtoMapper mapper = new DdcRegistryProtoMapper(common);
        DdcRegistryRpcProvider provider = new DdcRegistryRpcProvider(
                facade, common, mapper);
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        DdcServiceKey key = key();
        DdcServiceRegistration registration = new DdcServiceRegistration(
                "provider-1", key, "127.0.0.1", 19090, false,
                Map.of("zone", "east"), 30, 10,
                "test-admission-ticket");
        DdcServiceLeaseRequest lease = new DdcServiceLeaseRequest();
        lease.setServiceKey(key);
        lease.setInstanceId("provider-1");
        lease.setLeaseId("lease-1");
        lease.setAdmissionTicket("test-admission-ticket");
        DdcServiceQuery query = new DdcServiceQuery(
                "biz", "test", "app", DdcServiceKind.RPC_PROVIDER,
                "grpc", null, null, null);
        DdcLeaseSession session = new DdcLeaseSession(
                "provider-1", "lease-1", DdcLeaseRole.RPC_PROVIDER,
                30, 10, now, now.plusSeconds(30));
        DdcLeaseOperationResult renewed = new DdcLeaseOperationResult(
                DdcLeaseOperationStatus.RENEWED, now.plusSeconds(30));
        DdcLeaseOperationResult deleted = new DdcLeaseOperationResult(
                DdcLeaseOperationStatus.DELETED, null);
        DdcServiceSnapshot snapshot = new DdcServiceSnapshot(
                key, 2L, List.of(), now);
        DdcServiceCatalogSnapshot catalog = new DdcServiceCatalogSnapshot(
                query, 3L, List.of(key), now);
        when(facade.register(registration)).thenReturn(session);
        when(facade.heartbeat(any(DdcServiceLeaseRequest.class))).thenReturn(renewed);
        when(facade.deregister(any(DdcServiceLeaseRequest.class))).thenReturn(deleted);
        when(facade.getInstances(key)).thenReturn(snapshot);
        when(facade.getServiceKeys(query)).thenReturn(catalog);

        principal().bind(Context.current()).run(() -> {
            assertThat(provider.registerService(mapper.toRegisterRequest(registration))
                    .getSession()).isEqualTo(common.toProto(session));
            assertThat(provider.heartbeatService(mapper.toHeartbeatRequest(lease))
                    .getResult()).isEqualTo(common.toProto(renewed));
            assertThat(provider.deregisterService(mapper.toDeregisterRequest(lease))
                    .getResult()).isEqualTo(common.toProto(deleted));
            assertThat(provider.getServiceInstances(GetServiceInstancesRequest
                    .newBuilder().setServiceKey(common.toProto(key)).build()))
                    .isEqualTo(mapper.toInstancesResponse(snapshot));
            assertThat(provider.getServices(GetServicesRequest.newBuilder()
                    .setQuery(common.toProto(query)).build()))
                    .isEqualTo(mapper.toServicesResponse(catalog));
        });

        verify(facade).register(registration);
        verify(facade).heartbeat(argThat(value ->
                value.getInstanceId().equals("provider-1")
                        && value.getLeaseId().equals("lease-1")));
        verify(facade).deregister(argThat(value ->
                value.getInstanceId().equals("provider-1")
                        && value.getLeaseId().equals("lease-1")));
        verify(facade).getInstances(key);
        verify(facade).getServiceKeys(query);
    }

    private DdcServicePrincipal principal() {
        return new DdcServicePrincipal(
                "registry-a", "REGISTRY", Set.of("*"), Set.of("*"),
                Set.of("*"), Set.of("*"), "app", "test", "biz");
    }

    private DdcServiceKey key() {
        return new DdcServiceKey(
                "biz", "test", "app", DdcServiceKind.RPC_PROVIDER,
                "OrderQueryService", "default", "1.0.0", "grpc");
    }
}
