package top.egon.cola.component.rpc.ddc.registry;

import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.format.ServiceInstanceMetaCodec;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderDirectory;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderEndpoint;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderQuery;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderSnapshot;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderSubscription;

import java.util.function.Consumer;

/**
 * DDC RPC Provider 发现到中立 Provider Directory SPI 的桥接。
 * / Bridge from DDC RPC Provider discovery to the neutral Provider Directory SPI.
 */
public final class DdcRpcProviderDirectory implements RpcProviderDirectory {

    private final DdcServiceRegistryClient client;

    public DdcRpcProviderDirectory(DdcServiceRegistryClient client) {
        this.client = client;
    }

    @Override
    public RpcProviderSubscription subscribe(
            RpcProviderQuery query,
            Consumer<RpcProviderSnapshot> listener) {
        DdcServiceKey key = new DdcServiceKey(
                query.bizCode(),
                query.env(),
                query.appCode(),
                DdcServiceKind.RPC_PROVIDER,
                query.serviceName(),
                query.group(),
                query.version(),
                query.protocol()
        );
        var subscription = client.subscribe(key, snapshot -> listener.accept(
                new RpcProviderSnapshot(
                        snapshot.revision(),
                        snapshot.observedAt(),
                        snapshot.instances().stream().map(instance -> {
                            ServiceInstanceMetaCodec.decode(
                                    instance.metadata()
                            );
                            return new RpcProviderEndpoint(
                                    instance.instanceId(),
                                    instance.leaseId(),
                                    instance.host(),
                                    instance.port(),
                                    instance.secure(),
                                    instance.leaseExpireAt()
                            );
                        }).toList()
                )
        ));
        return subscription::close;
    }
}
