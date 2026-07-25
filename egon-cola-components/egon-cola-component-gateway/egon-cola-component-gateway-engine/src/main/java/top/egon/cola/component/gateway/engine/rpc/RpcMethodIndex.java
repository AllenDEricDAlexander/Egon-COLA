package top.egon.cola.component.gateway.engine.rpc;

import java.util.Map;
import java.util.Optional;

public final class RpcMethodIndex {

    private final Map<String, RuntimeRpcRoute> routes;

    RpcMethodIndex(Map<String, RuntimeRpcRoute> routes) {
        this.routes = Map.copyOf(routes);
    }

    public Optional<RuntimeRpcRoute> find(String fullMethodName) {
        return Optional.ofNullable(routes.get(fullMethodName));
    }

    public Map<String, RuntimeRpcRoute> routes() {
        return routes;
    }
}
