package top.egon.cola.component.rpc.consumer.proxy;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationExecutor;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationMode;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationPlan;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.descriptor.RpcMethodDescriptor;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

/** Thin CGLIB callback with constant-time method-plan dispatch. */
public final class RpcConsumerMethodInterceptor
        implements MethodInterceptor {

    @FunctionalInterface
    public interface Dispatcher {

        Object invoke(Object proxy, Method method, Object[] args) throws Throwable;
    }

    private final RpcContractDescriptor contract;
    private final Map<Method, RpcInvocationPlan> plans;
    private final Map<Method, RpcInvocationMode> modes;
    private final RpcInvocationExecutor executor;
    private final Dispatcher compatibilityDispatcher;

    public RpcConsumerMethodInterceptor(
            RpcContractDescriptor contract,
            Map<Method, RpcInvocationPlan> plans,
            Map<Method, RpcInvocationMode> modes,
            RpcInvocationExecutor executor) {
        this.contract = Objects.requireNonNull(contract, "contract");
        this.plans = Map.copyOf(plans);
        this.modes = Map.copyOf(modes);
        this.executor = Objects.requireNonNull(executor, "executor");
        this.compatibilityDispatcher = null;
    }

    public RpcConsumerMethodInterceptor(
            RpcContractDescriptor contract,
            Map<Method, RpcInvocationMode> modes,
            Dispatcher compatibilityDispatcher) {
        this.contract = Objects.requireNonNull(contract, "contract");
        this.plans = Map.of();
        this.modes = Map.copyOf(modes);
        this.executor = null;
        this.compatibilityDispatcher = Objects.requireNonNull(
                compatibilityDispatcher,
                "compatibilityDispatcher"
        );
    }

    @Override
    public Object intercept(
            Object proxy,
            Method method,
            Object[] args,
            MethodProxy methodProxy) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return objectMethod(proxy, method, args);
        }
        RpcMethodDescriptor descriptor;
        try {
            descriptor = contract.method(method);
        } catch (IllegalArgumentException exception) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                    "RPC method is not part of the compiled contract",
                    exception
            );
        }
        Object request = request(args, descriptor);
        if (compatibilityDispatcher != null) {
            return compatibilityDispatcher.invoke(proxy, method, args);
        }
        RpcInvocationPlan plan = plans.get(method);
        if (plan == null) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                    "RPC method has no compiled invocation plan"
            );
        }
        RpcInvocationMode mode = modes.get(method);
        if (mode == RpcInvocationMode.ASYNC) {
            return executor.executeAsync(plan, request);
        }
        return executor.executeBlocking(plan, request);
    }

    private Object request(
            Object[] args,
            RpcMethodDescriptor descriptor) {
        if (args == null || args.length != 1
                || args[0] == null
                || !descriptor.requestType().isInstance(args[0])) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_INVALID_REQUEST,
                    "RPC request must be one Protobuf Message"
            );
        }
        return args[0];
    }

    private Object objectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "EgonRpcCglibProxy["
                    + contract.contractType().getName() + "]";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null ? null : args[0]);
            default -> throw new IllegalStateException(
                    "unsupported Object method: " + method
            );
        };
    }
}
