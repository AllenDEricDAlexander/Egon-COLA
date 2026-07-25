package top.egon.cola.component.gateway.engine.rpc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RpcMethodIndexCompiler {

    public RpcMethodIndex compile(List<RuntimeRpcRoute> routes) {
        Map<String, RuntimeRpcRoute> index = new LinkedHashMap<>();
        for (RuntimeRpcRoute route : Objects.requireNonNull(routes, "routes")) {
            RuntimeRpcRoute previous = index.putIfAbsent(
                    route.fullMethodName(),
                    route
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                        "duplicate RPC route for " + route.fullMethodName()
                );
            }
        }
        return new RpcMethodIndex(index);
    }
}
