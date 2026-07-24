package top.egon.cola.component.rpc.provider;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RpcProviderAvailabilityRegistry {

    private final Set<RpcServiceIdentity> available =
            ConcurrentHashMap.newKeySet();

    public boolean isAvailable(RpcServiceIdentity service) {
        return available.contains(service);
    }

    public void available(RpcServiceIdentity service) {
        available.add(service);
    }

    public void unavailable(RpcServiceIdentity service) {
        available.remove(service);
    }

    public void clear() {
        available.clear();
    }
}
