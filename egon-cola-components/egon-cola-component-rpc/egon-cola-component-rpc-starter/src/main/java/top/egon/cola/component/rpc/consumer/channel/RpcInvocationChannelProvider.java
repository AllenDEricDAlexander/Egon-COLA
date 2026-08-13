package top.egon.cola.component.rpc.consumer.channel;

import io.grpc.ManagedChannel;

import java.util.Set;

/**
 * 为一次 RPC 调用选择并管理传输 Channel 的中立策略。
 *
 * <p>Registry-neutral strategy that selects and manages the transport channel
 * for an RPC invocation.
 */
public interface RpcInvocationChannelProvider {

    /**
     * 获取当前可用且尚未尝试过的 Channel。
     *
     * <p>Returns the current channel that has not already been attempted.
     *
     * @param excluded 本次调用已尝试的 Channel / channels already attempted
     * @return 当前 Channel / current channel
     */
    ManagedChannel currentChannel(Set<ManagedChannel> excluded);

    /**
     * 记录 Channel 调用失败。
     *
     * <p>Records a failed channel invocation.
     *
     * @param channel 失败的 Channel / failed channel
     */
    void recordFailure(ManagedChannel channel);

    /**
     * 返回单次逻辑调用允许的最大传输尝试次数。
     *
     * <p>Returns the maximum transport attempts for one logical invocation.
     *
     * @return 最大尝试次数 / maximum attempts
     */
    int maxAttempts();
}
