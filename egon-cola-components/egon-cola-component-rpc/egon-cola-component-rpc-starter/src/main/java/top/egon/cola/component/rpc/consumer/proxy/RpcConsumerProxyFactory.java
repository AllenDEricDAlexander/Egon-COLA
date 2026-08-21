package top.egon.cola.component.rpc.consumer.proxy;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.context.ApplicationContext;
import top.egon.cola.component.rpc.consumer.channel.RpcChannelLease;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelPool;
import top.egon.cola.component.rpc.consumer.channel.RpcInvocationChannelProvider;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInterceptorFactory;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInvocation;
import top.egon.cola.component.rpc.consumer.interceptor.RpcConsumerClientInterceptor;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationExecutor;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationMode;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationPlan;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalancers;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceDefinition;
import top.egon.cola.component.rpc.consumer.reference.RpcReferencePolicy;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategy;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.descriptor.RpcMethodDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** Builds one CGLIB proxy with immutable, precompiled method plans. */
public class RpcConsumerProxyFactory {

    private final RpcContractValidator contractValidator;
    private final RpcInvocationChannelProvider legacyChannelProvider;
    private final RpcProcessIdentity processIdentity;
    private final RpcStatusExceptionMapper statusMapper;
    private final long defaultTimeoutMs;
    private final List<RpcClientInterceptorFactory> interceptorFactories;
    private final RpcConsumerChannelPool channelPool;
    private final RpcInvocationExecutor executor;
    private final RpcLoadBalancers loadBalancers;
    private final ApplicationContext applicationContext;

    /**
     * Compatibility constructor for programmatic direct clients. The client
     * still receives a CGLIB proxy; its owned channel provider is dispatched
     * through the compatibility callback below.
     */
    public RpcConsumerProxyFactory(
            RpcContractValidator contractValidator,
            RpcInvocationChannelProvider channelProvider,
            RpcProcessIdentity processIdentity,
            RpcStatusExceptionMapper statusMapper,
            long defaultTimeoutMs) {
        this(
                contractValidator,
                channelProvider,
                processIdentity,
                statusMapper,
                defaultTimeoutMs,
                List.of()
        );
    }

    /** Compatibility constructor with ordered request interceptors. */
    public RpcConsumerProxyFactory(
            RpcContractValidator contractValidator,
            RpcInvocationChannelProvider channelProvider,
            RpcProcessIdentity processIdentity,
            RpcStatusExceptionMapper statusMapper,
            long defaultTimeoutMs,
            List<RpcClientInterceptorFactory> interceptorFactories) {
        this.contractValidator = contractValidator;
        this.legacyChannelProvider = channelProvider;
        this.processIdentity = processIdentity;
        this.statusMapper = statusMapper == null
                ? new RpcStatusExceptionMapper() : statusMapper;
        this.defaultTimeoutMs = requireTimeout(defaultTimeoutMs);
        this.interceptorFactories = interceptorFactories == null
                ? List.of() : List.copyOf(interceptorFactories);
        this.channelPool = null;
        this.executor = null;
        this.loadBalancers = null;
        this.applicationContext = null;
    }

    /** Runtime constructor used by the fixed-mode Consumer graph. */
    public RpcConsumerProxyFactory(
            RpcContractValidator contractValidator,
            RpcConsumerChannelPool channelPool,
            RpcInvocationExecutor executor,
            RpcProcessIdentity processIdentity,
            RpcLoadBalancers loadBalancers,
            RpcStatusExceptionMapper statusMapper,
            long defaultTimeoutMs,
            List<RpcClientInterceptorFactory> interceptorFactories,
            ApplicationContext applicationContext) {
        this.contractValidator = contractValidator;
        this.legacyChannelProvider = null;
        this.processIdentity = java.util.Objects.requireNonNull(
                processIdentity,
                "processIdentity"
        );
        this.statusMapper = statusMapper == null
                ? new RpcStatusExceptionMapper() : statusMapper;
        this.defaultTimeoutMs = requireTimeout(defaultTimeoutMs);
        this.interceptorFactories = interceptorFactories == null
                ? List.of() : List.copyOf(interceptorFactories);
        this.channelPool = java.util.Objects.requireNonNull(
                channelPool,
                "channelPool"
        );
        this.executor = java.util.Objects.requireNonNull(executor, "executor");
        this.loadBalancers = java.util.Objects.requireNonNull(
                loadBalancers,
                "loadBalancers"
        );
        this.applicationContext = applicationContext;
    }

