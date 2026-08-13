package top.egon.cola.component.rpc.provider.registration;

import java.time.Instant;

/**
 * RPC Provider 注册中心实现返回的当前有效租约。
 *
 * <p>Active lease returned by an RPC Provider registry implementation.
 */
public record RpcProviderLease(
        String instanceId,
        String leaseId,
        Instant registeredAt,
        Instant leaseExpireAt
) {

    public RpcProviderLease {
        if (instanceId == null || instanceId.isBlank()
                || leaseId == null || leaseId.isBlank()) {
            throw new IllegalArgumentException(
                    "RPC Provider lease identity is required"
            );
        }
        if (registeredAt == null || leaseExpireAt == null
                || !leaseExpireAt.isAfter(registeredAt)) {
            throw new IllegalArgumentException(
                    "RPC Provider lease timeline is invalid"
            );
        }
        instanceId = instanceId.trim();
        leaseId = leaseId.trim();
    }
}
