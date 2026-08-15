package top.egon.cola.component.rpc.consumer.provider;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RpcConsumerProviderManagerTest {

    @Test
    void subscribesEachExactQueryOnce() {
        SnapshotDirectory directory = new SnapshotDirectory();
        RpcConsumerProviderManager manager = manager(
                directory, new StubChannelFactory());
        RpcProviderQuery orders = query("orders", "orders-api");
        RpcProviderQuery payments = query("payments", "payments-api");

        manager.register(orders);
        manager.register(orders);
        manager.register(payments);
        manager.start();

        assertThat(directory.subscribeCounts)
                .containsEntry(orders, 1)
                .containsEntry(payments, 1)
                .hasSize(2);

        manager.stop();
        assertThat(directory.closedQueries)
                .containsExactlyInAnyOrder(orders, payments);
    }

    @Test
    void roundRobinsOnlyActiveProviderLeases() {
        SnapshotDirectory directory = new SnapshotDirectory();
        StubChannelFactory channels = new StubChannelFactory();
        RpcConsumerProviderManager manager = manager(directory, channels);
        RpcProviderQuery query = query("orders", "orders-api");
        ProviderRpcInvocationChannelProvider provider = manager.register(query);
        manager.start();

        directory.publish(query, snapshot(2L,
                endpoint("provider-b", "lease-2", 19092),
                endpoint("provider-expired", "lease-expired", 19093,
                        Instant.now().minusSeconds(1)),
                endpoint("provider-a", "lease-1", 19091)));

        assertThat(provider.currentChannel(Set.of()))
                .isSameAs(channels.channelsByPort.get(19091));
        assertThat(provider.currentChannel(Set.of()))
                .isSameAs(channels.channelsByPort.get(19092));
        assertThat(provider.currentChannel(Set.of()))
                .isSameAs(channels.channelsByPort.get(19091));
        assertThat(provider.maxAttempts()).isOne();
        assertThat(channels.channelsByPort).doesNotContainKey(19093);

        directory.publish(query, snapshot(1L));
        assertThat(provider.currentChannel(Set.of())).isNotNull();
        manager.stop();
    }

    @Test
    void replacesLeaseAndDrainsOldChannel() {
        SnapshotDirectory directory = new SnapshotDirectory();
        StubChannelFactory channels = new StubChannelFactory();
        RpcConsumerProviderManager manager = manager(directory, channels);
        RpcProviderQuery query = query("orders", "orders-api");
        ProviderRpcInvocationChannelProvider provider = manager.register(query);
        manager.start();
        directory.publish(query, snapshot(1L,
                endpoint("provider-a", "lease-1", 19091)));
        ManagedChannel oldChannel = provider.currentChannel(Set.of());

        directory.publish(query, snapshot(2L,
                endpoint("provider-a", "lease-2", 19092)));
        ManagedChannel replacement = provider.currentChannel(Set.of());

        assertThat(replacement).isNotSameAs(oldChannel);
        verify(oldChannel).shutdown();

        provider.recordFailure(replacement);
        verify(replacement).shutdown();
        assertProviderUnavailable(provider);

        manager.stop();
        verify(oldChannel).shutdownNow();
        verify(replacement).shutdownNow();
    }

    @Test
    void neverFallsBackToGateway() {
        SnapshotDirectory directory = new SnapshotDirectory();
        RpcConsumerProviderManager manager = manager(
                directory, new StubChannelFactory());
        ProviderRpcInvocationChannelProvider provider = manager.register(
                query("orders", "orders-api"));
        manager.start();

        assertProviderUnavailable(provider);
        assertThat(RpcConsumerProviderManager.class.getDeclaredFields())
                .noneMatch(field -> field.getType().getName()
                        .contains("RpcGatewayDirectory"));

        manager.stop();
    }

    @Test
    void rejectsWildcardOrNonGrpcQueries() {
        assertInvalidQuery(() -> new RpcProviderQuery(
                "*", "orders", "test", "orders-api",
                "default", "1.0.0", "grpc"));
        assertInvalidQuery(() -> new RpcProviderQuery(
                "commerce", "orders", "test", "orders-api",
                "default", "1.0.0", "http"));
    }

    private void assertProviderUnavailable(
            ProviderRpcInvocationChannelProvider provider) {
        assertThatThrownBy(() -> provider.currentChannel(Set.of()))
                .isInstanceOfSatisfying(EgonRpcException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(
                                EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE));
    }

    private void assertInvalidQuery(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable constructor) {
        assertThatThrownBy(constructor)
                .isInstanceOfSatisfying(EgonRpcException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(
                                EgonRpcErrorCode.RPC_INVALID_CONTRACT));
    }

    private RpcConsumerProviderManager manager(
            SnapshotDirectory directory,
            StubChannelFactory channels) {
        EgonRpcProperties properties = new EgonRpcProperties();
        properties.getConsumer().setDefaultTimeoutMs(30);
        properties.getConsumer().setChannelDrainTimeoutMs(60000);
        return new RpcConsumerProviderManager(directory, channels, properties);
    }

    private RpcProviderQuery query(String appCode, String serviceName) {
        return new RpcProviderQuery(
                "commerce", appCode, "test", serviceName,
                "default", "1.0.0", "grpc");
    }

    private RpcProviderSnapshot snapshot(
            long revision,
            RpcProviderEndpoint... endpoints) {
        return new RpcProviderSnapshot(
                revision, Instant.now(), List.of(endpoints));
    }

    private RpcProviderEndpoint endpoint(
            String instanceId,
            String leaseId,
            int port) {
        return endpoint(instanceId, leaseId, port,
                Instant.now().plusSeconds(300));
    }

    private RpcProviderEndpoint endpoint(
            String instanceId,
            String leaseId,
            int port,
            Instant leaseExpireAt) {
        return new RpcProviderEndpoint(
                instanceId, leaseId, "127.0.0.1", port,
                false, leaseExpireAt);
    }

    private static final class StubChannelFactory
            extends RpcConsumerChannelFactory {

        private final Map<Integer, ManagedChannel> channelsByPort =
                new HashMap<>();

        @Override
        public ManagedChannel create(RpcEndpoint endpoint) {
            ManagedChannel channel = mock(ManagedChannel.class);
            when(channel.getState(true)).thenReturn(ConnectivityState.READY);
            channelsByPort.put(endpoint.port(), channel);
            return channel;
        }

        @Override
        public boolean awaitReady(ManagedChannel channel, long timeoutMs) {
            return true;
        }
    }

    private static final class SnapshotDirectory
            implements RpcProviderDirectory {

        private final Map<RpcProviderQuery, Integer> subscribeCounts =
                new HashMap<>();

        private final Map<RpcProviderQuery, Consumer<RpcProviderSnapshot>> listeners =
                new HashMap<>();

        private final Set<RpcProviderQuery> closedQueries =
                new java.util.HashSet<>();

        @Override
        public RpcProviderSubscription subscribe(
                RpcProviderQuery query,
                Consumer<RpcProviderSnapshot> listener) {
            subscribeCounts.merge(query, 1, Integer::sum);
            listeners.put(query, listener);
            return () -> {
                listeners.remove(query);
                closedQueries.add(query);
            };
        }

        void publish(RpcProviderQuery query, RpcProviderSnapshot snapshot) {
            listeners.get(query).accept(snapshot);
        }
    }
}
