package top.egon.cola.component.rpc.test.mockgateway;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MockProviderChannelFactory implements AutoCloseable {

    private final Map<String, ManagedChannel> channels =
            new ConcurrentHashMap<>();

    ManagedChannel channel(MockProviderEndpoint endpoint) {
        return channels.computeIfAbsent(
                endpoint.channelKey(),
                ignored -> create(endpoint)
        );
    }

    void retain(Collection<MockProviderEndpoint> endpoints) {
        java.util.Set<String> retained = endpoints.stream()
                .map(MockProviderEndpoint::channelKey)
                .collect(java.util.stream.Collectors.toSet());
        channels.entrySet().removeIf(entry -> {
            if (retained.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().shutdownNow();
            return true;
        });
    }

    int size() {
        return channels.size();
    }

    @Override
    public void close() {
        channels.values().forEach(ManagedChannel::shutdownNow);
        channels.clear();
    }

    private ManagedChannel create(MockProviderEndpoint endpoint) {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(
                endpoint.host(),
                endpoint.port()
        ).disableRetry();
        return endpoint.secure()
                ? builder.useTransportSecurity().build()
                : builder.usePlaintext().build();
    }
}
