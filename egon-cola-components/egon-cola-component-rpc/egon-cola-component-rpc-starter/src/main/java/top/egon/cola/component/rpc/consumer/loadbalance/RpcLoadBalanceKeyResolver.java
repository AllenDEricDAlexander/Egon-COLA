package top.egon.cola.component.rpc.consumer.loadbalance;

/** Supplies a stable business-owned affinity key for consistent hashing. */
@FunctionalInterface
public interface RpcLoadBalanceKeyResolver {

    String resolve(RpcLoadBalanceContext context);
}
