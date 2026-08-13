package top.egon.cola.component.rpc.contract.descriptor;

import java.lang.reflect.Method;
import java.util.List;

public record RpcContractDescriptor(
        Class<?> contractType,
        String serviceName,
        String group,
        String version,
        List<RpcMethodDescriptor> methods
) {

    public RpcContractDescriptor {
        methods = List.copyOf(methods);
    }

    public RpcMethodDescriptor method(Method javaMethod) {
        return methods.stream()
                .filter(method -> method.javaMethod().equals(javaMethod))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "method is not part of the RPC contract: " + javaMethod
                ));
    }
}
