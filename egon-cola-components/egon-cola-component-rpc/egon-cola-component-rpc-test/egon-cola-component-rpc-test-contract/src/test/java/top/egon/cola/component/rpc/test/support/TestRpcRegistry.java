package top.egon.cola.component.rpc.test.support;

import top.egon.cola.component.rpc.consumer.gateway.RpcGatewayDirectory;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistry;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLease;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistration;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;

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
