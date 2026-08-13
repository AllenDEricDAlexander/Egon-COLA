package top.egon.cola.component.rpc.test.support;

import top.egon.cola.component.rpc.consumer.gateway.RpcGatewayQuery;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewaySnapshot;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewaySubscription;
import top.egon.cola.component.rpc.provider.registration.RpcLeaseOperationResult;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLease;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLeaseIdentity;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistration;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class InMemoryRpcRegistryClient implements TestRpcRegistry {

    private final InMemoryRpcRegistryBackend backend;

    private final CopyOnWriteArrayList<RpcGatewayQuery> subscribedQueries =
            new CopyOnWriteArrayList<>();

    public InMemoryRpcRegistryClient(InMemoryRpcRegistryBackend backend) {
        this.backend = backend;
    }

    @Override
    public RpcProviderLease register(RpcProviderRegistration registration) {
        return backend.register(registration);
    }

    @Override
    public RpcProviderLease registerGateway(
            RpcProviderRegistration registration) {
        return backend.registerGateway(registration);
    }

    @Override
    public RpcLeaseOperationResult heartbeat(
            RpcProviderLeaseIdentity lease) {
        return backend.heartbeat(lease);
    }

    @Override
    public RpcLeaseOperationResult deregister(
            RpcProviderLeaseIdentity lease) {
        return backend.deregister(lease);
    }

    @Override
    public RpcGatewaySubscription subscribe(
            RpcGatewayQuery query,
            Consumer<RpcGatewaySnapshot> listener) {
        subscribedQueries.add(query);
        RpcServiceIdentity identity = new RpcServiceIdentity(
                query.serviceName(),
                query.group(),
                query.version()
        );
        TestRpcSubscription subscription = backend.subscribe(
                identity,
                ignored -> listener.accept(backend.gateways(query))
        );
        return () -> {
            subscribedQueries.remove(query);
            subscription.close();
        };
    }

    public TestRpcServiceSnapshot getInstances(
            RpcServiceIdentity identity) {
        return backend.instances(identity);
    }

    public List<RpcServiceIdentity> getServiceIdentities(String env) {
        return backend.serviceIdentities(env);
    }

    public TestRpcSubscription subscribeService(
            RpcServiceIdentity identity,
            Consumer<TestRpcServiceSnapshot> listener) {
        return backend.subscribe(identity, listener);
    }

    public TestRpcSubscription subscribeServices(
            String env,
            Consumer<List<RpcServiceIdentity>> listener) {
        return backend.subscribeServices(env, listener);
    }

    public List<RpcGatewayQuery> subscribedQueries() {
        return List.copyOf(subscribedQueries);
    }
}
