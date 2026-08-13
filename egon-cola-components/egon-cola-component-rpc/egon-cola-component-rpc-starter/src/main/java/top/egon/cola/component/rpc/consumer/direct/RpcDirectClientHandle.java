package top.egon.cola.component.rpc.consumer.direct;

import io.grpc.ManagedChannel;

/**
 * 同时持有类型化代理和其专属 Channel 所有权的直连客户端句柄。
 *
 * <p>Direct client handle that owns both the typed proxy and its dedicated
 * channel.
 *
 * @param <T> RPC 契约类型 / RPC contract type
 */
public final class RpcDirectClientHandle<T> implements AutoCloseable {

    private final T client;

    private final DirectRpcInvocationChannelProvider channelProvider;

    RpcDirectClientHandle(
            T client,
            DirectRpcInvocationChannelProvider channelProvider) {
        this.client = client;
        this.channelProvider = channelProvider;
    }

    /**
     * 返回类型化 RPC 代理。
     *
     * <p>Returns the typed RPC proxy.
     *
     * @return RPC 代理 / RPC proxy
     */
    public T client() {
        return client;
    }

    /**
     * 返回本句柄拥有的 Channel。
     *
     * <p>Returns the channel owned by this handle.
     *
     * @return Channel / channel
     */
    public ManagedChannel channel() {
        return channelProvider.channel();
    }

    @Override
    public void close() {
        channelProvider.close();
    }
}
