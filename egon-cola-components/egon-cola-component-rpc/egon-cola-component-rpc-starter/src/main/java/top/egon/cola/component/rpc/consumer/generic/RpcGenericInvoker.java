package top.egon.cola.component.rpc.consumer.generic;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import top.egon.cola.component.rpc.consumer.channel.RpcChannelLease;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelPool;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInterceptorFactory;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInvocation;
import top.egon.cola.component.rpc.consumer.interceptor.RpcConsumerClientInterceptor;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationExecutor;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationPlan;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalancers;
import top.egon.cola.component.rpc.consumer.reference.RpcReferencePolicy;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Executes canonical raw-Protobuf unary calls through the typed core. */
public final class RpcGenericInvoker {

    private static final MethodDescriptor.Marshaller<byte[]> BYTES =
            new ByteArrayMarshaller();

    private final RpcGenericTargetCache targetCache;
    private final RpcConsumerChannelPool channelPool;
    private final RpcInvocationExecutor executor;
    private final RpcProcessIdentity processIdentity;
    private final List<RpcClientInterceptorFactory> interceptorFactories;
    public RpcGenericInvoker(
            RpcGenericTargetCache targetCache,
            RpcConsumerChannelPool channelPool,
            RpcInvocationExecutor executor,
            RpcProcessIdentity processIdentity,
            List<RpcClientInterceptorFactory> interceptorFactories,
            RpcLoadBalancers loadBalancers) {
        this.targetCache = Objects.requireNonNull(targetCache, "targetCache");
        this.channelPool = Objects.requireNonNull(channelPool, "channelPool");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.processIdentity = Objects.requireNonNull(
                processIdentity,
                "processIdentity"
        );
        this.interceptorFactories = interceptorFactories == null
                ? List.of() : List.copyOf(interceptorFactories);
        Objects.requireNonNull(loadBalancers, "loadBalancers");
    }

    public RpcGenericInvoker(
            RpcGenericTargetCache targetCache,
            RpcConsumerChannelPool channelPool,
            RpcInvocationExecutor executor,
            RpcProcessIdentity processIdentity) {
        this(
                targetCache,
                channelPool,
                executor,
                processIdentity,
                List.of(),
                new RpcLoadBalancers()
        );
    }

    public byte[] invokeBlocking(RpcGenericInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        RpcGenericTargetCache.Entry entry = targetCache.resolve(invocation);
        try {
            Object value = executor.executeBlocking(
                    plan(invocation, entry),
                    invocation.requestPayload()
            );
            return copyResponse(value);
        } finally {
            entry.release();
        }
    }

    public CompletionStage<byte[]> invokeAsync(
            RpcGenericInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        RpcGenericTargetCache.Entry entry = targetCache.resolve(invocation);
        CompletionStage<Object> source;
        try {
            source = executor.executeAsync(
                    plan(invocation, entry),
                    invocation.requestPayload()
            );
        } catch (RuntimeException exception) {
            entry.release();
            throw exception;
        }
        AtomicBoolean released = new AtomicBoolean();
        CompletableFuture<byte[]> result = new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean sourceCancelled = source.toCompletableFuture()
                        .cancel(mayInterruptIfRunning);
                boolean cancelled = super.cancel(mayInterruptIfRunning);
                releaseEntry(entry, released);
                return cancelled || sourceCancelled;
            }
        };
        source.whenComplete((value, error) -> {
            releaseEntry(entry, released);
            if (error != null) {
                result.completeExceptionally(error);
                return;
            }
            try {
                result.complete(copyResponse(value));
            } catch (RuntimeException exception) {
                result.completeExceptionally(exception);
            }
        });
        return result;
    }

    private RpcInvocationPlan plan(
            RpcGenericInvocation invocation,
            RpcGenericTargetCache.Entry entry) {
        RpcReferencePolicy policy = new RpcReferencePolicy(
                invocation.timeoutMs(),
                invocation.retries(),
                invocation.loadBalance(),
                invocation.failStrategy(),
                invocation.failStrategy()
                        == top.egon.cola.component.rpc.annotation.FailStrategy.LOCAL_FALLBACK
                        ? "generic"
                        : "",
                invocation.affinityKey() == null
                        ? null
                        : context -> invocation.affinityKey()
        );
        return new RpcInvocationPlan(
                invocation.serviceName(),
                invocation.fullMethodName(),
                invocation.mode(),
                policy,
                entry.strategy(),
                entry.loadBalancer(),
                channelPool,
                byte[].class,
                rawInvoker(invocation),
                invocation.fallback() == null
                        ? null
                        : request -> invocation.fallback().apply(
                                Arrays.copyOf(
                                        (byte[]) request,
                                        ((byte[]) request).length
                                )
                        )
        );
    }

    private RpcInvocationPlan.UnaryInvoker rawInvoker(
            RpcGenericInvocation invocation) {
        MethodDescriptor<byte[], byte[]> method = rawMethod(
                invocation.fullMethodName()
        );
        return (request, lease, timeout, invocationId) -> {
            byte[] payload = Arrays.copyOf(
                    (byte[]) request,
                    ((byte[]) request).length
            );
            RpcClientInvocation context = RpcClientInvocation.generic(
                    invocation,
                    processIdentity,
                    invocationId
            );
            List<ClientInterceptor> interceptors = new ArrayList<>();
            interceptors.add(RpcConsumerClientInterceptor.forTarget(
                    invocation.serviceName(),
                    invocation.group(),
                    invocation.version(),
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
            ListenableFuture<byte[]> future = ClientCalls.futureUnaryCall(
                    invocationChannel.newCall(method, callOptions),
                    payload
            );
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
            return new RpcInvocationPlan.Attempt(
                    completion,
                    () -> future.cancel(true)
            );
        };
    }

    private MethodDescriptor<byte[], byte[]> rawMethod(String fullMethodName) {
        return MethodDescriptor.<byte[], byte[]>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(fullMethodName)
                .setRequestMarshaller(BYTES)
                .setResponseMarshaller(BYTES)
                .build();
    }

    private byte[] copyResponse(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof byte[] bytes)) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_INTERNAL,
                    "RPC generic provider returned an invalid response type"
            );
        }
        return Arrays.copyOf(bytes, bytes.length);
    }

    private void releaseEntry(
            RpcGenericTargetCache.Entry entry,
            AtomicBoolean released) {
        if (released.compareAndSet(false, true)) {
            entry.release();
        }
    }

    private static final class ByteArrayMarshaller
            implements MethodDescriptor.Marshaller<byte[]> {

        @Override
        public InputStream stream(byte[] value) {
            return new ByteArrayInputStream(
                    value == null ? new byte[0] : value
            );
        }

        @Override
        public byte[] parse(InputStream stream) {
            try {
                return stream.readAllBytes();
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "failed to read raw RPC payload",
                        exception
                );
            }
        }
    }
}
