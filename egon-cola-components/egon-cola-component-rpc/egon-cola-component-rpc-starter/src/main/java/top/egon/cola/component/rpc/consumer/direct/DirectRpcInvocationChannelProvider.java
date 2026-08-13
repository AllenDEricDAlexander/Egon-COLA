package top.egon.cola.component.rpc.consumer.direct;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import top.egon.cola.component.rpc.config.RpcTransportSecurity;
import top.egon.cola.component.rpc.consumer.channel.RpcInvocationChannelProvider;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 持有单个基础设施直连 Channel 的调用策略。
 *
 * <p>Invocation strategy that owns one direct infrastructure channel.
 */
public final class DirectRpcInvocationChannelProvider
        implements RpcInvocationChannelProvider, AutoCloseable {

    private final RpcDirectClientSettings settings;

    private final ManagedChannel channel;

    /**
     * 按配置创建并持有一个直连 Channel。
     *
     * <p>Creates and owns one direct channel from the supplied settings.
     *
     * @param settings 直连客户端配置 / direct client settings
     */
    public DirectRpcInvocationChannelProvider(
            RpcDirectClientSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException(
                    "RPC direct client settings are required"
            );
        }
        this.settings = settings;
        this.channel = channel(settings);
    }

    @Override
    public ManagedChannel currentChannel(Set<ManagedChannel> excluded) {
        if (channel.isShutdown() || excluded.contains(channel)) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE,
                    "the direct RPC channel is unavailable"
            );
        }
        return channel;
    }

    @Override
    public void recordFailure(ManagedChannel failed) {
        // Direct transport has one attempt. Ownership remains with the handle.
    }

    @Override
    public int maxAttempts() {
        return 1;
    }

    /**
     * 返回该策略持有的 Channel。
     *
     * <p>Returns the channel owned by this strategy.
     *
     * @return 持有的 Channel / owned channel
     */
    public ManagedChannel channel() {
        return channel;
    }

    RpcDirectClientSettings settings() {
        return settings;
    }

    @Override
    public void close() {
        channel.shutdown();
        try {
            if (!channel.awaitTermination(
                    settings.shutdownTimeoutMs(),
                    TimeUnit.MILLISECONDS
            )) {
                channel.shutdownNow();
                channel.awaitTermination(
                        settings.shutdownTimeoutMs(),
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            channel.shutdownNow();
        }
    }

    private ManagedChannel channel(RpcDirectClientSettings directSettings) {
        NettyChannelBuilder builder = NettyChannelBuilder
                .forTarget(directSettings.target())
                .defaultLoadBalancingPolicy(
                        directSettings.loadBalancingPolicy()
                )
                .maxInboundMessageSize(
                        directSettings.maxInboundMessageSize()
                )
                .withOption(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        directSettings.connectTimeoutMs()
                )
                .disableRetry();
        RpcTransportSecurity security =
                directSettings.transportSecurity();
        if (security.enabled()) {
            builder.sslContext(security.clientContext());
        } else {
            builder.usePlaintext();
        }
        return builder.build();
    }
}
