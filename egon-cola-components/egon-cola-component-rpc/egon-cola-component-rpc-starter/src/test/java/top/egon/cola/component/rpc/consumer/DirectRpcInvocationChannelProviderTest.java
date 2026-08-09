package top.egon.cola.component.rpc.consumer;

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.config.RpcTransportSecurity;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DirectRpcInvocationChannelProviderTest {

    @Test
    void ownsOneConfiguredChannelAndClosesItDeterministically() {
        RpcDirectClientSettings settings = new RpcDirectClientSettings(
                "dns:///localhost:65535",
                processIdentity(),
                RpcTransportSecurity.developmentPlaintextConfig(),
                500,
                "round_robin",
                1024 * 1024,
                100
        );
        DirectRpcInvocationChannelProvider provider =
                new DirectRpcInvocationChannelProvider(settings);

        ManagedChannel first = provider.currentChannel(Set.of());
        ManagedChannel second = provider.currentChannel(Set.of());

        assertThat(first).isSameAs(second);
        assertThat(first.authority()).isEqualTo("localhost:65535");
        assertThat(provider.maxAttempts()).isOne();
        assertThat(provider.settings().loadBalancingPolicy())
                .isEqualTo("round_robin");

        provider.recordFailure(first);
        assertThat(first.isShutdown()).isFalse();

        provider.close();
        assertThat(first.isShutdown()).isTrue();
        assertThat(first.isTerminated()).isTrue();
    }

    private RpcProcessIdentity processIdentity() {
        return new RpcProcessIdentity(
                "direct-test",
                "test",
                "default",
                "127.0.0.1",
                1,
                "direct-1"
        );
    }
}
