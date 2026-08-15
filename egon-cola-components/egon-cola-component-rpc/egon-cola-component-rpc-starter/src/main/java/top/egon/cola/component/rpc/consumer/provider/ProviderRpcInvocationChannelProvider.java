package top.egon.cola.component.rpc.consumer.provider;

import io.grpc.ManagedChannel;
import top.egon.cola.component.rpc.consumer.channel.RpcInvocationChannelProvider;

import java.util.Set;

/**
 * 将一个精确 Provider 查询绑定为 RPC 调用 Channel 策略。
 *
 * <p>Binds one exact Provider query as an RPC invocation channel strategy.
 */
public final class ProviderRpcInvocationChannelProvider
        implements RpcInvocationChannelProvider {

    private final RpcConsumerProviderManager providerManager;

    private final RpcProviderQuery query;

    public ProviderRpcInvocationChannelProvider(
            RpcConsumerProviderManager providerManager,
            RpcProviderQuery query) {
        if (providerManager == null || query == null) {
            throw new IllegalArgumentException(
                    "RPC Provider Manager and query are required"
            );
        }
        this.providerManager = providerManager;
        this.query = query;
    }

    @Override
    public ManagedChannel currentChannel(Set<ManagedChannel> excluded) {
        return providerManager.currentChannel(query, excluded);
    }

    @Override
    public void recordFailure(ManagedChannel channel) {
        providerManager.recordFailure(query, channel);
    }

    @Override
    public int maxAttempts() {
        return 1;
    }
}
