package top.egon.cola.component.rpc.ddc.registry;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderEndpoint;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderQuery;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderSnapshot;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderSubscription;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRpcProviderDirectoryTest {

    @Test
    void queriesExactRpcProviderServiceKey() {
        DdcServiceRegistryClient client =
                mock(DdcServiceRegistryClient.class);
        DdcRegistrySubscription ddcSubscription =
                mock(DdcRegistrySubscription.class);
        ArgumentCaptor<DdcServiceKey> key =
                ArgumentCaptor.forClass(DdcServiceKey.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<DdcServiceSnapshot>> listener =
                ArgumentCaptor.forClass(Consumer.class);
        when(client.subscribe(key.capture(), listener.capture()))
                .thenReturn(ddcSubscription);
        DdcRpcProviderDirectory directory =
                new DdcRpcProviderDirectory(client);

        RpcProviderSubscription subscription = directory.subscribe(
                query(),
                ignored -> {
                }
        );

        assertThat(key.getValue()).isEqualTo(new DdcServiceKey(
                "commerce",
                "test",
                "orders",
                DdcServiceKind.RPC_PROVIDER,
                "egon.rpc.orders.v1.OrderService",
                "blue",
                "2.1.0",
                "grpc"
        ));
        subscription.close();
        verify(ddcSubscription).close();
    }

    @Test
    void mapsLeaseSnapshotAndPreservesRevision() {
        DdcServiceRegistryClient client =
                mock(DdcServiceRegistryClient.class);
        ArgumentCaptor<DdcServiceKey> key =
                ArgumentCaptor.forClass(DdcServiceKey.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<DdcServiceSnapshot>> listener =
                ArgumentCaptor.forClass(Consumer.class);
        when(client.subscribe(key.capture(), listener.capture()))
                .thenReturn(() -> {
                });
        DdcRpcProviderDirectory directory =
                new DdcRpcProviderDirectory(client);
        AtomicReference<RpcProviderSnapshot> observed =
                new AtomicReference<>();
        directory.subscribe(query(), observed::set);
        Instant now = Instant.parse("2026-08-15T00:00:00Z");
        Instant leaseExpireAt = now.plusSeconds(30);

        listener.getValue().accept(new DdcServiceSnapshot(
                key.getValue(),
                27,
                List.of(new DdcServiceInstance(
                        "provider-1",
                        "lease-1",
                        key.getValue(),
                        "127.0.0.1",
                        19091,
                        true,
                        Map.of("gateway.weight", "80"),
                        30,
                        10,
                        now.minusSeconds(10),
                        now,
                        leaseExpireAt,
                        "ONLINE",
                        27,
                        "resource-orders",
                        3L,
                        "kid-test",
                        now.plusSeconds(20)
                )),
                now
        ));

        assertThat(observed.get().revision()).isEqualTo(27);
        assertThat(observed.get().observedAt()).isEqualTo(now);
        assertThat(observed.get().endpoints()).singleElement()
                .satisfies(endpoint -> assertEndpoint(
                        endpoint,
                        leaseExpireAt
                ));
    }

    private void assertEndpoint(
            RpcProviderEndpoint endpoint,
            Instant leaseExpireAt) {
        assertThat(endpoint.instanceId()).isEqualTo("provider-1");
        assertThat(endpoint.leaseId()).isEqualTo("lease-1");
        assertThat(endpoint.host()).isEqualTo("127.0.0.1");
        assertThat(endpoint.port()).isEqualTo(19091);
        assertThat(endpoint.secure()).isTrue();
        assertThat(endpoint.leaseExpireAt()).isEqualTo(leaseExpireAt);
    }

    private RpcProviderQuery query() {
        return new RpcProviderQuery(
                "commerce",
                "orders",
                "test",
                "egon.rpc.orders.v1.OrderService",
                "blue",
                "2.1.0",
                "grpc"
        );
    }
}
