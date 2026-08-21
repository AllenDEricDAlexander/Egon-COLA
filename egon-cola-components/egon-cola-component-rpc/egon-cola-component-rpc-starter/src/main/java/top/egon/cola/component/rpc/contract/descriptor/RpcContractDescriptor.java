package top.egon.cola.component.rpc.contract.descriptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Immutable validated contract descriptor with O(1) method indexes. */
public final class RpcContractDescriptor {

    private final Class<?> contractType;
    private final String serviceName;
    private final String group;
    private final String version;
    private final List<RpcMethodDescriptor> methods;
    private final Map<Method, RpcMethodDescriptor> byJavaMethod;
    private final Map<String, RpcMethodDescriptor> byFullMethodName;

    public RpcContractDescriptor(
            Class<?> contractType,
            String serviceName,
            String group,
            String version,
            List<RpcMethodDescriptor> methods) {
        this.contractType = contractType;
        this.serviceName = serviceName;
        this.group = group;
        this.version = version;
        this.methods = List.copyOf(methods);
        Map<Method, RpcMethodDescriptor> javaIndex = new HashMap<>();
        Map<String, RpcMethodDescriptor> wireIndex = new HashMap<>();
        for (RpcMethodDescriptor method : this.methods) {
            if (javaIndex.putIfAbsent(method.javaMethod(), method) != null
                    || wireIndex.putIfAbsent(method.fullMethodName(), method) != null) {
                throw new IllegalArgumentException("duplicate RPC contract method binding");
            }
        }
        this.byJavaMethod = Map.copyOf(javaIndex);
        this.byFullMethodName = Map.copyOf(wireIndex);
    }

    public Class<?> contractType() {
        return contractType;
    }

    public String serviceName() {
        return serviceName;
    }

    public String group() {
        return group;
    }

    public String version() {
        return version;
    }

    public List<RpcMethodDescriptor> methods() {
        return methods;
    }

    public RpcMethodDescriptor method(Method javaMethod) {
        RpcMethodDescriptor method = byJavaMethod.get(javaMethod);
        if (method == null) {
            throw new IllegalArgumentException(
                    "method is not part of the RPC contract: " + javaMethod);
        }
        return method;
    }

    public RpcMethodDescriptor methodByFullMethodName(String fullMethodName) {
        RpcMethodDescriptor method = byFullMethodName.get(fullMethodName);
        if (method == null) {
            throw new IllegalArgumentException(
                    "full method is not part of the RPC contract: " + fullMethodName);
        }
        return method;
    }
}
