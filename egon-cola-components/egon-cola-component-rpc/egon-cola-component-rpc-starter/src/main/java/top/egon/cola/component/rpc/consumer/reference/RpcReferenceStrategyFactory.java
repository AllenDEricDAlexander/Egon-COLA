package top.egon.cola.component.rpc.consumer.reference;

import top.egon.cola.component.rpc.consumer.gateway.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.provider.RpcConsumerProviderManager;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

/** Creates exactly one fixed transport strategy and tracks it for shutdown. */
public final class RpcReferenceStrategyFactory implements AutoCloseable {

    private final RpcConsumerGatewayManager gatewayManager;
    private final RpcConsumerProviderManager providerManager;
    private final Set<RpcReferenceStrategy> active =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public RpcReferenceStrategyFactory(
            RpcConsumerGatewayManager gatewayManager,
            RpcConsumerProviderManager providerManager) {
        this.gatewayManager = gatewayManager;
        this.providerManager = providerManager;
    }

    public synchronized RpcReferenceStrategy create(RpcReferenceDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        RpcReferenceStrategy strategy = switch (definition.mode()) {
            case GATEWAY -> {
                if (gatewayManager == null) {
                    throw new IllegalStateException("RPC Gateway manager is required");
                }
                yield new GatewayRpcReferenceStrategy(definition, gatewayManager);
            }
            case DIRECT -> {
                if (providerManager == null) {
                    throw new IllegalStateException("RPC Provider manager is required");
                }
                yield new DirectRpcReferenceStrategy(definition, providerManager);
            }
        };
        active.add(strategy);
        return strategy;
    }

    @Override
    public synchronized void close() {
        active.forEach(RpcReferenceStrategy::close);
        active.clear();
    }
}