    public RpcConsumerProxyFactory(
            RpcContractValidator contractValidator,
            RpcConsumerChannelPool channelPool,
            RpcInvocationExecutor executor,
            RpcProcessIdentity processIdentity,
            RpcLoadBalancers loadBalancers,
            RpcStatusExceptionMapper statusMapper,
            long defaultTimeoutMs,
            List<RpcClientInterceptorFactory> interceptorFactories) {
        this(
                contractValidator,
                channelPool,
                executor,
                processIdentity,
                loadBalancers,
                statusMapper,
                defaultTimeoutMs,
                interceptorFactories,
                null
        );
    }

    /** Creates a fixed-mode CGLIB proxy from a resolved definition/strategy. */
    public <T> T create(
            RpcContractDescriptor contract,
            RpcReferenceDefinition definition,
            RpcReferenceStrategy strategy) {
        if (executor == null || channelPool == null || loadBalancers == null) {
            throw new IllegalStateException(
                    "RPC runtime proxy factory is not configured"
            );
        }
        if (contract == null || definition == null || strategy == null) {
            throw new IllegalArgumentException(
                    "RPC proxy contract, definition and strategy are required"
            );
        }
        Map<Method, RpcInvocationPlan> plans = new java.util.HashMap<>();
        Map<Method, RpcInvocationMode> modes = new java.util.HashMap<>();
        try {
            for (RpcMethodDescriptor method : contract.methods()) {
                RpcReferencePolicy policy = definition.policyFor(
                        method.javaMethod()
                );
                plans.put(
                        method.javaMethod(),
                        plan(contract, definition, strategy, method, policy)
                );
                modes.put(method.javaMethod(), method.invocationMode());
            }
            RpcConsumerMethodInterceptor callback =
                    new RpcConsumerMethodInterceptor(
                            contract,
                            plans,
                            modes,
                            executor
                    );
            return cglib(contract, callback);
        } catch (RuntimeException exception) {
            throw exception;
        }
    }

    /**
     * Legacy programmatic entry point retained for source compatibility. It
     * now creates the same interface-only CGLIB proxy type.
     */
    public <T> T create(Class<T> contractType, long referenceTimeoutMs) {
        if (legacyChannelProvider == null) {
            throw new IllegalStateException(
                    "legacy RPC channel provider is not configured"
            );
        }
        RpcContractDescriptor contract = contractValidator.validate(contractType);
        long timeoutMs = referenceTimeoutMs > 0
                ? Math.min(referenceTimeoutMs, defaultTimeoutMs)
                : defaultTimeoutMs;
        Map<Method, RpcInvocationMode> modes = new java.util.HashMap<>();
        for (RpcMethodDescriptor method : contract.methods()) {
            modes.put(method.javaMethod(), method.invocationMode());
        }
        RpcConsumerMethodInterceptor callback =
                new RpcConsumerMethodInterceptor(
                        contract,
                        modes,
                        (proxy, method, args) -> legacyInvoke(
                                contract,
                                method,
                                args,
                                timeoutMs
                        )
                );
        return cglib(contract, callback);
    }

    private RpcInvocationPlan plan(
            RpcContractDescriptor contract,
            RpcReferenceDefinition definition,
            RpcReferenceStrategy strategy,
            RpcMethodDescriptor method,
            RpcReferencePolicy policy) {
        return new RpcInvocationPlan(
                contract.serviceName(),
                method.fullMethodName(),
                definition.mode(),
                policy,
                strategy,
                loadBalancers.loadBalancer(policy.loadBalance()),
                channelPool,
                method.responseType(),
                typedInvoker(contract, definition, method),
                fallback(contract, method, policy)
        );
    }

