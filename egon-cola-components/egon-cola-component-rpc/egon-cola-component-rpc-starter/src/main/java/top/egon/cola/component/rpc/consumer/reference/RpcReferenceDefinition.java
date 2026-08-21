package top.egon.cola.component.rpc.consumer.reference;

import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderQuery;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

/** Immutable resolved target, mode and per-method policy index. */
public record RpcReferenceDefinition(
        RpcReferenceMode mode,
        RpcServiceIdentity serviceIdentity,
        RpcProviderQuery directQuery,
        Map<Method, RpcReferencePolicy> typedPolicies
) {

    public RpcReferenceDefinition {
        mode = Objects.requireNonNull(mode, "mode");
        serviceIdentity = Objects.requireNonNull(serviceIdentity, "serviceIdentity");
        if (mode == RpcReferenceMode.DIRECT && directQuery == null) {
            throw new IllegalArgumentException("DIRECT reference requires provider query");
        }
        if (mode == RpcReferenceMode.GATEWAY && directQuery != null) {
            throw new IllegalArgumentException("GATEWAY reference cannot carry provider query");
        }
        typedPolicies = typedPolicies == null ? Map.of() : Map.copyOf(typedPolicies);
    }

    public RpcReferencePolicy policyFor(Method method) {
        RpcReferencePolicy policy = typedPolicies.get(method);
        if (policy == null) {
            throw new IllegalArgumentException(
                    "RPC method is not part of the resolved reference: " + method);
        }
        return policy;
    }

    public String queryIdentity() {
        return mode == RpcReferenceMode.DIRECT
                ? directQuery.toString()
                : serviceIdentity.registrySuffix();
    }
}
