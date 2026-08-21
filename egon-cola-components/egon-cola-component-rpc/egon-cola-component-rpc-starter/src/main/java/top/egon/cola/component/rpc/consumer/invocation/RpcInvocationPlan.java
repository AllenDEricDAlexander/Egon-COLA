package top.egon.cola.component.rpc.consumer.invocation;

import top.egon.cola.component.rpc.consumer.channel.RpcChannelLease;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelPool;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalancer;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;
import top.egon.cola.component.rpc.consumer.reference.RpcReferencePolicy;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategy;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/** Immutable executable data compiled from a typed or generic reference. */
public final class RpcInvocationPlan {

    @FunctionalInterface
    public interface UnaryInvoker {
        Attempt invoke(
                Object request,
                RpcChannelLease lease,
                Duration timeout,
                String invocationId) throws Exception;
    }

    public record Attempt(CompletionStage<Object> completion, Runnable cancel) {

        public Attempt {
            completion = Objects.requireNonNull(completion, "completion");
            cancel = cancel == null ? () -> { } : cancel;
        }
    }

    private final String serviceName;
    private final String fullMethodName;
    private final RpcReferenceMode referenceMode;
    private final RpcReferencePolicy policy;
    private final RpcReferenceStrategy strategy;
    private final RpcLoadBalancer loadBalancer;
    private final RpcConsumerChannelPool channelPool;
    private final Class<?> responseType;
    private final UnaryInvoker invoker;
    private final Function<Object, Object> fallback;

    public RpcInvocationPlan(
            String serviceName,
            String fullMethodName,
            RpcReferenceMode referenceMode,
            RpcReferencePolicy policy,
            RpcReferenceStrategy strategy,
            RpcLoadBalancer loadBalancer,
            RpcConsumerChannelPool channelPool,
            Class<?> responseType,
            UnaryInvoker invoker) {
        this(serviceName, fullMethodName, referenceMode, policy, strategy,
                loadBalancer, channelPool, responseType, invoker, null);
    }

    public RpcInvocationPlan(
            String serviceName,
            String fullMethodName,
            RpcReferenceMode referenceMode,
            RpcReferencePolicy policy,
            RpcReferenceStrategy strategy,
            RpcLoadBalancer loadBalancer,
            RpcConsumerChannelPool channelPool,
            Class<?> responseType,
            UnaryInvoker invoker,
            Function<Object, Object> fallback) {
        this.serviceName = required(serviceName, "serviceName");
        this.fullMethodName = required(fullMethodName, "fullMethodName");
        this.referenceMode = Objects.requireNonNull(referenceMode, "referenceMode");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
        if (strategy.mode() != referenceMode) {
            throw new IllegalArgumentException("reference strategy mode does not match plan");
        }
        this.loadBalancer = Objects.requireNonNull(loadBalancer, "loadBalancer");
        this.channelPool = Objects.requireNonNull(channelPool, "channelPool");
        this.responseType = Objects.requireNonNull(responseType, "responseType");
        this.invoker = Objects.requireNonNull(invoker, "invoker");
        this.fallback = fallback;
    }

    public String serviceName() {
        return serviceName;
    }

    public String fullMethodName() {
        return fullMethodName;
    }

    public RpcReferenceMode referenceMode() {
        return referenceMode;
    }

    public RpcReferencePolicy policy() {
        return policy;
    }

    public RpcReferenceStrategy strategy() {
        return strategy;
    }

    public RpcLoadBalancer loadBalancer() {
        return loadBalancer;
    }

    public RpcConsumerChannelPool channelPool() {
        return channelPool;
    }

    public Class<?> responseType() {
        return responseType;
    }

    public UnaryInvoker invoker() {
        return invoker;
    }

    public Function<Object, Object> fallback() {
        return fallback;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
