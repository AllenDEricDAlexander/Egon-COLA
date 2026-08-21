package top.egon.cola.component.rpc.consumer.reference;

import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;
import top.egon.cola.component.rpc.consumer.provider.RpcConsumerProviderManager;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderQuery;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderSnapshot;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Direct-only strategy bound to one exact Provider query. */
public final class DirectRpcReferenceStrategy implements RpcReferenceStrategy {

    private final RpcConsumerProviderManager manager;
    private final RpcProviderQuery query;
    private final RpcConsumerProviderManager.Demand demand;
    private final AtomicBoolean closed = new AtomicBoolean();

    public DirectRpcReferenceStrategy(
            RpcReferenceDefinition definition,
            RpcConsumerProviderManager manager) {
        if (definition.mode() != RpcReferenceMode.DIRECT || definition.directQuery() == null) {
            throw new IllegalArgumentException("Direct strategy requires DIRECT definition");
        }
        this.manager = manager;
        this.query = definition.directQuery();
        this.demand = manager.retain(query);
    }

    @Override
    public RpcReferenceMode mode() {
        return RpcReferenceMode.DIRECT;
    }

    @Override
    public String queryIdentity() {
        return query.toString();
    }

    @Override
    public long revision() {
        RpcProviderSnapshot snapshot = manager.snapshot(query);
        return snapshot == null ? -1 : snapshot.revision();
    }

    @Override
    public List<? extends RpcEndpoint> candidates() {
        if (closed.get()) {
            throw new IllegalStateException("RPC reference strategy is closed");
        }
        RpcProviderSnapshot snapshot = manager.snapshot(query);
        return snapshot == null ? List.of() : snapshot.endpoints();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true) && demand != null) {
            demand.close();
        }
    }
}
