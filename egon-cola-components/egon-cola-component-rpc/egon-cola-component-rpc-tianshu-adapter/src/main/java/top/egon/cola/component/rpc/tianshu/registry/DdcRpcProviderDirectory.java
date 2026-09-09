package top.egon.cola.component.rpc.tianshu.registry;

import top.egon.cola.component.tianshu.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.tianshu.format.ServiceInstanceMetaCodec;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKind;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderDirectory;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderEndpoint;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderQuery;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderSnapshot;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderSubscription;

import java.util.function.Consumer;

/**
 * Tianshu RPC Provider 发现到中立 Provider Directory SPI 的桥接。
 * / Bridge from Tianshu RPC Provider discovery to the neutral Provider Directory SPI.
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
                            var meta = ServiceInstanceMetaCodec.decode(
                                    instance.metadata()
                            );
                            return new RpcProviderEndpoint(
                                    instance.instanceId(),
                                    instance.leaseId(),
                                    instance.host(),
                                    instance.port(),
                                    instance.secure(),
                                    instance.leaseExpireAt(),
                                    meta.weight()
                            );
                        }).toList()
                )
        ));
        return subscription::close;
    }
}
