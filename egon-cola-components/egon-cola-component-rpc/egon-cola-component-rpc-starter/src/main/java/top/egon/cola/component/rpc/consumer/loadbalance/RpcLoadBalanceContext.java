package top.egon.cola.component.rpc.consumer.loadbalance;

import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable, credential-free input presented to an endpoint selector. */
public record RpcLoadBalanceContext(
        String queryIdentity,
        String serviceName,
        String fullMethodName,
        Object request,
        List<? extends RpcEndpoint> candidates,
        Set<String> excluded,
        byte[] affinityDigest,
        long revision
) {

    public RpcLoadBalanceContext {
        queryIdentity = required(queryIdentity, "queryIdentity");
        serviceName = required(serviceName, "serviceName");
        fullMethodName = required(fullMethodName, "fullMethodName");
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        excluded = excluded == null ? Set.of() : Set.copyOf(excluded);
        affinityDigest = affinityDigest == null
                ? null : Arrays.copyOf(affinityDigest, affinityDigest.length);
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
    }

    @Override
    public byte[] affinityDigest() {
        return affinityDigest == null
                ? null : Arrays.copyOf(affinityDigest, affinityDigest.length);
    }

    private static String required(String value, String name) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
