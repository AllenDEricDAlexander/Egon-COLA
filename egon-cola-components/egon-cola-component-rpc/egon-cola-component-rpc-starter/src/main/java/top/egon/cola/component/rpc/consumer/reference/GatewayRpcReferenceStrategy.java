package top.egon.cola.component.rpc.consumer.reference;

import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;
import top.egon.cola.component.rpc.consumer.gateway.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewaySnapshot;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Gateway-only strategy; it never imports or queries the Provider manager. */
public final class GatewayRpcReferenceStrategy implements RpcReferenceStrategy {

    private final RpcConsumerGatewayManager manager;
    private final RpcConsumerGatewayManager.Demand demand;
    private final String queryIdentity;
    private final AtomicBoolean closed = new AtomicBoolean();

    public GatewayRpcReferenceStrategy(
            RpcReferenceDefinition definition,
            RpcConsumerGatewayManager manager) {
        if (definition.mode() != RpcReferenceMode.GATEWAY) {
            throw new IllegalArgumentException("Gateway strategy requires GATEWAY definition");
        }
        this.manager = manager;
        this.demand = manager.retainDemand();
        this.queryIdentity = definition.queryIdentity();
    }

    @Override
    public RpcReferenceMode mode() {
        return RpcReferenceMode.GATEWAY;
    }

    @Override
    public String queryIdentity() {
        return queryIdentity;
    }

    @Override
    public long revision() {
        RpcGatewaySnapshot snapshot = manager.snapshot();
        return snapshot == null ? -1 : snapshot.revision();
    }

    @Override
    public List<? extends RpcEndpoint> candidates() {
        if (closed.get()) {
            throw new IllegalStateException("RPC reference strategy is closed");
        }
        RpcGatewaySnapshot snapshot = manager.snapshot();
        return snapshot == null ? List.of() : snapshot.endpoints();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true) && demand != null) {
            demand.close();
        }
    }
}
