package top.egon.cola.component.rpc.provider.registration;

import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;

/**
 * 心跳或注销操作所指向的完整 RPC Provider 租约身份。
 *
 * <p>Exact RPC Provider lease targeted by a heartbeat or deregistration.
 */
public record RpcProviderLeaseIdentity(
        RpcServiceIdentity serviceIdentity,
        String instanceId,
        String leaseId
) {

    public RpcProviderLeaseIdentity {
        if (serviceIdentity == null
                || instanceId == null || instanceId.isBlank()
                || leaseId == null || leaseId.isBlank()) {
            throw new IllegalArgumentException(
                    "RPC Provider lease identity is required"
            );
        }
        instanceId = instanceId.trim();
        leaseId = leaseId.trim();
    }
}
