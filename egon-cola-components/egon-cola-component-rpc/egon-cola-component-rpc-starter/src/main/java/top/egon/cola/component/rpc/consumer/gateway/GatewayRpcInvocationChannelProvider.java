package top.egon.cola.component.rpc.consumer.gateway;

import io.grpc.ManagedChannel;
import top.egon.cola.component.rpc.consumer.channel.RpcInvocationChannelProvider;

import java.util.Set;

/**
 * 通过 RPC Gateway Manager 选择业务调用 Channel 的策略。
 *
 * <p>Invocation strategy backed by the RPC Gateway Manager for business RPC.
 */
public final class GatewayRpcInvocationChannelProvider
        implements RpcInvocationChannelProvider {

    private final RpcConsumerGatewayManager gatewayManager;

    /**
     * 创建基于指定 Gateway Manager 的调用策略。
     *
     * <p>Creates an invocation strategy backed by the supplied Gateway Manager.
     *
     * @param gatewayManager Gateway Channel 管理器 / Gateway channel manager
     */
    public GatewayRpcInvocationChannelProvider(
            RpcConsumerGatewayManager gatewayManager) {
        if (gatewayManager == null) {
            throw new IllegalArgumentException(
                    "RPC Consumer Gateway Manager is required"
            );
        }
        this.gatewayManager = gatewayManager;
    }

    @Override
    public ManagedChannel currentChannel(Set<ManagedChannel> excluded) {
        return gatewayManager.currentChannel(excluded);
    }

    @Override
    public void recordFailure(ManagedChannel channel) {
        gatewayManager.recordFailure(channel);
    }

    @Override
    public int maxAttempts() {
        return gatewayManager.maxAttempts();
    }
}
