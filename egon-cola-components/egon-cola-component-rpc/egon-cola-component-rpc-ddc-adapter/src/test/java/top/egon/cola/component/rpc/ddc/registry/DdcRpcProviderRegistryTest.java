package top.egon.cola.component.rpc.ddc.registry;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.model.lease.*;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionTicket;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.provider.*;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DdcRpcProviderRegistryTest {

    @Test
    void mapsNeutralProviderAndLeaseOperationsToDdc() {
        DdcServiceRegistryClient client = mock(DdcServiceRegistryClient.class);
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        when(client.register(any())).thenReturn(new DdcLeaseSession(
                "instance-1", "lease-1", DdcLeaseRole.RPC_PROVIDER,
                30, 10, now, now.plusSeconds(30)));
        when(client.heartbeat(any(DdcServiceLeaseRequest.class))).thenReturn(
                new DdcLeaseOperationResult(
                        DdcLeaseOperationStatus.RENEWED, now.plusSeconds(30)));
        when(client.deregister("instance-1", "lease-1")).thenReturn(
                new DdcLeaseOperationResult(DdcLeaseOperationStatus.DELETED, null));
        DdcRpcProviderRegistry registry = new DdcRpcProviderRegistry(
                client,
                "biz",
                "app",
                (biz, app, env, instance) -> new DdcAdmissionTicket(
                        "admission.jwt.value",
                        now.plusSeconds(20),
                        "resource-order",
                        URI.create("https://api.example/order"),
                        1L,
                        biz,
                        app,
                        env,
                        instance,
                        "kid-test"
                ));
        RpcProviderRegistration registration = new RpcProviderRegistration(
                new RpcServiceIdentity("OrderService", "default", "1.0.0"),
                new RpcProcessIdentity("orders", "test", "127.0.0.1", 1, "instance-1"),
                "127.0.0.1", 19090, false,
                Map.of("gateway.weight", "80"), 30, 10);

        RpcProviderLease lease = registry.register(registration);
        RpcProviderLeaseIdentity identity = new RpcProviderLeaseIdentity(
                registration.serviceIdentity(), lease.instanceId(), lease.leaseId());

        assertThat(registry.heartbeat(identity).renewed()).isTrue();
        assertThat(registry.deregister(identity).status())
                .isEqualTo(RpcLeaseOperationResult.Status.DELETED);
        verify(client).register(argThat((DdcServiceRegistration value) ->
                value.serviceKey().serviceKind() == DdcServiceKind.RPC_PROVIDER
                        && value.serviceKey().protocol().equals("grpc")
                        && value.instanceId().equals("instance-1")
                        && value.admissionTicket().equals("admission.jwt.value")));
        verify(client).heartbeat(argThat((DdcServiceLeaseRequest value) ->
                value.getServiceKey().serviceKind()
                        == DdcServiceKind.RPC_PROVIDER
                        && value.getAdmissionTicket()
                        .equals("admission.jwt.value")));
    }
}
