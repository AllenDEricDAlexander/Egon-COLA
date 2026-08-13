package top.egon.cola.component.rpc.ddc.registry;

import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.format.ServiceInstanceMetaCodec;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewayDirectory;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewayEndpoint;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewayQuery;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewaySnapshot;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewaySubscription;

import java.util.function.Consumer;

/** DDC 内部 Gateway 发现到 RPC Gateway Directory SPI 的桥接。 / Bridge from DDC gateway discovery to RPC directory SPI. */
public final class DdcRpcGatewayDirectory implements RpcGatewayDirectory {

    private final DdcServiceRegistryClient client;
    private final String defaultBizCode;
    private final String defaultAppCode;

    public DdcRpcGatewayDirectory(
            DdcServiceRegistryClient client,
            String defaultBizCode,
            String defaultAppCode) {
        this.client = client;
        this.defaultBizCode = defaultBizCode;
        this.defaultAppCode = defaultAppCode;
    }

    @Override
    public RpcGatewaySubscription subscribe(
            RpcGatewayQuery query,
            Consumer<RpcGatewaySnapshot> listener) {
        DdcServiceKey key = new DdcServiceKey(
                value(query.bizCode(), defaultBizCode), query.env(),
                value(query.appCode(), defaultAppCode),
                DdcServiceKind.INTERNAL_GATEWAY, query.serviceName(),
                query.group(), query.version(), "grpc"
        );
        var subscription = client.subscribe(key, snapshot -> listener.accept(
                new RpcGatewaySnapshot(
                        snapshot.revision(), snapshot.observedAt(),
                        snapshot.instances().stream().map(instance -> {
                            ServiceInstanceMetaCodec.decode(instance.metadata());
                            return new RpcGatewayEndpoint(
                                    instance.instanceId(), instance.leaseId(),
                                    instance.host(), instance.port(), instance.secure(),
                                    instance.leaseExpireAt()
                            );
                        }).toList()
                )
        ));
        return subscription::close;
    }

    private String value(String configured, String fallback) {
        return configured == null || configured.isBlank() ? fallback : configured;
    }
}
