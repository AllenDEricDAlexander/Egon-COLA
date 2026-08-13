package top.egon.cola.component.rpc.provider.binding;

import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RpcProviderMethodRegistry {

    private final Map<MethodKey, RpcProviderMethodBinding> methods;

    public RpcProviderMethodRegistry(Collection<RpcProviderBinding> providers) {
        Map<String, RpcServiceIdentity> wireServices = new LinkedHashMap<>();
        Map<MethodKey, RpcProviderMethodBinding> collected = new LinkedHashMap<>();
        for (RpcProviderBinding provider : providers) {
            RpcServiceIdentity service = provider.serviceIdentity();
            RpcServiceIdentity existing = wireServices.putIfAbsent(
                    service.serviceName(),
                    service
            );
            if (existing != null && !existing.equals(service)) {
                throw new EgonRpcException(
                        EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                        "multiple RPC identities share one gRPC wire service: "
                                + service.serviceName()
                );
            }
            for (top.egon.cola.component.rpc.contract.descriptor.RpcMethodDescriptor method
                    : provider.contract().methods()) {
                MethodKey key = new MethodKey(
                        service,
                        method.methodName()
                );
                if (collected.putIfAbsent(
                        key,
                        new RpcProviderMethodBinding(provider, method)
                ) != null) {
                    throw new EgonRpcException(
                            EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                            "duplicate RPC provider method: " + key
                    );
                }
            }
        }
        this.methods = Map.copyOf(collected);
    }

    public List<RpcProviderBinding> providers() {
        return methods.values().stream()
                .map(RpcProviderMethodBinding::provider)
                .distinct()
                .toList();
    }

    public List<RpcProviderMethodBinding> methods(RpcServiceIdentity service) {
        return methods.entrySet().stream()
                .filter(entry -> entry.getKey().service().equals(service))
                .map(Map.Entry::getValue)
                .toList();
    }

    private record MethodKey(
            RpcServiceIdentity service,
            String methodName
    ) {
    }
}
