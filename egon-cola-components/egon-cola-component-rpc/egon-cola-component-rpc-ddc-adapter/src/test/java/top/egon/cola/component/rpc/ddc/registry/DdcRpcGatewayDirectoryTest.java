package top.egon.cola.component.rpc.ddc.registry;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.model.registry.*;
import top.egon.cola.component.rpc.consumer.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DdcRpcGatewayDirectoryTest {

    @Test
    void mapsInternalGatewaySnapshotAndPreservesRevision() {
        DdcServiceRegistryClient client = mock(DdcServiceRegistryClient.class);
        ArgumentCaptor<DdcServiceKey> key = ArgumentCaptor.forClass(DdcServiceKey.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<DdcServiceSnapshot>> listener =
                ArgumentCaptor.forClass(Consumer.class);
        when(client.subscribe(key.capture(), listener.capture()))
                .thenReturn(() -> { });
        DdcRpcGatewayDirectory directory = new DdcRpcGatewayDirectory(
                client, "biz", "gateway-app");
        AtomicReference<RpcGatewaySnapshot> observed = new AtomicReference<>();

        RpcGatewaySubscription subscription = directory.subscribe(
                new RpcGatewayQuery("test", null, null,
                        "GatewayService", "default", "1.0.0"),
                observed::set);
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        listener.getValue().accept(new DdcServiceSnapshot(
                key.getValue(), 12,
                List.of(new DdcServiceInstance(
                        "gateway-1", "lease-1", key.getValue(),
                        "127.0.0.1", 19091, false,
                        Map.of("gateway.weight", "80"), 30, 10,
                        now.minusSeconds(10), now, now.plusSeconds(30),
                        "ONLINE", 12)), now));

        assertThat(key.getValue().serviceKind())
                .isEqualTo(DdcServiceKind.INTERNAL_GATEWAY);
        assertThat(key.getValue().bizCode()).isEqualTo("biz");
        assertThat(observed.get().revision()).isEqualTo(12);
        assertThat(observed.get().endpoints()).singleElement()
                .extracting(RpcGatewayEndpoint::port).isEqualTo(19091);
        subscription.close();
    }
}
