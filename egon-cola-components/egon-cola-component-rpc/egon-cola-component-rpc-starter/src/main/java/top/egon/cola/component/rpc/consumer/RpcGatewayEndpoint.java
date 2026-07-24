package top.egon.cola.component.rpc.consumer;

import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;

import java.time.Instant;

public record RpcGatewayEndpoint(
        String instanceId,
        String leaseId,
        String host,
        int port,
        boolean secure,
        Instant leaseExpireAt
) {

    public static RpcGatewayEndpoint from(DdcServiceInstance instance) {
        return new RpcGatewayEndpoint(
                instance.instanceId(),
                instance.leaseId(),
                instance.host(),
                instance.port(),
                instance.secure(),
                instance.leaseExpireAt()
        );
    }

    public boolean activeAt(Instant now) {
        return leaseExpireAt != null
                && leaseExpireAt.isAfter(now);
    }
}
