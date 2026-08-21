package top.egon.cola.component.rpc.consumer.lifecycle;

import org.junit.jupiter.api.Test;
import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelPool;
import top.egon.cola.component.rpc.consumer.gateway.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.provider.RpcConsumerProviderManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RpcConsumerLifecycleCoordinatorTest {

    @Test
    void startsInOrderAndStopsInReverseOwnershipOrderOnce() {
        RpcConsumerChannelPool pool = mock(RpcConsumerChannelPool.class);
        RpcConsumerGatewayManager gateway = mock(RpcConsumerGatewayManager.class);
        RpcConsumerProviderManager provider = mock(RpcConsumerProviderManager.class);
        List<String> events = new ArrayList<>();
        RpcConsumerLifecycleCoordinator coordinator = new RpcConsumerLifecycleCoordinator(
                pool, gateway, provider, List.of(() -> events.add("hook.close")));

        coordinator.start();
        assertThat(coordinator.state()).isEqualTo(RpcConsumerRuntimeState.READY);
        coordinator.requireAccepting();
        AtomicInteger callbacks = new AtomicInteger();
        coordinator.stop(callbacks::incrementAndGet);
        coordinator.stop(callbacks::incrementAndGet);

        assertThat(coordinator.state()).isEqualTo(RpcConsumerRuntimeState.STOPPED);
        assertThat(callbacks).hasValue(1);
        verify(pool).start();
        verify(gateway).start();
        verify(provider).start();
        verify(pool).close();
    }

    @Test
    void startupFailureClosesAlreadyStartedResourcesAndRejectsCalls() {
        RpcConsumerChannelPool pool = mock(RpcConsumerChannelPool.class);
        RpcConsumerGatewayManager gateway = mock(RpcConsumerGatewayManager.class);
        RpcConsumerProviderManager provider = mock(RpcConsumerProviderManager.class);
        RuntimeException failure = new RuntimeException("discovery");
        when(gateway.isRunning()).thenThrow(failure);
        RpcConsumerLifecycleCoordinator coordinator = new RpcConsumerLifecycleCoordinator(
                pool, gateway, provider, List.of());

        assertThatThrownBy(coordinator::start).isSameAs(failure);
        assertThat(coordinator.state()).isEqualTo(RpcConsumerRuntimeState.FAILED);
        assertThatThrownBy(coordinator::requireAccepting)
                .isInstanceOf(RuntimeException.class);
        verify(pool).close();
    }
}
