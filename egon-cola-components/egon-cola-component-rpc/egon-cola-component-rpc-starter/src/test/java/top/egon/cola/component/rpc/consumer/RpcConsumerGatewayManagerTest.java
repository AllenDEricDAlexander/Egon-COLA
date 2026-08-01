package top.egon.cola.component.rpc.consumer;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.registry.DdcServiceKeyFactory;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
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
    void shouldRoundRobinAcrossMultipleGatewaysAndRemoveFailures() {
        SnapshotRegistry registry = new SnapshotRegistry();
        StubChannelFactory channels = new StubChannelFactory();
        RpcConsumerGatewayManager manager = manager(registry, channels);
        registry.snapshot = snapshot(instance("gateway-1", "lease-1", 19090));

        manager.start();

        assertThat(manager.state()).isEqualTo(RpcGatewayState.READY);
        assertThat(manager.endpoint().instanceId()).isEqualTo("gateway-1");
        assertThat(channels.createCount).isOne();
        assertThat(registry.subscriptionKey.bizCode())
                .isEqualTo("platform-biz");
        assertThat(registry.subscriptionKey.appCode())
                .isEqualTo("gateway-app");
        assertThat(registry.subscriptionKey.env()).isEqualTo("test");

        registry.publish(snapshot(
                instance("gateway-1", "lease-1", 19090),
                instance("gateway-2", "lease-2", 19091)
        ));

        assertThat(manager.state()).isEqualTo(RpcGatewayState.READY);
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
    void shouldFailStartupAfterEmptyDiscoveryTimeout() {
        SnapshotRegistry registry = new SnapshotRegistry();
        registry.snapshot = snapshot();
        RpcConsumerGatewayManager manager =
                manager(registry, new StubChannelFactory());

        assertThatThrownBy(manager::start)
                .isInstanceOfSatisfying(EgonRpcException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(
                                        EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE
                                )
                );
    }

    @Test
    void shouldStartWithMultipleGateways() {
        SnapshotRegistry registry = new SnapshotRegistry();
        registry.snapshot = snapshot(
                instance("gateway-1", "lease-1", 19090),
                instance("gateway-2", "lease-2", 19091)
        );
        RpcConsumerGatewayManager manager =
                manager(registry, new StubChannelFactory());

        manager.start();

        assertThat(manager.state()).isEqualTo(RpcGatewayState.READY);
        assertThat(manager.endpoints()).hasSize(2);
        manager.stop();
    }

    @Test
    void shouldAcceptOnlineGatewayStatus() {
        SnapshotRegistry registry = new SnapshotRegistry();
        registry.snapshot = snapshot(instance(
                "gateway-1", "lease-1", 19090, "ONLINE",
                Instant.now().plusSeconds(30)
        ));
        RpcConsumerGatewayManager manager =
                manager(registry, new StubChannelFactory());

        manager.start();

        assertThat(manager.state()).isEqualTo(RpcGatewayState.READY);
        manager.stop();
    }

    @Test
    void shouldTreatMissingGatewayStatusAsUnavailable() {
        SnapshotRegistry registry = new SnapshotRegistry();
        registry.snapshot = snapshot(instance(
                "gateway-1", "lease-1", 19090, null,
                Instant.now().plusSeconds(30)
        ));
        RpcConsumerGatewayManager manager =
                manager(registry, new StubChannelFactory());

        assertThatThrownBy(manager::start)
                .isInstanceOfSatisfying(EgonRpcException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(
                                EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE
                        )
                );
    }

    @ParameterizedTest
    @MethodSource("nonCurrentLeaseExpireAts")
    void shouldRejectOnlineGatewayWithoutCurrentLease(Instant leaseExpireAt) {
        SnapshotRegistry registry = new SnapshotRegistry();
        registry.snapshot = snapshot(instance(
                "gateway-1", "lease-1", 19090, "ONLINE", leaseExpireAt
        ));
        RpcConsumerGatewayManager manager =
                manager(registry, new StubChannelFactory());

        assertThatThrownBy(manager::start)
                .isInstanceOfSatisfying(EgonRpcException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(
                                EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE
                        )
                );
    }

    @Test
    void shouldCloseDrainingChannelsAndRestartCleanly() {
        SnapshotRegistry registry = new SnapshotRegistry();
        StubChannelFactory channels = new StubChannelFactory();
        RpcConsumerGatewayManager manager = manager(registry, channels);
        registry.snapshot = snapshot(instance("gateway-1", "lease-1", 19090));

        manager.start();
        ManagedChannel first = channels.lastChannel;
        registry.publish(snapshot(instance("gateway-2", "lease-2", 19091)));
        ManagedChannel second = channels.lastChannel;

        verify(first).shutdown();
        manager.stop();
        verify(first).shutdownNow();
        verify(second).shutdownNow();

        registry.snapshot = snapshot(instance("gateway-3", "lease-3", 19092));
        manager.start();

        assertThat(manager.state()).isEqualTo(RpcGatewayState.READY);
        assertThat(channels.createCount).isEqualTo(3);
        manager.stop();
    }

    @Test
    void shouldTreatInvalidGatewayEndpointAsUnavailable() {
        SnapshotRegistry registry = new SnapshotRegistry();
        RpcConsumerGatewayManager manager =
                manager(registry, new StubChannelFactory());
        registry.snapshot = snapshot(new DdcServiceInstance(
                "gateway-1",
                "lease-1",
                gatewayKey(),
                "0.0.0.0",
                19090,
                false,
                java.util.Map.of(),
                30,
                10,
                Instant.now(),
                Instant.now(),
                Instant.now().plusSeconds(30),
                "UP",
                1
        ));

        assertThatThrownBy(manager::start)
                .isInstanceOfSatisfying(EgonRpcException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(
                                EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE
                        )
                );
    }

    private RpcConsumerGatewayManager manager(
            SnapshotRegistry registry,
            StubChannelFactory channels) {
        EgonRpcProperties properties = new EgonRpcProperties();
        properties.getConsumer().setGatewayDiscoveryTimeoutMs(30);
        properties.getConsumer().setChannelDrainTimeoutMs(60000);
        properties.getConsumer().setGatewayBizCode("platform-biz");
        properties.getConsumer().setGatewayAppCode("gateway-app");
        return new RpcConsumerGatewayManager(
                registry,
                channels,
                properties,
                processIdentity(),
                serviceKeyFactory()
        );
    }

    private DdcServiceSnapshot snapshot(DdcServiceInstance... instances) {
        return new DdcServiceSnapshot(
                gatewayKey(),
                1,
                List.of(instances),
                Instant.now()
        );
    }

    private DdcServiceInstance instance(
            String instanceId,
            String leaseId,
            int port) {
        Instant now = Instant.now();
        return instance(instanceId, leaseId, port, "UP", now.plusSeconds(30));
    }

    private DdcServiceInstance instance(
            String instanceId,
            String leaseId,
            int port,
            String status,
            Instant leaseExpireAt) {
        return new DdcServiceInstance(
                instanceId,
                leaseId,
                gatewayKey(),
                "127.0.0.1",
                port,
                false,
                java.util.Map.of(),
                30,
                10,
                leaseExpireAt.minusSeconds(30),
                leaseExpireAt.minusSeconds(10),
                leaseExpireAt,
                status,
                1
        );
    }

    private static Stream<Instant> nonCurrentLeaseExpireAts() {
        Instant now = Instant.now();
        return Stream.of(now.minusSeconds(1), now);
    }

    private DdcServiceKey gatewayKey() {
        return new DdcServiceKey(
                "platform-biz",
                "test",
                "gateway-app",
                DdcServiceKind.INTERNAL_GATEWAY,
                "egon-internal-rpc-gateway",
                "default",
                "1.0.0",
                "grpc"
        );
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

    private DdcServiceKeyFactory serviceKeyFactory() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail-biz");
        properties.setAppCode("orders-app");
        properties.setEnv("test");
        properties.setNamespace("default");
        return new DdcServiceKeyFactory(properties);
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

    private static final class SnapshotRegistry
            implements DdcServiceRegistryClient {

        private DdcServiceSnapshot snapshot;

        private Consumer<DdcServiceSnapshot> listener;

        private DdcServiceKey subscriptionKey;

        @Override
        public DdcRegistrySubscription subscribe(
                DdcServiceKey serviceKey,
                Consumer<DdcServiceSnapshot> listener) {
            this.subscriptionKey = serviceKey;
            this.listener = listener;
            listener.accept(snapshot);
            return () -> this.listener = null;
        }

        void publish(DdcServiceSnapshot next) {
            snapshot = next;
            listener.accept(next);
        }

        @Override
        public DdcLeaseSession register(DdcServiceRegistration registration) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcLeaseOperationResult heartbeat(
                String instanceId,
                String leaseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcLeaseOperationResult deregister(
                String instanceId,
                String leaseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcServiceCatalogSnapshot getServiceKeys(
                DdcServiceQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcRegistrySubscription subscribeServices(
                DdcServiceQuery query,
                Consumer<DdcServiceCatalogSnapshot> listener) {
            throw new UnsupportedOperationException();
        }
    }
}
