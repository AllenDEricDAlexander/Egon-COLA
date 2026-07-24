package top.egon.cola.component.rpc.consumer;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RpcConsumerChannelFactory {

    public ManagedChannel create(RpcGatewayEndpoint endpoint) {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(
                endpoint.host(),
                endpoint.port()
        ).disableRetry();
        if (endpoint.secure()) {
            builder.useTransportSecurity();
        } else {
            builder.usePlaintext();
        }
        return builder.build();
    }

    public boolean awaitReady(ManagedChannel channel, long timeoutMs) {
        long deadline = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(Math.max(timeoutMs, 1));
        ConnectivityState state = channel.getState(true);
        while (state != ConnectivityState.READY
                && state != ConnectivityState.SHUTDOWN
                && System.nanoTime() < deadline) {
            CountDownLatch changed = new CountDownLatch(1);
            channel.notifyWhenStateChanged(state, changed::countDown);
            try {
                long remaining = Math.max(1, deadline - System.nanoTime());
                changed.await(remaining, TimeUnit.NANOSECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
            state = channel.getState(false);
        }
        return state == ConnectivityState.READY;
    }
}
