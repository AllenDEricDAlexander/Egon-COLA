package top.egon.cola.component.gateway.engine.rpc;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

import java.util.Objects;

public record RpcProviderChannelKey(
        ProviderServiceKey serviceKey,
        String instanceId,
        String leaseId,
        String host,
        int port,
        boolean secure
) {

    public RpcProviderChannelKey {
        serviceKey = Objects.requireNonNull(serviceKey, "serviceKey");
        instanceId = Objects.requireNonNull(instanceId, "instanceId");
        leaseId = Objects.requireNonNull(leaseId, "leaseId");
        host = Objects.requireNonNull(host, "host");
    }

    public static RpcProviderChannelKey from(ProviderInstance provider) {
        return new RpcProviderChannelKey(
                provider.serviceKey(),
                provider.instanceId(),
                provider.leaseId(),
                provider.host(),
                provider.port(),
                provider.secure()
        );
    }
}