    private RpcInvocationPlan.UnaryInvoker typedInvoker(
            RpcContractDescriptor contract,
            RpcReferenceDefinition definition,
            RpcMethodDescriptor method) {
        return (request, lease, timeout, invocationId) -> {
            Message message = (Message) request;
            RpcClientInvocation context = RpcClientInvocation.typed(
                    contract,
                    method,
                    message,
                    processIdentity,
                    definition.serviceIdentity().group(),
                    definition.serviceIdentity().version(),
                    invocationId
            );
            List<ClientInterceptor> interceptors = new ArrayList<>();
            interceptors.add(RpcConsumerClientInterceptor.forTarget(
                    contract.serviceName(),
                    definition.serviceIdentity().group(),
                    definition.serviceIdentity().version(),
                    processIdentity,
                    invocationId
            ));
            interceptorFactories.stream()
                    .map(factory -> factory.create(context))
                    .forEach(interceptors::add);
            Channel invocationChannel = ClientInterceptors.interceptForward(
                    lease.channel(),
                    interceptors
            );
            long timeoutNanos = Math.max(1, timeout.toNanos());
            CallOptions callOptions = CallOptions.DEFAULT.withDeadlineAfter(
                    timeoutNanos,
                    TimeUnit.NANOSECONDS
            );
            ListenableFuture<Message> future = ClientCalls.futureUnaryCall(
                    invocationChannel.newCall(method.grpcMethod(), callOptions),
                    message
            );
            CompletableFuture<Object> completion = futureCompletion(future);
            return new RpcInvocationPlan.Attempt(
                    completion,
                    () -> future.cancel(true)
            );
        };
    }

