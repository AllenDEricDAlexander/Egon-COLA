package top.egon.cola.component.rpc.consumer.gateway;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RpcConsumerGatewayManagerTest {

    @Test
    void roundRobinsAcrossDirectoryGatewaysAndRemovesFailures() {
        SnapshotDirectory directory = new SnapshotDirectory();
        StubChannelFactory channels = new StubChannelFactory();
        RpcConsumerGatewayManager manager = manager(directory, channels);
        directory.snapshot = snapshot(endpoint(
                "gateway-1",
                "lease-1",
                19090
        ));

        manager.start();

        assertThat(manager.state()).isEqualTo(RpcGatewayState.READY);
        assertThat(manager.endpoint().instanceId()).isEqualTo("gateway-1");
        assertThat(channels.createCount).isOne();
        assertThat(directory.query.bizCode()).isEqualTo("platform-biz");
        assertThat(directory.query.appCode()).isEqualTo("gateway-app");
        assertThat(directory.query.env()).isEqualTo("test");
        assertThat(directory.query.serviceName())
                .isEqualTo("egon-gateway-rpc");

        directory.publish(snapshot(
                endpoint("gateway-1", "lease-1", 19090),
                endpoint("gateway-2", "lease-2", 19091)
        ));

        assertThat(manager.endpoints())
                .extracting(RpcGatewayEndpoint::instanceId)
                .containsExactly("gateway-1", "gateway-2");
        ManagedChannel first = manager.currentChannel();
        ManagedChannel second = manager.currentChannel();
        assertThat(first).isNotSameAs(second);

        manager.recordFailure(first);

        assertThat(manager.currentChannel()).isSameAs(second);
        verify(first).shutdown();
        manager.stop();
    }

    @Test
    void failsStartupAfterEmptyDiscoveryTimeout() {
        SnapshotDirectory directory = new SnapshotDirectory();
        directory.snapshot = snapshot();
        RpcConsumerGatewayManager manager = manager(
                directory,
                new StubChannelFactory()
        );

        assertThatThrownBy(manager::start)
                .isInstanceOfSatisfying(EgonRpcException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(
                                EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE
                        )
                );
    }

    @Test
    void startsWithMultipleGateways() {
        SnapshotDirectory directory = new SnapshotDirectory();
        directory.snapshot = snapshot(
                endpoint("gateway-1", "lease-1", 19090),
                endpoint("gateway-2", "lease-2", 19091)
        );
        RpcConsumerGatewayManager manager = manager(
                directory,
                new StubChannelFactory()
        );

        manager.start();

        assertThat(manager.state()).isEqualTo(RpcGatewayState.READY);
        assertThat(manager.endpoints()).hasSize(2);
        manager.stop();
    }

    @ParameterizedTest
    @MethodSource("nonCurrentLeaseExpireAts")
    void rejectsGatewayWithoutCurrentLease(Instant leaseExpireAt) {
        SnapshotDirectory directory = new SnapshotDirectory();
        directory.snapshot = snapshot(endpoint(
                "gateway-1",
                "lease-1",
                19090,
                leaseExpireAt
        ));
        RpcConsumerGatewayManager manager = manager(
                directory,
                new StubChannelFactory()
        );

        assertThatThrownBy(manager::start)
                .isInstanceOfSatisfying(EgonRpcException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(
                                EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE
                        )
                );
    }

    @Test
    void closesDrainingChannelsAndRestartsCleanly() {
        SnapshotDirectory directory = new SnapshotDirectory();
        StubChannelFactory channels = new StubChannelFactory();
        RpcConsumerGatewayManager manager = manager(directory, channels);
        directory.snapshot = snapshot(endpoint(
                "gateway-1",
                "lease-1",
                19090
        ));

        manager.start();
        ManagedChannel first = channels.lastChannel;
        directory.publish(snapshot(endpoint(
                "gateway-2",
                "lease-2",
                19091
        )));
        ManagedChannel second = channels.lastChannel;

        verify(first).shutdown();
        manager.stop();
        verify(first).shutdownNow();
        verify(second).shutdownNow();

        directory.snapshot = snapshot(endpoint(
                "gateway-3",
                "lease-3",
                19092
        ));
        manager.start();

        assertThat(manager.state()).isEqualTo(RpcGatewayState.READY);
        assertThat(channels.createCount).isEqualTo(3);
        manager.stop();
    }

    private RpcConsumerGatewayManager manager(
            SnapshotDirectory directory,
            StubChannelFactory channels) {
        EgonRpcProperties properties = new EgonRpcProperties();
        properties.getConsumer().setGatewayDiscoveryTimeoutMs(30);
        properties.getConsumer().setChannelDrainTimeoutMs(60000);
        properties.getConsumer().setGatewayBizCode("platform-biz");
        properties.getConsumer().setGatewayAppCode("gateway-app");
        return new RpcConsumerGatewayManager(
                directory,
                channels,
                properties,
                processIdentity()
        );
    }

    private RpcGatewaySnapshot snapshot(RpcGatewayEndpoint... endpoints) {
        return new RpcGatewaySnapshot(
                1,
                Instant.now(),
                List.of(endpoints)
        );
    }

    private RpcGatewayEndpoint endpoint(
            String instanceId,
            String leaseId,
            int port) {
        return endpoint(
                instanceId,
                leaseId,
                port,
                Instant.now().plusSeconds(30)
        );
    }

    private RpcGatewayEndpoint endpoint(
            String instanceId,
            String leaseId,
            int port,
            Instant leaseExpireAt) {
        return new RpcGatewayEndpoint(
                instanceId,
                leaseId,
                "127.0.0.1",
                port,
                false,
                leaseExpireAt
        );
    }

    private static Stream<Instant> nonCurrentLeaseExpireAts() {
        Instant now = Instant.now();
        return Stream.of(now.minusSeconds(1), now);
    }

    private RpcProcessIdentity processIdentity() {
        return new RpcProcessIdentity(
                "consumer-test",
                "test",
                "default",
                "127.0.0.1",
                1,
                "consumer-1"
        );
    }

    private static final class StubChannelFactory
            extends RpcConsumerChannelFactory {

        private int createCount;

        private ManagedChannel lastChannel;

        @Override
        public ManagedChannel create(RpcGatewayEndpoint endpoint) {
            createCount++;
            lastChannel = mock(ManagedChannel.class);
            when(lastChannel.getState(true))
                    .thenReturn(ConnectivityState.READY);
            return lastChannel;
        }

        @Override
        public boolean awaitReady(ManagedChannel channel, long timeoutMs) {
            return true;
        }
    }

    private static final class SnapshotDirectory
            implements RpcGatewayDirectory {

        private RpcGatewaySnapshot snapshot;

        private Consumer<RpcGatewaySnapshot> listener;

        private RpcGatewayQuery query;

        @Override
        public RpcGatewaySubscription subscribe(
                RpcGatewayQuery query,
                Consumer<RpcGatewaySnapshot> listener) {
            this.query = query;
            this.listener = listener;
            listener.accept(snapshot);
            return () -> this.listener = null;
        }

        void publish(RpcGatewaySnapshot next) {
            snapshot = next;
            listener.accept(next);
        }
    }
}
