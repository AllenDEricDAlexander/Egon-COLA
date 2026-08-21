package top.egon.cola.component.rpc.consumer.channel;

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RpcConsumerChannelPoolTest {

    @Test
    void concurrentAcquireCreatesOneChannelAndReturnsIndependentLeases()
            throws Exception {
        RpcConsumerChannelFactory factory = mock(RpcConsumerChannelFactory.class);
        ManagedChannel channel = mock(ManagedChannel.class);
        AtomicInteger creations = new AtomicInteger();
        when(factory.create(any(RpcChannelKey.class))).thenAnswer(ignored -> {
            creations.incrementAndGet();
            return channel;
        });
        RpcConsumerChannelPool pool = new RpcConsumerChannelPool(
                factory, Duration.ofSeconds(1));
        RpcEndpoint endpoint = endpoint();
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch ready = new CountDownLatch(16);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RpcChannelLease>> futures = new ArrayList<>();
        for (int index = 0; index < 16; index++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return pool.acquire(endpoint);
            }));
        }
        assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        List<RpcChannelLease> leases = new ArrayList<>();
        for (Future<RpcChannelLease> future : futures) {
            leases.add(future.get(2, TimeUnit.SECONDS));
        }

        assertThat(creations).hasValue(1);
        assertThat(leases).allSatisfy(lease -> assertThat(lease.channel()).isSameAs(channel));
        leases.forEach(RpcChannelLease::close);
        pool.close();
        executor.shutdownNow();
    }

    @Test
    void inFlightCallDrainsGracefullyBeforeForceClose() throws Exception {
        RpcConsumerChannelFactory factory = mock(RpcConsumerChannelFactory.class);
        ManagedChannel channel = mock(ManagedChannel.class);
        when(factory.create(any(RpcChannelKey.class))).thenReturn(channel);
        RpcConsumerChannelPool pool = new RpcConsumerChannelPool(
                factory, Duration.ofMillis(80));
        RpcChannelLease lease = pool.acquire(endpoint());
        lease.beginCall();

        pool.close();
        verify(channel).shutdown();
        verify(channel, never()).shutdownNow();
        lease.endCall();
        lease.close();
        verify(channel, never()).shutdownNow();
    }

    @Test
    void timeoutForceClosesAnOutstandingCallExactlyOnce() throws Exception {
        RpcConsumerChannelFactory factory = mock(RpcConsumerChannelFactory.class);
        ManagedChannel channel = mock(ManagedChannel.class);
        when(factory.create(any(RpcChannelKey.class))).thenReturn(channel);
        RpcConsumerChannelPool pool = new RpcConsumerChannelPool(
                factory, Duration.ofMillis(20));
        RpcChannelLease lease = pool.acquire(endpoint());
        lease.beginCall();

        pool.close();
        Thread.sleep(100);

        verify(channel).shutdownNow();
        verify(channel).shutdown();
        lease.close();
        verify(channel).shutdownNow();
    }

    @Test
    void poolRejectsAcquireAfterDrainAndFactoryFailureDoesNotPoisonKey() {
        RpcConsumerChannelFactory factory = mock(RpcConsumerChannelFactory.class);
        ManagedChannel channel = mock(ManagedChannel.class);
        when(factory.create(any(RpcChannelKey.class)))
                .thenThrow(new IllegalStateException("connect-failed"))
                .thenReturn(channel);
        RpcConsumerChannelPool pool = new RpcConsumerChannelPool(
                factory, Duration.ofSeconds(1));

        assertThatThrownBy(() -> pool.acquire(endpoint()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("connect-failed");
        assertThat(pool.acquire(endpoint()).channel()).isSameAs(channel);
        pool.close();
        assertThatThrownBy(() -> pool.acquire(endpoint()))
                .isInstanceOf(EgonRpcException.class)
                .satisfies(error -> assertThat(((EgonRpcException) error).getCode())
                        .isEqualTo(EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE));
    }

    @Test
    void leaseCloseIsIdempotentAndEndsOutstandingCalls() {
        RpcConsumerChannelFactory factory = mock(RpcConsumerChannelFactory.class);
        ManagedChannel channel = mock(ManagedChannel.class);
        when(factory.create(any(RpcChannelKey.class))).thenReturn(channel);
        RpcConsumerChannelPool pool = new RpcConsumerChannelPool(
                factory, Duration.ofSeconds(1));
        RpcChannelLease lease = pool.acquire(endpoint());
        lease.beginCall();
        lease.close();
        lease.close();
        lease.endCall();
        verify(channel).shutdown();
        pool.close();
    }

    private static RpcEndpoint endpoint() {
        return new RpcEndpoint() {
            @Override
            public String host() {
                return "127.0.0.1";
            }

            @Override
            public int port() {
                return 19090;
            }

            @Override
            public boolean secure() {
                return false;
            }
        };
    }
}