    private java.util.function.Function<Object, Object> fallback(
            RpcContractDescriptor contract,
            RpcMethodDescriptor method,
            RpcReferencePolicy policy) {
        if (policy.failStrategy()
                != top.egon.cola.component.rpc.annotation.FailStrategy.LOCAL_FALLBACK) {
            return null;
        }
        if (applicationContext == null) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                    "RPC local fallback requires an ApplicationContext"
            );
        }
        Object fallbackBean = applicationContext.getBean(policy.fallbackBean());
        if (!contract.contractType().isInstance(fallbackBean)) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                    "RPC fallback bean does not implement the contract"
            );
        }
        return request -> {
            try {
                return method.javaMethod().invoke(fallbackBean, request);
            } catch (InvocationTargetException exception) {
                throw new RuntimeException(exception.getTargetException());
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        };
    }

    private Object legacyInvoke(
            RpcContractDescriptor contract,
            Method method,
            Object[] args,
            long timeoutMs) throws Throwable {
        RpcMethodDescriptor rpcMethod = contract.method(method);
        Message request = (Message) args[0];
        List<ClientInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new RpcConsumerClientInterceptor(
                contract,
                processIdentity
        ));
        RpcClientInvocation context = new RpcClientInvocation(
                contract,
                rpcMethod,
                request,
                processIdentity
        );
        interceptorFactories.stream()
                .map(factory -> factory.create(context))
                .forEach(interceptors::add);
        Set<ManagedChannel> attempted = java.util.Collections
                .newSetFromMap(new IdentityHashMap<>());
        long deadline = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < legacyChannelProvider.maxAttempts(); attempt++) {
            ManagedChannel managedChannel =
                    legacyChannelProvider.currentChannel(attempted);
            attempted.add(managedChannel);
            Channel invocationChannel = ClientInterceptors.interceptForward(
                    managedChannel,
                    interceptors
            );
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                break;
            }
            CallOptions callOptions = CallOptions.DEFAULT.withDeadlineAfter(
                    remainingNanos,
                    TimeUnit.NANOSECONDS
            );
            try {
                if (rpcMethod.invocationMode() == RpcInvocationMode.ASYNC) {
                    ListenableFuture<Message> future = ClientCalls.futureUnaryCall(
                            invocationChannel.newCall(
                                    rpcMethod.grpcMethod(),
                                    callOptions
                            ),
                            request
                    );
                    return futureCompletion(future);
                }
                return ClientCalls.blockingUnaryCall(
                        invocationChannel,
                        rpcMethod.grpcMethod(),
                        callOptions,
                        request
                );
            } catch (io.grpc.StatusRuntimeException exception) {
                lastFailure = statusMapper.map(exception);
                if (attempt + 1 >= legacyChannelProvider.maxAttempts()) {
                    throw lastFailure;
                }
                legacyChannelProvider.recordFailure(managedChannel);
            }
        }
        throw lastFailure == null
                ? new EgonRpcException(
                EgonRpcErrorCode.RPC_DEADLINE_EXCEEDED,
                "RPC deadline exceeded"
        ) : lastFailure;
    }

    private <T> CompletableFuture<Object> futureCompletion(
            ListenableFuture<T> future) {
        CompletableFuture<Object> completion = new CompletableFuture<>();
        future.addListener(() -> {
            try {
                completion.complete(future.get());
            } catch (CancellationException exception) {
                completion.cancel(false);
            } catch (ExecutionException exception) {
                completion.completeExceptionally(exception.getCause());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                completion.completeExceptionally(exception);
            }
        }, Runnable::run);
        return completion;
    }

    private <T> T cglib(
            RpcContractDescriptor contract,
            RpcConsumerMethodInterceptor callback) {
        Enhancer enhancer = new Enhancer();
        ClassLoader contractLoader = contract.contractType().getClassLoader();
        boolean accessibleContract = isPublicType(contract.contractType());
        enhancer.setClassLoader(accessibleContract
                ? new CglibClassLoader(contractLoader)
                : contractLoader);
        enhancer.setSuperclass(Object.class);
        enhancer.setInterfaces(new Class<?>[]{contract.contractType()});
        enhancer.setCallback(callback);
        enhancer.setUseFactory(!accessibleContract);
        Object proxy = accessibleContract
                ? enhancer.create()
                : createPackagePrivateCglib(enhancer, contract, callback);
        @SuppressWarnings("unchecked")
        T typed = (T) proxy;
        return typed;
    }

    private Object createPackagePrivateCglib(
            Enhancer enhancer,
            RpcContractDescriptor contract,
            RpcConsumerMethodInterceptor callback) {
        java.util.concurrent.atomic.AtomicReference<byte[]> generated =
                new java.util.concurrent.atomic.AtomicReference<>();
        synchronized (RpcConsumerProxyFactory.class) {
            ReflectUtils.setGeneratedClassHandler(
                    (ignored, bytes) -> generated.set(bytes)
            );
            try {
                try {
                    return enhancer.create();
                } catch (RuntimeException generationFailure) {
                    byte[] bytes = generated.get();
                    if (bytes == null) {
                        return compatibilityProxy(contract, callback);
                    }
                    try {
                        Class<?> generatedType = MethodHandles.privateLookupIn(
                                        contract.contractType(),
                                        MethodHandles.lookup()
                                )
                                .defineClass(bytes);
                        Object proxy = generatedType.getDeclaredConstructor()
                                .newInstance();
                        ((Factory) proxy).setCallbacks(
                                new org.springframework.cglib.proxy.Callback[]{callback}
                        );
                        return proxy;
                    } catch (ReflectiveOperationException | LinkageError ignored) {
                        return compatibilityProxy(contract, callback);
                    }
                }
            } finally {
                ReflectUtils.setGeneratedClassHandler(null);
            }
        }
    }

    /**
     * Package-private test fixtures cannot be defined into the application
     * class loader on Java's module path. Keep the compatibility surface
     * callable while public application contracts always use CGLIB above.
     */
    private Object compatibilityProxy(
            RpcContractDescriptor contract,
            RpcConsumerMethodInterceptor callback) {
        try {
            Class<?> proxyType = Proxy.getProxyClass(
                    contract.contractType().getClassLoader(),
                    contract.contractType()
            );
            java.lang.reflect.Constructor<?> constructor =
                    proxyType.getConstructor(java.lang.reflect.InvocationHandler.class);
            return constructor.newInstance(
                    (java.lang.reflect.InvocationHandler) (proxy, method, args) ->
                            callback.intercept(proxy, method, args, null)
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "failed to initialize RPC compatibility proxy",
                    exception
            );
        }
    }

    private boolean isPublicType(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            if (!Modifier.isPublic(current.getModifiers())) {
                return false;
            }
            current = current.getEnclosingClass();
        }
        return true;
    }

    /** ClassLoader hook used by Spring's repackaged CGLIB on Java modules. */
    public static final class CglibClassLoader extends ClassLoader {

        public CglibClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> publicDefineClass(
                String name,
                byte[] bytes,
                java.security.ProtectionDomain protectionDomain) {
            return defineClass(name, bytes, 0, bytes.length, protectionDomain);
        }
    }

    private static long requireTimeout(long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("RPC default timeout must be positive");
        }
        return timeoutMs;
    }
}
