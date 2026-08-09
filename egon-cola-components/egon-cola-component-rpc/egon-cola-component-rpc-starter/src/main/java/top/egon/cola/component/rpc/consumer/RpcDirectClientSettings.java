package top.egon.cola.component.rpc.consumer;

import top.egon.cola.component.rpc.config.RpcTransportSecurity;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;

/**
 * 程序化 RPC 直连客户端的完整构建参数。
 *
 * <p>Complete construction settings for a programmatic direct RPC client.
 *
 * @param target gRPC 目标 / gRPC target
 * @param processIdentity 调用方身份 / caller process identity
 * @param transportSecurity 传输安全配置 / transport security settings
 * @param deadlineMs 默认调用期限 / default call deadline
 * @param loadBalancingPolicy 负载均衡策略 / load-balancing policy
 * @param maxInboundMessageSize 最大入站消息字节数 / maximum inbound bytes
 * @param shutdownTimeoutMs 关闭等待时间 / shutdown wait time
 */
public record RpcDirectClientSettings(
        String target,
        RpcProcessIdentity processIdentity,
        RpcTransportSecurity transportSecurity,
        long deadlineMs,
        String loadBalancingPolicy,
        int maxInboundMessageSize,
        long shutdownTimeoutMs
) {

    public RpcDirectClientSettings {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("RPC direct target is required");
        }
        if (processIdentity == null) {
            throw new IllegalArgumentException(
                    "RPC direct process identity is required"
            );
        }
        if (transportSecurity == null) {
            throw new IllegalArgumentException(
                    "RPC direct transport security is required"
            );
        }
        if (deadlineMs <= 0) {
            throw new IllegalArgumentException(
                    "RPC direct deadline must be positive"
            );
        }
        if (loadBalancingPolicy == null || loadBalancingPolicy.isBlank()) {
            throw new IllegalArgumentException(
                    "RPC direct load-balancing policy is required"
            );
        }
        if (maxInboundMessageSize <= 0) {
            throw new IllegalArgumentException(
                    "RPC direct maximum inbound message size must be positive"
            );
        }
        if (shutdownTimeoutMs < 0) {
            throw new IllegalArgumentException(
                    "RPC direct shutdown timeout must not be negative"
            );
        }
        target = target.trim();
        loadBalancingPolicy = loadBalancingPolicy.trim();
    }

    /**
     * 使用推荐传输默认值创建配置。
     *
     * <p>Creates settings with the recommended transport defaults.
     *
     * @param target gRPC 目标 / gRPC target
     * @param processIdentity 调用方身份 / caller identity
     * @param transportSecurity 传输安全配置 / transport security
     * @param deadlineMs 默认调用期限 / default deadline
     * @return 直连客户端配置 / direct client settings
     */
    public static RpcDirectClientSettings defaults(
            String target,
            RpcProcessIdentity processIdentity,
            RpcTransportSecurity transportSecurity,
            long deadlineMs) {
        return new RpcDirectClientSettings(
                target,
                processIdentity,
                transportSecurity,
                deadlineMs,
                "round_robin",
                4 * 1024 * 1024,
                5000
        );
    }
}
