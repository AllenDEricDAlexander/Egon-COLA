package top.egon.cola.component.rpc.test.support;

import top.egon.cola.component.rpc.consumer.RpcGatewayDirectory;
import top.egon.cola.component.rpc.provider.RpcProviderRegistry;
import top.egon.cola.component.rpc.provider.RpcProviderLease;
import top.egon.cola.component.rpc.provider.RpcProviderRegistration;
import top.egon.cola.component.rpc.provider.RpcServiceIdentity;

import java.util.List;
import java.util.function.Consumer;

public interface TestRpcRegistry
        extends RpcProviderRegistry, RpcGatewayDirectory {

    RpcProviderLease registerGateway(RpcProviderRegistration registration);

    TestRpcServiceSnapshot getInstances(RpcServiceIdentity identity);

    List<RpcServiceIdentity> getServiceIdentities(String env);

    TestRpcSubscription subscribeService(
            RpcServiceIdentity identity,
            Consumer<TestRpcServiceSnapshot> listener
    );

    TestRpcSubscription subscribeServices(
            String env,
            Consumer<List<RpcServiceIdentity>> listener
    );
}
