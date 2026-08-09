package top.egon.cola.component.rpc.consumer;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcGatewayDirectoryContractsTest {

    @Test
    void queryRequiresCompleteServiceScopeAndPairedTargetScope() {
        assertThatThrownBy(() -> new RpcGatewayQuery(
                " ",
                null,
                null,
                "gateway",
                "default",
                "1.0.0"
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RpcGatewayQuery(
                "test",
                "retail",
                null,
                "gateway",
                "default",
                "1.0.0"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void snapshotRequiresObservationTimeAndCopiesEndpoints() {
        List<RpcGatewayEndpoint> endpoints = new ArrayList<>();
        endpoints.add(new RpcGatewayEndpoint(
                "gateway-1",
                "lease-1",
                "127.0.0.1",
                19090,
                false,
                Instant.now().plusSeconds(30)
        ));
        RpcGatewaySnapshot snapshot = new RpcGatewaySnapshot(
                1,
                Instant.now(),
                endpoints
        );
        endpoints.clear();

        assertThat(snapshot.endpoints()).hasSize(1);
        assertThatThrownBy(() -> new RpcGatewaySnapshot(1, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RpcGatewaySnapshot(-1, Instant.now(),
                List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
